package dev.everyonemek.gravity.client;
import dev.everyonemek.gravity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=MekGravity.ID,value=Dist.CLIENT)
public final class GravityClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(Content.MENU.get(),ReactorScreen::new);e.register(Content.FUEL_MENU.get(),FuelScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Content.PART.get(),CoreRenderer::new);}
    @SubscribeEvent public static void additional(ModelEvent.RegisterAdditional e){ConnectedGlassModel.additional(e);CoreRenderer.additional(e);}
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult e){ConnectedGlassModel.bake(e);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        net.neoforged.fml.ModList.get().getModContainerById(MekGravity.ID).orElseThrow().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(net.neoforged.neoforge.client.gui.IConfigScreenFactory)net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
        for(var kind:java.util.List.of(PartBlock.Kind.COOLANT,PartBlock.Kind.ENERGY))net.minecraft.client.renderer.item.ItemProperties.register(Content.PARTS.get(kind).asItem(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"output"),(s,l,p,seed)->{var t=s.get(Content.STOCK.get());return t!=null&&t.getBoolean("output")?1:0;});
    });}
}
