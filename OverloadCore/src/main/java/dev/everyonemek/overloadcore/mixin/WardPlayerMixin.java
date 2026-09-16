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
    @WrapMethod(method = "die")
    private void overload$deathBoundary(DamageSource source, Operation<Void> original) {
        // Vanilla totems have already been checked before this call. Stop before death packets, loot and game events.
        if (!ThunderWard.rescue((ServerPlayer)(Object)this)) original.call(source);
    }
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
    @WrapMethod(method = "changeDimension")
    private net.minecraft.world.entity.Entity overload$dimension(net.minecraft.world.level.portal.DimensionTransition transition,
          Operation<net.minecraft.world.entity.Entity> original) {
        var player = (ServerPlayer)(Object)this;
        dev.everyonemek.overloadcore.WardRuntime.clearShield(player);
        ThunderWard.beginLifecycle(player);
        try { return original.call(transition); }
        finally { ThunderWard.endLifecycle(player); }
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void overload$inspectBeforeTick(CallbackInfo ci) { ThunderWard.inspect((ServerPlayer)(Object)this); }
    @Inject(method = "tick", at = @At("TAIL"))
    private void overload$inspectAfterTick(CallbackInfo ci) { ThunderWard.inspect((ServerPlayer)(Object)this); }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void overload$loadedState(net.minecraft.nbt.CompoundTag data, CallbackInfo ci) {
        // Only an already tracked player can pay; ordinary disk loading/respawn is not a resurrection trigger.
        ThunderWard.inspect((ServerPlayer)(Object)this);
    }
}
