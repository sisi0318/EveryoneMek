package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.*;
import mekanism.api.*;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.lib.multiblock.MultiblockData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(targets = "mekanism.generators.common.content.turbine.TurbineMultiblockData", remap = false)
public abstract class GeneratorTurbineMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lmekanism/api/energy/IEnergyContainer;insert(JLmekanism/api/Action;Lmekanism/api/AutomationType;)J"))
    private long overload$generate(IEnergyContainer storage, long amount, Action action, AutomationType automation, Operation<Long> original) {
        return GenerationHooks.insert(DeviceScope.device((MultiblockData)(Object)this), amount, action, automation, value -> original.call(storage,value,action,automation));
    }
}
