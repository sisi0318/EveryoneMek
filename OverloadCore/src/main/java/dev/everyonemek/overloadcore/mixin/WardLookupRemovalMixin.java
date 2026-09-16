package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityLookup.class)
public abstract class WardLookupRemovalMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void overload$lookup(EntityAccess entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            var manager = ((WardServerLevelAccess)player.serverLevel()).overload$manager();
            if (((WardManagerAccess)manager).overload$lookup() == (Object)this && ThunderWard.untracking(player)) ci.cancel();
        }
    }
}
