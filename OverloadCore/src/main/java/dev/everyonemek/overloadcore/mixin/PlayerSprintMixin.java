package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.MetalLoad;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class PlayerSprintMixin {
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void overload$sprint(boolean sprint, CallbackInfo ci) { if (sprint && (Object)this instanceof Player player && MetalLoad.blocksSprint(player)) ci.cancel(); }
}
