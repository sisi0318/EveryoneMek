package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.*;
import mekanism.api.*;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = BasicEnergyContainer.class, remap = false)
public abstract class ManualEnergyMixin {
    @WrapMethod(method = "extract")
    private long overload$extract(long amount, Action action, AutomationType automation, Operation<Long> original) {
        var context = RecipeHooks.current();
        if (!CoreConfig.WORK.get() || amount <= 0 || automation != AutomationType.INTERNAL || context != null && context.paying
              || !((Object) this instanceof MachineEnergyOwner owner) || owner.overload$tile() != DeviceTracker.ticking()
              || !DeviceScope.supported(owner.overload$tile())) return original.call(amount, action, automation);
        var bearer = DeviceScope.bearer(owner.overload$tile());
        if (bearer == null) return original.call(amount, action, automation);
        long paid = original.call(mekanism.api.math.MathUtils.multiplyClamped(amount, 2), action, automation);
        long useful = paid / 2;
        if (action.execute() && paid > 0) { CoreBinding.recover(bearer, paid - useful); DeviceTracker.worked(owner.overload$tile()); }
        return useful;
    }
}
