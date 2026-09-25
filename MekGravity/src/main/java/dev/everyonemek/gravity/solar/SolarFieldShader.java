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
        if(shader==null||!dev.everyonemek.gravity.VisualConfig.SHADERS.get())return false;var v=buffers.getBuffer(TYPE);float time=(float)((phase*.03)%(Math.PI*2));
        var transformed=new org.joml.Vector3f();var matrix=pose.last().pose();
        SolarField.emit(phase,strength,size,dev.everyonemek.gravity.VisualConfig.reduced(),(A,B,width,rgb,alpha,halo)->{
            double dx=B.x()-A.x(),dy=B.y()-A.y(),dz=B.z()-A.z();
            double vx=camera.x-(A.x()+B.x())*.5,vy=camera.y-(A.y()+B.y())*.5,vz=camera.z-(A.z()+B.z())*.5;
            double nx=vy*dz-vz*dy,ny=vz*dx-vx*dz,nz=vx*dy-vy*dx;
            double length=nx*nx+ny*ny+nz*nz;
            if(length<1E-12){nx=dz;ny=0;nz=-dx;length=nx*nx+nz*nz;if(length<1E-12){nx=0;ny=-dz;nz=dy;length=ny*ny+nz*nz;}}
            if(length<1E-20)return;
            float span=halo?3:1;double scale=width*span/Math.sqrt(length);nx*=scale;ny*=scale;nz*=scale;
            for(int i=0;i<4;i++){
                var end=i==0||i==3?A:B;double sign=i<2?-1:1;
                matrix.transformPosition((float)(end.x()+nx*sign),(float)(end.y()+ny*sign),(float)(end.z()+nz*sign),transformed);
                v.addVertex(transformed.x,transformed.y,transformed.z).setUv(i<2?-span:span,(float)(end.x()*2+end.y()*3+end.z()*4)-time).setColor(rgb>>16&255,rgb>>8&255,rgb&255,alpha);
            }
        });return true;
    }
    private SolarFieldShader(){}
}
