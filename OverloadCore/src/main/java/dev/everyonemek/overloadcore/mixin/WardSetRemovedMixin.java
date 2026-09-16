package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(Entity.class)
public abstract class WardSetRemovedMixin {
    @Inject(method = "onRemovedFromLevel", at = @At("HEAD"), cancellable = true)
    private void overload$removedFromLevel(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player && ThunderWard.untracking(player)) ci.cancel();
    }
    @WrapMethod(method = "isRemoved")
    private boolean overload$removedView(Operation<Boolean> original) {
        boolean removed = original.call();
        return (Object)this instanceof ServerPlayer player ? ThunderWard.removedView(player, removed) : removed;
    }
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void overload$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player && ThunderWard.removal(player, reason)) ci.cancel();
    }
}
