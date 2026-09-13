package dev.everyonemek.botania.mixin;

import appeng.api.util.KeyTypeSelectionHost;
import dev.everyonemek.botania.compat.ae2.ManaVisibilityDefaults;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "appeng.parts.reporting.AbstractTerminalPart", remap = false)
public abstract class ManaTerminalDefaultsMixin {
    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void botanicalmekanism$showManaInitially(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        if (!tag.getBoolean(ManaVisibilityDefaults.MARKER)) ManaVisibilityDefaults.enableMana(((KeyTypeSelectionHost) this).getKeyTypeSelection());
    }
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void botanicalmekanism$rememberChoice(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        tag.putBoolean(ManaVisibilityDefaults.MARKER, true);
    }
}
