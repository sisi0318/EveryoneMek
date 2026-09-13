package dev.everyonemek.botania.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appbot.item.ManaCellItem", remap = false)
public abstract class AppliedBotanicsCellCapacityMixin {
    @Inject(method = "getTotalBytes", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$binaryTiers(CallbackInfoReturnable<Long> result) { result.setReturnValue(result.getReturnValue() / 1000 * 1024); }
}
