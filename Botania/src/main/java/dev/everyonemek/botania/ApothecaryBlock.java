package dev.everyonemek.botania;

import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public final class ApothecaryBlock extends BlockTile<MechanicalApothecary, Machine<MechanicalApothecary>> {
    public ApothecaryBlock(Machine<MechanicalApothecary> type) { super(type, properties -> properties.strength(4, 12)); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.getFluidHandler(stack).isPresent()) {
            if (!IBlockSecurityUtils.INSTANCE.canAccess(player, level, pos, level.getBlockEntity(pos))) return ItemInteractionResult.FAIL;
            if (level.isClientSide || FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
