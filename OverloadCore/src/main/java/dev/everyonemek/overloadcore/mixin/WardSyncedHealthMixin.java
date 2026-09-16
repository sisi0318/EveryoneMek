package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.ThunderWard;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
@Mixin(SynchedEntityData.class)
public abstract class WardSyncedHealthMixin {
    @Shadow @Final private SyncedDataHolder entity;
    @WrapMethod(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V")
    private <T> void overload$directHealth(EntityDataAccessor<T> key, T value, boolean force, Operation<Void> original) {
        if (entity instanceof ServerPlayer player && key.equals(WardLivingAccess.overload$healthId()) && value instanceof Float health) {
            float normalized = ThunderWard.healthInput(player, health);
            if (ThunderWard.setter(player, normalized)) return;
            original.call(key, normalized, force);
        } else original.call(key, value, force);
    }
    @Inject(method = "assignValues", at = @At("TAIL"))
    private void overload$assignedHealth(List<SynchedEntityData.DataValue<?>> values, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) ThunderWard.inspect(player);
    }
}
