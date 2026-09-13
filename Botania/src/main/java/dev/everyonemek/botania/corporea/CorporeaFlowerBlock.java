package dev.everyonemek.botania.corporea;

import java.util.List;
import com.mojang.serialization.MapCodec;
import dev.everyonemek.botania.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import vazkii.botania.common.item.CorporeaSparkItem;

public final class CorporeaFlowerBlock extends BaseEntityBlock {
    public CorporeaFlowerBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends CorporeaFlowerBlock> codec() { return simpleCodec(CorporeaFlowerBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CorporeaFlower(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(4, 0, 4, 12, 16, 12); }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return PlantSupport.canStand(level, pos) || level instanceof Level world && world.hasChunkAt(pos.below())
              && !world.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty() && CorporeaIntegration.cableSupport.test(world, pos.below());
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, other, level, pos, otherPos);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (LexiconGuide.open(player, stack, this)) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        if (stack.getItem() instanceof CorporeaSparkItem) {
            var tile = level.getBlockEntity(pos);
            if (!level.isClientSide && tile != null) { Flowers.claim(tile, player); if (!Flowers.owns(player, tile)) return ItemInteractionResult.FAIL; }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Flowers.open(player, level.getBlockEntity(pos)); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CorporeaFlower flower) Flowers.placed(flower, placer, stack);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return Flowers.withState(super.getDrops(state, builder), builder, this); }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof CorporeaFlower flower) flower.backend().removed();
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Content.CORPOREA_TILE.get(), (world, pos, blockState, tile) -> tile.tick());
    }
}
