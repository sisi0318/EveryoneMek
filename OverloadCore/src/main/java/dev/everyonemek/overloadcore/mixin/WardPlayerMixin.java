package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(ServerPlayer.class)
public abstract class WardPlayerMixin {
    @WrapMethod(method = "hurt")
    private boolean overload$damageIncident(DamageSource source, float amount, Operation<Boolean> original) {
        var player = (ServerPlayer)(Object)this;
        ThunderWard.beginDamage(player);
        try { return original.call(source, amount); }
        finally { ThunderWard.endDamage(player); }
    }
    @Inject(method = "die", at = @At("TAIL"))
    private void overload$deathCommitted(DamageSource source, CallbackInfo ci) {
        ThunderWard.finalized((ServerPlayer)(Object)this);
    }
}
