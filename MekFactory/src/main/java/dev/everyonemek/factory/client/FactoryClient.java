package dev.everyonemek.factory.client;
import dev.everyonemek.factory.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid=MekFactory.ID,value=Dist.CLIENT)
public final class FactoryClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(Content.MENU.get(),FactoryScreen::new);e.register(Content.WAREHOUSE_MENU.get(),WarehouseScreen::new);}
    @SubscribeEvent public static void additional(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){FactoryModels.additional(e);}
    @SubscribeEvent public static void bake(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult e){FactoryModels.bake(e);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        FactoryAppearance.clientReceiver=FactorySkins::receive;
        for(var block:Content.PORTS.values())net.minecraft.client.renderer.item.ItemProperties.register(block.asItem(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MekFactory.ID,"output"),(stack,level,entity,seed)->{var data=stack.get(Content.PORT_DATA.get());return data!=null&&data.getBoolean("output")?1:0;});
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactorySkins::unload);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FactorySkins::chunkUnload);
    });}
}
