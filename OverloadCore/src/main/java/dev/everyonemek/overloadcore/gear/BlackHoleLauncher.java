package dev.everyonemek.overloadcore.gear;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import dev.everyonemek.overloadcore.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Each charge belongs to one actual stack and hand; all timing and payment are server-owned. */
@EventBusSubscriber(modid=OverloadCore.ID)
public final class BlackHoleLauncher extends Item {
    public static final int USE_DURATION=72000;
    private record Charge(ItemStack stack, InteractionHand hand, long start, int ticks) {}
    private static final Map<Player,Charge> CHARGES=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static final Map<Player,WeakReference<BlackHoleEntity>> ACTIVE=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    public BlackHoleLauncher(){super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));}
    private static boolean active(Player player){var ref=ACTIVE.get(player);var hole=ref==null?null:ref.get();return hole!=null&&!hole.isRemoved()&&hole.level()==player.level();}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!player.isAlive()||player.isSpectator()||player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
        if(!level.isClientSide&&active(player)){player.displayClientMessage(CoreContent.text("black_hole.active"),true);return InteractionResultHolder.fail(stack);}
        if(GearEnergy.stored(stack)<GearCombat.joules(CoreConfig.BLACK_HOLE_COST.get())){
            if(!level.isClientSide)player.displayClientMessage(CoreContent.text("weapon.no_energy"),true);
            return InteractionResultHolder.fail(stack);
        }
        if(!level.isClientSide)CHARGES.put(player,new Charge(stack,hand,level.getGameTime(),CoreConfig.BLACK_HOLE_CHARGE.get()));
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return USE_DURATION;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
        if(!(entity instanceof ServerPlayer player))return;
        var charge=CHARGES.remove(player);
        if(charge==null||charge.stack!=stack||player.getItemInHand(charge.hand)!=stack||!player.isAlive()||player.isSpectator()
            ||level.getGameTime()-charge.start<charge.ticks||USE_DURATION-remaining<charge.ticks
            ||player.getCooldowns().isOnCooldown(this)||active(player))return;
        long cost=GearCombat.joules(CoreConfig.BLACK_HOLE_COST.get());
        if(GearEnergy.stored(stack)<cost){player.displayClientMessage(CoreContent.text("weapon.no_energy"),true);return;}
        var hole=new BlackHoleEntity(BlackHoleEntity.TYPE.get(),level);
        hole.setOwner(player);hole.setPos(player.getEyePosition());hole.setDeltaMovement(player.getLookAngle().scale(1.25));
        if(!level.addFreshEntity(hole))return;
        new GearEnergy(stack).extract(cost,mekanism.api.Action.EXECUTE,mekanism.api.AutomationType.MANUAL);
        hole.setPaidEnergy(cost);ACTIVE.put(player,new WeakReference<>(hole));
        player.getCooldowns().addCooldown(this,CoreConfig.BLACK_HOLE_COOLDOWN.get());
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,net.minecraft.sounds.SoundSource.PLAYERS,.65F,.55F);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        var p=event.getEntity();if(p.level().isClientSide)return;var charge=CHARGES.get(p);
        if(charge!=null&&(!p.isAlive()||!p.isUsingItem()||p.getUseItem()!=charge.stack||p.getItemInHand(charge.hand)!=charge.stack))CHARGES.remove(p);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){clear(event.getEntity());}
    @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){clear(event.getEntity());}
    private static void clear(Player p){CHARGES.remove(p);var ref=ACTIVE.remove(p);if(ref!=null&&ref.get()!=null)ref.get().discard();}
    @Override public boolean isBarVisible(ItemStack stack){return true;}
    @Override public int getBarWidth(ItemStack stack){return GearEnergy.bar(stack);}
    @Override public int getBarColor(ItemStack stack){return 0xE6BE72;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(CoreContent.text("black_hole.flavor").withStyle(net.minecraft.ChatFormatting.GOLD));
        lines.add(CoreContent.text("black_hole.use"));
        lines.add(CoreContent.text("black_hole.cost",CoreConfig.BLACK_HOLE_COST.get(),CoreConfig.BLACK_HOLE_CHARGE.get()/20D));
        GearEnergy.tooltip(stack,lines);
    }
}
