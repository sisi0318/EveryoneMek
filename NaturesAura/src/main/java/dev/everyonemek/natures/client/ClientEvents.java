package dev.everyonemek.natures.client;

import dev.everyonemek.natures.Content;
import dev.everyonemek.natures.NaturesMekanism;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = NaturesMekanism.ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(Content.MENU.get(), MachineScreen::new);
    }
}
