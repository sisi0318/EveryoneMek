package dev.everyonemek.overloadcore.gear;

import java.util.*;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.training.TrainingTarget;
import mekanism.common.content.gear.IRadialModuleContainerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid=OverloadCore.ID)
public final class TacticalCombat {
    private static final class State {long swap=-100,guardUntil,perfectUntil,rearm,drained=-1,fluxUntil,counterUntil,toggle=-100,notice=-100;}
    private record Mark(int stacks,long until){}
    private static final Map<ServerPlayer,State> STATES=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static final Map<LivingEntity,Map<UUID,Mark>> MARKS=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static final class Hit {final LivingEntity target;float actual;Hit(LivingEntity target){this.target=target;}}
    private static final ThreadLocal<Hit> HIT=new ThreadLocal<>();
    private static State state(ServerPlayer p){return STATES.computeIfAbsent(p,k->new State());}
    public static boolean switchForm(ServerPlayer p,boolean offhand){
        if(!p.isAlive()||p.isSpectator()||p.containerMenu!=p.inventoryMenu)return false;
        var s=state(p);long now=p.level().getGameTime();if(now-s.swap<4)return false;
        var stack=p.getItemInHand(offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);var form=MekaCombat.form(stack);
        if(form==null||!(stack.getItem() instanceof IRadialModuleContainerItem item))return false;
        item.setMode(stack,p,CombatModule.RADIAL.get(),form==CombatModule.Form.MELEE?CombatModule.Form.RANGED:CombatModule.Form.MELEE);
        if(MekaCombat.form(stack)==form)return false;s.swap=now;p.displayClientMessage(MekaCombat.form(stack).getTextComponent(),true);return true;
    }
    private static ItemStack chest(ServerPlayer p){return p.getItemBySlot(EquipmentSlot.CHEST);}
    private static boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&p.containerMenu==p.inventoryMenu&&GearEffects.level(chest(p),GearUpgrade.DEFLECTOR)>0;}
    public static boolean guarding(ServerPlayer p){return eligible(p)&&state(p).guardUntil>p.level().getGameTime();}
    public static boolean guardInput(ServerPlayer p,boolean down){
        State s=state(p);long now=p.level().getGameTime();
        if(!down){boolean active=s.guardUntil>0;s.guardUntil=s.perfectUntil=0;if(active)TacticalPackets.guard(p,0,0);return true;}
        if(!eligible(p)||MekaCombat.energy(chest(p))<GearCombat.joules(CoreConfig.GUARD_UPKEEP.get())){
            if(now-s.notice>=40){s.notice=now;p.displayClientMessage(CoreContent.text("tactical.guard_unavailable"),true);}return false;
        }
        boolean start=s.guardUntil<=now;
        if(start){if(now-s.toggle<2)return false;s.toggle=now;s.perfectUntil=now>=s.rearm?now+CoreConfig.GUARD_WINDOW.get():0;s.rearm=Math.max(s.rearm,now+CoreConfig.GUARD_REARM.get());}
        s.guardUntil=now+20;
        if(start)TacticalPackets.guard(p,20,(int)Math.max(0,s.perfectUntil-now));return true;
    }
    private static boolean front(ServerPlayer p,Vec3 from){var delta=from.subtract(p.getEyePosition());return delta.lengthSqr()>.001&&delta.normalize().dot(p.getLookAngle())>=.55;}
    private static boolean hostileSource(ServerPlayer p,Entity source){return source!=null&&source!=p&&!source.isAlliedTo(p)&&(!(source instanceof Player other)||p.server.isPvpAllowed()&&p.canHarmPlayer(other));}
    public static boolean block(ServerPlayer p,Entity source,Vec3 from,float amount){
        if(!guarding(p)||!hostileSource(p,source)||!front(p,from)||!Float.isFinite(amount)||amount<=0)return false;
        double price=Math.ceil(amount*CoreConfig.GUARD_DAMAGE_FE.get());if(price>Integer.MAX_VALUE||!GearCombat.pay(chest(p),(long)price))return false;
        var s=state(p);long now=p.level().getGameTime();
        if(now<s.perfectUntil){s.perfectUntil=0;s.counterUntil=now+80;TacticalPackets.own(p,p,2,80,1);p.displayClientMessage(CoreContent.text("tactical.counter_ready"),true);}
        TacticalPackets.guard(p,20,0);return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void incoming(LivingIncomingDamageEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)||e.getSource().is(DamageTypeTags.BYPASSES_ARMOR))return;
        if(p.invulnerableTime>10&&!e.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN))return;
        Vec3 from=e.getSource().getSourcePosition();if(from!=null&&block(p,e.getSource().getEntity(),from,e.getAmount()))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void projectile(ProjectileImpactEvent e){
        if(!(e.getProjectile() instanceof Arrow||e.getProjectile() instanceof SpectralArrow)||!(e.getRayTraceResult() instanceof EntityHitResult hit)||!(hit.getEntity() instanceof ServerPlayer p))return;
        var arrow=(AbstractArrow)e.getProjectile();var velocity=arrow.getDeltaMovement();
        float amount=(float)Math.max(1,Math.ceil(velocity.length()*arrow.getBaseDamage()));
        if(block(p,arrow.getOwner(),arrow.position(),amount)){
            var normal=p.getLookAngle();var reflected=velocity.subtract(normal.scale(2*velocity.dot(normal)));
            if(reflected.dot(normal)<=0)reflected=normal.scale(Math.max(.5,velocity.length()));
            arrow.setOwner(p);arrow.setPos(hit.getLocation().add(reflected.normalize().scale(.05)));arrow.setDeltaMovement(reflected);arrow.hasImpulse=true;arrow.hurtMarked=true;e.setCanceled(true);
        }
    }
    public static int marks(ServerPlayer p,LivingEntity target){var map=MARKS.get(target);var mark=map==null?null:map.get(p.getUUID());return mark!=null&&mark.until>p.level().getGameTime()?mark.stacks:0;}
    private static void mark(ServerPlayer p,LivingEntity target){
        var map=MARKS.computeIfAbsent(target,k->new HashMap<>());long now=p.level().getGameTime();map.values().removeIf(m->m.until<=now);
        if(map.size()>=8&&!map.containsKey(p.getUUID()))return;
        int n=Math.min(3,marks(p,target)+1);map.put(p.getUUID(),new Mark(n,now+CoreConfig.POLAR_TICKS.get()));TacticalPackets.own(p,target,1,CoreConfig.POLAR_TICKS.get(),n);
    }
    private static void flux(ServerPlayer p,LivingEntity target){if(target instanceof TrainingTarget)return;state(p).fluxUntil=p.level().getGameTime()+80;TacticalPackets.own(p,p,3,80,1);}
    public static int consumeFlux(ServerPlayer p,ItemStack stack){
        var s=state(p);boolean ready=s.fluxUntil>p.level().getGameTime()&&GearEffects.level(stack,GearUpgrade.POLARIZATION)>0;s.fluxUntil=0;
        if(ready)TacticalPackets.own(p,p,3,0,0);return ready?1:0;
    }
    public static float consumeCounter(ServerPlayer p){var s=state(p);boolean ready=s.counterUntil>p.level().getGameTime()&&GearEffects.level(chest(p),GearUpgrade.DEFLECTOR)>0;s.counterUntil=0;
        if(ready)TacticalPackets.own(p,p,2,0,0);return ready?1.35F:1;
    }
    public static boolean damage(ServerPlayer p,ItemStack stack,LivingEntity target,DamageSource source,float amount,int kind){
        int level=GearEffects.level(stack,GearUpgrade.POLARIZATION),stacks=kind==1&&level>0?marks(p,target):0;
        Hit previous=HIT.get(),hit=new Hit(target);HIT.set(hit);
        try{target.hurt(source,amount+stacks*(2+2*level));}finally{if(previous==null)HIT.remove();else HIT.set(previous);}
        if(hit.actual<=0)return false;
        if(level>0){if(kind==0&&target.isAlive())mark(p,target);else if(kind==1){if(stacks>0){MARKS.get(target).remove(p.getUUID());TacticalPackets.own(p,target,1,0,0);GearVisuals.send(p,target.position(),target.getEyePosition(),2);}flux(p,target);}}
        return true;
    }
    @SubscribeEvent public static void damagePost(LivingDamageEvent.Post e){
        if(!Float.isFinite(e.getNewDamage())||e.getNewDamage()<=0)return;
        if(e.getEntity() instanceof TrainingTarget target)target.recordHit(e.getSource(),e.getNewDamage());
        var hit=HIT.get();if(hit!=null){if(hit.target==e.getEntity())hit.actual+=e.getNewDamage();return;}
        if(e.getSource().is(DamageTypes.PLAYER_ATTACK)&&e.getSource().getEntity() instanceof ServerPlayer p){var stack=p.getMainHandItem();
            if(MekaCombat.form(stack)==CombatModule.Form.MELEE&&GearEffects.level(stack,GearUpgrade.POLARIZATION)>0&&GearCombat.target(p,e.getEntity()))flux(p,e.getEntity());}
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(!(e.getEntity() instanceof ServerPlayer p))return;var s=STATES.get(p);if(s==null)return;long now=p.level().getGameTime();
        if(s.guardUntil>0){if(!guarding(p)||s.drained!=now&&!GearCombat.pay(chest(p),CoreConfig.GUARD_UPKEEP.get())){s.guardUntil=s.perfectUntil=0;TacticalPackets.guard(p,0,0);}
            else{s.drained=now;if(now%10==0)TacticalPackets.guard(p,20,(int)Math.max(0,s.perfectUntil-now));}}
    }
    private static void clear(Player player){if(player instanceof ServerPlayer p){for(var map:MARKS.values())map.remove(p.getUUID());if(STATES.remove(p)==null)return;TacticalPackets.guard(p,0,0);TacticalPackets.own(p,p,2,0,0);TacticalPackets.own(p,p,3,0,0);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){clear(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){clear(e.getEntity());}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof Player p)clear(p);}
    private TacticalCombat(){}
}
