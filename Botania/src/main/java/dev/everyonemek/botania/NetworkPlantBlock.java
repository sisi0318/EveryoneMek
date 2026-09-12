package dev.everyonemek.botania;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class NetworkPlantBlock extends BaseEntityBlock {
    private final boolean core;
    public NetworkPlantBlock(Properties properties, boolean core) {
        super(properties); this.core = core;
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(properties -> new NetworkPlantBlock(properties, core)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.FACING, context.getClickedFace().getOpposite()); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(4, 0, 4, 12, core ? 16 : 12, 12); }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return PlantSupport.canStand(level, pos); }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, other, level, pos, otherPos);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new NetworkPlant(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Content.NETWORK_TILE.get(), (world, pos, blockState, tile) -> tile.tick());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Flowers.open(player, level.getBlockEntity(pos)); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof NetworkPlant plant)
            ManaNetworks.get((net.minecraft.server.level.ServerLevel) level).removed(plant);
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof NetworkPlant plant) {
            Flowers.placed(plant, placer, stack);
            if (!stack.has(Content.STATE.get())) plant.detectPool();
        }
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return Flowers.withState(super.getDrops(state, builder), builder, this); }
}
