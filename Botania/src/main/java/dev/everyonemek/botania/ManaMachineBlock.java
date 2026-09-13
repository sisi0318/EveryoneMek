package dev.everyonemek.botania;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;

public final class ManaMachineBlock extends BlockTile<ManaMachine, Machine<ManaMachine>> {
    public final ManaMachineKind kind;
    public ManaMachineBlock(ManaMachineKind kind, Machine<ManaMachine> type) {
        super(type, properties -> properties.strength(4, 12).noOcclusion()); this.kind = kind;
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state,
          net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return BotanicalMachineShapes.get(kind.id, mekanism.common.block.attribute.Attribute.getFacing(state));
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
          net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
          net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (LexiconGuide.open(player, stack, this)) return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        if (stack.is(vazkii.botania.common.item.BotaniaItems.WAND_OF_THE_FOREST)) {
            if (!mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(player, level, pos, level.getBlockEntity(pos))) return net.minecraft.world.ItemInteractionResult.FAIL;
            var binding = vazkii.botania.common.item.WandOfTheForestItem.getBindingAttempt(stack);
            if (player.isSecondaryUseActive() && binding.isPresent() && binding.get().dimension().equals(level.dimension())
                  && !binding.get().pos().equals(pos) && level.hasChunkAt(binding.get().pos())
                  && vazkii.botania.api.block.WandBindable.LOOKUP.find(level, binding.get().pos(),
                        vazkii.botania.common.item.WandOfTheForestItem.getBindingSide(stack).orElse(null)) != null) {
                stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
            }
            // Consume this interaction before Mek treats the wand as a dismantling wrench.
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind.chemical && stack.getItem() instanceof vazkii.botania.common.item.ManaSparkItem) {
            if (!mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(player, level, pos, level.getBlockEntity(pos))) return net.minecraft.world.ItemInteractionResult.FAIL;
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
