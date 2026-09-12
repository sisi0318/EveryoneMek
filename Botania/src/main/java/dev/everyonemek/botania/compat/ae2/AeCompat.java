package dev.everyonemek.botania.compat.ae2;

import appeng.api.AECapabilities;
import dev.everyonemek.botania.Content;
import dev.everyonemek.botania.corporea.CorporeaIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Loaded only after ModList confirms that AE2 is installed. */
public final class AeCompat {
    public static void register(IEventBus bus) {
        CorporeaIntegration.factory = AeBridge::new;
        CorporeaIntegration.cableSupport = (level, pos) -> level.hasChunkAt(pos) && level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null;
        bus.addListener((RegisterCapabilitiesEvent event) -> event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
              Content.CORPOREA_TILE.get(), (flower, unused) -> (AeBridge) flower.backend()));
    }
    private AeCompat() { }
}
