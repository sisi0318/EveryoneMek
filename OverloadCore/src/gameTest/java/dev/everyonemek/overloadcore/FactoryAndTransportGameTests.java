package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.api.*;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.factory.TileEntityItemToItemFactory;
import mekanism.common.tile.transmitter.TileEntityMechanicalPipe;
import mekanism.common.lib.transmitter.ConnectionType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import static dev.everyonemek.overloadcore.CoreGameTests.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class FactoryAndTransportGameTests {
    @GameTest(template="empty", timeoutTicks=360)
    public static void threeFactoryLanesHaveIndependentQualifiedOutputs(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20));bind(wearer);
        var factoryBlock=MekanismBlocks.getFactory(mekanism.common.tier.FactoryTier.BASIC,mekanism.common.content.blocktype.FactoryType.ENRICHING).get();
        h.setBlock(new BlockPos(20,4,26),factoryBlock);h.setBlock(new BlockPos(29,4,26),factoryBlock);
        var cursed=(TileEntityItemToItemFactory)h.getBlockEntity(new BlockPos(20,4,26));var normal=(TileEntityItemToItemFactory)h.getBlockEntity(new BlockPos(29,4,26));
        DeviceScope.placed(cursed,wearer.getUUID());DeviceScope.placed(normal,UUID.randomUUID());
        for(var side:RelativeSide.values()) { cursed.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY).setDataType(DataType.INPUT,side); normal.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY).setDataType(DataType.INPUT,side); }
        cursed.getEnergyContainer().setEnergy(cursed.getEnergyContainer().getMaxEnergy());normal.getEnergyContainer().setEnergy(normal.getEnergyContainer().getMaxEnergy());
        cursed.invalidateCapabilitiesFull();normal.invalidateCapabilitiesFull();
        power(h,new BlockPos(20,4,26));power(h,new BlockPos(29,4,26));
        long[] energyUsed={0,0};h.onEachTick(() -> { energyUsed[0]+=cursed.getLastUsage();energyUsed[1]+=normal.getLastUsage(); });
        for(var slot:slots(cursed,DataType.INPUT))slot.setStack(new ItemStack(Items.IRON_ORE));
        for(var slot:slots(normal,DataType.INPUT))slot.setStack(new ItemStack(Items.IRON_ORE));
        h.startSequence().thenWaitUntil(()->check(output(cursed)==12&&output(normal)==6,"Factory outputs "+output(cursed)+"/"+output(normal)+", energy="+cursed.getEnergyContainer().getEnergy()+"/"+normal.getEnergyContainer().getEnergy()))
              .thenExecute(()->{
                  check(energyUsed[0]==2*energyUsed[1],"Factory power was multiplied once per lane and again globally");
                  check(DeviceScope.data(cursed).getCompound("jobs").getAllKeys().size()==3,"Factory shared one qualification across all lanes");remove(wearer);
              }).thenSucceed();
    }
    private static void power(GameTestHelper h,BlockPos target) {
        h.setBlock(target.south(),MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        h.setBlock(target.south(2),MekanismBlocks.BASIC_ENERGY_CUBE.get());
        var cube=(mekanism.common.tile.TileEntityEnergyCube)h.getBlockEntity(target.south(2));
        cube.getEnergyContainer().setEnergy(cube.getEnergyContainer().getMaxEnergy());
        var config=cube.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);
        config.setDataType(DataType.OUTPUT,RelativeSide.fromDirections(cube.getDirection(),Direction.NORTH));config.setEjecting(true);cube.invalidateCapabilitiesFull();
    }
    private record Line(TileEntityFluidTank source,TileEntityMechanicalPipe pipe,TileEntityFluidTank target) { }
    private static Line line(GameTestHelper h,BlockPos pos,UUID owner) {
        h.setBlock(pos,MekanismBlocks.BASIC_MECHANICAL_PIPE.get());h.setBlock(pos.west(),MekanismBlocks.BASIC_FLUID_TANK.get());h.setBlock(pos.east(),MekanismBlocks.BASIC_FLUID_TANK.get());
        var pipe=(TileEntityMechanicalPipe)h.getBlockEntity(pos);var source=(TileEntityFluidTank)h.getBlockEntity(pos.west());var target=(TileEntityFluidTank)h.getBlockEntity(pos.east());
        DeviceScope.placed(pipe,owner);DeviceScope.placed(source,owner);DeviceScope.placed(target,owner);
        source.fluidTank.setStack(new FluidStack(Fluids.WATER,30000));pipe.getTransmitter().setConnectionTypeRaw(Direction.WEST,ConnectionType.PULL);
        return new Line(source,pipe,target);
    }
    @GameTest(template="empty", timeoutTicks=150)
    public static void realFluidPipesReduceSustainedPullAndKeepAllWater(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20));bind(wearer);
        var cursed=line(h,new BlockPos(20,4,26),wearer.getUUID());var normal=line(h,new BlockPos(29,4,26),UUID.randomUUID());
        var before=new int[2];
        h.startSequence().thenIdle(30).thenExecute(()->{before[0]=cursed.target.fluidTank.getFluidAmount();before[1]=normal.target.fluidTank.getFluidAmount();})
              .thenIdle(30).thenExecute(()->{
                  int actual=cursed.target.fluidTank.getFluidAmount()-before[0],reference=normal.target.fluidTank.getFluidAmount()-before[1];
                  check(reference>0 && Math.abs(2*actual-reference)<=4,"Fluid rate should be half after warmup: cursed="+actual+", normal="+reference);
                  long sum=cursed.source.fluidTank.getFluidAmount()+cursed.target.fluidTank.getFluidAmount()+cursed.pipe.getTransmitter().getBufferWithFallback().getAmount();
                  check(sum==30000,"Pipe limitation lost or created water: "+sum);
                  check(cursed.pipe.getTransmitter().getCapacity()==normal.pipe.getTransmitter().getCapacity(),"Pipe capacity was reduced");
                  remove(wearer);
              }).thenSucceed();
    }
    @GameTest(template="empty", timeoutTicks=45)
    public static void externalOwnershipAndWorkingHazardsAreLocalAndMufflingIsRespected(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20));bind(wearer);var machine=enrichment(h,new BlockPos(20,4,21),wearer.getUUID());
        try {
            DeviceTracker.begin(machine);DeviceTracker.end(null);DeviceTracker.worked(machine);
            var data=CoreBinding.data(wearer);data.putInt("noise",95);CoreBinding.save(wearer,data);
            Workplace.tick(wearer);check(CoreBinding.data(wearer).contains("shock_warning"),"No warning before the first electric shock");
            check(CoreBinding.data(wearer).getInt("noise")>=95,"A working nearby machine did not build noise exposure");
            machine.getComponent().addUpgrades(Upgrade.MUFFLING,4);
            data=CoreBinding.data(wearer);data.putInt("noise",50);CoreBinding.save(wearer,data);Workplace.tick(wearer);
            check(CoreBinding.data(wearer).getInt("noise")<50,"Muffling upgrades did not reduce the curse exposure");
            h.setBlock(new BlockPos(20,5,20),Blocks.STONE);
            long next=CoreBinding.data(wearer).getLong("next_shock");Workplace.tick(wearer);
            check(CoreBinding.data(wearer).getLong("next_shock")==next,"A repeated hazard call ignored its personal cooldown");
            h.succeed();
        } finally { remove(wearer); }
    }
}
