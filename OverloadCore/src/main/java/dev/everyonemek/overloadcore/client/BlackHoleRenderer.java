package dev.everyonemek.overloadcore.client;

import com.mojang.blaze3d.vertex.*;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.gear.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid=OverloadCore.ID,value=Dist.CLIENT)
public final class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final RenderType FALLBACK=RenderType.create("overloadcore_black_hole_fallback",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,32768,false,false,
        RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLightningShader))
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
    private static final RenderType FALLBACK_DISC=RenderType.create("overloadcore_black_hole_disc_fallback",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,16384,false,true,
        RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLightningShader))
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
    public BlackHoleRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent e){
        e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            @Override public boolean applyForgeHandTransform(PoseStack pose,net.minecraft.client.player.LocalPlayer player,net.minecraft.world.entity.HumanoidArm arm,net.minecraft.world.item.ItemStack stack,float partial,float equip,float swing){
                int sign=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
                float charge=player.isUsingItem()&&player.getUseItem()==stack?Math.clamp((player.getTicksUsingItem()+partial)/CoreConfig.BLACK_HOLE_CHARGE.get(),0,1):0;
                pose.translate(sign*(.5-.10*charge),-.45-.6*equip+.055*charge,-.72-.08*charge);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-8*(float)Math.sin(Math.sqrt(swing)*Math.PI)));
                return true;
            }
            @Override public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.InteractionHand hand,net.minecraft.world.item.ItemStack stack){
                return entity.isUsingItem()&&entity.getUsedItemHand()==hand?net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD:null;
            }
        },CoreContent.BLACK_HOLE_LAUNCHER.get());
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(BlackHoleEntity.TYPE.get(),BlackHoleRenderer::new);}
    @Override public boolean shouldRender(BlackHoleEntity hole,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){
        if(!hole.shouldRender(x,y,z))return false;
        float radius=BlackHoleOptics.displayRadius(Math.max(hole.visualRadius(0),hole.visualRadius(1)),hole.isOpen());
        return hole.noCulling||frustum.isVisible(hole.getBoundingBox().inflate(radius*BlackHoleGeometry.LENS_REACH+.5));
    }
    @Override public void render(BlackHoleEntity hole,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(hole.distanceToSqr(entityRenderDispatcher.camera.getPosition())>GearVisualConfig.DISTANCE.get()*GearVisualConfig.DISTANCE.get())return;
        if(BlackHolePass.queue(hole,partial))return;
        float radius=BlackHoleOptics.displayRadius(hole.visualRadius(partial),hole.isOpen());
        pose.pushPose();pose.scale(radius,radius,radius);
        var core=buffers.getBuffer(FALLBACK);
        BlackHoleGeometry.emit(BlackHoleGeometry.sphere(false),(x,y,z,u,v)->core.addVertex(pose.last().pose(),x,y,z).setColor(0,0,0,255));
        pose.mulPose(BlackHoleOptics.orientation(hole.getUUID()));var disc=buffers.getBuffer(FALLBACK_DISC);
        BlackHoleGeometry.emit(BlackHoleGeometry.disc(false),(x,y,z,u,v)->disc.addVertex(pose.last().pose(),x,y,z).setColor(255,214,132,(int)(190*(1-v))));
        pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(BlackHoleEntity entity){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.screen!=null||mc.options.hideGui||!p.isAlive()||!p.isUsingItem()||!p.getUseItem().is(CoreContent.BLACK_HOLE_LAUNCHER))return;
        int progress=Math.clamp(p.getTicksUsingItem()*100/CoreConfig.BLACK_HOLE_CHARGE.get(),0,100);
        int x=e.getGuiGraphics().guiWidth()/2,y=e.getGuiGraphics().guiHeight()-74;
        var text=CoreContent.text(progress==100?"black_hole.ready":"black_hole.charging",progress);
        e.getGuiGraphics().drawCenteredString(mc.font,text,x,y,0xFFF0C7);
        e.getGuiGraphics().fill(x-40,y+12,x+40,y+15,0xB020252B);
        e.getGuiGraphics().fill(x-40,y+12,x-40+progress*80/100,y+15,0xFFE3BC76);
    }
}
