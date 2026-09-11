package dev.everyonemek.forbidden;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ClibanoPort extends BlockEntity {
    public ClibanoPort(BlockPos pos, BlockState state) { super(Content.CLIBANO_PORT_TILE.get(), pos, state); }
    public void tick() {
        if (level.getGameTime() % 10 != 0) return;
        net.minecraft.core.Direction outward = null;
        for (var side : net.minecraft.core.Direction.values()) if (ClibanoPorts.controller(this, side) != null) { outward = side; break; }
        BlockState state = getBlockState().setValue(ClibanoPortBlock.CONNECTED, outward != null);
        if (outward != null) state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, outward);
        if (state != getBlockState()) level.setBlockAndUpdate(worldPosition, state);
    }
}
