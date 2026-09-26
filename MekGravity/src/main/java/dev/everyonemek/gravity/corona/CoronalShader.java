package dev.everyonemek.gravity.corona;
import java.io.IOException;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.everyonemek.gravity.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
public final class CoronalShader {
    private static ShaderInstance shader;
    private static final RenderType TYPE=RenderType.create("mekgravity_coronal_processing",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,16384,false,false,
        RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader)).setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
    public static void register(RegisterShadersEvent event){shader=null;try{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"coronal_processing"),DefaultVertexFormat.POSITION_TEX_COLOR),s->shader=s);}catch(IOException e){LogUtils.getLogger().error("Could not load coronal shader; using simple heat rings",e);}}
    public static void draw(PoseStack pose,MultiBufferSource buffers,double phase,float progress,float strength){
        boolean custom=shader!=null&&VisualConfig.SHADERS.get();var v=buffers.getBuffer(custom?TYPE:RenderType.lightning());
        int p=Math.clamp((int)(progress*255),0,255),s=(int)((Math.sin(phase*.08)+1)*127.5),c=(int)((Math.cos(phase*.08)+1)*127.5),a=Math.clamp((int)(strength*220),0,255);
        var matrix=pose.last().pose();var point=new org.joml.Vector3f();
        CoronalField.emit(VisualConfig.reduced(),(x,y,z,angle,across)->{matrix.transformPosition(x,y,z,point);var vertex=v.addVertex(point.x,point.y,point.z);if(custom)vertex.setUv(angle,across).setColor(p,s,c,a);else vertex.setColor(255,150+p/3,40,a/2);});
    }
    private CoronalShader(){}
}
