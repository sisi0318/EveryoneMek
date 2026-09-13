package dev.everyonemek.botania.compat.ae2;

import appeng.api.AECapabilities;
import dev.everyonemek.botania.Content;
import dev.everyonemek.botania.corporea.CorporeaIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Loaded only after ModList confirms that AE2 is installed. */
public final class AeCompat {
    public static void register(IEventBus bus) {
        var types = net.neoforged.neoforge.registries.DeferredRegister.create(appeng.api.stacks.AEKeyType.REGISTRY_KEY, dev.everyonemek.botania.BotanicalMekanism.ID);
        if (!dev.everyonemek.botania.AppliedBotanics.loaded()) { types.register("mana", () -> ManaKey.TYPE); types.register(bus); }
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(appeng.api.stacks.AEKeyType.REGISTRY_KEY)) {
                var appbot = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("appbot", "mana");
                event.getRegistry().addAlias(dev.everyonemek.botania.AppliedBotanics.loaded() ? ManaKey.ID : appbot,
                      dev.everyonemek.botania.AppliedBotanics.loaded() ? appbot : ManaKey.ID);
            }
        });
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            appeng.api.storage.StorageCells.addCellHandler(ManaCell.HANDLER);
            // Appbot's native strategies already cover pools and our SafeMana machine adapter.
            if (dev.everyonemek.botania.AppliedBotanics.loaded()) return;
            appeng.api.behaviors.GenericSlotCapacities.register(ManaKey.TYPE, 100_000L);
            appeng.api.behaviors.ContainerItemStrategy.register(ManaKey.TYPE, ManaKey.class, new ManaContainerStrategy());
            appeng.api.behaviors.StackImportStrategy.register(ManaKey.TYPE, ManaBusStorage::at);
            appeng.api.behaviors.StackExportStrategy.register(ManaKey.TYPE, ManaBusStorage::at);
            appeng.api.behaviors.ExternalStorageStrategy.register(ManaKey.TYPE, (level, pos, side) -> (extractable, changed) -> {
                var access = dev.everyonemek.botania.ManaAccess.at(level, pos, side);
                return access == null ? null : new ManaBusStorage(access, extractable, changed);
            });
        }));
        CorporeaIntegration.factory = AeBridge::new;
        CorporeaIntegration.cableSupport = (level, pos) -> level.hasChunkAt(pos) && level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null;
        bus.addListener((RegisterCapabilitiesEvent event) -> event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
              Content.CORPOREA_TILE.get(), (flower, unused) -> (AeBridge) flower.backend()));
    }
    private AeCompat() { }
}
