package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.common.block.properties.clibano.ClibanoCenterType;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import dev.everyonemek.forbidden.mixin.ClibanoAccess;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** The outward face uses the native center model, so its edge pixels meet the surrounding masonry. */
public final class ClibanoControllerBlock extends MachineBlock {
    public enum Shell implements StringRepresentable {
        STANDALONE, SIDE, FRONT_OFF, FRONT_FIRE, FRONT_SOUL_FIRE, FRONT_ENCHANTED_FIRE;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Shell> SHELL = EnumProperty.create("shell", Shell.class);
    public ClibanoControllerBlock(Machine<Controller> type) {
        super(MachineKind.CLIBANO, type);
        registerDefaultState(defaultBlockState().setValue(SHELL, Shell.STANDALONE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SHELL);
    }
    public static void updateAppearance(Controller controller, ClibanoMainBlockEntity main) {
        Shell shell = Shell.STANDALONE;
        if (main != null && controller.binding.embedded) {
            var pos = controller.getBlockPos(); var center = main.getBlockPos();
            Direction outward = Direction.fromDelta(pos.getX() - center.getX(), 0, pos.getZ() - center.getZ());
            if (controller.getDirection() != outward) controller.setFacing(outward);
            shell = Shell.SIDE;
            if (outward == ((ClibanoAccess) main).forbiddenmekanism$front()) {
                shell = controller.observed[1] <= 0 ? Shell.FRONT_OFF : switch (controller.observed[7]) {
                    case 1 -> Shell.FRONT_SOUL_FIRE;
                    case 2 -> Shell.FRONT_ENCHANTED_FIRE;
                    default -> Shell.FRONT_FIRE;
                };
            }
        }
        var state = controller.getBlockState();
        if (state.getValue(SHELL) != shell) controller.getLevel().setBlockAndUpdate(controller.getBlockPos(), state.setValue(SHELL, shell));
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Shell shell = state.getValue(SHELL);
        if (shell == Shell.STANDALONE || shell == Shell.SIDE) return;
        var nativeState = ModBlocks.CLIBANO_CENTER.get().defaultBlockState()
              .setValue(ModBlockStateProperties.CLIBANO_CENTER_TYPE, ClibanoCenterType.valueOf(shell.name()))
              .setValue(BlockStateProperties.FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        ModBlocks.CLIBANO_CENTER.get().animateTick(nativeState, level, pos, random);
    }
}
