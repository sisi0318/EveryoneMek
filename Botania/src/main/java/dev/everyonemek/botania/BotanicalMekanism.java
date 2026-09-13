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
        MechanicalSparks.register(bus);
        ApothecaryContent.register(bus);
        ManaContent.register(bus);
        SparkExpansion.register(bus);
        dev.everyonemek.botania.corporea.CorporeaIntegration.register();
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) dev.everyonemek.botania.compat.ae2.AeCompat.register(bus);
        bus.addListener(FlowerPackets::register);
    }
}
