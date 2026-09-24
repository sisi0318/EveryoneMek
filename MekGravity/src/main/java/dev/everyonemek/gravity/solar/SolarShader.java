package dev.everyonemek.gravity.solar;

import java.io.IOException;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.everyonemek.gravity.MekGravity;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Vector3f;

/** A single opaque sphere pass. Per-star data travels in vertices, not shared mutable uniforms. */
public final class SolarShader {
    private static ShaderInstance shader,gravityShader;
    private static final RenderType TYPE=type("stellar_surface",()->shader),GRAVITY_TYPE=type("gravity_surface",()->gravityShader);
    private static RenderType type(String name,java.util.function.Supplier<ShaderInstance> shader){return RenderType.create("mekgravity_"+name,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,VertexFormat.Mode.QUADS,131072,false,false,
          RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(shader))
          .setTransparencyState(RenderStateShard.NO_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
          .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).setCullState(RenderStateShard.CULL).createCompositeState(false));}
    public static void register(RegisterShadersEvent event){
        shader=gravityShader=null;
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"stellar_surface"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),loaded->shader=loaded);}
        catch(IOException error){LogUtils.getLogger().error("Could not load stellar surface shader; using the baked photosphere",error);}
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"gravity_surface"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),loaded->gravityShader=loaded);}
        catch(IOException error){LogUtils.getLogger().error("Could not load gravity surface shader; using the baked core",error);}
    }
    public static boolean draw(PoseStack pose,MultiBufferSource buffers,double phase,float heat,double distance){
        if(shader==null)return false;
        draw(pose,buffers,phase,heat,distance,TYPE);return true;
    }
    public static boolean drawGravity(PoseStack pose,MultiBufferSource buffers,double phase,float strength,double distance){
        if(gravityShader==null)return false;pose.pushPose();
        try{pose.scale(.3F,.3F,.3F);draw(pose,buffers,phase,strength,distance/.3,GRAVITY_TYPE);}finally{pose.popPose();}return true;
    }
    private static void draw(PoseStack pose,MultiBufferSource buffers,double phase,float heat,double distance,RenderType type){
        var vertices=SolarSphereMesh.vertices(distance);var vertex=buffers.getBuffer(type);var transform=pose.last();
        // A circular phase avoids discontinuities and precision loss during long sessions.
        double angle=phase*.006;float phaseX=(float)Math.cos(angle),phaseY=(float)Math.sin(angle);int thermal=Math.clamp(Math.round(heat*255),0,255);
        var position=new Vector3f();var normal=new Vector3f();
        for(int i=0;i<vertices.length;i+=3){float x=vertices[i],y=vertices[i+1],z=vertices[i+2];
            transform.pose().transformPosition(x*SolarSphereMesh.RADIUS,y*SolarSphereMesh.RADIUS,z*SolarSphereMesh.RADIUS,position);
            transform.transformNormal(x,y,z,normal);
            vertex.addVertex(position.x,position.y,position.z).setUv(phaseX,phaseY)
                  .setColor(Math.round((x+1)*127.5F),Math.round((y+1)*127.5F),Math.round((z+1)*127.5F),thermal).setNormal(normal.x,normal.y,normal.z);
        }
    }
    private SolarShader(){}
}
