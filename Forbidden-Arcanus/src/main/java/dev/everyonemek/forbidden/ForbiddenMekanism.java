package dev.everyonemek.forbidden;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ForbiddenMekanism.ID)
public final class ForbiddenMekanism {
    public static final String ID = "forbiddenmekanism";

    public ForbiddenMekanism(IEventBus bus, ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, MachineConfig.SPEC);
        Content.register(bus);
        bus.addListener(SetRecipePayload::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Binding::interact);
    }
}
