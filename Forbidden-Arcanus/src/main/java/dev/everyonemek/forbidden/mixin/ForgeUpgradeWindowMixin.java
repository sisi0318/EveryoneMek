package dev.everyonemek.forbidden.mixin;

import dev.everyonemek.forbidden.client.ResourceUpgradeWindow;
import mekanism.client.gui.element.window.GuiUpgradeWindow;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiUpgradeWindow.class, remap = false)
public abstract class ForgeUpgradeWindowMixin {
    @Inject(method = "renderForeground", at = @At(value = "INVOKE",
          target = "Lmekanism/client/gui/element/scroll/GuiUpgradeScrollList;hasSelection()Z"), cancellable = true)
    private void forbiddenmekanism$resourceDetails(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if ((Object) this instanceof ResourceUpgradeWindow window && window.renderResourceDetails(graphics)) ci.cancel();
    }
}
