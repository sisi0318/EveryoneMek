package dev.everyonemek.overloadcore.client;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.gear.*;
import dev.everyonemek.overloadcore.training.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=OverloadCore.ID,value=Dist.CLIENT)
public final class TacticalClient {
    private static final KeyMapping SWAP=new KeyMapping("key.overloadcore.combat_swap",net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.overloadcore");
    private static final KeyMapping GUARD=new KeyMapping("key.overloadcore.guard",net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_Z,"key.categories.overloadcore");
    private record Fx(TacticalPackets.Visual data,double received,double until){}
    private static final Map<Long,Fx> FX=new LinkedHashMap<>(32,.75F,true);
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static boolean guardHeld;
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(SWAP);e.register(GUARD);}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(TrainingContent.MENU.get(),TrainingScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(TrainingContent.TARGET.get(),TargetRenderer::new);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->TacticalPackets.client=p->{
        var mc=Minecraft.getInstance();if(mc.level==null)return;if(world!=mc.level){FX.clear();world=mc.level;}
        long key=((long)p.entity()<<3)|p.kind();if(p.ticks()==0)FX.remove(key);else{if(FX.size()>=256)FX.remove(FX.keySet().iterator().next());FX.put(key,new Fx(p,time(),time()+p.ticks()));}
    });}
    private static double time(){var mc=Minecraft.getInstance();return mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();
        if(mc.level!=world){FX.clear();world=mc.level;guardHeld=false;}if(mc.player==null)return;
        boolean allowed=mc.screen==null&&mc.isWindowActive()&&!mc.isPaused()&&mc.player.isAlive()&&!mc.player.isSpectator();
        while(SWAP.consumeClick())if(allowed){boolean off=MekaCombat.form(mc.player.getMainHandItem())==null&&MekaCombat.form(mc.player.getOffhandItem())!=null;PacketDistributor.sendToServer(new TacticalPackets.Input(0,true,off));}
        boolean down=allowed&&GUARD.isDown();if(down!=guardHeld||down&&mc.player.tickCount%8==0){guardHeld=down;PacketDistributor.sendToServer(new TacticalPackets.Input(1,down,false));}
        FX.values().removeIf(f->f.until<=time());
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){FX.clear();world=null;guardHeld=false;}
    @SubscribeEvent public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e){
        var stack=e.getItemStack();
        if(MekaCombat.form(stack)!=null||stack.is(CoreContent.COMBAT_MODULE))e.getToolTip().add(CoreContent.text("tactical.swap_hint",SWAP.getTranslatedKeyMessage()));
        if(GearEffects.level(stack,GearUpgrade.DEFLECTOR)>0||stack.is(CoreContent.UPGRADE_ITEMS.get(GearUpgrade.DEFLECTOR)))e.getToolTip().add(CoreContent.text("tactical.guard_hint",GUARD.getTranslatedKeyMessage()));
    }
    public static boolean hasWorldEffects(){return !FX.isEmpty();}
    public static void drawWorld(PoseStack pose,GearEffectGeometry.Sink sink,float partial){
        var mc=Minecraft.getInstance();if(mc.level==null)return;double now=time();var camera=mc.gameRenderer.getMainCamera();
        for(var f:FX.values()){
            if(f.until<=now||f.data.kind()>1)continue;var entity=mc.level.getEntity(f.data.entity());double distance=GearVisualConfig.DISTANCE.get();if(entity==null||!entity.getUUID().equals(f.data.identity())||!entity.isAlive()||entity.distanceToSqr(camera.getPosition())>distance*distance)continue;
            pose.pushPose();var pos=entity.getPosition(partial);
            if(f.data.kind()==0){var look=entity.getViewVector(partial);pos=pos.add(0,entity.getEyeHeight()-.35,0).add(look.scale(.8));pose.translate(pos.x,pos.y,pos.z);
                pose.mulPose(new org.joml.Quaternionf().rotationTo(0,0,1,(float)look.x,(float)look.y,(float)look.z));TacticalGeometry.guard(sink,now-f.received<f.data.value());
            }else{pos=pos.add(0,entity.getBbHeight()+.3,0);pose.translate(pos.x,pos.y,pos.z);pose.mulPose(camera.rotation());TacticalGeometry.mark(sink,f.data.value());}
            pose.popPose();
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.options.hideGui||!mc.player.isAlive())return;
        int y=e.getGuiGraphics().guiHeight()-65;
        for(int kind:new int[]{0,2,3}){var f=FX.get(((long)mc.player.getId()<<3)|kind);if(f==null||f.until<=time())continue;
            var text=CoreContent.text(kind==0?"tactical.guarding":kind==2?"tactical.counter_ready":"tactical.flux_ready");
            e.getGuiGraphics().drawString(mc.font,text,(e.getGuiGraphics().guiWidth()-mc.font.width(text))/2,y,0xFF8CE5B3,true);y-=11;
        }
    }
    private static final class TargetRenderer extends EntityRenderer<TrainingTarget>{
        TargetRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=0;}
        @Override public void render(TrainingTarget e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){GearRenderer.renderTrainingTarget(pose,buffers);super.render(e,yaw,partial,pose,buffers,light);}
        @Override public ResourceLocation getTextureLocation(TrainingTarget e){return ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"textures/item/gear_alloy.png");}
    }
}
