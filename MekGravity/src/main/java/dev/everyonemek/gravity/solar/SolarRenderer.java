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
/** Fixed-budget solar surface, corona, prominences and outward energy streams. */
public final class SolarRenderer implements BlockEntityRenderer<SolarPart> {
    private static final ModelResourceLocation IDLE=id("sun_idle"),ACTIVE=id("sun_active");
    private final WeakHashMap<SolarPart,CoreMotion> motion=new WeakHashMap<>();
    private static ModelResourceLocation id(String s){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"block/solar/"+s));}
    public SolarRenderer(BlockEntityRendererProvider.Context context){}
    public static void additional(ModelEvent.RegisterAdditional e){e.register(IDLE);e.register(ACTIVE);}
    @Override public boolean shouldRender(SolarPart p,Vec3 camera){return p.kind()==SolarBlock.Kind.SEED&&p.getLevel()!=null&&BlockEntityRenderer.super.shouldRender(p,camera);}
    @Override public AABB getRenderBoundingBox(SolarPart p){return p.kind()==SolarBlock.Kind.SEED?new AABB(p.getBlockPos()).inflate(4):BlockEntityRenderer.super.getRenderBoundingBox(p);}
    private static void strip(VertexConsumer v,PoseStack pose,Vec3 a,Vec3 b,float width,int alpha){var m=pose.last().pose();
        for(int plane=0;plane<2;plane++){var off=plane==0?new Vec3(width,0,0):new Vec3(0,width,0);for(var p:new Vec3[]{a.subtract(off),b.subtract(off),b.add(off),a.add(off)})v.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor(255,176,65,alpha);}
    }
    @Override public void render(SolarPart p,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(p.kind()!=SolarBlock.Kind.SEED||p.getLevel()==null)return;
        var m=motion.computeIfAbsent(p,k->new CoreMotion());boolean active=p.getBlockState().getValue(SolarBlock.ACTIVE)&&p.getBlockState().getValue(SolarBlock.FORMED);m.update(p.getLevel().getGameTime()+(double)partial,active?.3+.7*p.visual()/100D:0);
        float strength=m.strength();double phase=m.phase();float size=.2F+.8F*strength;
        pose.pushPose();pose.translate(.5,.5,.5);pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(phase*.2%360)));pose.mulPose(Axis.ZP.rotationDegrees(12));pose.scale(size,size,size);pose.translate(-.5,-.5,-.5);
        var mc=Minecraft.getInstance();mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),p.getBlockState(),mc.getModelManager().getModel(strength>.05?ACTIVE:IDLE),1,1,1,LightTexture.FULL_BRIGHT,overlay,ModelData.EMPTY,null);pose.popPose();
        if(strength>.002){var v=buffers.getBuffer(RenderType.lightning());
            for(int ring=0;ring<3;ring++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(phase*.14+ring*120)));pose.mulPose(Axis.ZP.rotationDegrees(30+ring*37));
                for(int i=0;i<40;i++){double a=i*Math.PI/20,b=(i+1)*Math.PI/20;double r=(1.28+.025*Math.sin(phase*.05+i*.4))*size;strip(v,pose,new Vec3(Math.cos(a)*r,0,Math.sin(a)*r),new Vec3(Math.cos(b)*r,0,Math.sin(b)*r),.025F*size,(int)(22*strength));}pose.popPose();
            }
            for(int plume=0;plume<4;plume++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(plume*90+(float)(phase*.09%360)));pose.mulPose(Axis.ZP.rotationDegrees(20+plume*31));
                for(int i=0;i<16;i++){double a=i/16D,b=(i+1)/16D,am=-.4+a*.8,bm=-.4+b*.8;double ra=(1.22+.31*Math.sin(a*Math.PI)*(.7+.3*Math.sin(phase*.045+plume)))*size,rb=(1.22+.31*Math.sin(b*Math.PI)*(.7+.3*Math.sin(phase*.045+plume)))*size;
                    strip(v,pose,new Vec3(Math.cos(am)*ra,Math.sin(am)*ra,0),new Vec3(Math.cos(bm)*rb,Math.sin(bm)*rb,0),.016F*size,(int)(125*strength));}pose.popPose();
            }
            for(int axis:new int[]{0,2})for(int sign:new int[]{-1,1})for(int i=0;i<4;i++){double t=(phase/55+i*.25)%1,at=sign*(1.4+2.25*t);var a=axis==0?new Vec3(at,0,0):new Vec3(0,0,at);var b=axis==0?new Vec3(at+sign*.10,0,0):new Vec3(0,0,at+sign*.10);strip(v,pose,a,b,.02F,(int)(150*strength*(1-t*.5)));}
        }
        pose.popPose();
    }
}
