package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityTickList.class)
public abstract class WardTickListMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void overload$ticking(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player && ((WardServerLevelAccess)player.serverLevel()).overload$tickList() == (Object)this
              && ThunderWard.untracking(player)) ci.cancel();
    }
}
