package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public abstract class WardTrackingEndMixin {
    @Shadow @Final private ServerLevel this$0;
    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void overload$tracking(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player && player.serverLevel() == this$0 && ThunderWard.untracking(player)) ci.cancel();
    }
}
