package dev.everyonemek.gravity.solar;
import dev.everyonemek.gravity.MekGravity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=MekGravity.ID,value=Dist.CLIENT)
public final class SolarClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(dev.everyonemek.gravity.expansion.ModuleContent.MENU.get(),dev.everyonemek.gravity.expansion.ModuleScreen::new);e.register(dev.everyonemek.gravity.corona.CoronalContent.MENU.get(),dev.everyonemek.gravity.corona.CoronalScreen::new);e.register(SolarContent.MENU.get(),SolarScreen::new);e.register(SolarContent.FUEL_MENU.get(),SolarFuelScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){dev.everyonemek.gravity.expansion.ModuleContent.TILE.values().forEach(t->e.registerBlockEntityRenderer(t.get(),dev.everyonemek.gravity.expansion.ModuleRenderer::new));e.registerBlockEntityRenderer(dev.everyonemek.gravity.corona.CoronalContent.TILE.get(),dev.everyonemek.gravity.corona.CoronalRenderer::new);e.registerBlockEntityRenderer(SolarContent.PART.get(),SolarRenderer::new);}
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){SolarRenderer.additional(e);dev.everyonemek.gravity.expansion.FlareCellRenderer.models(e);}
    @SubscribeEvent public static void shaders(RegisterShadersEvent e){dev.everyonemek.gravity.expansion.ModuleShader.register(e);dev.everyonemek.gravity.corona.CoronalShader.register(e);SolarShader.register(e);SolarShader.registerFlare(e);SolarFieldShader.register(e);dev.everyonemek.gravity.client.GravityRingShader.register(e);}
    @SubscribeEvent public static void itemExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){SolarSeedItemRenderer.register(e);dev.everyonemek.gravity.expansion.FlareCellRenderer.register(e);dev.everyonemek.gravity.client.GravityCoreItemRenderer.register(e);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(SolarContent.block(SolarBlock.Kind.ENERGY,0).asItem(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"output"),(s,l,p,seed)->{var t=s.get(SolarContent.STOCK.get());return t!=null&&t.getBoolean("output")?1:0;}));}
}
