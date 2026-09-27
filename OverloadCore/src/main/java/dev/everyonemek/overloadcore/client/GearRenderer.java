package dev.everyonemek.overloadcore.client;

import java.io.IOException;
import java.util.ArrayDeque;
import com.mojang.blaze3d.vertex.*;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.gear.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Baked casing plus small immutable glow meshes. No particles/entities or per-item shader uniform writes. */
@EventBusSubscriber(modid=OverloadCore.ID,value=Dist.CLIENT)
public final class GearRenderer extends BlockEntityWithoutLevelRenderer {
    private static ShaderInstance shader;
    private static final RenderType SOLID=type(false),BEAM=type(true);
    private record Trace(GearVisuals.Beam beam,long expires){}
    private static final ArrayDeque<Trace> TRACES=new ArrayDeque<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private GearRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,path);}
    private static ModelResourceLocation model(boolean rail,String suffix){return ModelResourceLocation.standalone(id("item/"+(rail?"rail_lance":"thunder_blade")+suffix));}
    private static RenderType type(boolean translucent){return RenderType.create("overloadcore_gear_"+translucent,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,VertexFormat.Mode.QUADS,8192,false,translucent,
          RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader)).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setTransparencyState(translucent?RenderStateShard.LIGHTNING_TRANSPARENCY:RenderStateShard.NO_TRANSPARENCY)
                .setWriteMaskState(translucent?RenderStateShard.COLOR_WRITE:RenderStateShard.COLOR_DEPTH_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));}
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){for(boolean rail:new boolean[]{true,false}){e.register(model(rail,"_base"));e.register(model(rail,"_fallback"));}}
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){private GearRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new GearRenderer();return renderer;}},CoreContent.RAILGUN.get(),CoreContent.BLADE.get());}
    @SubscribeEvent public static void shaders(RegisterShadersEvent e){shader=null;try{e.registerShader(new ShaderInstance(e.getResourceProvider(),id("gear_field"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),s->shader=s);}catch(IOException error){com.mojang.logging.LogUtils.getLogger().error("Gear shader unavailable; using baked weapons",error);}}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        GearVisuals.client=beam->{var mc=Minecraft.getInstance();if(mc.level==null||!GearVisualConfig.BEAMS.get()||!Double.isFinite(beam.from().lengthSqr()+beam.to().lengthSqr())||beam.from().distanceToSqr(beam.to())>256*256)return;
            if(lastLevel!=mc.level){TRACES.clear();lastLevel=mc.level;}while(TRACES.size()>=96)TRACES.removeFirst();TRACES.addLast(new Trace(beam,mc.level.getGameTime()+8));};
        var helper=mekanism.api.gear.IModuleHelper.INSTANCE;
        helper.addMekaSuitModuleModels(id("models/entity/equipment_modules.obj"));
        helper.addMekaSuitModuleModelSpec("overloadcore_coupler",EquipmentModules.RESIDUAL_COUPLING,net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.addMekaSuitModuleModelSpec("overloadcore_heat_sink",EquipmentModules.get(GearUpgrade.HEAT_SINK),net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.addMekaSuitModuleModelSpec("overloadcore_magnetic",EquipmentModules.get(GearUpgrade.MAGNETIC),net.minecraft.world.entity.EquipmentSlot.LEGS);
    });}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){TRACES.clear();lastLevel=null;}
    private static float phase(){var mc=Minecraft.getInstance();return mc.level==null||!GearVisualConfig.ANIMATE.get()?0:(float)((mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false))*.18%(Math.PI*2));}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        boolean rail=stack.is(CoreContent.RAILGUN);var mc=Minecraft.getInstance();boolean custom=shader!=null&&GearVisualConfig.SHADERS.get();
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),Blocks.IRON_BLOCK.defaultBlockState(),mc.getModelManager().getModel(model(rail,custom?"_base":"_fallback")),1,1,1,light,overlay,ModelData.EMPTY,null);
        if(!custom)return;
        float[] mesh=rail?GearGlowMesh.RAIL:GearGlowMesh.BLADE;float strength=(float)Math.clamp(GearEnergy.stored(stack)/(double)Math.max(1,GearEnergy.capacity(stack)),0,1);float phase=phase();
        if(mc.player!=null&&mc.player.isUsingItem()&&mc.player.getUseItem()==stack&&stack.getItem() instanceof WeaponItem weapon)
            strength=Math.min(1,strength+.6F*Math.clamp(mc.player.getTicksUsingItem()/(float)weapon.chargeTicks(stack),0,1));
        var out=buffers.getBuffer(SOLID);var point=new org.joml.Vector3f();var normal=new org.joml.Vector3f();
        for(int i=0;i<mesh.length;i+=6){pose.last().pose().transformPosition(mesh[i],mesh[i+1],mesh[i+2],point);pose.last().transformNormal(mesh[i+3],mesh[i+4],mesh[i+5],normal);
            out.addVertex(point.x,point.y,point.z).setUv(mesh[i+1]+mesh[i+2],phase).setColor(rail?0:1,(int)(strength*255),0,255).setNormal(normal.x,normal.y,normal.z);}
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.options.hideGui||mc.player.isSpectator())return;
        var stack=mc.player.getMainHandItem();if(!(stack.getItem() instanceof WeaponItem weapon))return;var g=e.getGuiGraphics();int x=g.guiWidth()/2,y=g.guiHeight()/2+16;
        if(mc.player.isUsingItem()&&mc.player.getUseItem()==stack){float progress=Math.clamp(mc.player.getTicksUsingItem()/(float)weapon.chargeTicks(stack),0,1);g.fill(x-30,y,x+30,y+3,0xA0222430);g.fill(x-30,y,x-30+(int)(60*progress),y+3,progress>=1?0xFFB6FFF2:0xFFAA88DD);}
        if(weapon.rail){var text=CoreContent.text("weapon.magazine",GearCombat.ammo(stack),GearCombat.magazine(stack));g.drawString(mc.font,text,x-mc.font.width(text)/2,y+7,0xFFD9E7E4,false);}
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();
        if(mc.level==null||mc.level!=lastLevel){TRACES.clear();lastLevel=mc.level;return;}
        long now=mc.level.getGameTime();while(!TRACES.isEmpty()&&TRACES.peekFirst().expires<=now)TRACES.removeFirst();
        if(TRACES.isEmpty()||!GearVisualConfig.BEAMS.get())return;
        boolean custom=shader!=null&&GearVisualConfig.SHADERS.get();var type=custom?BEAM:RenderType.lightning();var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        var camera=e.getCamera().getPosition();var pose=e.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);double distance=GearVisualConfig.DISTANCE.get();
        for(var trace:TRACES){var beam=trace.beam;if(Math.min(camera.distanceToSqr(beam.from()),camera.distanceToSqr(beam.to()))>distance*distance)continue;
            Vec3 direction=beam.to().subtract(beam.from()).normalize();if(direction.lengthSqr()<.01)continue;
            Vec3 width=direction.cross(new Vec3(0,1,0));if(width.lengthSqr()<.01)width=direction.cross(new Vec3(1,0,0));width=width.normalize().scale(.035);
            int alpha=(int)(180*Math.clamp((trace.expires-now)/8D,0,1));
            for(int plane=0;plane<2;plane++){var offset=plane==0?width:direction.cross(width);var points=new Vec3[]{beam.from().subtract(offset),beam.to().subtract(offset),beam.to().add(offset),beam.from().add(offset)};
                for(int i=0;i<4;i++){var point=points[i];var v=out.addVertex(pose.last().pose(),(float)point.x,(float)point.y,(float)point.z);
                    if(custom)v.setUv(i==1||i==2?1:0,phase()).setColor(2+Math.clamp(beam.kind(),0,2),255,0,alpha).setNormal(0,1,0);
                    else v.setColor(beam.kind()==0?110:195,190,255,alpha);}
            }
        }
        pose.popPose();buffers.endBatch(type);
    }
}
