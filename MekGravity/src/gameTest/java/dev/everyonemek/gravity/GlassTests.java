package dev.everyonemek.gravity;

import static dev.everyonemek.gravity.ReactorTests.check;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class GlassTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void connectedGlassKeepsOuterAndConcaveBordersOnEveryFace(GameTestHelper h){
        var glass=Content.PARTS.get(PartBlock.Kind.GLASS).get().defaultBlockState();var level=h.getLevel();
        for(var face:Direction.values()){
            var pos=h.absolutePos(new BlockPos(6+face.ordinal()*5,4,6));
            var a=face.getAxis()==Direction.Axis.X?Direction.SOUTH:Direction.EAST;
            var b=face.getAxis()==Direction.Axis.Y?Direction.SOUTH:Direction.UP;
            level.setBlockAndUpdate(pos,glass);
            check(GlassConnections.visibleParts(GlassConnections.sample(level,pos,glass),face)==255,"Isolated glass lost its frame: "+face);
            // Block identity, not facing/active state, determines whether panes join.
            level.setBlockAndUpdate(pos.relative(a),glass.setValue(PartBlock.FACING,Direction.SOUTH).setValue(PartBlock.ACTIVE,true));
            int pair=GlassConnections.sample(level,pos,glass);
            check(GlassConnections.visibleParts(pair,face)==253&&GlassConnections.visibleParts(pair,a)==0,"Two panes kept an internal seam or shared face: "+face);
            level.setBlockAndUpdate(pos.relative(b),glass);
            check(GlassConnections.visibleParts(GlassConnections.sample(level,pos,glass),face)==245,"L-shaped glass lost its inner corner: "+face);
            var diagonal=pos.relative(a).relative(b);level.setBlockAndUpdate(diagonal,glass);
            check(GlassConnections.visibleParts(GlassConnections.sample(level,pos,glass),face)==117,"Filled square kept a corner seam: "+face);
            level.setBlockAndUpdate(diagonal,Blocks.AIR.defaultBlockState());
            check(GlassConnections.visibleParts(GlassConnections.sample(level,pos,glass),face)==245,"Removing a diagonal did not restore its border: "+face);
            level.setBlockAndUpdate(diagonal,Content.PARTS.get(PartBlock.Kind.FRAME).get().defaultBlockState());
            check(GlassConnections.visibleParts(GlassConnections.sample(level,pos,glass),face)==245,"Glass connected through a casing: "+face);
            for(int u=-1;u<=1;u++)for(int v=-1;v<=1;v++)level.setBlockAndUpdate(pos.relative(a,u).relative(b,v),glass);
            check(GlassConnections.visibleParts(GlassConnections.sample(level,pos,glass),face)==0,"A full glass sheet retained internal borders: "+face);
        }
        h.succeed();
    }
}
