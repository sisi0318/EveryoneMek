package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class WardSetRemovedMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void overload$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player && ThunderWard.removal(player, reason)) ci.cancel();
    }
}
