package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.DeviceTracker;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = TileEntityTransmitter.class, remap = false)
public abstract class TransmitterTickMixin {
    @WrapMethod(method = "tickServer")
    private static void overload$tick(Level level, BlockPos pos, BlockState state, TileEntityTransmitter tile, Operation<Void> original) {
        var previous=DeviceTracker.begin(tile);
        try { original.call(level,pos,state,tile); } finally { DeviceTracker.end(previous); }
    }
}
