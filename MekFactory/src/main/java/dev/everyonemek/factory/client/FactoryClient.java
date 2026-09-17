package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid=MekFactory.ID,value=Dist.CLIENT)
public final class FactoryClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(Content.MENU.get(),FactoryScreen::new);}
    @SubscribeEvent public static void additional(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){FactoryModels.additional(e);}
    @SubscribeEvent public static void bake(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult e){FactoryModels.bake(e);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        FactoryAppearance.clientReceiver=FactorySkins::receive;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactorySkins::unload);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactorySkins::chunkUnload);
    });}
}
