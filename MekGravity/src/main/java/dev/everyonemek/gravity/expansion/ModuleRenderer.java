package dev.everyonemek.gravity.expansion;
import java.util.WeakHashMap;
import dev.everyonemek.gravity.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
public final class ModuleRenderer implements BlockEntityRenderer<OrbitalModule>{
    private final WeakHashMap<OrbitalModule,CoreMotion> states=new WeakHashMap<>();
    public ModuleRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(OrbitalModule m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(m.getLevel()==null)return;var mc=Minecraft.getInstance();double distance=mc.gameRenderer.getMainCamera().getPosition().distanceTo(m.getBlockPos().getCenter());if(distance>VisualConfig.EFFECT_DISTANCE.get())return;
        var state=states.computeIfAbsent(m,k->new CoreMotion());state.update(m.getLevel().getGameTime()+(double)partial,m.displayRunning?1:0);double phase=VisualConfig.phase(state.phase());int kind=m.kind().ordinal();
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(180-m.getDirection().toYRot()));if(m instanceof NodeModule node){if(VisualConfig.effects())NodeSnapshotShader.draw(node,pose,buffers,phase,state.strength()*(float)Math.clamp((VisualConfig.EFFECT_DISTANCE.get()-distance)/6,0,1));pose.popPose();return;}if(kind==2||kind==4){pose.translate(0,.0625,-.332);pose.scale(.8F,.8F,.8F);}else{pose.translate(0,0,-.10);pose.scale(.8F,.8F,.8F);}
        if(m.kind().processor()&&!m.displayItem.isEmpty()){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(phase*2%360)));pose.scale(.3F,.3F,.3F);mc.getItemRenderer().renderStatic(m.displayItem,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,overlay,pose,buffers,m.getLevel(),0);pose.popPose();}
        if(VisualConfig.effects()&&state.strength()>.003)ModuleShader.draw(pose,buffers,kind,phase,m.displayProgress/100F,state.strength()*(float)Math.clamp((VisualConfig.EFFECT_DISTANCE.get()-distance)/6,0,1));pose.popPose();
    }
}
