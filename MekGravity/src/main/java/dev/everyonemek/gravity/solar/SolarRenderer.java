package dev.everyonemek.gravity.solar;
import dev.everyonemek.gravity.CoreMotion;
import dev.everyonemek.gravity.MekGravity;
import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
/** Animated stellar surface and a bounded, camera-facing gravitational containment field. */
public final class SolarRenderer implements BlockEntityRenderer<SolarPart> {
    private static final ModelResourceLocation IDLE=id("sun_idle"),ACTIVE=id("sun_active");
    private record Motion(CoreMotion activity,CoreMotion heat){Motion(){this(new CoreMotion(),new CoreMotion());}}
    private final WeakHashMap<SolarPart,Motion> motion=new WeakHashMap<>();
    private static ModelResourceLocation id(String s){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"block/solar/"+s));}
    public SolarRenderer(BlockEntityRendererProvider.Context context){}
    public static void additional(ModelEvent.RegisterAdditional e){e.register(IDLE);e.register(ACTIVE);}
    @Override public boolean shouldRender(SolarPart p,Vec3 camera){return p.kind()==SolarBlock.Kind.SEED&&p.getLevel()!=null&&BlockEntityRenderer.super.shouldRender(p,camera);}
    @Override public AABB getRenderBoundingBox(SolarPart p){return p.kind()==SolarBlock.Kind.SEED?new AABB(p.getBlockPos()).inflate(4):BlockEntityRenderer.super.getRenderBoundingBox(p);}
    private static void strip(VertexConsumer v,PoseStack pose,Vec3 camera,SolarField.Point A,SolarField.Point B,double width,int rgb,int alpha){
        var a=new Vec3(A.x(),A.y(),A.z());var b=new Vec3(B.x(),B.y(),B.z());var direction=b.subtract(a);var view=camera.subtract(a.add(b).scale(.5));var normal=view.cross(direction);
        if(normal.lengthSqr()<1E-12){normal=new Vec3(0,1,0).cross(direction);if(normal.lengthSqr()<1E-12)normal=new Vec3(1,0,0).cross(direction);}
        var off=normal.normalize().scale(width);var m=pose.last().pose();
        for(var point:new Vec3[]{a.subtract(off),b.subtract(off),b.add(off),a.add(off)})v.addVertex(m,(float)point.x,(float)point.y,(float)point.z).setColor(rgb>>16&255,rgb>>8&255,rgb&255,alpha);
    }
    @Override public void render(SolarPart p,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(p.kind()!=SolarBlock.Kind.SEED||p.getLevel()==null)return;
        var m=motion.computeIfAbsent(p,k->new Motion());boolean formed=p.getBlockState().getValue(SolarBlock.FORMED),active=formed&&p.getBlockState().getValue(SolarBlock.ACTIVE),hot=formed&&p.hot();double tick=p.getLevel().getGameTime()+(double)partial;
        m.activity.update(tick,active?.3+.7*p.visual()/100D:hot?.2:0);m.heat.update(tick,hot?1:0);
        float strength=m.activity.strength();double phase=m.activity.phase();float size=.2F+.8F*m.heat.strength();
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.getMainCamera().getPosition().subtract(p.getBlockPos().getCenter());double distance=camera.length();
        pose.pushPose();pose.translate(.5,.5,.5);pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(phase*1.2%360)));pose.mulPose(Axis.ZP.rotationDegrees(12));pose.scale(size,size,size);
        if(!SolarShader.draw(pose,buffers,phase,m.heat.strength(),distance)){
            pose.translate(-.5,-.5,-.5);mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),p.getBlockState(),mc.getModelManager().getModel(strength>.05?ACTIVE:IDLE),1,1,1,LightTexture.FULL_BRIGHT,overlay,ModelData.EMPTY,null);
        }pose.popPose();
        if(strength>.002&&distance<48){var v=buffers.getBuffer(RenderType.lightning());
            float fieldStrength=strength*(float)Math.clamp((48-distance)/8,0,1);
            SolarField.emit(phase,fieldStrength,size,(a,b,width,rgb,alpha,halo)->{if(halo)strip(v,pose,camera,a,b,width*3,rgb,alpha/5);strip(v,pose,camera,a,b,width,rgb,alpha);});}
        pose.popPose();
    }
}
