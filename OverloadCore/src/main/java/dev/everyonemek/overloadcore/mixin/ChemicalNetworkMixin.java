package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.TransportHooks;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.lib.transmitter.DynamicNetwork;
import mekanism.api.chemical.ChemicalStack;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = ChemicalNetwork.class, remap = false)
public abstract class ChemicalNetworkMixin {
    @WrapMethod(method = "tickEmit")
    private long overload$emit(ChemicalStack chemical, Operation<Long> original) {
        var context = TransportHooks.begin((DynamicNetwork<?, ?, ?>)(Object)this);
        try { return original.call(chemical); } finally { TransportHooks.end(context); }
    }
}
