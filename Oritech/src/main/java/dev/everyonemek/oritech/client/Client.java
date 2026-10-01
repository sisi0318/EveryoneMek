package dev.everyonemek.oritech.client;

import dev.everyonemek.oritech.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=Content.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class Client {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(Content.MENU.get(),ProcessorScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Content.TILE.get(),context->new ProcessorRenderer());}
}
