package dev.everyonemek.overloadcore.gear;

import java.util.Map;
import dev.everyonemek.overloadcore.*;
import mekanism.api.gear.IModuleHelper;
import mekanism.common.registries.MekanismItems;
import mekanism.common.util.StorageUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@net.neoforged.fml.common.EventBusSubscriber(modid=OverloadCore.ID)
public final class MekaCombat {
    public static final int USE_DURATION=72000;
    private record Use(ItemStack stack,CombatModule.Form form,InteractionHand hand,long start,int charge){}
    private static final Map<Player,Use> USES=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    public static CombatModule.Form form(ItemStack stack){
        if(!stack.is(MekanismItems.MEKA_TOOL))return null;
        var module=IModuleHelper.INSTANCE.getIfEnabled(stack,EquipmentModules.COMBAT);
        return module==null?null:module.getCustomInstance().form();
    }
    public static long energy(ItemStack stack){var tank=StorageUtils.getEnergyContainer(stack,0);return tank==null?0:tank.getEnergy();}
    public static int chargeTicks(ItemStack stack){return chargeTicks(stack,form(stack));}
    private static int chargeTicks(ItemStack stack,CombatModule.Form form){return Math.max(6,(form==CombatModule.Form.RANGED?CoreConfig.RAIL_CHARGE.get():CoreConfig.BLADE_CHARGE.get())-5*GearEffects.level(stack,GearUpgrade.ACCELERATOR));}
    public static InteractionResultHolder<ItemStack> begin(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);var form=form(stack);
        if(form==null||player.isShiftKeyDown())return null; // Native teleport/use behavior remains accessible.
        if(player.isSpectator()||player.getCooldowns().isOnCooldown(stack.getItem()))return InteractionResultHolder.fail(stack);
        if(!level.isClientSide)LegacyWeaponItem.feedReserve(stack);
        long cost=GearCombat.joules(form==CombatModule.Form.RANGED?CoreConfig.RAIL_COST.get():CoreConfig.BLADE_BURST_COST.get());
        if(energy(stack)<cost){if(!level.isClientSide)player.displayClientMessage(CoreContent.text("weapon.no_energy"),true);return InteractionResultHolder.fail(stack);}
        if(form==CombatModule.Form.RANGED&&GearCombat.ammo(stack)==0&&player instanceof ServerPlayer server){
            GearCombat.reload(server,stack);if(GearCombat.ammo(stack)==0)return InteractionResultHolder.fail(stack);
        }
        if(level.isClientSide&&form==CombatModule.Form.RANGED&&GearCombat.ammo(stack)==0&&player.getInventory().items.stream().noneMatch(s->s.is(GearCombat.AMMO)))
            return InteractionResultHolder.fail(stack);
        if(!level.isClientSide)USES.put(player,new Use(stack,form,hand,level.getGameTime(),chargeTicks(stack,form)));
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    public static void release(ItemStack stack,Level level,LivingEntity entity,int remaining){
        if(!(entity instanceof ServerPlayer player))return;
        Use use=USES.remove(player);
        if(use==null||use.stack!=stack||player.getItemInHand(use.hand)!=stack||form(stack)!=use.form||!player.isAlive()||player.isSpectator()
            ||level.getGameTime()-use.start<use.charge||USE_DURATION-remaining<use.charge||player.getCooldowns().isOnCooldown(stack.getItem()))return;
        boolean fired=use.form==CombatModule.Form.RANGED?GearCombat.shoot(player,stack):GearCombat.burst(player,stack);
        if(fired)player.getCooldowns().addCooldown(stack.getItem(),use.form==CombatModule.Form.RANGED?10:16);
    }
    public static void cancel(Player player){USES.remove(player);if(player.isUsingItem()&&player.getUseItem().is(MekanismItems.MEKA_TOOL))player.stopUsingItem();}
    @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        Use use=USES.get(player);
        if(use!=null&&(!player.isUsingItem()||player.getUseItem()!=use.stack||form(use.stack)!=use.form))cancel(player);
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);if(stack.has(CoreContent.GEAR_ENERGY))LegacyWeaponItem.feedReserve(stack);
        }
    }
    @net.neoforged.bus.api.SubscribeEvent public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event){
        var stack=event.getItemStack();var form=form(stack);if(form==null)return;
        event.getToolTip().add(CoreContent.text("combat.selected",form.getTextComponent()));
        event.getToolTip().add(CoreContent.text(form==CombatModule.Form.RANGED?"combat.ranged_hint":"combat.melee_hint"));
        event.getToolTip().add(CoreContent.text("combat.controls"));
        if(form==CombatModule.Form.RANGED)event.getToolTip().add(CoreContent.text("weapon.magazine",GearCombat.ammo(stack),GearCombat.magazine(stack)));
    }
    @net.neoforged.bus.api.SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){cancel(event.getEntity());}
    private MekaCombat(){}
}
