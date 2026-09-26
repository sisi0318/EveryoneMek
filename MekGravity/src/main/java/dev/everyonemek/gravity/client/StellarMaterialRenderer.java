package dev.everyonemek.gravity.client;
import java.io.IOException;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.everyonemek.gravity.VisualConfig;
import dev.everyonemek.gravity.solar.*;
import dev.everyonemek.gravity.expansion.ModuleContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Static small meshes; all per-item phase, material and remaining fuel are carried by vertices. */
public final class StellarMaterialRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String[] NAMES={"compressed_stellar_matter","stellar_fuel_preform","stellar_fuel","stellar_alloy","stellar_fuel_capsule"};
    private static ShaderInstance shader;
    private static final RenderType TYPE=RenderType.create("mekgravity_stellar_material",DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,VertexFormat.Mode.QUADS,16384,false,false,RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader)).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).createCompositeState(false));
    private static ModelResourceLocation model(int kind){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("mekgravity","item/"+NAMES[kind]+"_fallback"));}
    private StellarMaterialRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static void models(ModelEvent.RegisterAdditional e){for(int i=0;i<NAMES.length;i++)e.register(model(i));}
    public static void register(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){private StellarMaterialRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new StellarMaterialRenderer();return renderer;}},SolarContent.COMPRESSED.get(),SolarContent.PREFORM.get(),SolarContent.FUEL.get(),ModuleContent.ALLOY.get(),SolarContent.CAPSULE.get());}
    public static void shaders(RegisterShadersEvent e){shader=null;try{e.registerShader(new ShaderInstance(e.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("mekgravity","stellar_material"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),s->shader=s);}catch(IOException ex){LogUtils.getLogger().error("Stellar material shader unavailable; using baked models",ex);}}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){int kind=stack.is(SolarContent.COMPRESSED.get())?0:stack.is(SolarContent.PREFORM.get())?1:stack.is(SolarContent.FUEL.get())?2:stack.is(ModuleContent.ALLOY.get())?3:4;var mc=Minecraft.getInstance();
        if(shader==null||!VisualConfig.SHADERS.get()){mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),SolarContent.CONTROLLER.get().defaultBlockState(),mc.getModelManager().getModel(model(kind)),1,1,1,light,overlay,ModelData.EMPTY,null);return;}
        float charge=1;if(kind==4){var tag=stack.get(SolarContent.FUEL_DATA.get());charge=tag!=null&&SolarFuelRecipe.validReserve(tag)?(float)(tag.getLong("remaining")/(double)tag.getLong("total")):0;}
        double time=mc.level==null||!VisualConfig.ANIMATE_ITEMS.get()?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);float amplitude=.15F+.85F*Math.clamp(charge,0,1),cos=(float)Math.cos(time*.022)*amplitude,sin=(float)Math.sin(time*.022)*amplitude;
        var mesh=StellarMaterialMesh.vertices(kind,context==ItemDisplayContext.GROUND||VisualConfig.reduced());var out=buffers.getBuffer(TYPE);var point=new org.joml.Vector3f();var normal=new org.joml.Vector3f();
        for(int i=0;i<mesh.length;i+=7){float x=mesh[i],y=mesh[i+1],z=mesh[i+2];pose.last().pose().transformPosition(x+.5F,y+.5F,z+.5F,point);pose.last().transformNormal(mesh[i+3],mesh[i+4],mesh[i+5],normal);out.addVertex(point.x,point.y,point.z).setUv(cos,sin).setColor(Math.round((x+.5F)*255),Math.round((y+.5F)*255),Math.round((z+.5F)*255),(kind*3+(int)mesh[i+6])*17).setNormal(normal.x,normal.y,normal.z);}
    }
}
