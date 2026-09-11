package dev.everyonemek.forbidden;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;

public class MachineBlock extends BlockTile<Controller, Machine<Controller>> {
    public final MachineKind kind;
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
          net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof Controller controller)
            ClibanoEmbedding.remove(controller);
        super.onRemove(state, level, pos, replacement, moving);
    }
    public MachineBlock(MachineKind kind, Machine<Controller> type) {
        super(type, properties -> properties.strength(4, 12));
        this.kind = kind;
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
          net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
          net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
          net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (kind.forge() && stack.getItem() instanceof ForgeTierInstallerItem installer && level.getBlockEntity(pos) instanceof Controller machine)
            return installer.install(machine, player, stack);
        if (!kind.forge() && stack.is(com.stal111.forbidden_arcanus.core.init.ModItems.MUNDABITUR_DUST.get())
              && level.getBlockEntity(pos) instanceof Controller machine)
            return ClibanoEmbedding.activate(machine, player, hand, hit);
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
