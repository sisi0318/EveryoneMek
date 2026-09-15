package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.*;
import mekanism.common.content.network.transmitter.Transmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
@Mixin(targets = {"mekanism.common.content.network.transmitter.PressurizedTube", "mekanism.common.content.network.transmitter.UniversalCable"}, remap = false)
public abstract class LongPullMixin {
    @ModifyReturnValue(method = "getAvailablePull", at = @At("RETURN"))
    private long overload$pull(long amount) {
        var pipe=(Transmitter<?,?,?>)(Object)this; var tile=pipe.getLevel().getBlockEntity(pipe.getBlockPos());
        return CoreConfig.TRANSPORT.get() && tile != null && DeviceScope.bearer(tile) != null
              ? amount / 2 + (amount % 2 == 1 && pipe.getLevel().getGameTime() % 2 == 0 ? 1 : 0) : amount;
    }
}
