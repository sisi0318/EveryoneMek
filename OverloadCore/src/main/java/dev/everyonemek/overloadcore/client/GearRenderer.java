package dev.everyonemek.overloadcore.client;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import com.mojang.blaze3d.vertex.*;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.gear.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
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
    private record Trace(GearVisuals.Beam beam,Vec3 from,double created){}
    private static final ArrayDeque<Trace> TRACES=new ArrayDeque<>();
    private static final IdentityHashMap<ItemStack,Integer> CHARGING=new IdentityHashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private GearRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,path);}
    private static ModelResourceLocation model(boolean rail,String suffix){return ModelResourceLocation.standalone(id("item/"+(rail?"rail_lance":"thunder_blade")+suffix));}
    private static RenderType type(boolean translucent){return RenderType.create("overloadcore_gear_"+translucent,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,VertexFormat.Mode.QUADS,8192,false,translucent,
          RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader)).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setTransparencyState(translucent?RenderStateShard.LIGHTNING_TRANSPARENCY:RenderStateShard.NO_TRANSPARENCY)
                .setWriteMaskState(translucent?RenderStateShard.COLOR_WRITE:RenderStateShard.COLOR_DEPTH_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));}
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){for(boolean rail:new boolean[]{true,false}){e.register(model(rail,"_base"));e.register(model(rail,"_fallback"));}}
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){
        private GearRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new GearRenderer();return renderer;}
        @Override public HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,InteractionHand hand,ItemStack stack){
            if(entity.isUsingItem()&&entity.getUsedItemHand()!=hand)return HumanoidModel.ArmPose.ITEM;
            if(stack.is(CoreContent.RAILGUN))return (entity.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).isEmpty()?GearArmPoses.RAIL_HOLD:GearArmPoses.RAIL_SINGLE).getValue();
            return entity.isUsingItem()&&entity.getUseItem()==stack?GearArmPoses.BLADE_READY.getValue():HumanoidModel.ArmPose.ITEM;
        }
        @Override public boolean applyForgeHandTransform(PoseStack pose,net.minecraft.client.player.LocalPlayer player,HumanoidArm arm,ItemStack stack,float partial,float equip,float swing){
            float charge=player.isUsingItem()&&player.getUseItem()==stack?Math.clamp((player.getTicksUsingItem()+partial)/((WeaponItem)stack.getItem()).chargeTicks(stack),0,1):0;
            pose.mulPose(GearPose.first(stack.is(CoreContent.RAILGUN),arm==HumanoidArm.RIGHT?1:-1,charge,equip,swing));return true;
        }
    },CoreContent.RAILGUN.get(),CoreContent.BLADE.get());}
    @SubscribeEvent public static void shaders(RegisterShadersEvent e){shader=null;try{e.registerShader(new ShaderInstance(e.getResourceProvider(),id("gear_field"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),s->shader=s);}catch(IOException error){com.mojang.logging.LogUtils.getLogger().error("Gear shader unavailable; using baked weapons",error);}}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        GearVisuals.client=beam->{var mc=Minecraft.getInstance();if(mc.level==null||!GearVisualConfig.BEAMS.get()||beam.kind()>2||!Double.isFinite(beam.from().lengthSqr()+beam.to().lengthSqr())||beam.from().distanceToSqr(beam.to())>256*256)return;
            if(lastLevel!=mc.level){TRACES.clear();CHARGING.clear();lastLevel=mc.level;}
            while(TRACES.size()>=96)TRACES.removeFirst();TRACES.addLast(new Trace(beam,muzzle(beam),clock()));};
        var helper=mekanism.api.gear.IModuleHelper.INSTANCE;
        helper.addMekaSuitModuleModels(id("models/entity/equipment_modules.obj"));
        helper.addMekaSuitModuleModelSpec("overloadcore_coupler",EquipmentModules.RESIDUAL_COUPLING,net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.addMekaSuitModuleModelSpec("overloadcore_heat_sink",EquipmentModules.get(GearUpgrade.HEAT_SINK),net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.addMekaSuitModuleModelSpec("overloadcore_magnetic",EquipmentModules.get(GearUpgrade.MAGNETIC),net.minecraft.world.entity.EquipmentSlot.LEGS);
    });}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){TRACES.clear();CHARGING.clear();lastLevel=null;}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        CHARGING.clear();var mc=Minecraft.getInstance();if(mc.level==null)return;
        for(var player:mc.level.players())if(player.isUsingItem()&&player.getUseItem().getItem() instanceof WeaponItem){CHARGING.put(player.getUseItem(),player.getTicksUsingItem());if(CHARGING.size()>=64)break;}
    }
    private static double clock(){var mc=Minecraft.getInstance();return mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);}
    private static Vec3 muzzle(GearVisuals.Beam beam){
        if(beam.kind()!=0||beam.from().distanceToSqr(beam.to())<4)return beam.from();
        var mc=Minecraft.getInstance();int sign=beam.rightHand()?1:-1;
        if(mc.player!=null&&beam.shooter()==mc.player.getId()&&mc.options.getCameraType().isFirstPerson()){
            var local=GearPose.first(true,sign,1,0,0).translate(-.5F,-.5F,-.5F).transformPosition(new org.joml.Vector3f(.5F,7/16F,-.75F));
            local.rotate(mc.gameRenderer.getMainCamera().rotation());return mc.gameRenderer.getMainCamera().getPosition().add(local.x,local.y,local.z);
        }
        var forward=beam.to().subtract(beam.from()).normalize();var right=forward.cross(new Vec3(0,1,0)).normalize();
        return beam.from().add(right.scale(.3*sign)).add(0,-.22,0).add(forward.scale(1.1));
    }
    private static float phase(){var mc=Minecraft.getInstance();return mc.level==null||!GearVisualConfig.ANIMATE.get()?0:(float)((mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false))*.18%(Math.PI*2));}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        boolean rail=stack.is(CoreContent.RAILGUN);var mc=Minecraft.getInstance();boolean custom=shader!=null&&GearVisualConfig.SHADERS.get();
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),Blocks.IRON_BLOCK.defaultBlockState(),mc.getModelManager().getModel(model(rail,custom?"_base":"_fallback")),1,1,1,light,overlay,ModelData.EMPTY,null);
        if(!custom)return;
        float[] mesh=rail?GearGlowMesh.RAIL:GearGlowMesh.BLADE;float strength=(float)Math.clamp(GearEnergy.stored(stack)/(double)Math.max(1,GearEnergy.capacity(stack)),0,1);float phase=phase();
        float charge=0;
        if(context!=ItemDisplayContext.GUI&&context!=ItemDisplayContext.GROUND&&context!=ItemDisplayContext.FIXED&&stack.getItem() instanceof WeaponItem weapon){
            Integer ticks=CHARGING.get(stack);if(mc.player!=null&&mc.player.isUsingItem()&&mc.player.getUseItem()==stack)ticks=mc.player.getTicksUsingItem();
            if(ticks!=null)charge=Math.clamp((ticks+mc.getTimer().getGameTimeDeltaPartialTick(false))/weapon.chargeTicks(stack),0,1);
        }
        strength=Math.min(1,strength+.6F*charge);
        var out=buffers.getBuffer(SOLID);var point=new org.joml.Vector3f();var normal=new org.joml.Vector3f();
        for(int i=0;i<mesh.length;i+=6){pose.last().pose().transformPosition(mesh[i],mesh[i+1],mesh[i+2],point);pose.last().transformNormal(mesh[i+3],mesh[i+4],mesh[i+5],normal);
            out.addVertex(point.x,point.y,point.z).setUv(mesh[i+1]+mesh[i+2],phase).setColor(rail?0:1,(int)(strength*255),0,255).setNormal(normal.x,normal.y,normal.z);}
        if(charge>0)GearEffectGeometry.charge(sink(buffers.getBuffer(BEAM),pose,true,phase),rail,charge);
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.options.hideGui||mc.player.isSpectator())return;
        if(!mc.options.getCameraType().isFirstPerson())return;
        var stack=mc.player.isUsingItem()?mc.player.getUseItem():mc.player.getMainHandItem();if(!(stack.getItem() instanceof WeaponItem weapon))return;var g=e.getGuiGraphics();int x=g.guiWidth()/2,y=g.guiHeight()/2+16;
        if(weapon.rail){var text=CoreContent.text("weapon.magazine",GearCombat.ammo(stack),GearCombat.magazine(stack));g.drawString(mc.font,text,x-mc.font.width(text)/2,y+7,0xFFD9E7E4,false);}
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();
        if(mc.level==null||mc.level!=lastLevel){TRACES.clear();CHARGING.clear();lastLevel=mc.level;return;}
        double now=clock();while(!TRACES.isEmpty()&&TRACES.peekFirst().created+15<=now)TRACES.removeFirst();
        if(TRACES.isEmpty()||!GearVisualConfig.BEAMS.get())return;
        boolean custom=shader!=null&&GearVisualConfig.SHADERS.get();var type=custom?BEAM:RenderType.lightning();var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        var camera=e.getCamera().getPosition();var pose=e.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);double distance=GearVisualConfig.DISTANCE.get();
        for(var trace:TRACES){var beam=trace.beam;
            Vec3 delta=beam.to().subtract(trace.from);double length=delta.length();if(length<.01)continue;
            double nearest=Math.clamp(camera.subtract(trace.from).dot(delta)/(length*length),0,1);
            if(camera.distanceToSqr(trace.from.add(delta.scale(nearest)))>distance*distance)continue;
            var direction=delta.scale(1/length);pose.pushPose();pose.translate(trace.from.x,trace.from.y,trace.from.z);
            pose.mulPose(new org.joml.Quaternionf().rotationTo(0,0,1,(float)direction.x,(float)direction.y,(float)direction.z));
            GearEffectGeometry.shot(sink(out,pose,custom,phase()),beam.kind(),(float)length,(float)(now-trace.created),beam.impact());pose.popPose();
        }
        pose.popPose();buffers.endBatch(type);
    }
    private static GearEffectGeometry.Sink sink(VertexConsumer out,PoseStack pose,boolean custom,float phase){
        return (x,y,z,u,v,material,power,alpha)->{
            var vertex=out.addVertex(pose.last().pose(),x,y,z);int a=(int)(255*Math.clamp(alpha,0,1));
            if(custom)vertex.setUv(u,v).setColor(material,(int)(power*255),(int)(phase/(2*Math.PI)*255),a).setNormal(0,1,0);
            else vertex.setColor(material==2||material==6?255:100,220,material==2||material==6?125:190,a);
        };
    }
}
