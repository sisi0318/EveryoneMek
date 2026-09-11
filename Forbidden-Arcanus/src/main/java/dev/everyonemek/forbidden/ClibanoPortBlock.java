package dev.everyonemek.forbidden;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Ports own no resources; the corresponding controller side decides their behavior. */
public final class ClibanoPortBlock extends Block implements EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CONNECTED = net.minecraft.world.level.block.state.properties.BooleanProperty.create("connected");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty FORMED = net.minecraft.world.level.block.state.properties.BooleanProperty.create("formed");
    public ClibanoPortBlock() {
        super(Properties.of().strength(4, 12));
        registerDefaultState(stateDefinition.any().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.NORTH).setValue(CONNECTED, false).setValue(FORMED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, CONNECTED, FORMED);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, context.getClickedFace());
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return !level.isClientSide && type == Content.CLIBANO_PORT_TILE.get() ? (world, pos, blockState, tile) -> ((ClibanoPort) tile).tick() : null;
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
          java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("description.forbiddenmekanism.clibano_port"));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ClibanoPort(pos, state); }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) ClibanoEmbedding.removePart(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
