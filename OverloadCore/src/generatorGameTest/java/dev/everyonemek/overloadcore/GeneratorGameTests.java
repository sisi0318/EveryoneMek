package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.api.*;
import mekanism.common.registries.MekanismItems;
import mekanism.common.registries.MekanismChemicals;
import mekanism.generators.common.registries.*;
import mekanism.generators.common.tile.TileEntityBioGenerator;
import mekanism.generators.common.tile.turbine.TileEntityTurbineCasing;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import static dev.everyonemek.overloadcore.CoreGameTests.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class GeneratorGameTests {
    @GameTest(template="empty", timeoutTicks=90)
    public static void bioGeneratorsBurnEqualFuelButStoreHalfNewPower(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20));bind(wearer);
        h.setBlock(new BlockPos(20,4,27),GeneratorsBlocks.BIO_GENERATOR.get());h.setBlock(new BlockPos(29,4,27),GeneratorsBlocks.BIO_GENERATOR.get());
        var cursed=(TileEntityBioGenerator)h.getBlockEntity(new BlockPos(20,4,27));var normal=(TileEntityBioGenerator)h.getBlockEntity(new BlockPos(29,4,27));
        DeviceScope.placed(cursed,wearer.getUUID());DeviceScope.placed(normal,UUID.randomUUID());
        cursed.getInventorySlots(null).getFirst().setStack(new ItemStack(MekanismItems.BIO_FUEL.get()));normal.getInventorySlots(null).getFirst().setStack(new ItemStack(MekanismItems.BIO_FUEL.get()));
        h.startSequence().thenIdle(20).thenExecute(()->{
            long c=cursed.getEnergyContainer().getEnergy(),n=normal.getEnergyContainer().getEnergy();
            check(c>0 && Math.abs(2*c-n)<=1,"Bio generation was not halved: "+c+" / "+n);
            check(cursed.getFluidTanks(null).getFirst().getFluidAmount()==normal.getFluidTanks(null).getFirst().getFluidAmount(),"Generation reduction also changed fuel consumption");
            var data=DeviceScope.data(cursed).copy();long energy=c;
            GenerationHooks.insert(DeviceScope.device(cursed),cursed.getEnergyContainer(),1,Action.SIMULATE,AutomationType.INTERNAL);
            check(data.equals(DeviceScope.data(cursed)) && cursed.getEnergyContainer().getEnergy()==energy,"Production simulation spent the fractional carry or stored power");
            cursed.setControlType(mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl.HIGH);normal.setControlType(mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl.HIGH);
            remove(wearer);
        }).thenSucceed();
    }
    private static final class Turbine extends GeneratorsBuilders.TurbineBuilder {
        void place(net.minecraft.world.level.Level level,BlockPos start) { build(level,start,false); }
    }
    @GameTest(template="empty", timeoutTicks=300)
    public static void formedTurbinesLoseElectricityOnceAndKeepSteamAccounting(GameTestHelper h) {
        var start=new BlockPos(10,2,10);var second=new BlockPos(42,2,10);
        new Turbine().place(h.getLevel(),h.absolutePos(start));new Turbine().place(h.getLevel(),h.absolutePos(second));
        var wearer=player(h,new BlockPos(32,4,8));bind(wearer);
        var a=(TileEntityTurbineCasing)h.getBlockEntity(start);var b=(TileEntityTurbineCasing)h.getBlockEntity(second);
        h.startSequence().thenWaitUntil(()->check(a.getMultiblock().isFormed()&&b.getMultiblock().isFormed(),"Native turbine structures did not form"))
              .thenExecute(()->{
                  DeviceScope.claim(a,wearer.getUUID());DeviceScope.claim(b,UUID.randomUUID());
                  var first=a.getMultiblock();var other=b.getMultiblock();
                  long steam=Math.min(first.getSteamCapacity(),1000000);
                  first.chemicalTank.setStack(MekanismChemicals.STEAM.asStack(steam));other.chemicalTank.setStack(MekanismChemicals.STEAM.asStack(steam));
                  check(DeviceScope.bearer(first)==wearer,"Multiblock range or explicit registration failed");
              }).thenIdle(5).thenExecute(()->{
                  var first=a.getMultiblock();var other=b.getMultiblock();
                  check(other.energyContainer.getEnergy()>0 && Math.abs(2*first.energyContainer.getEnergy()-other.energyContainer.getEnergy())<=2,"Turbine electricity was not halved once");
                  check(first.chemicalTank.getStored()==other.chemicalTank.getStored(),"Curse also removed steam or modified its flow");
                  check(first.getEnergyCapacity()==other.getEnergyCapacity(),"Existing turbine storage capacity changed");
                  first.chemicalTank.setEmpty();other.chemicalTank.setEmpty();remove(wearer);
              }).thenSucceed();
    }
}
