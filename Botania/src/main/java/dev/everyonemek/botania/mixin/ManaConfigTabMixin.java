package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.client.ManaConfigContext;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInsetElement;
import mekanism.client.gui.element.tab.GuiConfigTypeTab;
import mekanism.client.gui.element.window.GuiSideConfiguration;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiConfigTypeTab.class, remap = false)
public abstract class ManaConfigTabMixin extends GuiInsetElement<Void> {
    @Shadow @Final private TransmissionType transmission;
    @Shadow @Final private GuiSideConfiguration<?> config;
    @Shadow @Final private java.util.Map<TransmissionType, Tooltip> typeTooltips;
    protected ManaConfigTabMixin(IGuiWrapper gui) { super(null, gui, null, 0, 0, 26, 18, false); }
    private boolean botanicalmekanism$isMana() {
        return transmission == TransmissionType.CHEMICAL && ((ManaConfigContext) config).botanicalmekanism$isManaMachine();
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void botanicalmekanism$tooltip(IGuiWrapper gui, TransmissionType type, int x, int y, GuiSideConfiguration<?> config, boolean left, CallbackInfo ci) {
        if (botanicalmekanism$isMana()) typeTooltips.put(type, Tooltip.create(Component.translatable("gui.botanicalmekanism.mana_type")));
    }
    @Override protected void drawBackgroundOverlay(GuiGraphics gui) {
        if (botanicalmekanism$isMana()) dev.everyonemek.botania.client.ManaIcon.draw(gui, getButtonX() + 1, getButtonY() + 1);
        else super.drawBackgroundOverlay(gui);
    }
}
