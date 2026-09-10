package dev.everyonemek.forbidden.client;

import dev.everyonemek.forbidden.Content;
import dev.everyonemek.forbidden.ForbiddenMekanism;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ForbiddenMekanism.ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) { event.register(Content.MENU.get(), MachineScreen::new); }
}
