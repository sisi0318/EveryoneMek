package dev.everyonemek.overloadcore.training;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class TrainingBlock extends BlockTile<TrainingProjector,Machine<TrainingProjector>> {
    private static final VoxelShape SHAPE=net.minecraft.world.level.block.Block.box(0,0,0,16,9.1,16);
    public TrainingBlock(Machine<TrainingProjector> type){super(type,p->p.strength(4).noOcclusion());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
}
