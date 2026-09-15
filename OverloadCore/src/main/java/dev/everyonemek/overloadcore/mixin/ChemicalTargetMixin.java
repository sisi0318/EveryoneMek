package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.TransportHooks;
import mekanism.api.Action;
import mekanism.api.chemical.*;
import mekanism.common.content.network.distribution.ChemicalHandlerTarget;
import mekanism.common.lib.distribution.SplitInfo;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = ChemicalHandlerTarget.class, remap = false)
public abstract class ChemicalTargetMixin {
    @WrapMethod(method = "acceptAmount(Lmekanism/api/chemical/IChemicalHandler;Lmekanism/common/lib/distribution/SplitInfo;Lmekanism/api/chemical/ChemicalStack;J)V")
    private void overload$deliver(IChemicalHandler handler, SplitInfo split, ChemicalStack resource, long amount, Operation<Void> original) {
        long allowed = TransportHooks.limit(handler, amount); long before = split.getTotalSent();
        original.call(handler, split, resource, allowed);
        TransportHooks.accepted(handler, Math.max(0, split.getTotalSent() - before));
    }
}
