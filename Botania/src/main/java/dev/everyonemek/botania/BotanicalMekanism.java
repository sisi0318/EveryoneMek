package dev.everyonemek.botania;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(BotanicalMekanism.ID)
public final class BotanicalMekanism {
    public static final String ID = "botanicalmekanism";

    public BotanicalMekanism(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Balance.SPEC);
        Content.register(bus);
        ApothecaryContent.register(bus);
        ManaContent.register(bus);
        SparkExpansion.register(bus);
        bus.addListener(FlowerPackets::register);
    }
}
