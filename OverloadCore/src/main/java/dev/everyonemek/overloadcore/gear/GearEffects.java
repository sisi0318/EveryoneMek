package dev.everyonemek.overloadcore.gear;

import dev.everyonemek.overloadcore.CoreConfig;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.gear.IModuleHelper;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class GearEffects {
    public static int level(ItemStack stack, GearUpgrade upgrade) {
        var container = IModuleHelper.INSTANCE.getModuleContainer(stack);
        var module = container == null ? null : container.getIfEnabled(EquipmentModules.get(upgrade));
        return module == null ? 0 : Math.min(upgrade.maximum, module.getInstalledCount());
    }

    /** Called by the workplace sampler once per ten ticks, including its original hysteresis. */
    public static int magnetic(ServerPlayer player, int load) {
        var stack = player.getItemBySlot(EquipmentSlot.LEGS);
        int level = level(stack, GearUpgrade.MAGNETIC);
        if (level == 0 || load < Math.max(0, CoreConfig.METAL_LIMIT.get() - 32)) return 0;
        int reduced = Math.min(load, CoreConfig.MAGNETIC_PER_LEVEL.get() * level);
        return pay(stack, (long)CoreConfig.MAGNETIC_COST_FE.get() * level) ? reduced : 0;
    }

    public static int cool(ServerPlayer player, int heat, int heatStep) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        int level = level(stack, GearUpgrade.HEAT_SINK);
        if (level == 0 || heat <= 0 && heatStep <= 0) return heatStep;
        return pay(stack, (long)CoreConfig.HEAT_SINK_COST_FE.get() * level)
              ? heatStep - CoreConfig.HEAT_SINK_PER_LEVEL.get() * level : heatStep;
    }

    private static boolean pay(ItemStack stack, long fe) {
        long cost = EnergyUnit.FORGE_ENERGY.convertFrom(fe);
        var tank = StorageUtils.getEnergyContainer(stack, 0);
        if (tank == null || tank.extract(cost, Action.SIMULATE, AutomationType.MANUAL) != cost) return false;
        return tank.extract(cost, Action.EXECUTE, AutomationType.MANUAL) == cost;
    }
    private GearEffects() { }
}
