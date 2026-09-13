package dev.everyonemek.botania.mixin;

import appeng.api.networking.IGridNode;
import dev.everyonemek.botania.compat.ae2.SparkMeEndpoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.me.GridNode", remap = false)
public abstract class SparkMeNodeCapacityMixin {
    @Inject(method = "getMaxChannels", at = @At("RETURN"), cancellable = true)
    private void botanicalmekanism$dedicatedCapacity(CallbackInfoReturnable<Integer> result) {
        if (((IGridNode) this).getOwner() instanceof SparkMeEndpoint endpoint) result.setReturnValue(endpoint.maximumChannels());
    }
}
