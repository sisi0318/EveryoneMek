package dev.everyonemek.gravity.solar;

import java.io.IOException;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.everyonemek.gravity.MekGravity;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Soft containment ribbons, one quad per segment; depth-tested with no transparent depth writes. */
public final class SolarFieldShader {
    private static ShaderInstance shader;
    private static final RenderType TYPE=RenderType.create("mekgravity_stellar_field",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,49152,false,false,
          RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader))
          .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
          .setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
    public static void register(RegisterShadersEvent event){shader=null;
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"stellar_field"),DefaultVertexFormat.POSITION_TEX_COLOR),loaded->shader=loaded);}
        catch(IOException error){LogUtils.getLogger().error("Could not load stellar field shader; using simple light ribbons",error);}
    }
    public static boolean draw(PoseStack pose,MultiBufferSource buffers,Vec3 camera,double phase,float strength,float size){
        if(shader==null)return false;var v=buffers.getBuffer(TYPE);float time=(float)((phase*.03)%(Math.PI*2));
        SolarField.emit(phase,strength,size,(A,B,width,rgb,alpha,halo)->{
            var a=new Vec3(A.x(),A.y(),A.z());var b=new Vec3(B.x(),B.y(),B.z());var direction=b.subtract(a);var normal=camera.subtract(a.add(b).scale(.5)).cross(direction);
            if(normal.lengthSqr()<1E-12){normal=new Vec3(0,1,0).cross(direction);if(normal.lengthSqr()<1E-12)normal=new Vec3(1,0,0).cross(direction);}
            float span=halo?3:1;var off=normal.normalize().scale(width*span);var matrix=pose.last().pose();
            Vec3[] ends={a,b,b,a};int i=0;
            for(var point:new Vec3[]{a.subtract(off),b.subtract(off),b.add(off),a.add(off)}){
                var end=ends[i];v.addVertex(matrix,(float)point.x,(float)point.y,(float)point.z)
                      .setUv(i<2?-span:span,(float)(end.x*2+end.y*3+end.z*4)-time)
                      .setColor(rgb>>16&255,rgb>>8&255,rgb&255,alpha);i++;
            }
        });return true;
    }
    private SolarFieldShader(){}
}
