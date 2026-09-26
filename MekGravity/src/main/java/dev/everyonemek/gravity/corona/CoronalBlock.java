package dev.everyonemek.gravity.corona;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
/** The visible dock sits against the wing rather than filling the complete anchor block. */
public final class CoronalBlock extends BlockTile<CoronalMachine,Machine<CoronalMachine>>{
    public CoronalBlock(Machine<CoronalMachine> type){super(type,p->p.strength(6,20).noOcclusion());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return CoronalShapes.get(Attribute.getFacing(state));}
}
