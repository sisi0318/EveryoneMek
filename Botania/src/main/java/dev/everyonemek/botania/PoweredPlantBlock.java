package dev.everyonemek.botania;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import vazkii.botania.api.block_entity.SpecialFlowerBlockEntity;
import vazkii.botania.common.block.flower.PoweredSpecialFlowerBlock;

public final class PoweredPlantBlock extends PoweredSpecialFlowerBlock {
    public PoweredPlantBlock(Properties properties, Supplier<BlockEntityType<? extends SpecialFlowerBlockEntity>> type) {
        super(MobEffects.MOVEMENT_SPEED, 1, properties, type);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos position) { return PlantSupport.canStand(level, position); }
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
          net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.is(vazkii.botania.common.item.BotaniaItems.WAND_OF_THE_FOREST) || stack.is(vazkii.botania.common.item.BotaniaItems.FLORAL_OBEDIENCE_STICK))
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Flowers.open(player, level.getBlockEntity(pos)); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) != null) Flowers.placed(level.getBlockEntity(pos), placer, stack);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return Flowers.withState(super.getDrops(state, builder), builder, this); }
}
