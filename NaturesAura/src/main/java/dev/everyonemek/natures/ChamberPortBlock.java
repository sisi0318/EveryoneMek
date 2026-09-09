package dev.everyonemek.natures;

import com.mojang.serialization.MapCodec;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ChamberPortBlock extends BaseEntityBlock {
    public static final BooleanProperty OUTPUT = BooleanProperty.create("output");
    public static final MapCodec<ChamberPortBlock> CODEC = simpleCodec(ChamberPortBlock::new);
    public ChamberPortBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(OUTPUT, false)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OUTPUT); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ChamberPortEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Content.CHAMBER_PORT_TILE.get(), ChamberPortEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var port = (ChamberPortEntity) level.getBlockEntity(pos);
        var machine = port == null ? null : port.controller();
        if (machine != null && !IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, level, machine.getBlockPos(), machine))
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            boolean output = !state.getValue(OUTPUT);
            level.setBlockAndUpdate(pos, state.setValue(OUTPUT, output));
            level.invalidateCapabilities(pos);
            player.displayClientMessage(Component.translatable("gui.naturesmekanism.port." + (output ? "output" : "input")), true);
        } else if (machine != null) return machine.openGui(player);
        else player.displayClientMessage(Component.translatable("gui.naturesmekanism.status.20"), true);
        return InteractionResult.CONSUME;
    }
}
