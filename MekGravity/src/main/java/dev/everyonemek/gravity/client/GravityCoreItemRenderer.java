package dev.everyonemek.gravity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.everyonemek.gravity.Content;
import dev.everyonemek.gravity.PartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.extensions.common.*;

/** The same purple surface and counter-rotating shader rings used by the placed core. */
public final class GravityCoreItemRenderer extends BlockEntityWithoutLevelRenderer {
    private GravityCoreItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static void register(RegisterClientExtensionsEvent event){event.registerItem(new IClientItemExtensions(){
        private GravityCoreItemRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new GravityCoreItemRenderer();return renderer;}
    },Content.PARTS.get(PartBlock.Kind.CORE).asItem());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double phase=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        try{
            pose.translate(.5,.5,.5);
            // GUI uses the medium sphere mesh. Item rendering does not create a fake reactor
            // or emit the world-space six-direction beams around an inventory slot.
            CoreRenderer.drawCore(Content.PARTS.get(PartBlock.Kind.CORE).get().defaultBlockState(),pose,buffers,light,overlay,phase,1,context==ItemDisplayContext.GUI?6:0);
        }finally{pose.popPose();}
    }
}
