package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.ManaMachine;
import dev.everyonemek.botania.client.ManaConfigContext;
import mekanism.client.gui.element.window.GuiSideConfiguration;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = GuiSideConfiguration.class, remap = false)
public abstract class ManaSideConfigMixin implements ManaConfigContext {
    @Shadow @Final private TileEntityMekanism tile;
    @Shadow private TransmissionType currentType;
    @Override public boolean botanicalmekanism$isManaMachine() { return tile instanceof ManaMachine machine && machine.kind().chemical; }
    @ModifyArg(method = "renderForeground", at = @At(value = "INVOKE", target = "Lmekanism/common/MekanismLang;translate([Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"), index = 0)
    private Object[] botanicalmekanism$manaTitle(Object[] args) {
        return botanicalmekanism$isManaMachine() && currentType == TransmissionType.CHEMICAL
              ? new Object[]{Component.translatable("gui.botanicalmekanism.mana_type")} : args;
    }
}
