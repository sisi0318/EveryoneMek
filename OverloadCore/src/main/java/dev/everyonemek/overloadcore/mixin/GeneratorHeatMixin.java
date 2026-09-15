package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.*;
import mekanism.api.*;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(targets = "mekanism.generators.common.tile.TileEntityHeatGenerator", remap = false)
public abstract class GeneratorHeatMixin {
    @WrapOperation(method = "simulate", at = @At(value = "INVOKE", target = "Lmekanism/common/capabilities/energy/BasicEnergyContainer;insert(JLmekanism/api/Action;Lmekanism/api/AutomationType;)J"))
    private long overload$generate(BasicEnergyContainer storage, long amount, Action action, AutomationType automation, Operation<Long> original) {
        return GenerationHooks.insert(DeviceScope.device((BlockEntity)(Object)this), amount, action, automation, value -> original.call(storage,value,action,automation));
    }
}
