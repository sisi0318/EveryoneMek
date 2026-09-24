package dev.everyonemek.gravity.client;

import dev.everyonemek.gravity.*;
import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Three independently animated meshes plus bounded light ribbons. No particle/entity spawning. */
public final class CoreRenderer implements BlockEntityRenderer<Part> {
    private static final ModelResourceLocation ENERGY=model("energy"),ENERGY_ACTIVE=model("energy_active"),INNER=model("ring_0"),OUTER=model("ring_1");
    private final WeakHashMap<Part,CoreMotion> motions=new WeakHashMap<>();
    public CoreRenderer(BlockEntityRendererProvider.Context context){}
    private static ModelResourceLocation model(String part){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"block/core_"+part));}
    public static void additional(ModelEvent.RegisterAdditional event){event.register(ENERGY);event.register(ENERGY_ACTIVE);event.register(INNER);event.register(OUTER);}
    @Override public boolean shouldRender(Part p,Vec3 camera){return p.kind()==PartBlock.Kind.CORE&&p.getLevel()!=null&&BlockEntityRenderer.super.shouldRender(p,camera);}
    @Override public AABB getRenderBoundingBox(Part p){return p.kind()==PartBlock.Kind.CORE?new AABB(p.getBlockPos()).inflate(1.1):BlockEntityRenderer.super.getRenderBoundingBox(p);}
    private static void drawModel(Part part,ModelResourceLocation id,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var minecraft=Minecraft.getInstance();var baked=minecraft.getModelManager().getModel(id);
        pose.pushPose();pose.translate(-.5,-.5,-.5);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),part.getBlockState(),baked,1,1,1,light,overlay,ModelData.EMPTY,null);
        pose.popPose();
    }
    private static void vertex(VertexConsumer v,Matrix4f matrix,int axis,float axial,int transverse,float offset,int alpha,boolean spark){
        float x=axis==0?axial:transverse==0?offset:0,y=axis==1?axial:transverse==1?offset:0,z=axis==2?axial:transverse==2?offset:0;
        v.addVertex(matrix,x,y,z).setColor(spark?229:150,spark?214:106,spark?255:224,alpha);
    }
    private static void ribbon(VertexConsumer v,Matrix4f matrix,int axis,float start,float end,float width,int alpha,boolean spark){
        for(int cross=1;cross<=2;cross++){int perpendicular=(axis+cross)%3;
            vertex(v,matrix,axis,start,perpendicular,-width,alpha,spark);vertex(v,matrix,axis,end,perpendicular,-width,alpha,spark);
            vertex(v,matrix,axis,end,perpendicular,width,alpha,spark);vertex(v,matrix,axis,start,perpendicular,width,alpha,spark);
        }
    }
    private static void arc(PoseStack pose,VertexConsumer v,float radius,float width,double start,double length,int segments,int alpha){
        var m=pose.last().pose();for(int i=0;i<segments;i++){
            double a=start+length*i/segments,b=start+length*(i+1)/segments;
            int fade=(int)(alpha*(.28+.72*(i+1)/segments));
            v.addVertex(m,(float)Math.cos(a)*radius,0,(float)Math.sin(a)*radius).setColor(177,134,242,fade);
            v.addVertex(m,(float)Math.cos(b)*radius,0,(float)Math.sin(b)*radius).setColor(177,134,242,fade);
            v.addVertex(m,(float)Math.cos(b)*(radius-width),0,(float)Math.sin(b)*(radius-width)).setColor(177,134,242,fade);
            v.addVertex(m,(float)Math.cos(a)*(radius-width),0,(float)Math.sin(a)*(radius-width)).setColor(177,134,242,fade);
        }
    }
    @Override public void render(Part p,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(p.kind()!=PartBlock.Kind.CORE||p.getLevel()==null)return;
        double tick=p.getLevel().getGameTime()+(double)partial;
        var motion=motions.computeIfAbsent(p,k->new CoreMotion());
        boolean working=p.getBlockState().getValue(PartBlock.ACTIVE)&&p.getBlockState().getValue(PartBlock.FORMED);
        motion.update(tick,working?.25+.75*p.visualLoad()/100D:0);
        float intensity=motion.strength();double phase=motion.phase();
        float pulse=(float)(.86+.14*Math.sin(phase*.13));
        double distance=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(p.getBlockPos().getCenter());
        pose.pushPose();pose.translate(.5,.5,.5);
        // The core body replaces the baked block entirely, including when stopped; no static ghost mesh.
        pose.pushPose();pose.translate(0,Math.sin(phase*.055)*.035*intensity,0);
        pose.mulPose(Axis.YP.rotationDegrees((float)(phase*.55%360)));pose.mulPose(Axis.XP.rotationDegrees((float)Math.sin(phase*.018)*9*intensity));
        float size=1+(float)Math.sin(phase*.09)*.022F*intensity;pose.scale(size,size,size);
        if(!dev.everyonemek.gravity.solar.SolarShader.drawGravity(pose,buffers,phase,intensity,distance))drawModel(p,intensity>.035F?ENERGY_ACTIVE:ENERGY,pose,buffers,intensity>.035F?LightTexture.FULL_BRIGHT:light,overlay);pose.popPose();
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(phase*1.2%360)));if(!GravityRingShader.draw(0,pose,buffers,phase,intensity))drawModel(p,INNER,pose,buffers,light,overlay);pose.popPose();
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(-phase*1.6%360)));if(!GravityRingShader.draw(1,pose,buffers,phase,intensity))drawModel(p,OUTER,pose,buffers,light,overlay);pose.popPose();
        if(intensity>.001F&&distance<32){
            intensity*=(float)Math.clamp((32-distance)/8,0,1);
            var v=buffers.getBuffer(RenderType.lightning());
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(phase*.65%360)));pose.mulPose(Axis.ZP.rotationDegrees(18));
            arc(pose,v,.435F,.012F,0,Math.PI*2,48,(int)(130*intensity*pulse));
            arc(pose,v,.456F,.026F,-phase*.11,Math.PI*.72,20,(int)(225*intensity));pose.popPose();
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(-phase*1.6%360)));pose.mulPose(Axis.ZP.rotationDegrees(-20));pose.mulPose(Axis.XP.rotationDegrees(-55));
            arc(pose,v,.700F,.015F,phase*.075,Math.PI*.52,16,(int)(160*intensity));pose.popPose();
            var matrix=pose.last().pose();
            for(int axis=0;axis<3;axis++)for(int sign=-1;sign<=1;sign+=2){
                ribbon(v,matrix,axis,sign*.40F,sign*1.375F,.009F+.006F*intensity,(int)(65*intensity*pulse),false);
                for(int spark=0;spark<3;spark++){
                    double fraction=(phase/24+axis*.17+(sign+1)*.13+spark/3D)%1;float at=sign*(1.34F-(float)fraction*.91F);
                    ribbon(v,matrix,axis,at-sign*.04F,at+sign*.04F,.018F+.009F*intensity,(int)(220*intensity),true);
                }
            }
        }
        pose.popPose();
    }
}
