package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.TransportHooks;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.content.network.distribution.EnergyAcceptorTarget;
import mekanism.common.lib.distribution.SplitInfo;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = EnergyAcceptorTarget.class, remap = false)
public abstract class EnergyTargetMixin {
    @WrapMethod(method = "acceptAmount(Lmekanism/api/energy/IStrictEnergyHandler;Lmekanism/common/lib/distribution/SplitInfo;Ljava/lang/Void;J)V")
    private void overload$deliver(IStrictEnergyHandler handler, SplitInfo split, Void unused, long amount, Operation<Void> original) {
        long allowed = TransportHooks.limit(handler, amount); long before = split.getTotalSent();
        original.call(handler, split, unused, allowed);
        TransportHooks.accepted(handler, Math.max(0, split.getTotalSent() - before));
    }
}
