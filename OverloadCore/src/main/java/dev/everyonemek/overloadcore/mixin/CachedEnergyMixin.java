package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.*;
import java.util.function.LongSupplier;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = CachedRecipe.class, remap = false)
public abstract class CachedEnergyMixin {
    @Shadow private LongSupplier perTickEnergy;
    @Unique private IEnergyContainer overload$energy;
    @Inject(method = "setEnergyRequirements", at = @At("RETURN"))
    private void overload$requirements(LongSupplier original, IEnergyContainer energy, CallbackInfoReturnable<?> cir) {
        overload$energy = energy;
        if (energy instanceof MachineEnergyOwner owner && DeviceScope.supported(owner.overload$tile()))
            perTickEnergy = () -> CoreConfig.WORK.get() && DeviceScope.bearer(owner.overload$tile()) != null
                  ? mekanism.api.math.MathUtils.multiplyClamped(original.getAsLong(), 2) : original.getAsLong();
    }
    @WrapMethod(method = "useEnergy")
    private void overload$pay(int operations, Operation<Void> original) {
        var context = RecipeHooks.current(); long before = overload$energy == null ? 0 : overload$energy.getEnergy();
        if (context != null) context.paying = true;
        try { original.call(operations); } finally { if (context != null) context.paying = false; }
        if (overload$energy != null) RecipeHooks.paid(Math.max(0, before - overload$energy.getEnergy()));
    }
}
