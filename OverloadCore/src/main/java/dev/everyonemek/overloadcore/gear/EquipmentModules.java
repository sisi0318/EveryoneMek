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
    public static final ModuleRegistryObject<ResidualCouplingUnit> RESIDUAL_COUPLING = MODULES.registerInstanced(
          "residual_coupling_unit", ResidualCouplingUnit::new, () -> CoreContent.COUPLING_MODULE,
          builder -> builder.maxStackSize(4).rendersHUD());

    public static void register(IEventBus bus) {
        MODULES.register(bus);
        bus.addListener(EquipmentModules::enqueue);
    }

    private static void enqueue(InterModEnqueueEvent event) {
        MekanismIMC.addMekaSuitBodyarmorModules(RESIDUAL_COUPLING);
    }

    private EquipmentModules() { }
}
