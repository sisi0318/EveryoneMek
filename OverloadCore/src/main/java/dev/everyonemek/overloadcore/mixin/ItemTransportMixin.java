package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.TransportHooks;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
@Mixin(value = LogisticalTransporterBase.class, remap = false)
public abstract class ItemTransportMixin {
    @ModifyExpressionValue(method = "onUpdateServer", at = @At(value = "INVOKE", target = "Lmekanism/common/tier/TransporterTier;getSpeed()I"))
    private int overload$speed(int speed, @Local TransporterStack stack) {
        var pipe = (LogisticalTransporterBase)(Object)this;
        return TransportHooks.itemSpeed(speed, pipe.getLevel().getBlockEntity(BlockPos.of(pipe.getWorldPositionLong())), stack);
    }
}
