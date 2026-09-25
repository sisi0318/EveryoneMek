package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class GravityCompletionTests {
    @GameTest(template="empty",timeoutTicks=80)
    public static void gravityFillsTheLastJoulesAndReformsImmediately(GameTestHelper h){var c=ReactorTests.formed(h);c.enabled=c.ignited=true;c.fuelRemaining=c.fuelTotal=1_000_000;
        try{for(int room=1;room<=100;room++){
            c.stored=c.capacity()-room;c.gross=c.structure.grade.power();long fuel=c.fuelRemaining;c.react();
            check(c.stored==c.capacity()&&fuel-c.fuelRemaining==room+c.selfUse,"Last-fill rounding lost energy or failed to fill: "+room);
            fuel=c.fuelRemaining;c.react();check(c.gross==0&&c.fuelRemaining==fuel,"Full gravity reactor kept burning fuel");
        }}finally{c.enabled=false;}
        var first=c.structure.at(0,0,0);var last=c.structure.at(6,6,6);var state=h.getLevel().getBlockState(first);
        h.getLevel().setBlockAndUpdate(first,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(last,Blocks.AIR.defaultBlockState());check(!c.structure.valid(),"Incomplete reactor formed");
        // Repair the earlier failure first, then the previously unscanned final block.
        h.getLevel().setBlockAndUpdate(first,state);
        h.startSequence().thenIdle(2).thenExecute(()->{check(!c.structure.formed,"Far missing part accepted");h.getLevel().setBlockAndUpdate(last,state);})
          .thenIdle(2).thenExecute(()->{check(c.structure.formed,"Last block waited for the old 20-tick polling cycle");check(h.getLevel().getBlockState(last).getValue(PartBlock.FORMED),"Final frame appearance stayed stale");})
          .thenSucceed();
    }
}
