package dev.everyonemek.overloadcore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OverloadCore.ID)
public final class OverloadCore {
    public static final String ID = "overloadcore";
    public OverloadCore(IEventBus bus, ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, CoreConfig.SPEC);
        CoreContent.register(bus); bus.addListener(CorePackets::register);
    }
}
