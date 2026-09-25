package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.solar.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class AssemblyUpgradeTests {
    @GameTest(template="empty",timeoutTicks=100) public static void gravityPartialBuildAndProtectedUpgrade(GameTestHelper h){run(h,false);h.succeed();}
    @GameTest(template="empty",timeoutTicks=100) public static void solarPartialBuildAndProtectedUpgrade(GameTestHelper h){run(h,true);h.succeed();}
    private static void run(GameTestHelper h,boolean solar){
        var c=solar?null:ReactorTests.create(h);var s=solar?SolarTests.create(h):null;var pos=solar?s.getBlockPos():c.getBlockPos();var p=ReactorTests.player(h,pos.north());
        try{
            if(solar)SolarMenu.open(p,s,pos);else ReactorMenu.open(p,c,pos);var menu=p.containerMenu;
            var plan=solar?SolarConstruction.plan(s):Construction.plan(c);var first=plan.entrySet().iterator().next();p.getInventory().add(new ItemStack(first.getValue().getBlock(),1));
            check(!menu.clickMenuButton(p,3),"Partial build unexpectedly completed");check(h.getLevel().getBlockState(first.getKey()).is(first.getValue().getBlock()),"Available material was not placed during partial build");
            var needed=new HashMap<Item,Integer>();for(var e:plan.entrySet())if(!h.getLevel().getBlockState(e.getKey()).is(e.getValue().getBlock()))needed.merge(e.getValue().getBlock().asItem(),1,Integer::sum);needed.forEach((item,n)->p.getInventory().add(new ItemStack(item,n)));
            check(menu.clickMenuButton(p,3),"Build could not resume");check(menu.clickMenuButton(p,20),"Build grade button rejected");
            check((solar?s.buildTier:c.buildTier)==1,"Server build grade did not change");
            var highPlan=solar?SolarConstruction.plan(s):Construction.plan(c);var upgrades=new LinkedHashMap<BlockPos,BlockState>();var refunds=new HashMap<Item,Integer>();
            for(var e:highPlan.entrySet()){var old=h.getLevel().getBlockState(e.getKey());if(old.getBlock()!=e.getValue().getBlock()){upgrades.put(e.getKey(),e.getValue());refunds.merge(old.getBlock().asItem(),1,Integer::sum);p.getInventory().add(new ItemStack(e.getValue().getBlock()));}}
            check(upgrades.size()==(solar?86:6),"Unexpected upgrade scope "+upgrades.size());
            var target=upgrades.keySet().iterator().next();var old=h.getLevel().getBlockState(target);var be=h.getLevel().getBlockEntity(target);be.getPersistentData().putInt("upgrade_marker",73);
            var hatch=solar?s.structure.fuelHatches.getFirst().inventory:c.structure.fuelHatches.getFirst().inventory;Item fuel=solar?SolarContent.FUEL.get():Content.PELLET.get();hatch.setStackInSlot(0,new ItemStack(fuel,11));
            java.util.function.Consumer<BlockEvent.EntityPlaceEvent> deny=e->{if(e.getEntity()==p&&e.getPos().equals(target))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(deny);
            int before=p.getInventory().countItem(upgrades.get(target).getBlock().asItem());
            try{check(!menu.clickMenuButton(p,21),"Upgrade bypassed placement protection");}finally{NeoForge.EVENT_BUS.unregister(deny);}
            check(h.getLevel().getBlockState(target).is(old.getBlock())&&h.getLevel().getBlockEntity(target).getPersistentData().getInt("upgrade_marker")==73,"Rejected upgrade failed to restore original part data");
            check(p.getInventory().countItem(upgrades.get(target).getBlock().asItem())==before,"Rejected upgrade consumed material");
            java.util.function.Consumer<BlockEvent.BreakEvent> denyBreak=e->{if(e.getPlayer()==p&&e.getPos().equals(target))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(denyBreak);
            try{check(!menu.clickMenuButton(p,21),"Upgrade bypassed break protection");}finally{NeoForge.EVENT_BUS.unregister(denyBreak);}
            check(h.getLevel().getBlockState(target).is(old.getBlock())&&p.getInventory().countItem(upgrades.get(target).getBlock().asItem())==before,"Rejected break altered parts or materials");
            check(menu.clickMenuButton(p,21),"Upgrade failed");
            check((solar?s.structure.valid():c.structure.valid()),"Upgraded assembly did not reform");check(hatch.getStackInSlot(0).getCount()==11,"Upgrade lost hatch inventory");
            check(h.getLevel().getBlockEntity(target).getPersistentData().getInt("upgrade_marker")==73,"Upgrade lost existing BE data");
            refunds.forEach((item,n)->check(p.getInventory().countItem(item)==n,"Old parts not refunded exactly"));
            // A lower build grade must not flag higher parts as obstacles or downgrade them.
            if(solar)s.buildTier=0;else c.buildTier=0;
            check(menu.clickMenuButton(p,3),"Existing higher tier blocked repair/build");check(h.getLevel().getBlockState(target).is(upgrades.get(target).getBlock()),"Builder downgraded existing part");
            if(!solar){var far=c.structure.at(6,6,6);p.setPos(far.above().getCenter());((Part)h.getLevel().getBlockEntity(far)).open(p);check(p.containerMenu instanceof ReactorMenu&&p.containerMenu.stillValid(p),"Far frame GUI was closed by a second controller-distance check");}
        }finally{if(solar)s.enabled=false;else c.enabled=false;p.getInventory().clearContent();ReactorTests.close(p);}
    }
}
