package dev.everyonemek.gravity.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;

/**
 * 字体效果参考作者：huige233。
 * 参考 com.huige233.autism_and_insomnia.client.DreamJournalClientTooltipComponent.styleGlitchRGB。
 * 复用本仓库 OverloadCore 的独立红蓝色散实现，适配行动栏透明度、居中及窄屏宽度。
 * 危险警告直接显示全文，不使用饰品的逐行等待效果；无需安装原作者模组或 OverloadCore。
 */
public final class ChromaticWarningText {
    public static boolean matches(Component message){
        if(!(message.getContents() instanceof TranslatableContents text))return false;
        return switch(text.getKey()){
            case "mekgravity.solar.heat_warning","mekgravity.solar.heat_burning","mekgravity.solar.heat_contact"->true;
            default->false;
        };
    }

    public static int draw(GuiGraphics graphics,Font font,Component message,int x,int y,int originalWidth,int color){
        var text=message.copy().withStyle(ChatFormatting.ITALIC).getVisualOrderText();
        int width=font.width(text),alpha=color>>>24;
        // Gui.renderOverlayMessage already supplies the fade, HUD-height offset and F1 visibility.
        float scale=Math.min(1F,Math.max(1,graphics.guiWidth()-18)/(float)Math.max(1,width+8));
        long time=Util.getMillis(),phase=Math.floorMod(time,2_400L),frame=time/40L;
        boolean surge=phase<420L;
        float dx=(float)Math.sin(phase/170D)*.18F,dy=(float)Math.cos(phase/230D)*.10F;
        if(surge){dx+=noise(frame,0)*.55F;dy+=noise(frame,37)*.35F;}
        float separation=surge?1.35F:1F;
        graphics.pose().pushPose();
        try{
            graphics.pose().translate(x+originalWidth/2F,y,0);graphics.pose().scale(scale,scale,1);
            float left=-width/2F;
            int backdrop=Minecraft.getInstance().options.getBackgroundColor(0F);
            if(backdrop!=0)graphics.fill((int)Math.floor(left)-4,-3,(int)Math.ceil(left+width)+4,font.lineHeight+3,FastColor.ARGB32.multiply(backdrop,color));
            layer(graphics,font,tint(text,0),left+dx+.8F,dy+.8F,alpha*3/4);
            // Sequence Style colors must also change: otherwise the red/gold warning style
            // overrides Font.drawInBatch's color and both echoes become the same color.
            layer(graphics,font,tint(text,0x0000FF),left+dx-separation,dy-.15F,alpha);
            layer(graphics,font,tint(text,0xFF0000),left+dx+separation,dy+.15F,alpha);
            if(surge)layer(graphics,font,tint(text,0xFFFFFF),left-dx,-dy,alpha*2/5);
            layer(graphics,font,text,left+dx,dy,alpha);
        }finally{graphics.pose().popPose();}
        return x+Math.round(width*scale);
    }
    private static void layer(GuiGraphics graphics,Font font,FormattedCharSequence text,float x,float y,int alpha){
        // Font treats alpha 0..3 as unspecified/opaque, so do not draw those fading echoes.
        if(alpha<4)return;
        font.drawInBatch(text,x,y,alpha<<24|0xFFFFFF,false,graphics.pose().last().pose(),graphics.bufferSource(),Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);
    }
    private static FormattedCharSequence tint(FormattedCharSequence text,int color){
        return sink->text.accept((index,style,codePoint)->sink.accept(index,style.withColor(color),codePoint));
    }
    private static float noise(long frame,int line){long value=(frame^(line*0x9E3779B9L))*0x45D9F3BL;value^=value>>>16;return (value&1023L)/511.5F-1F;}
    private ChromaticWarningText(){}
}
