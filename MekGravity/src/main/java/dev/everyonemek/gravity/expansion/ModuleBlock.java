package dev.everyonemek.gravity.expansion;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
public final class ModuleBlock extends BlockTile<OrbitalModule,Machine<OrbitalModule>> {
    public final ModuleKind kind;
    public ModuleBlock(Machine<OrbitalModule> type,ModuleKind kind){super(type,p->p.strength(6,20).noOcclusion());this.kind=kind;}
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return kind==ModuleKind.OBSERVATORY;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof OrbitalModule m?m.signal:0;}
    @Override protected boolean isSignalSource(BlockState state){return kind==ModuleKind.OBSERVATORY;}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return kind==ModuleKind.OBSERVATORY&&side.getOpposite()==mekanism.common.block.attribute.Attribute.getFacing(state)&&level.getBlockEntity(pos) instanceof OrbitalModule m?m.signal:0;}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return ModuleShapes.get(kind,mekanism.common.block.attribute.Attribute.getFacing(state));}
}
