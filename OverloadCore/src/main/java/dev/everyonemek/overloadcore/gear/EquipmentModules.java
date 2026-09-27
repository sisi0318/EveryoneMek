package dev.everyonemek.overloadcore.gear;

import dev.everyonemek.overloadcore.CoreContent;
import dev.everyonemek.overloadcore.OverloadCore;
import mekanism.api.MekanismIMC;
import mekanism.common.registration.impl.ModuleDeferredRegister;
import mekanism.common.registration.impl.ModuleRegistryObject;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

public final class EquipmentModules {
    private static final ModuleDeferredRegister MODULES = new ModuleDeferredRegister(OverloadCore.ID);
    private static final java.util.Map<GearUpgrade, ModuleRegistryObject<GearModule>> UPGRADES = new java.util.EnumMap<>(GearUpgrade.class);
    public static final ModuleRegistryObject<ResidualCouplingUnit> RESIDUAL_COUPLING = MODULES.registerInstanced(
          "residual_coupling_unit", ResidualCouplingUnit::new, () -> CoreContent.COUPLING_MODULE,
          builder -> builder.maxStackSize(4).rendersHUD());
    static {
        for (var upgrade : GearUpgrade.values()) UPGRADES.put(upgrade, MODULES.registerInstanced(upgrade.id,
              () -> new GearModule(upgrade), () -> CoreContent.UPGRADE_ITEMS.get(upgrade), b -> b.maxStackSize(upgrade.maximum)));
    }
    public static ModuleRegistryObject<GearModule> get(GearUpgrade upgrade) { return UPGRADES.get(upgrade); }

    public static void register(IEventBus bus) {
        MODULES.register(bus);
        bus.addListener(EquipmentModules::enqueue);
    }

    private static void enqueue(InterModEnqueueEvent event) {
        MekanismIMC.addMekaSuitBodyarmorModules(RESIDUAL_COUPLING);
        container(CoreContent.CORE, "overloadcore_core", 8);
        container(CoreContent.WARD, "overloadcore_ward", 4);
        container(CoreContent.RAILGUN, "overloadcore_rail", 32);
        container(CoreContent.BLADE, "overloadcore_blade", 64);
        net.neoforged.fml.InterModComms.sendTo("mekanism", "overloadcore_rail", () -> mekanism.common.registries.MekanismModules.ENERGY_UNIT);
        net.neoforged.fml.InterModComms.sendTo("mekanism", "overloadcore_blade", () -> mekanism.common.registries.MekanismModules.ENERGY_UNIT);
        for (var upgrade : GearUpgrade.values()) {
            var module = get(upgrade);
            if ((upgrade.targets & 1) != 0) MekanismIMC.addMekaSuitBodyarmorModules(module);
            if ((upgrade.targets & 2) != 0) MekanismIMC.addMekaSuitPantsModules(module);
            if ((upgrade.targets & 16) != 0) MekanismIMC.addMekaToolModules(module);
        }
    }
    public static void container(net.minecraft.core.Holder<net.minecraft.world.item.Item> item, String method, int mask) {
        MekanismIMC.addModuleContainer(item, method);
        for (var upgrade : GearUpgrade.values()) if ((upgrade.targets & mask) != 0)
            net.neoforged.fml.InterModComms.sendTo("mekanism", method, () -> get(upgrade));
    }

    private EquipmentModules() { }
}
