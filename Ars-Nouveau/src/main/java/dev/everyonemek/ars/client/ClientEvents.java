package dev.everyonemek.ars.client;

import dev.everyonemek.ars.ArsMekanism;
import dev.everyonemek.ars.Content;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ArsMekanism.ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) { event.register(Content.MENU.get(), MachineScreen::new); }
}
