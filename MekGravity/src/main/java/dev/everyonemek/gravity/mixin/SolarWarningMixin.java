package dev.everyonemek.gravity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.everyonemek.gravity.client.ChromaticWarningText;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Replace only the stellar heat warning messages at the native actionbar draw call. */
@Mixin(Gui.class)
public abstract class SolarWarningMixin {
    @WrapOperation(method="renderOverlayMessage",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)I"))
    private int gravity$warning(GuiGraphics graphics,Font font,Component message,int x,int y,int width,int color,Operation<Integer> original){
        if(ChromaticWarningText.matches(message))return ChromaticWarningText.draw(graphics,font,message,x,y,width,color);
        return original.call(graphics,font,message,x,y,width,color);
    }
}
