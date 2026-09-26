package dev.everyonemek.gravity.expansion;
import java.io.IOException;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.everyonemek.gravity.VisualConfig;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
public final class ModuleShader {
    private static ShaderInstance shader;
    private static final RenderType TYPE=RenderType.create("mekgravity_orbital_module",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,16384,false,false,
        RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader)).setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
    public static void register(RegisterShadersEvent e){shader=null;try{e.registerShader(new ShaderInstance(e.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("mekgravity","orbital_module"),DefaultVertexFormat.POSITION_TEX_COLOR),s->shader=s);}catch(IOException error){LogUtils.getLogger().error("Could not load orbital module shader; using simple fields",error);}}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int kind,double phase,float progress,float strength){
        boolean custom=shader!=null&&VisualConfig.SHADERS.get();var out=buffers.getBuffer(custom?TYPE:RenderType.lightning());var point=new org.joml.Vector3f();
        int mode=Math.clamp(Math.round((kind+Math.clamp(progress,0,1)*.8F)*51),0,255),sin=(int)((Math.sin(phase*.08)+1)*127.5),cos=(int)((Math.cos(phase*.08)+1)*127.5),alpha=Math.clamp((int)(strength*210),0,255);
        ModuleField.emit(kind,VisualConfig.reduced(),(x,y,z,u,v)->{pose.last().pose().transformPosition(x,y,z,point);var vertex=out.addVertex(point.x,point.y,point.z);if(custom)vertex.setUv(u,v).setColor(mode,sin,cos,alpha);else vertex.setColor(kind==0?255:135,kind==0?160:190,kind==0?50:255,alpha/3);});
    }
    private ModuleShader(){}
}
