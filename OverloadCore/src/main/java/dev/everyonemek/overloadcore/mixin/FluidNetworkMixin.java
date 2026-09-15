package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.TransportHooks;
import mekanism.common.content.network.FluidNetwork;
import mekanism.common.lib.transmitter.DynamicNetwork;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = FluidNetwork.class, remap = false)
public abstract class FluidNetworkMixin {
    @WrapMethod(method = "tickEmit")
    private int overload$emit(FluidStack fluid, Operation<Integer> original) {
        var context = TransportHooks.begin((DynamicNetwork<?, ?, ?>)(Object)this);
        try { return original.call(fluid); } finally { TransportHooks.end(context); }
    }
}
