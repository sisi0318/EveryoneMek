package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.solar.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import mekanism.api.RelativeSide;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class PortFairnessTests {
    @GameTest(template="empty",timeoutTicks=80) public static void gravityPortsShareLowSupply(GameTestHelper h){run(h,false);}
    @GameTest(template="empty",timeoutTicks=80) public static void solarPortsShareLowSupply(GameTestHelper h){run(h,true);}
    private static void run(GameTestHelper h,boolean solar){
        var c=solar?null:ReactorTests.formed(h);var s=solar?SolarTests.formed(h):null;var cubes=new ArrayList<TileEntityEnergyCube>();var positions=new ArrayList<BlockPos>();
        if(solar){s.ignited=true;s.autoEject=true;s.stored=s.reserve();for(var p:s.structure.ports)if(p.kind()==SolarBlock.Kind.ENERGY&&p.output())positions.add(p.getBlockPos().relative(SolarTests.side(s,p)));}
        else{c.ignited=true;c.autoEject=true;c.stored=c.reserve();for(var p:c.structure.ports)if(p.kind()==PartBlock.Kind.ENERGY&&p.output())positions.add(p.getBlockPos().relative(ReactorTests.side(c,p)));}
        for(var pos:positions){h.getLevel().setBlockAndUpdate(pos,MekanismBlocks.BASIC_ENERGY_CUBE.get().defaultBlockState());var cube=(TileEntityEnergyCube)h.getLevel().getBlockEntity(pos);
            var config=cube.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);for(var side:RelativeSide.values())config.setDataType(mekanism.common.tile.component.config.DataType.INPUT,side);config.setEjecting(false);h.getLevel().invalidateCapabilities(pos);cubes.add(cube);}
        long[] supplied={0};boolean[] supplying={true};
        h.onEachTick(()->{if(supplying[0]){long stored=solar?s.stored:c.stored,reserve=solar?s.reserve():c.reserve();long add=Math.max(0,4000-(stored-reserve));supplied[0]+=add;if(solar)s.stored+=add;else c.stored+=add;}});
        h.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo t){}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){}private void stop(){supplying[0]=false;if(solar)s.autoEject=false;else c.autoEject=false;}public void testPassed(GameTestInfo t,GameTestRunner r){stop();}public void testFailed(GameTestInfo t,GameTestRunner r){stop();}});
        h.startSequence().thenIdle(12).thenExecute(()->{
            check(cubes.size()==4&&cubes.stream().allMatch(cube->cube.getEnergyContainers(null).getFirst().getEnergy()>0),"Fixed port order starved a connected receiver");
            long received=cubes.stream().mapToLong(cube->cube.getEnergyContainers(null).getFirst().getEnergy()).sum();long remaining=solar?s.stored-s.reserve():c.stored-c.reserve();
            check(received+remaining==supplied[0],"Round-robin export lost or duplicated energy");
            check(solar?s.structure.ports.stream().anyMatch(p->p.outputRate()>0):c.structure.ports.stream().anyMatch(p->p.outputRate()>0),"Per-port telemetry did not track exports");
            supplying[0]=false;if(solar){s.autoEject=false;s.stored=s.reserve();}else{c.autoEject=false;c.stored=c.reserve();}
        }).thenIdle(3).thenExecute(()->check(solar?s.structure.ports.stream().allMatch(p->p.outputRate()==0):c.structure.ports.stream().allMatch(p->p.outputRate()==0),"Idle port rate remained stale")).thenSucceed();
    }
}
