package dev.everyonemek.gravity.expansion;
import java.io.IOException;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.everyonemek.gravity.VisualConfig;
import mekanism.api.MekanismAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** One atlas-sampled quad per visible active node. Per-instance state lives in vertex attributes. */
public final class NodeSnapshotShader {
    private static ShaderInstance shader;
    private static final RenderType TYPE=RenderType.create("mekgravity_node_snapshot",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1024,false,true,
        RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader)).setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS,false,false)).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
    public static void register(RegisterShadersEvent e){shader=null;try{e.registerShader(new ShaderInstance(e.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("mekgravity","node_snapshot"),DefaultVertexFormat.NEW_ENTITY),s->shader=s);}catch(IOException ex){LogUtils.getLogger().error("Could not load node projection shader; using atlas preview",ex);}}
    public static void draw(NodeModule node,PoseStack pose,MultiBufferSource buffers,double phase,float strength){if(node.snapshotType<0||node.snapshotId==null||strength<.01F)return;var mc=Minecraft.getInstance();TextureAtlasSprite sprite;int tint=0xFFFFFF;
        if(node.snapshotType==2){var fluid=BuiltInRegistries.FLUID.get(node.snapshotId);var stack=new FluidStack(fluid,1);var ext=IClientFluidTypeExtensions.of(fluid);var texture=ext.getStillTexture(stack);if(texture==null)return;sprite=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);tint=ext.getTintColor(stack);}
        else if(node.snapshotType==3){var chemical=MekanismAPI.CHEMICAL_REGISTRY.get(node.snapshotId);if(chemical==null||chemical.isEmptyType())return;sprite=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(chemical.getIcon());tint=chemical.getTint();}
        else{var item=BuiltInRegistries.ITEM.get(node.snapshotId);sprite=mc.getItemRenderer().getModel(new ItemStack(item),node.getLevel(),null,0).getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);}
        boolean custom=shader!=null&&VisualConfig.SHADERS.get();var out=buffers.getBuffer(custom?TYPE:RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));var p=new org.joml.Vector3f();float sin=(float)Math.sin(phase*.055),cos=(float)Math.cos(phase*.055);int alpha=Math.clamp((int)(strength*240),0,255);
        for(int i=0;i<4;i++){float u=i<2?0:1,v=i==0||i==3?1:0;pose.last().pose().transformPosition((u-.5F)*.54F,(.5F-v)*.54F,-.332F,p);var vertex=out.addVertex(p.x,p.y,p.z).setColor(tint>>16&255,tint>>8&255,tint&255,alpha).setUv(u==0?sprite.getU0():sprite.getU1(),v==0?sprite.getV0():sprite.getV1());
            if(custom)vertex.setUv1((int)(u*256),(int)(v*256)).setUv2(0,node.getBlockPos().hashCode()&255).setNormal(cos,sin,node.snapshotType/3F);else vertex.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0,0,-1);
        }
    }
    private NodeSnapshotShader(){}
}
