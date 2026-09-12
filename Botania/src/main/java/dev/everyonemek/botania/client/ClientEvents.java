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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.block_entity.BindableSpecialFlowerBlockEntity;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;

@EventBusSubscriber(modid = BotanicalMekanism.ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandHUD.BLOCK_LOOKUP),
              Content.LOTUS_TILE.get(), (tile, unused) -> new BindableSpecialFlowerBlockEntity.BindableFlowerWandHud<>(tile));
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(dev.everyonemek.botania.ApothecaryContent.MENU.get(), ApothecaryScreen::new);
        event.<dev.everyonemek.botania.FlowerMenu, net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<dev.everyonemek.botania.FlowerMenu>>register(Content.MENU.get(), (menu, inventory, title) -> menu.level.getBlockState(menu.position).is(Content.CORE.get())
              || menu.level.getBlockState(menu.position).is(Content.NODE.get()) ? new ResonanceScreen(menu, inventory, title) : new FlowerScreen(menu, inventory, title));
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var block : new net.minecraft.world.level.block.Block[]{Content.LOTUS.get(), Content.AMARANTHUS.get(), Content.CORE.get(), Content.NODE.get()})
                ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
        });
    }
}
