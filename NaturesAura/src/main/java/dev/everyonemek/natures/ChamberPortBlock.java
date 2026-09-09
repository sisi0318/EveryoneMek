package dev.everyonemek.natures;

import com.mojang.serialization.MapCodec;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.tile.component.config.DataType;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;

public final class ChamberPortBlock extends BaseEntityBlock {
    public static final BooleanProperty OUTPUT = BooleanProperty.create("output");
    public static final EnumProperty<ItemMode> ITEM_MODE = EnumProperty.create("item_mode", ItemMode.class);
    public enum ItemMode implements StringRepresentable {
        LEGACY(null), NONE(DataType.NONE), INPUT(DataType.INPUT), OUTPUT(DataType.OUTPUT),
        INPUT_OUTPUT(DataType.INPUT_OUTPUT), ENERGY(DataType.ENERGY);
        final DataType type;
        ItemMode(DataType type) { this.type = type; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        public static ItemMode of(DataType type) {
            for (var mode : values()) if (mode.type == type) return mode;
            return NONE;
        }
    }
    public static final MapCodec<ChamberPortBlock> CODEC = simpleCodec(ChamberPortBlock::new);
    public ChamberPortBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(OUTPUT, false).setValue(ITEM_MODE, ItemMode.LEGACY)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OUTPUT, ITEM_MODE); }
    public static DataType itemMode(BlockState state) {
        var mode = state.getValue(ITEM_MODE);
        return mode == ItemMode.LEGACY ? state.getValue(OUTPUT) ? DataType.OUTPUT : DataType.INPUT : mode.type;
    }
    public static BlockState withItemMode(BlockState state, DataType type) {
        return state.setValue(ITEM_MODE, ItemMode.of(type)).setValue(OUTPUT, type.canOutput());
    }
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
            boolean output = !itemMode(state).canOutput();
            level.setBlockAndUpdate(pos, withItemMode(state, output ? DataType.OUTPUT : DataType.INPUT));
            level.invalidateCapabilities(pos);
            player.displayClientMessage(Component.translatable("gui.naturesmekanism.port." + (output ? "output" : "input")), true);
        } else if (machine != null) return machine.openGui(player);
        else player.displayClientMessage(Component.translatable("gui.naturesmekanism.status.20"), true);
        return InteractionResult.CONSUME;
    }
}
