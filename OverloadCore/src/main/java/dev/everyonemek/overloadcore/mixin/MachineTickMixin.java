package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.*;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class MachineTickMixin {
    @WrapMethod(method = "tickServer")
    private static void overload$tick(Level level, BlockPos pos, BlockState state, TileEntityMekanism tile, Operation<Void> original) {
        var previous = DeviceTracker.begin(tile);
        try { original.call(level, pos, state, tile); } finally { DeviceTracker.end(previous); }
    }
}
