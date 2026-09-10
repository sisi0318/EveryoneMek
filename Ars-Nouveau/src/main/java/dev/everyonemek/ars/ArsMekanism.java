package dev.everyonemek.ars;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ArsMekanism.ID)
public final class ArsMekanism {
    public static final String ID = "arsmekanism";

    public ArsMekanism(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, MachineConfig.SPEC);
        Content.register(bus);
        bus.addListener(SetRecipeLockPayload::register);
    }
}
