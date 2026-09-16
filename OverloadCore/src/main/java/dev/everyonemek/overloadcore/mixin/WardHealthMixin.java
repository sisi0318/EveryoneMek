package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(LivingEntity.class)
public abstract class WardHealthMixin {
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void overload$clearHealth(float health, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player && ThunderWard.setter(player, health)) ci.cancel();
    }
    @WrapMethod(method = "getHealth")
    private float overload$paidHealthView(Operation<Float> original) {
        float health = original.call();
        return (Object)this instanceof ServerPlayer player ? ThunderWard.visibleHealth(player, health) : health;
    }
}
