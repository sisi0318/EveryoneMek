package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.solar.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class RuntimeSummaryTests {
    @GameTest(template="empty",timeoutTicks=80)
    public static void fuelSummaryIsReadOnlySaturatesAndDistinguishesHotStandby(GameTestHelper h){
        var s=SolarTests.formed(h);var inv=s.structure.fuelHatches.getFirst().inventory;
        inv.setStackInSlot(0,new ItemStack(SolarContent.FUEL.get(),64));inv.setStackInSlot(1,new ItemStack(SolarContent.FUEL.get(),64));
        var summary=s.fuelStock();check(summary.portions==128&&summary.energy==Long.MAX_VALUE,"Large stellar reserve overflowed or showed a negative total");
        check(inv.getStackInSlot(0).getCount()==64&&s.fuelRemaining==0,"Reading reserve fuel consumed inventory");
        var capsule=new ItemStack(SolarContent.CAPSULE.get());var data=new CompoundTag();data.putLong("remaining",123);data.putLong("total",1000);capsule.set(SolarContent.FUEL_DATA.get(),data);inv.setStackInSlot(0,capsule);inv.setStackInSlot(1,ItemStack.EMPTY);
        check(s.fuelStock().energy==123&&s.fuelStock().portions==1,"Residual capsule was counted as full fuel");
        s.enabled=s.ignited=true;s.stored=s.capacity();s.react();check(s.gross==0&&s.isCoreHot(),"Full buffer was reported as a cold core");s.enabled=false;check(!s.isCoreHot(),"Stopped core remained hot in UI summary");
        var player=ReactorTests.player(h,s.getBlockPos().north());
        try{
            SolarMenu.open(player,s,s.getBlockPos());var menu=(SolarMenu)player.containerMenu;check(menu.clickMenuButton(player,31)&&menu.portIndex==1,"Port telemetry paging was not server controlled");
            check(!menu.clickMenuButton(player,999),"Unknown menu action accepted");
            menu.clickMenuButton(player,20);var saved=s.saveWithFullMetadata(h.getLevel().registryAccess());s.loadWithComponents(saved,h.getLevel().registryAccess());check(s.buildTier==1,"Build tier did not survive saved settings");
        }finally{ReactorTests.close(player);s.enabled=false;}
        h.succeed();
    }
}
