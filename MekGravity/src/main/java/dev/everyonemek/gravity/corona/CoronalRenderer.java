package dev.everyonemek.gravity.corona;
import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.everyonemek.gravity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
public final class CoronalRenderer implements BlockEntityRenderer<CoronalMachine>{
    private final WeakHashMap<CoronalMachine,CoreMotion> motions=new WeakHashMap<>();
    public CoronalRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(CoronalMachine tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(tile.getLevel()==null)return;var mc=Minecraft.getInstance();double distance=mc.gameRenderer.getMainCamera().getPosition().distanceTo(tile.getBlockPos().getCenter());
        if(distance>VisualConfig.EFFECT_DISTANCE.get())return;var motion=motions.computeIfAbsent(tile,t->new CoreMotion());motion.update(tile.getLevel().getGameTime()+(double)partial,tile.displayRunning?1:0);double phase=VisualConfig.phase(motion.phase());
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(180-tile.getDirection().toYRot()));pose.translate(0,0,.13);
        if(!tile.displayItem.isEmpty()){
            pose.pushPose();pose.translate(0,Math.sin(phase*.06)*.015,0);pose.mulPose(Axis.YP.rotationDegrees((float)(phase*2%360)));pose.scale(.32F,.32F,.32F);
            mc.getItemRenderer().renderStatic(tile.displayProgress>=90&&!tile.displayResult.isEmpty()?tile.displayResult:tile.displayItem,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,overlay,pose,buffers,tile.getLevel(),0);pose.popPose();
        }
        if(VisualConfig.effects()&&motion.strength()>.003){float fade=(float)Math.clamp((VisualConfig.EFFECT_DISTANCE.get()-distance)/6,0,1);CoronalShader.draw(pose,buffers,phase,tile.displayProgress/100F,motion.strength()*fade);}
        pose.popPose();
    }
}
