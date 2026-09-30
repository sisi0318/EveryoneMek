package dev.everyonemek.overloadcore.gear;

import java.util.*;
import dev.everyonemek.overloadcore.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class GearCombat {
    public static long joules(long fe){return EnergyUnit.FORGE_ENERGY.convertFrom(fe);}
    public static boolean pay(ItemStack stack,long fe){var tank=StorageUtils.getEnergyContainer(stack,0);long cost=joules(fe);
        return tank!=null&&tank.extract(cost,Action.SIMULATE,AutomationType.MANUAL)==cost&&tank.extract(cost,Action.EXECUTE,AutomationType.MANUAL)==cost;}
    public static int recoveryTicks(ItemStack stack){return Math.max(2,10-2*GearEffects.level(stack,GearUpgrade.MAGAZINE));}
    public static boolean target(ServerPlayer p,LivingEntity entity){return entity!=p&&entity.isAlive()&&!entity.isSpectator()&&!entity.isAlliedTo(p)
          && (!(entity instanceof dev.everyonemek.overloadcore.training.TrainingTarget training)||training.canTrain(p))
          && (!(entity instanceof Player other)||p.canHarmPlayer(other)&&p.server.isPvpAllowed());}
    private static boolean visible(ServerPlayer p,Vec3 a,Vec3 b){var delta=b.subtract(a);return endLoaded(p,a,delta.normalize(),delta.length()).distanceToSqr(b)<.001&&p.level().clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    private static Vec3 endLoaded(ServerPlayer p,Vec3 start,Vec3 direction,double range){Vec3 end=start;
        for(int i=1;i<=Math.ceil(range);i++){var next=start.add(direction.scale(Math.min(i,range)));if(!p.level().hasChunkAt(BlockPos.containing(next)))break;end=next;}return end;}
    public static boolean shoot(ServerPlayer p,ItemStack stack){
        if(!pay(stack,CoreConfig.RAIL_COST.get())){p.displayClientMessage(CoreContent.text("weapon.no_energy"),true);return false;}
        int penetration=1+GearEffects.level(stack,GearUpgrade.PIERCING)+TacticalCombat.consumeFlux(p,stack);float multiplier=TacticalCombat.consumeCounter(p);
        Vec3 start=p.getEyePosition(),end=endLoaded(p,start,p.getLookAngle(),Math.min(192,CoreConfig.RAIL_RANGE.get()+16*GearEffects.level(stack,GearUpgrade.FOCUS)));
        var hit=p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()!=HitResult.Type.MISS)end=hit.getLocation();
        final Vec3 destination=end;
        record Impact(LivingEntity entity,Vec3 point){}
        var impacts=p.level().getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(.3),e->target(p,e)).stream()
              .map(e->e.getBoundingBox().inflate(.12).clip(start,destination).map(point->new Impact(e,point)).orElse(null)).filter(Objects::nonNull)
              .sorted(Comparator.comparingDouble(i->i.point.distanceToSqr(start))).limit(penetration).toList();
        var source=p.damageSources().source(ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"rail")),p);
        int index=0;for(var impact:impacts){dev.everyonemek.overloadcore.training.TrainingTarget.power(impact.entity,p,joules(CoreConfig.RAIL_COST.get()));TacticalCombat.damage(p,stack,impact.entity,source,(float)(CoreConfig.RAIL_DAMAGE.get()*Math.pow(.8,index++)*multiplier),0);}
        // A rail slug stops at its final allowed penetration, rather than drawing past that target.
        boolean stopped=impacts.size()==penetration;
        if(stopped)end=impacts.getLast().point;
        GearVisuals.send(p,start,end,0,stopped||hit.getType()!=HitResult.Type.MISS);p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.TRIDENT_THUNDER.value(),net.minecraft.sounds.SoundSource.PLAYERS,.35F,1.7F);return true;
    }
    public static boolean burst(ServerPlayer p,ItemStack stack){
        if(!pay(stack,CoreConfig.BLADE_BURST_COST.get())){p.displayClientMessage(CoreContent.text("weapon.no_energy"),true);return false;}
        float multiplier=TacticalCombat.consumeCounter(p);
        int tier=GearEffects.level(stack,GearUpgrade.BLADE_FIELD);double radius=3+tier*2;Vec3 eye=p.getEyePosition(),look=p.getLookAngle();
        var targets=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),e->target(p,e)&&e.getBoundingBox().getCenter().distanceToSqr(eye)<=radius*radius)
              .stream().sorted(Comparator.comparingDouble(e->e.distanceToSqr(p))).limit(16).toList();
        boolean chained=false;
        for(var entity:targets){var point=entity.getBoundingBox().getCenter();if(point.subtract(eye).normalize().dot(look)<.4||!visible(p,eye,point))continue;
            dev.everyonemek.overloadcore.training.TrainingTarget.power(entity,p,joules(CoreConfig.BLADE_BURST_COST.get()));
            if(TacticalCombat.damage(p,stack,entity,p.damageSources().playerAttack(p),(float)((CoreConfig.BLADE_DAMAGE.get()*1.5+tier*3)*multiplier),1)){if(!chained){resonate(p,stack,entity);chained=true;}}
        }
        Vec3 fieldEnd=endLoaded(p,eye,look,radius);
        var wall=p.level().clip(new ClipContext(eye,fieldEnd,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS)fieldEnd=wall.getLocation();
        GearVisuals.send(p,eye,fieldEnd,1);p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,net.minecraft.sounds.SoundSource.PLAYERS,.8F,.6F);return true;
    }
    public static void resonate(ServerPlayer p,ItemStack stack,LivingEntity primary){
        int level=GearEffects.level(stack,GearUpgrade.RESONANCE);if(level==0)return;
        Vec3 origin=primary.getBoundingBox().getCenter();
        var targets=p.level().getEntitiesOfClass(LivingEntity.class,primary.getBoundingBox().inflate(3+level),e->e!=primary&&target(p,e)&&(e instanceof Enemy||e instanceof dev.everyonemek.overloadcore.training.TrainingTarget||e.getLastHurtByMob()==p))
              .stream().sorted(Comparator.comparingDouble(e->e.distanceToSqr(primary))).limit(level).toList();
        for(var entity:targets){var point=entity.getBoundingBox().getCenter();if(!visible(p,origin,point))continue;
            if(!pay(stack,CoreConfig.ARC_COST.get()))break;
            dev.everyonemek.overloadcore.training.TrainingTarget.power(entity,p,joules(CoreConfig.ARC_COST.get()));
            TacticalCombat.damage(p,stack,entity,p.damageSources().playerAttack(p),(float)(CoreConfig.ARC_DAMAGE.get()+level),2);GearVisuals.send(p,origin,point,2);
        }
    }
    private GearCombat(){}
}
