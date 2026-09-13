package dev.everyonemek.botania.mixin;

import appeng.api.networking.IGridConnection;
import dev.everyonemek.botania.compat.ae2.SparkMeNetworks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.me.GridConnection", remap = false)
public abstract class SparkMeConnectionCapacityMixin {
    @Inject(method = "getMaxChannels", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$dedicatedCapacity(CallbackInfoReturnable<Integer> result) {
        result.setReturnValue(SparkMeNetworks.connectionCapacity((IGridConnection) this, result.getReturnValue()));
    }
}
