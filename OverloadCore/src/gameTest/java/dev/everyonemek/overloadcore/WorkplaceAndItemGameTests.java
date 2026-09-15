package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.api.*;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import mekanism.common.lib.transmitter.ConnectionType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;
import static dev.everyonemek.overloadcore.CoreGameTests.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class WorkplaceAndItemGameTests {
    @GameTest(template="empty", timeoutTicks=60)
    public static void heaterPaysSurchargeWithoutDoublingHeatOrSavedPowerSetting(GameTestHelper h) {
        var wearer=player(h,new BlockPos(20,4,20));bind(wearer);
        h.setBlock(new BlockPos(20,4,26),MekanismBlocks.RESISTIVE_HEATER.get());h.setBlock(new BlockPos(29,4,26),MekanismBlocks.RESISTIVE_HEATER.get());
        var cursed=(TileEntityResistiveHeater)h.getBlockEntity(new BlockPos(20,4,26));var normal=(TileEntityResistiveHeater)h.getBlockEntity(new BlockPos(29,4,26));
        DeviceScope.placed(cursed,wearer.getUUID());DeviceScope.placed(normal,UUID.randomUUID());
        cursed.setEnergyUsageFromPacket(100);normal.setEnergyUsageFromPacket(100);
        cursed.getEnergyContainer().setEnergy(20000);normal.getEnergyContainer().setEnergy(20000);
        h.startSequence().thenIdle(15).thenExecute(()->{
            check(20000-cursed.getEnergyContainer().getEnergy()==2*(20000-normal.getEnergyContainer().getEnergy()),"Manual heater working energy did not double");
            check(Math.abs(Workplace.temperature(cursed)-Workplace.temperature(normal))<.001,"Extra cursed energy became useful heat");
            var copy=(TileEntityResistiveHeater)BlockEntity.loadStatic(cursed.getBlockPos(),cursed.getBlockState(),cursed.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            check(copy.getEnergyContainer().getEnergyPerTick()==100,"Saved heater setting permanently doubled");remove(wearer);
        }).thenSucceed();
    }
    private static ChestBlockEntity itemLine(GameTestHelper h,BlockPos start,UUID owner) {
        h.setBlock(start.north(),Blocks.CHEST);h.setBlock(start.south(5),Blocks.CHEST);
        var source=(ChestBlockEntity)h.getBlockEntity(start.north());var target=(ChestBlockEntity)h.getBlockEntity(start.south(5));
        DeviceScope.placed(source,owner);DeviceScope.placed(target,owner);source.setItem(0,new ItemStack(Items.IRON_INGOT));
        for(int i=0;i<5;i++){
            h.setBlock(start.south(i),MekanismBlocks.BASIC_LOGISTICAL_TRANSPORTER.get());var pipe=(TileEntityLogisticalTransporterBase)h.getBlockEntity(start.south(i));
            DeviceScope.placed(pipe,owner);if(i==0)pipe.getTransmitter().setConnectionTypeRaw(Direction.NORTH,ConnectionType.PULL);
        }
        return target;
    }
    @GameTest(template="empty", timeoutTicks=500)
    public static void fiveItemSegmentsTakeAboutTwiceTheTimeInsteadOfExponentialSlowdown(GameTestHelper h) {
        var wearer=player(h,new BlockPos(30,4,20));bind(wearer);
        var cursed=itemLine(h,new BlockPos(22,4,26),wearer.getUUID());var normal=itemLine(h,new BlockPos(40,4,26),UUID.randomUUID());
        long[] arrived={0};
        h.startSequence().thenWaitUntil(()->check(!normal.isEmpty(),"Reference item path did not deliver"))
              .thenExecute(()->arrived[0]=h.getTick())
              .thenWaitUntil(()->check(!cursed.isEmpty(),"Cursed item path did not deliver"))
              .thenExecute(()->{
                  check(h.getTick()>=arrived[0]*1.7&&h.getTick()<=arrived[0]*2.3,"Five segments compounded slowdown: "+arrived[0]+" / "+h.getTick());
                  check(cursed.getItem(0).getCount()==1&&normal.getItem(0).getCount()==1,"Item transport duplicated or lost cargo");remove(wearer);
              }).thenSucceed();
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void cloneAndRestoreKeepOneBoundInstanceAndItsEnergy(GameTestHelper h) {
        var original=player(h,new BlockPos(20,4,20));bind(original);CoreBinding.recover(original,1000);
        var replacement=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),original.getGameProfile());
        try {
            var before=CoreBinding.data(original).copy();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(replacement,original,true));
            CoreBinding.restore(replacement);CoreBinding.restore(replacement);
            var inventory=CuriosApi.getCuriosInventory(replacement).orElseThrow().getEquippedCurios();int count=0;
            for(int i=0;i<inventory.getSlots();i++)if(CoreBinding.matches(replacement,inventory.getStackInSlot(i)))count+=inventory.getStackInSlot(i).getCount();
            check(count==1&&CoreBinding.data(replacement).getUUID("instance").equals(before.getUUID("instance"))&&CoreBinding.data(replacement).getLong("energy")==250,"Respawn restoration duplicated the core or lost its stored energy");
            var equipped=CuriosApi.getCuriosInventory(replacement).orElseThrow().getStacksHandler(CoreBinding.SLOT).orElseThrow().getStacks().getStackInSlot(0);
            replacement.getInventory().setItem(0,equipped.copy());CoreBinding.restore(replacement);
            check(replacement.getInventory().getItem(0).isEmpty(),"A duplicate physical copy remained active after reconciliation");h.succeed();
        } finally { remove(original); }
    }
}
