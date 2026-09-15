package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.TransportHooks;
import mekanism.common.content.network.distribution.FluidHandlerTarget;
import mekanism.common.lib.distribution.SplitInfo;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = FluidHandlerTarget.class, remap = false)
public abstract class FluidTargetMixin {
    @WrapMethod(method = "acceptAmount(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lmekanism/common/lib/distribution/SplitInfo;Lnet/neoforged/neoforge/fluids/FluidStack;J)V")
    private void overload$deliver(IFluidHandler handler, SplitInfo split, FluidStack resource, long amount, Operation<Void> original) {
        long allowed = TransportHooks.limit(handler, amount); long before = split.getTotalSent();
        original.call(handler, split, resource, allowed);
        TransportHooks.accepted(handler, Math.max(0, split.getTotalSent() - before));
    }
}
