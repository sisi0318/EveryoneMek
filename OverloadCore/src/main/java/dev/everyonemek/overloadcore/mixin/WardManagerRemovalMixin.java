package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PersistentEntitySectionManager.class)
public abstract class WardManagerRemovalMixin {
    @Inject(method = {"stopTracking", "stopTicking"}, at = @At("HEAD"), cancellable = true)
    private void overload$untrack(EntityAccess entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player && ((WardServerLevelAccess)player.serverLevel()).overload$manager() == (Object)this
              && ThunderWard.untracking(player)) ci.cancel();
    }
}
