package dev.everyonemek.gravity.client;
import dev.everyonemek.gravity.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
public final class CoreRenderer implements BlockEntityRenderer<Part> {
    public CoreRenderer(BlockEntityRendererProvider.Context context){}
    @Override public boolean shouldRender(Part p,net.minecraft.world.phys.Vec3 camera){return p.kind()==PartBlock.Kind.CORE&&p.getBlockState().getValue(PartBlock.ACTIVE)&&BlockEntityRenderer.super.shouldRender(p,camera);}
    @Override public void render(Part p,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(p.kind()!=PartBlock.Kind.CORE||!p.getBlockState().getValue(PartBlock.ACTIVE))return;
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees((p.getLevel().getGameTime()+partial)%720/2));pose.mulPose(Axis.ZP.rotationDegrees(18));
        var matrix=pose.last().pose();var v=buffers.getBuffer(RenderType.lightning());
        for(int i=0;i<32;i++){double a=i*Math.PI/16,b=(i+1)*Math.PI/16;float r=.58F,s=.55F;
            v.addVertex(matrix,(float)Math.cos(a)*r,0,(float)Math.sin(a)*r).setColor(163,122,222,210);
            v.addVertex(matrix,(float)Math.cos(b)*r,0,(float)Math.sin(b)*r).setColor(163,122,222,210);
            v.addVertex(matrix,(float)Math.cos(b)*s,0,(float)Math.sin(b)*s).setColor(163,122,222,210);
            v.addVertex(matrix,(float)Math.cos(a)*s,0,(float)Math.sin(a)*s).setColor(163,122,222,210);
        }pose.popPose();
    }
}
