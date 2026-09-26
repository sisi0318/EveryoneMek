package dev.everyonemek.gravity.expansion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Same caged flare in inventory, both hands, frames, dropped items and captor previews. */
public final class FlareCellRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation FRAME=id("flare_cell_frame"),FALLBACK=id("flare_cell_fallback");
    private static ModelResourceLocation id(String path){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("mekgravity","item/"+path));}
    private FlareCellRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static void models(ModelEvent.RegisterAdditional event){event.register(FRAME);event.register(FALLBACK);}
    public static void register(RegisterClientExtensionsEvent event){event.registerItem(new IClientItemExtensions(){private FlareCellRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new FlareCellRenderer();return renderer;}},ModuleContent.FLARE.get());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var mc=Minecraft.getInstance();double time=mc.level==null||!dev.everyonemek.gravity.VisualConfig.ANIMATE_ITEMS.get()?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(.5,.5,.5);pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(time*.8%360)));pose.scale(.23F,.24F,.23F);
        boolean shader=dev.everyonemek.gravity.solar.SolarShader.drawFlare(pose,buffers,time,context==ItemDisplayContext.GUI?20:40);pose.popPose();pose.translate(-.5,-.5,-.5);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),ModuleContent.BLOCK.get(ModuleKind.CAPTOR).get().defaultBlockState(),mc.getModelManager().getModel(shader?FRAME:FALLBACK),1,1,1,light,overlay,ModelData.EMPTY,null);pose.popPose();
    }
}
