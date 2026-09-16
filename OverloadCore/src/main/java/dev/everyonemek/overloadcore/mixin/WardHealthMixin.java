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
    @WrapMethod(method = "setHealth")
    private void overload$clearHealth(float health, Operation<Void> original) {
        if ((Object)this instanceof ServerPlayer player) {
            health = ThunderWard.healthInput(player, health);
            if (ThunderWard.setter(player, health)) return;
        }
        original.call(health);
    }
    @WrapMethod(method = "getHealth")
    private float overload$paidHealthView(Operation<Float> original) {
        float health = original.call();
        return (Object)this instanceof ServerPlayer player ? ThunderWard.visibleHealth(player, health) : health;
    }
    @WrapMethod(method = "getMaxHealth")
    private float overload$maximumHealth(Operation<Float> original) {
        float maximum = original.call();
        return (Object)this instanceof ServerPlayer player ? ThunderWard.maximumHealth(player, maximum) : maximum;
    }
    @WrapMethod(method = "isAlive")
    private boolean overload$alive(Operation<Boolean> original) {
        boolean alive = original.call();
        return (Object)this instanceof ServerPlayer player ? ThunderWard.aliveView(player, alive) : alive;
    }
    @WrapMethod(method = "isDeadOrDying")
    private boolean overload$dying(Operation<Boolean> original) {
        boolean dying = original.call();
        return (Object)this instanceof ServerPlayer player ? ThunderWard.dyingView(player, dying) : dying;
    }
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void overload$deathClock(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player && ThunderWard.deathTick(player)) ci.cancel();
    }
}
