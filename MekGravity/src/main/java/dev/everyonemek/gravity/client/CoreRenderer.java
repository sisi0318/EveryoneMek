package dev.everyonemek.gravity.client;

import dev.everyonemek.gravity.*;
import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.*;

/** One inner energy orbit, six ribbons and twelve sparks around the baked core and metal rings. */
public final class CoreRenderer implements BlockEntityRenderer<Part> {
    private final WeakHashMap<Part,Fade> effects=new WeakHashMap<>();
    private static final class Fade {
        boolean active;double since;float from;
        Fade(double tick){since=tick;}
        float value(double tick){float t=(float)Math.clamp((tick-since)/16D,0,1);return from+((active?1:0)-from)*t;}
        void update(boolean next,double tick){if(active!=next){from=value(tick);since=tick;active=next;}}
    }
    public CoreRenderer(BlockEntityRendererProvider.Context context){}
    @Override public boolean shouldRender(Part p,Vec3 camera){
        if(p.kind()!=PartBlock.Kind.CORE||p.getLevel()==null||!BlockEntityRenderer.super.shouldRender(p,camera))return false;
        var fade=effects.get(p);return p.getBlockState().getValue(PartBlock.ACTIVE)||fade!=null&&fade.value(p.getLevel().getGameTime())>0;
    }
    @Override public AABB getRenderBoundingBox(Part p){return p.kind()==PartBlock.Kind.CORE?new AABB(p.getBlockPos()).inflate(1.1):BlockEntityRenderer.super.getRenderBoundingBox(p);}
    private static void vertex(VertexConsumer v,Matrix4f matrix,int axis,float axial,int transverse,float offset,int alpha,boolean spark){
        float x=axis==0?axial:transverse==0?offset:0,y=axis==1?axial:transverse==1?offset:0,z=axis==2?axial:transverse==2?offset:0;
        v.addVertex(matrix,x,y,z).setColor(spark?228:154,spark?212:115,spark?255:219,alpha);
    }
    private static void ribbon(VertexConsumer v,Matrix4f matrix,int axis,float start,float end,float width,int alpha,boolean spark){
        for(int cross=1;cross<=2;cross++){int perpendicular=(axis+cross)%3;
            vertex(v,matrix,axis,start,perpendicular,-width,alpha,spark);vertex(v,matrix,axis,end,perpendicular,-width,alpha,spark);
            vertex(v,matrix,axis,end,perpendicular,width,alpha,spark);vertex(v,matrix,axis,start,perpendicular,width,alpha,spark);
        }
    }
    private static void ring(PoseStack pose,VertexConsumer v,float radius,float width,int alpha){
        var m=pose.last().pose();for(int i=0;i<32;i++){double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
            v.addVertex(m,(float)Math.cos(a)*radius,0,(float)Math.sin(a)*radius).setColor(168,127,230,alpha);
            v.addVertex(m,(float)Math.cos(b)*radius,0,(float)Math.sin(b)*radius).setColor(168,127,230,alpha);
            v.addVertex(m,(float)Math.cos(b)*(radius-width),0,(float)Math.sin(b)*(radius-width)).setColor(168,127,230,alpha);
            v.addVertex(m,(float)Math.cos(a)*(radius-width),0,(float)Math.sin(a)*(radius-width)).setColor(168,127,230,alpha);
        }
    }
    @Override public void render(Part p,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(p.kind()!=PartBlock.Kind.CORE||p.getLevel()==null)return;
        double tick=p.getLevel().getGameTime()+(double)partial;var fade=effects.computeIfAbsent(p,k->new Fade(tick));fade.update(p.getBlockState().getValue(PartBlock.ACTIVE),tick);
        float intensity=fade.value(tick);if(intensity<=0)return;
        float pulse=(float)(.9+.1*Math.sin(tick*.10));var v=buffers.getBuffer(RenderType.lightning());
        pose.pushPose();pose.translate(.5,.5,.5);
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees((float)(tick%720)*.5F));pose.mulPose(Axis.ZP.rotationDegrees(18));ring(pose,v,.44F,.016F,(int)(155*intensity*pulse));pose.popPose();
        var matrix=pose.last().pose();
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}){
            ribbon(v,matrix,axis,sign*.39F,sign*1.375F,.011F,(int)(65*intensity*pulse),false);
            for(int spark=0;spark<2;spark++){double phase=(tick/36+axis*.17+(sign+1)*.13+spark*.5)%1;float at=sign*(1.34F-(float)phase*.92F);
                ribbon(v,matrix,axis,at-.025F,at+.025F,.022F,(int)(180*intensity),true);
            }
        }
        pose.popPose();
    }
}
