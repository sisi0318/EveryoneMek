package dev.everyonemek.gravity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Cosmetic orientation reuses the existing facing property; coil aim remains player-controlled. */
public final class AssemblyAppearance {
    private static boolean boundary(int value){return value==0||value==6;}
    public static BlockState apply(BlockState state,Controller controller,int x,int y,int z,boolean formed){
        state=state.setValue(PartBlock.FORMED,formed);
        if(!(state.getBlock() instanceof PartBlock block))return state;
        var kind=block.kind;
        if(!formed){
            if(kind==PartBlock.Kind.CORE||kind==PartBlock.Kind.COIL)state=state.setValue(PartBlock.ACTIVE,false);
            return kind==PartBlock.Kind.FRAME?state.setValue(PartBlock.FACING,Direction.UP):state;
        }
        int edges=(boundary(x)?1:0)+(boundary(y)?1:0)+(boundary(z)?1:0);
        if(kind==PartBlock.Kind.FRAME){
            if(edges!=2)return state.setValue(PartBlock.FACING,Direction.DOWN); // Symmetric corner/face connector.
            var axis=boundary(x)&&boundary(z)?Direction.Axis.Y:boundary(y)&&boundary(z)?controller.getDirection().getClockWise().getAxis():controller.getDirection().getAxis();
            return state.setValue(PartBlock.FACING,axis==Direction.Axis.Y?Direction.UP:axis==Direction.Axis.X?Direction.EAST:Direction.NORTH);
        }
        if(edges!=1||kind==PartBlock.Kind.COIL||kind==PartBlock.Kind.CORE)return state;
        var inward=y==0?Direction.UP:y==6?Direction.DOWN:x==0?controller.getDirection().getClockWise():x==6?controller.getDirection().getCounterClockWise():z==0?controller.getDirection().getOpposite():controller.getDirection();
        return state.setValue(PartBlock.FACING,kind==PartBlock.Kind.CASING?inward:inward.getOpposite());
    }
    private AssemblyAppearance(){}
}
