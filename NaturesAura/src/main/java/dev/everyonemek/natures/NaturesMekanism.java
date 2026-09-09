package dev.everyonemek.natures;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(NaturesMekanism.ID)
public final class NaturesMekanism {
    public static final String ID = "naturesmekanism";

    public NaturesMekanism(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, MachineConfig.SPEC);
        Content.register(bus);
        bus.addListener(SetMachineSettingPayload::register);
    }
}
