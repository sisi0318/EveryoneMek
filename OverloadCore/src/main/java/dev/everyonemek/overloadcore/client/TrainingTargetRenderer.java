package dev.everyonemek.overloadcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.everyonemek.overloadcore.*;
import dev.everyonemek.overloadcore.training.TrainingTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.EntityHitResult;

/** Server-confirmed hits only; one short-lived number per projection, with ordinary depth testing. */
public final class TrainingTargetRenderer extends EntityRenderer<TrainingTarget> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"textures/item/gear_alloy.png");
    public TrainingTargetRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public void render(TrainingTarget target,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        float age=target.hitAge(partial),pulse=Math.max(0,1-age/8);
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        GearRenderer.renderTrainingTarget(pose,buffers,pulse,Math.min(1,(target.tickCount+partial)/6));pose.popPose();
        if(!Minecraft.getInstance().options.hideGui&&age<18&&target.feedbackDamage()>0&&entityRenderDispatcher.distanceToSqr(target)<=32*32){
            int alpha=(int)(255*Math.min(1,(18-age)/8));
            if(alpha>8){
                Component text=CoreContent.text("training.hit",TrainingReadout.number(target.feedbackDamage()));
                pose.pushPose();pose.translate(0,2.32+age*.018,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.scale(.024F,-.024F,.024F);
                // Font's filled background sits in front of the glyphs and can occlude later atlas batches.
                // Native text shadow separates the glyph depths without a depth-writing background rectangle.
                getFont().drawInBatch(text,-getFont().width(text)/2F,0,(alpha<<24)|0xE9FFC1,true,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);
                pose.popPose();
            }
        }
        super.render(target,yaw,partial,pose,buffers,light);
    }
    public static void hud(GuiGraphics g){
        var mc=Minecraft.getInstance();
        if(!(mc.hitResult instanceof EntityHitResult hit)||!(hit.getEntity() instanceof TrainingTarget target)||!target.isAlive())return;
        Component[] lines={CoreContent.text("training.aim"),CoreContent.text("training.last",target.hits()==0?"—":TrainingReadout.number(target.lastDamage())),CoreContent.text("training.dps",TrainingReadout.number(target.dps()))};
        int width=0;for(var line:lines)width=Math.max(width,mc.font.width(line));
        int x=Math.min(g.guiWidth()-width-12,g.guiWidth()/2+18),y=g.guiHeight()/2+16;
        g.fill(x-6,y-5,x+width+6,y+34,0xB0182420);g.fill(x-6,y-5,x-4,y+34,0xFF61C790);
        for(int i=0;i<lines.length;i++)g.drawString(mc.font,lines[i],x,y+i*11,i==0?0xFF91DDB4:0xFFE5F1E8,false);
    }
    @Override public ResourceLocation getTextureLocation(TrainingTarget target){return TEXTURE;}
}
