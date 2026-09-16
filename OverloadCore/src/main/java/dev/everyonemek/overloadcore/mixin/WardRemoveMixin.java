package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class WardRemoveMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void overload$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player && ThunderWard.removal(player, reason)) ci.cancel();
    }
}
