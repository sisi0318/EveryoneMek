package dev.everyonemek.gravity.solar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Uses the same surface shader in GUI, both hands, item frames and dropped-item rendering. */
public final class SolarSeedItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation FALLBACK=ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("mekgravity","block/solar/sun_active"));
    private SolarSeedItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static void register(RegisterClientExtensionsEvent event){event.registerItem(new IClientItemExtensions(){
        private SolarSeedItemRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new SolarSeedItemRenderer();return renderer;}
    },SolarContent.block(SolarBlock.Kind.SEED,0).asItem());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double phase=mc.level==null||!dev.everyonemek.gravity.VisualConfig.ANIMATE_ITEMS.get()?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        try{
            pose.translate(.5,.5,.5);pose.scale(.34F,.34F,.34F);pose.mulPose(Axis.YP.rotationDegrees((float)(phase*.45%360)));pose.mulPose(Axis.ZP.rotationDegrees(12));
            if(!SolarShader.draw(pose,buffers,phase,1,context==ItemDisplayContext.GUI?20:0)){
                pose.translate(-.5,-.5,-.5);mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),SolarContent.block(SolarBlock.Kind.SEED,0).get().defaultBlockState(),mc.getModelManager().getModel(FALLBACK),1,1,1,LightTexture.FULL_BRIGHT,overlay,ModelData.EMPTY,null);
            }
        }finally{pose.popPose();}
    }
}
