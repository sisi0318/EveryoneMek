package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.common.block.AgronomicSourcelinkBlock;
import com.hollingsworth.arsnouveau.common.block.TickableModBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FeSourcelinkBlock extends TickableModBlock {
    public FeSourcelinkBlock() { super(defaultProperties().noOcclusion().requiresCorrectToolForDrops()); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeSourcelinkBlockEntity(pos, state);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AgronomicSourcelinkBlock.shape;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FeSourcelinkBlockEntity tile) {
            player.displayClientMessage(tile.energyText().append(" · ").append(tile.statusText()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("description.arsmekanism.fe_sourcelink"));
        int stored = Math.clamp(stack.getOrDefault(Content.FE_ENERGY, 0), 0, FeSourcelinkBlockEntity.ENERGY_CAPACITY);
        if (stored > 0) tooltip.add(Component.translatable("gui.arsmekanism.fe_energy", stored, FeSourcelinkBlockEntity.ENERGY_CAPACITY));
    }
}
