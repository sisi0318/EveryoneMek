package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid=MekFactory.ID,value=Dist.CLIENT)
public final class FactoryClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(Content.MENU.get(),FactoryScreen::new);}
}
