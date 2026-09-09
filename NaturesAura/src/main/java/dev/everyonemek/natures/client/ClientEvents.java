package dev.everyonemek.natures.client;

import dev.everyonemek.natures.Content;
import dev.everyonemek.natures.NaturesMekanism;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;

@EventBusSubscriber(modid = NaturesMekanism.ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(Content.CHAMBER_PORT.asItem(),
              ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "port_output"), (stack, level, entity, seed) -> {
                  var state = stack.get(DataComponents.BLOCK_STATE);
                  return state != null && "true".equals(state.properties().get("output")) ? 1 : 0;
              }));
    }
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(Content.MENU.get(), MachineScreen::new);
    }
}
