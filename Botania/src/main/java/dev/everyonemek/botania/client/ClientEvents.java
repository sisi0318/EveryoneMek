package dev.everyonemek.botania.client;

import dev.everyonemek.botania.BotanicalMekanism;
import dev.everyonemek.botania.Content;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.block_entity.BindableSpecialFlowerBlockEntity;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;

@EventBusSubscriber(modid = BotanicalMekanism.ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(dev.everyonemek.botania.MechanicalSparks.ENTITY.get(), MechanicalSparkRenderer::new);
        event.registerBlockEntityRenderer(dev.everyonemek.botania.ManaContent.MACHINE_TILES.get(dev.everyonemek.botania.ManaMachineKind.INFUSER).get(), InfusionCatalystRenderer::new);
    }
    @SubscribeEvent public static void modelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "mana_cell"), new ManaCellModelLoader());
    }
    @SubscribeEvent public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) dev.everyonemek.botania.compat.ae2.ManaAeClient.colors(event);
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) dev.everyonemek.botania.compat.ae2.ManaAeClient.models(event);
    }

    @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandHUD.BLOCK_LOOKUP),
              Content.LOTUS_TILE.get(), (tile, unused) -> new BindableSpecialFlowerBlockEntity.BindableFlowerWandHud<>(tile));
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(dev.everyonemek.botania.MechanicalSparks.MENU.get(), SparkControllerScreen::new);
        event.register(dev.everyonemek.botania.ManaContent.MENU.get(), ManaMachineScreen::new);
        event.register(dev.everyonemek.botania.ApothecaryContent.MENU.get(), ApothecaryScreen::new);
        event.<dev.everyonemek.botania.FlowerMenu, net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<dev.everyonemek.botania.FlowerMenu>>register(Content.MENU.get(), (menu, inventory, title) -> menu.level.getBlockState(menu.position).is(Content.CORE.get())
              || menu.level.getBlockState(menu.position).is(Content.NODE.get()) ? new ResonanceScreen(menu, inventory, title)
                    : menu.level.getBlockState(menu.position).is(Content.CORPOREA.get()) ? new CorporeaFlowerScreen(menu, inventory, title) : new FlowerScreen(menu, inventory, title));
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (net.neoforged.fml.ModList.get().isLoaded("ae2")) dev.everyonemek.botania.compat.ae2.ManaAeClient.register();
            for (var block : Content.plants())
                ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout());
        });
    }
}
