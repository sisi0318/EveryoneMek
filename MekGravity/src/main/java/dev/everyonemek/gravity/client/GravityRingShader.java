package dev.everyonemek.gravity.client;

import java.io.IOException;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.everyonemek.gravity.MekGravity;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Vector3f;

/** Opaque metal and emissive channels in one pass; no texture or noise sampling. */
public final class GravityRingShader {
    private static ShaderInstance shader;
    private static final RenderType TYPE=RenderType.create("mekgravity_gravity_ring",DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,VertexFormat.Mode.QUADS,24576,false,false,
          RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader))
          .setTransparencyState(RenderStateShard.NO_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
          .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).setCullState(RenderStateShard.CULL).createCompositeState(false));
    public static void register(RegisterShadersEvent event){
        shader=null;
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"gravity_ring"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),loaded->shader=loaded);}
        catch(IOException error){LogUtils.getLogger().error("Could not load gravity ring shader; using the baked metal rings",error);}
    }
    public static boolean draw(int ring,PoseStack pose,MultiBufferSource buffers,double phase,float strength){
        if(shader==null)return false;
        var vertices=GravityRingMesh.vertices(ring);var vertex=buffers.getBuffer(TYPE);var transform=pose.last();
        double turn=(ring==0?phase:-phase)/85+ring*.5;float flow=(float)(turn-Math.floor(turn));int load=Math.clamp(Math.round(strength*255),0,255);
        var position=new Vector3f();var normal=new Vector3f();
        for(int i=0;i<vertices.length;i+=9){
            transform.pose().transformPosition(vertices[i],vertices[i+1],vertices[i+2],position);
            transform.transformNormal(vertices[i+3],vertices[i+4],vertices[i+5],normal);
            vertex.addVertex(position.x,position.y,position.z).setUv(vertices[i+6],flow)
                  .setColor(load,vertices[i+8]>0?255:0,Math.clamp(Math.round(vertices[i+7]*255),0,255),255).setNormal(normal.x,normal.y,normal.z);
        }return true;
    }
    private GravityRingShader(){}
}
