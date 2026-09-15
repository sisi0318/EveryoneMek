package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.TransportHooks;
import mekanism.common.content.network.EnergyNetwork;
import mekanism.common.lib.transmitter.DynamicNetwork;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = EnergyNetwork.class, remap = false)
public abstract class EnergyNetworkMixin {
    @WrapMethod(method = "tickEmit")
    private long overload$emit(long amount, Operation<Long> original) {
        var context = TransportHooks.begin((DynamicNetwork<?, ?, ?>)(Object)this);
        try { return original.call(amount); } finally { TransportHooks.end(context); }
    }
}
