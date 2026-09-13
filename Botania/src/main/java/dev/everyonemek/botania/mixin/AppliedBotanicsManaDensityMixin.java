package dev.everyonemek.botania.mixin;

import dev.everyonemek.botania.ManaCellTier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appbot.ae2.ManaKeyType", remap = false)
public abstract class AppliedBotanicsManaDensityMixin {
    @Inject(method = "getAmountPerByte", at = @At("HEAD"), cancellable = true)
    private void botanicalmekanism$cellDensity(CallbackInfoReturnable<Integer> result) { result.setReturnValue(ManaCellTier.MANA_PER_BYTE); }
}
