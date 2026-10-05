package dev.everyonemek.overloadcore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OverloadCore.ID)
public final class OverloadCore {
    public static final String ID = "overloadcore";
    public OverloadCore(IEventBus bus, ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, CoreConfig.SPEC);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, dev.everyonemek.overloadcore.gear.GearVisualConfig.SPEC);
        CoreContent.register(bus); bus.addListener(CorePackets::register);
        dev.everyonemek.overloadcore.gear.EquipmentModules.register(bus);
        bus.addListener(dev.everyonemek.overloadcore.gear.GearEnergy::register);
        bus.addListener(dev.everyonemek.overloadcore.gear.GearVisuals::register);
        bus.addListener(dev.everyonemek.overloadcore.gear.TacticalPackets::register);
        dev.everyonemek.overloadcore.training.TrainingContent.register(bus);
        dev.everyonemek.overloadcore.gear.BlackHoleEntity.register(bus);
    }
}
