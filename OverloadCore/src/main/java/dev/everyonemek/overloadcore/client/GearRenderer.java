package dev.everyonemek.overloadcore.client;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.gear.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Local effects on the native Meka-Tool and bounded combat visuals; no replacement item renderer or model. */
@EventBusSubscriber(modid=OverloadCore.ID,value=Dist.CLIENT)
public final class GearRenderer {
    private static ShaderInstance shader;
    private static final RenderType HELD_EFFECT=type(true,false),WORLD_EFFECT=type(true,true),WORLD_BODY=type(false,true);
    private static final RenderType WORLD_FALLBACK=fallback(false),WORLD_BODY_FALLBACK=fallback(true);
    private static RenderType fallback(boolean body){return RenderType.create("overloadcore_effect_fallback_"+body,DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,8192,false,false,
        RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLightningShader))
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setTransparencyState(body?RenderStateShard.NO_TRANSPARENCY:RenderStateShard.LIGHTNING_TRANSPARENCY)
            .setWriteMaskState(body?RenderStateShard.COLOR_DEPTH_WRITE:RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).setOutputState(RenderStateShard.PARTICLES_TARGET).createCompositeState(false));}
    private static final class Trace {
        final GearVisuals.Beam beam;final double received;Vec3 from;double started=Double.NaN;
        Trace(GearVisuals.Beam beam,double received){this.beam=beam;this.received=received;}
    }
    private record HeldMuzzle(org.joml.Vector3f view,org.joml.Matrix4f projection,double captured){}
    private static final HeldMuzzle[] MUZZLES=new HeldMuzzle[2];
    private static final double[] FIRED={-100,-100};
    private static final org.joml.Matrix4f INVERSE_WORLD_PROJECTION=new org.joml.Matrix4f();
    private static final ArrayDeque<Trace> TRACES=new ArrayDeque<>();
    private static final IdentityHashMap<ItemStack,Integer> CHARGING=new IdentityHashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private GearRenderer(){}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,path);}
    private static RenderType type(boolean translucent,boolean world){return RenderType.create("overloadcore_gear_"+translucent+"_"+world,DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,VertexFormat.Mode.QUADS,8192,false,false,
          RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(()->shader)).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setTransparencyState(translucent?RenderStateShard.LIGHTNING_TRANSPARENCY:RenderStateShard.NO_TRANSPARENCY)
                .setWriteMaskState(translucent?RenderStateShard.COLOR_WRITE:RenderStateShard.COLOR_DEPTH_WRITE).setCullState(RenderStateShard.NO_CULL)
                .setOutputState(world?RenderStateShard.PARTICLES_TARGET:RenderStateShard.MAIN_TARGET).createCompositeState(false));}
    @SubscribeEvent public static void shaders(RegisterShadersEvent e){shader=null;try{e.registerShader(new ShaderInstance(e.getResourceProvider(),id("gear_field"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),s->shader=s);}catch(IOException error){com.mojang.logging.LogUtils.getLogger().error("Gear shader unavailable; using baked weapons",error);}}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        GearVisuals.client=beam->{var mc=Minecraft.getInstance();if(mc.level==null||!GearVisualConfig.BEAMS.get()||beam.kind()>2||!Double.isFinite(beam.from().lengthSqr()+beam.to().lengthSqr())||beam.from().distanceToSqr(beam.to())>256*256)return;
            if(lastLevel!=mc.level){clear();lastLevel=mc.level;}
            while(TRACES.size()>=96)TRACES.removeFirst();TRACES.addLast(new Trace(beam,clock()));
            if(beam.kind()==0&&mc.player!=null&&beam.shooter()==mc.player.getId())FIRED[beam.rightHand()?0:1]=clock();};
        var helper=mekanism.api.gear.IModuleHelper.INSTANCE;
        helper.addMekaSuitModuleModels(id("models/entity/equipment_modules.obj"));
        helper.addMekaSuitModuleModelSpec("overloadcore_coupler",EquipmentModules.RESIDUAL_COUPLING,net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.addMekaSuitModuleModelSpec("overloadcore_heat_sink",EquipmentModules.get(GearUpgrade.HEAT_SINK),net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.addMekaSuitModuleModelSpec("overloadcore_magnetic",EquipmentModules.get(GearUpgrade.MAGNETIC),net.minecraft.world.entity.EquipmentSlot.LEGS);
    });}
    private static void clear(){TRACES.clear();CHARGING.clear();java.util.Arrays.fill(MUZZLES,null);java.util.Arrays.fill(FIRED,-100);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();lastLevel=null;}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        CHARGING.clear();var mc=Minecraft.getInstance();if(mc.level==null)return;
        for(var player:mc.level.players())if(player.isUsingItem()&&MekaCombat.form(player.getUseItem())!=null){CHARGING.put(player.getUseItem(),player.getTicksUsingItem());if(CHARGING.size()>=64)break;}
    }
    private static double clock(){var mc=Minecraft.getInstance();return mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);}
    private static Vec3 muzzle(GearVisuals.Beam beam){
        if(beam.kind()!=0||beam.from().distanceToSqr(beam.to())<4)return beam.from();
        var mc=Minecraft.getInstance();int sign=beam.rightHand()?1:-1;
        if(mc.player!=null&&beam.shooter()==mc.player.getId()&&mc.options.getCameraType().isFirstPerson()){
            var captured=MUZZLES[beam.rightHand()?0:1];
            if(captured==null||clock()-captured.captured()>2)return beam.from();
            var local=GearProjection.worldViewPoint(captured.view(),captured.projection(),INVERSE_WORLD_PROJECTION);
            local.rotate(mc.gameRenderer.getMainCamera().rotation());return mc.gameRenderer.getMainCamera().getPosition().add(local.x,local.y,local.z);
        }
        var forward=beam.to().subtract(beam.from()).normalize();var right=forward.cross(new Vec3(0,1,0)).normalize();
        return beam.from().add(right.scale(.3*sign)).add(0,-.22,0).add(forward.scale(1.1));
    }
    private static float phase(){var mc=Minecraft.getInstance();return mc.level==null||!GearVisualConfig.ANIMATE.get()?0:(float)((mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false))*.18%(Math.PI*2));}
    public static void renderTool(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers){
        if(context!=ItemDisplayContext.FIRST_PERSON_RIGHT_HAND&&context!=ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            &&context!=ItemDisplayContext.THIRD_PERSON_RIGHT_HAND&&context!=ItemDisplayContext.THIRD_PERSON_LEFT_HAND)return;
        var form=MekaCombat.form(stack);if(form==null)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        boolean rail=form==CombatModule.Form.RANGED,left=context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        pose.pushPose();pose.mulPose(MekaToolAnchor.nozzle(left));
        if(rail&&context.firstPerson()){
            var view=new org.joml.Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()).transformPosition(new org.joml.Vector3f());
            MUZZLES[left?1:0]=new HeldMuzzle(view,new org.joml.Matrix4f(RenderSystem.getProjectionMatrix()),clock());
        }
        if(shader!=null&&GearVisualConfig.SHADERS.get()){
            Integer ticks=CHARGING.get(stack);if(mc.player!=null&&mc.player.isUsingItem()&&mc.player.getUseItem()==stack)ticks=mc.player.getTicksUsingItem();
            float charge=ticks==null?0:Math.clamp((ticks+mc.getTimer().getGameTimeDeltaPartialTick(false))/MekaCombat.chargeTicks(stack),0,1);
            float power=MekaCombat.energy(stack)>0?1:0;
            var output=sink(buffers.getBuffer(HELD_EFFECT),pose,true,phase());
            GearEffectGeometry.toolForm(output,rail,power,charge);
            float age=(float)(clock()-FIRED[left?1:0]);
            if(rail&&context.firstPerson()&&GearVisualConfig.BEAMS.get()&&age>=0&&age<1.8F)GearEffectGeometry.muzzle(output,age);
        }
        pose.popPose();
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();
        if(mc.level==null||mc.level!=lastLevel){clear();lastLevel=mc.level;return;}
        INVERSE_WORLD_PROJECTION.set(e.getProjectionMatrix()).invert();
        double now=clock();while(!TRACES.isEmpty()&&TRACES.peekFirst().received+16<=now)TRACES.removeFirst();
        if(TRACES.isEmpty()||!GearVisualConfig.BEAMS.get())return;
        boolean custom=shader!=null&&GearVisualConfig.SHADERS.get();var buffers=mc.renderBuffers().bufferSource();
        var camera=e.getCamera().getPosition();var pose=e.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);double distance=GearVisualConfig.DISTANCE.get();
        for(int pass=0;pass<2;pass++){
        boolean body=pass==0;var type=body?(custom?WORLD_BODY:WORLD_BODY_FALLBACK):(custom?WORLD_EFFECT:WORLD_FALLBACK);var out=buffers.getBuffer(type);
        for(var trace:TRACES){var beam=trace.beam;
            if(body&&beam.kind()!=0)continue;
            if(Double.isNaN(trace.started)){trace.started=now;trace.from=muzzle(beam);}
            Vec3 delta=beam.to().subtract(trace.from);double length=delta.length();if(length<.01)continue;
            double nearest=Math.clamp(camera.subtract(trace.from).dot(delta)/(length*length),0,1);
            if(camera.distanceToSqr(trace.from.add(delta.scale(nearest)))>distance*distance)continue;
            var direction=delta.scale(1/length);pose.pushPose();pose.translate(trace.from.x,trace.from.y,trace.from.z);
            pose.mulPose(new org.joml.Quaternionf().rotationTo(0,0,1,(float)direction.x,(float)direction.y,(float)direction.z));
            boolean localGun=custom&&beam.kind()==0&&mc.player!=null&&beam.shooter()==mc.player.getId()&&mc.options.getCameraType().isFirstPerson();
            var target=sink(out,pose,custom,phase());
            GearEffectGeometry.shot((x,y,z,u,v,mat,power,a)->{if((mat==8)==body)target.vertex(x,y,z,u,v,mat,power,a);},beam.kind(),(float)length,(float)(now-trace.started),beam.impact(),!localGun);pose.popPose();
        }
        buffers.endBatch(type);
        }
        pose.popPose();
    }
    private static GearEffectGeometry.Sink sink(VertexConsumer out,PoseStack pose,boolean custom,float phase){
        return (x,y,z,u,v,material,power,alpha)->{
            var vertex=out.addVertex(pose.last().pose(),x,y,z);int a=(int)(255*Math.clamp(alpha,0,1));
            if(custom)vertex.setUv(u,v).setColor(material,(int)(power*255),(int)(phase/(2*Math.PI)*255),a).setNormal(0,1,0);
            else if(material==8){int shade=(int)(65+110*power);vertex.setColor(shade,shade+8,shade+10,255);}
            else vertex.setColor(material==6?255:100,220,material==6?125:190,a);
        };
    }
}
