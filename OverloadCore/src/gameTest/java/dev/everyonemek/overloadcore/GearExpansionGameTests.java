package dev.everyonemek.overloadcore;

import dev.everyonemek.overloadcore.gear.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.registries.*;
import mekanism.common.tile.TileEntityModificationStation;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import static dev.everyonemek.overloadcore.CoreGameTests.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class GearExpansionGameTests {
    private static void module(ItemStack stack,GearUpgrade kind,int count,ServerPlayer player){ModuleHelper.get().getModuleContainer(stack).addModule(player.registryAccess(),stack,EquipmentModules.get(kind),count);}
    private static long price(){return GearCombat.joules(CoreConfig.WARD_COST_FE.get());}
    private static TileEntityEnergyCube cube(GameTestHelper h,ServerPlayer p,long stored){var pos=new BlockPos(20,4,23);h.setBlock(pos,MekanismBlocks.ADVANCED_ENERGY_CUBE.get());var c=(TileEntityEnergyCube)h.getBlockEntity(pos);DeviceScope.placed(c,p.getUUID());c.getEnergyContainer().setEnergy(stored);return c;}
    @GameTest(template="empty",timeoutTicks=60)
    public static void armorModulesSpendOnlyForTheirRealBurdenAndStopWhenUnpowered(GameTestHelper h){
        var p=player(h,new BlockPos(20,4,20));bind(p);
        try {
            var chest=new ItemStack(MekanismItems.MEKASUIT_BODYARMOR.get());var pants=new ItemStack(MekanismItems.MEKASUIT_PANTS.get());
            module(chest,GearUpgrade.HEAT_SINK,4,p);module(pants,GearUpgrade.MAGNETIC,4,p);p.setItemSlot(EquipmentSlot.CHEST,chest);p.setItemSlot(EquipmentSlot.LEGS,pants);
            var ct=StorageUtils.getEnergyContainer(chest,0);var pt=StorageUtils.getEnergyContainer(pants,0);ct.setEnergy(100000);pt.setEnergy(100000);
            check(GearEffects.cool(p,0,0)==0&&ct.getEnergy()==100000,"Idle heat sink spent energy");
            check(GearEffects.cool(p,90,5)<0&&ct.getEnergy()==100000-GearCombat.joules(4L*CoreConfig.HEAT_SINK_COST_FE.get()),"Powered heat sink failed cooling/payment");
            check(GearEffects.magnetic(p,600)==512&&pt.getEnergy()==100000-GearCombat.joules(4L*CoreConfig.MAGNETIC_COST_FE.get()),"Magnetic compensation did not use real power");
            p.getInventory().setItem(0,new ItemStack(Items.IRON_BLOCK,64));ct.setEnergy(100000);pt.setEnergy(100000);
            var state=CoreBinding.data(p);state.putInt("heat",90);CoreBinding.save(p,state);Workplace.tick(p);
            check(CoreBinding.data(p).getInt("load")>=576&&CoreBinding.data(p).getInt("compensation")==512&&!CoreBinding.data(p).getBoolean("heavy")
                  &&CoreBinding.data(p).getInt("heat")<88&&ct.getEnergy()<100000&&pt.getEnergy()<100000,"Workplace did not apply paid modules to real burden/heat");
            ct.setEnergy(0);pt.setEnergy(0);check(GearEffects.cool(p,90,5)==5&&GearEffects.magnetic(p,600)==0,"Unpowered modules still applied benefits");
            Workplace.tick(p);check(CoreBinding.data(p).getBoolean("heavy")&&CoreBinding.data(p).getInt("compensation")==0,"Power loss did not restore actual sprint restriction");
            p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.getInventory().setItem(0,chest);ct.setEnergy(100000);check(GearEffects.cool(p,90,5)==5&&ct.getEnergy()==100000,"Backpack heat sink worked");
            h.succeed();
        }finally{remove(p);}
    }

    private static int playerSlot(net.minecraft.world.inventory.AbstractContainerMenu menu,ServerPlayer p,int slot){
        for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i).container==p.getInventory()&&menu.slots.get(i).getContainerSlot()==slot)return i;
        throw new IllegalStateException("Player slot unavailable");
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void removableWardUsesNativeStationAndReequipsWithUpgrades(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();
        WardCustodyGameTests.click(p,ClickType.PICKUP,0);
        var ward=p.containerMenu.getCarried();check(ward.is(CoreContent.WARD)&&!ward.has(CoreContent.WARD_SEAL),"Manual unequip did not release the original");
        p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setItem(0,ward);p.closeContainer();
        var pos=new BlockPos(20,4,22);h.setBlock(pos,MekanismBlocks.MODIFICATION_STATION.get());var station=(TileEntityModificationStation)h.getBlockEntity(pos);
        station.getEnergyContainer().setEnergy(station.getEnergyContainer().getMaxEnergy());long before=station.getEnergyContainer().getEnergy();
        station.getBlockState().useWithoutItem(h.getLevel(),p,new BlockHitResult(h.absolutePos(pos).getCenter(),Direction.NORTH,h.absolutePos(pos),false));
        var menu=p.containerMenu;check(menu instanceof mekanism.common.inventory.container.tile.MekanismTileContainer<?>,"Not the original Mek station menu");
        p.getInventory().setItem(1,new ItemStack(CoreContent.UPGRADE_ITEMS.get(GearUpgrade.CAPACITOR).get(),4));
        menu.quickMoveStack(p,playerSlot(menu,p,0));menu.quickMoveStack(p,playerSlot(menu,p,1));
        check(station.containerSlot.getStack().is(CoreContent.WARD),"Shift click did not insert ward into native equipment slot");
        h.startSequence().thenWaitUntil(()->check(GearEffects.level(station.containerSlot.getStack(),GearUpgrade.CAPACITOR)==4,"Native station has not completed installation"))
              .thenExecute(()->{try{
                  check(station.getEnergyContainer().getEnergy()<before&&station.getInventorySlots(null).getFirst().isEmpty(),"Native installation did not consume power/chips");
                  station.removeModule(p,EquipmentModules.get(GearUpgrade.CAPACITOR),false);
                  check(GearEffects.level(station.containerSlot.getStack(),GearUpgrade.CAPACITOR)==3&&p.getInventory().countItem(CoreContent.UPGRADE_ITEMS.get(GearUpgrade.CAPACITOR).get())==1,"Native remove did not return exactly one chip");
                  int output=-1;for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i) instanceof mekanism.common.inventory.container.slot.InventoryContainerSlot slot&&slot.getInventorySlot()==station.containerSlot)output=i;
                  check(output>=0,"Native equipment slot missing");menu.quickMoveStack(p,output);
                  ItemStack upgraded=ItemStack.EMPTY;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(CoreContent.WARD)){upgraded=p.getInventory().getItem(i);p.getInventory().setItem(i,ItemStack.EMPTY);break;}
                  check(!upgraded.isEmpty()&&station.containerSlot.isEmpty(),"Native shift extraction lost or duplicated ward");
                  p.closeContainer();var slots=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler(ThunderWardItem.SLOT).orElseThrow().getStacks();
                  check(slots.insertItem(0,upgraded,false).isEmpty()&&ThunderWard.equipped(p),"Modified ward did not re-equip");WardCustody.ensure(p);
                  check(GearEffects.level(WardRuntime.worn(p),GearUpgrade.CAPACITOR)==3,"Custody reverted native station changes");
                  check(!mekanism.api.gear.IModuleHelper.INSTANCE.isModuleContainer(new ItemStack(CoreContent.CORE.get())),"Core still accepts upgrades");
              }finally{ThunderWardGameTests.close(f);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void retiringCoreUpgradesPreservesEnergyAndRefundsOnce(GameTestHelper h){
        var p=player(h,new BlockPos(20,4,20));bind(p);
        try{
            var core=CoreBinding.worn(p);
            // Recreate the canonical saved component from alpha.17/18; current installation is intentionally unsupported.
            mekanism.common.content.gear.ModuleContainer.EMPTY.addModule(p.registryAccess(),core,EquipmentModules.get(GearUpgrade.RESERVOIR),2);
            var saved=CoreBinding.data(p);saved.put("equipment",core.save(p.registryAccess()));saved.putLong("energy",GearCombat.joules(700000));CoreBinding.save(p,saved);
            CoreBinding.restore(p);CoreBinding.restore(p);
            check(!CoreBinding.worn(p).has(MekanismDataComponents.MODULE_CONTAINER)&&CoreBinding.data(p).getLong("energy")==GearCombat.joules(700000),"Retirement lost energy or left an upgrade active");
            check(p.getInventory().countItem(CoreContent.UPGRADE_ITEMS.get(GearUpgrade.RESERVOIR).get())==2,"Retirement did not refund exactly once");
            CoreBinding.recover(p,100000);check(CoreBinding.data(p).getLong("energy")==GearCombat.joules(700000),"Over-capacity reserve was lost or old expansion remained active");h.succeed();
        }finally{remove(p);}
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void capacitorChargingRescueAndCustodyRestoreConserveEnergy(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();var ward=WardRuntime.worn(p);
        check(WardCustody.update(p,ward,s->{module(s,GearUpgrade.CAPACITOR,1,p);module(s,GearUpgrade.AFTERGUARD,2,p);}),"Legitimate module transaction rejected");
        var energy=new GearEnergy(ward);var cap=p.registryAccess();
        check(energy.insert(price(),Action.SIMULATE,AutomationType.INTERNAL)==0&&energy.getEnergy()==0,"Capacitor simulation changed the item");
        check(energy.insert(price(),Action.EXECUTE,AutomationType.INTERNAL)==0&&energy.getEnergy()==price(),"Capacitor did not charge");
        WardCustody.ensure(p);check(energy.getEnergy()==price(),"Custody erased legitimate charge");
        var power=cube(h,p,price()/2);
        energy.extract(price()/2,Action.EXECUTE,AutomationType.INTERNAL);power.getEnergyContainer().setEnergy(price()/2-1);
        check(!WardPower.pay(p)&&energy.getEnergy()==price()/2&&power.getEnergyContainer().getEnergy()==price()/2-1,"Insufficient joint payment partially drained energy");
        power.getEnergyContainer().setEnergy(price()/2);check(WardPower.pay(p)&&energy.getEnergy()==0&&power.getEnergyContainer().isEmpty(),"Joint payment failed conservation");
        WardCustody.ensure(p);check(energy.getEnergy()==0,"Custody restored spent capacitor energy");
        energy.insert(price(),Action.EXECUTE,AutomationType.INTERNAL);
        h.startSequence().thenIdle(65).thenExecute(()->{
            try{
                p.hurt(p.damageSources().generic(),40);check(p.getHealth()==1&&energy.getEnergy()==0&&WardRuntime.hits(p)==5,"Capacitor-only rescue or upgraded shield failed");
                WardCustody.ensure(p);check(energy.getEnergy()==0,"Paid rescue energy resurrected");
                var saved=WardLedger.load(WardLedger.get(p).save(new CompoundTag(),cap),cap);check(GearEnergy.stored(saved.worn.get(p.getUUID()).restore(cap))==0,"World save retained pre-debit energy");
                ward.set(CoreContent.GEAR_ENERGY,price());check(energy.getEnergy()==0,"Arbitrary mutation bypassed custody");
                WardCustodyGameTests.click(p,ClickType.PICKUP,0);check(!ThunderWard.equipped(p)&&!p.containerMenu.getCarried().has(CoreContent.WARD_SEAL),"Normal unequip broke after upgrades");
            }finally{ThunderWardGameTests.close(f);}
        }).thenSucceed();
    }

    private static Zombie mob(GameTestHelper h,BlockPos pos){var z=EntityType.ZOMBIE.create(h.getLevel());z.setPos(h.absolutePos(pos).getCenter());z.setNoAi(true);z.setPersistenceRequired();z.setInvulnerable(false);z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);z.setHealth(200);h.getLevel().addFreshEntity(z);return z;}
    @GameTest(template="empty",timeoutTicks=50)
    public static void nativeSuitChargesProtectedCurioWithoutDuplicatingItsSnapshot(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();var ward=WardRuntime.worn(p);
        check(WardCustody.update(p,ward,s->module(s,GearUpgrade.CAPACITOR,1,p)),"Capacitor install failed");
        var chest=new ItemStack(MekanismItems.MEKASUIT_BODYARMOR.get());var modules=ModuleHelper.get().getModuleContainer(chest);
        modules.addModule(p.registryAccess(),chest,MekanismModules.CHARGE_DISTRIBUTION_UNIT,1);modules=ModuleHelper.get().getModuleContainer(chest);
        var config=modules.get(MekanismModules.CHARGE_DISTRIBUTION_UNIT).<Boolean>getConfigOrThrow(mekanism.common.content.gear.mekasuit.ModuleChargeDistributionUnit.CHARGE_INVENTORY);
        modules.replaceModuleConfig(p.registryAccess(),chest,MekanismModules.CHARGE_DISTRIBUTION_UNIT,config.with(true));
        long initial=price()/2;StorageUtils.getEnergyContainer(chest,0).setEnergy(initial);p.setItemSlot(EquipmentSlot.CHEST,chest);
        h.startSequence().thenIdle(4).thenExecute(()->{try{
            check(GearEnergy.stored(ward)>0&&GearEnergy.stored(ward)+StorageUtils.getEnergyContainer(chest,0).getEnergy()==initial,"Native Curios charging failed conservation");
            long before=GearEnergy.stored(ward);WardCustody.ensure(p);check(GearEnergy.stored(ward)==before,"Custody undid native chest charging");
        }finally{ThunderWardGameTests.close(f);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=55)
    public static void weaponsUseAmmoPowerAndRespectWallsWhileNativeToolChains(GameTestHelper h){
        var fixture=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=fixture.player();p.setYRot(0);p.setXRot(0);var gun=new ItemStack(CoreContent.RAILGUN.get());var blade=new ItemStack(CoreContent.BLADE.get());
        // This range's wall is two blocks high; stand on its floor, not half a block above it.
        p.setPos(p.getX(),h.absolutePos(new BlockPos(20,4,20)).getY(),p.getZ());
        var first=mob(h,new BlockPos(20,4,25));var behind=mob(h,new BlockPos(20,4,30));
        try{
            p.setItemSlot(EquipmentSlot.MAINHAND,gun);p.getInventory().setItem(1,new ItemStack(Items.IRON_NUGGET,20));module(gun,GearUpgrade.MAGAZINE,2,p);module(gun,GearUpgrade.PIERCING,1,p);
            GearCombat.reload(p,gun);check(GearCombat.ammo(gun)==10&&p.getInventory().getItem(1).getCount()==10,"Reload created/lost rounds");
            new GearEnergy(gun).setEnergy(GearCombat.joules(200000));long before=GearEnergy.stored(gun);
            h.setBlock(new BlockPos(20,5,27),Blocks.STONE);h.setBlock(new BlockPos(20,4,27),Blocks.STONE);
            check(GearCombat.shoot(p,gun)&&first.getHealth()<200&&behind.getHealth()==200,"Rail did not hit or passed through a wall");
            check(GearCombat.ammo(gun)==9&&before-GearEnergy.stored(gun)==GearCombat.joules(CoreConfig.RAIL_COST.get()),"Rail shot was not paid exactly once");
            var wallShot=visuals(fixture).getLast();check(wallShot.kind()==0&&wallShot.impact()&&wallShot.shooter()==p.getId()&&wallShot.to().z()<=h.absolutePos(new BlockPos(20,5,27)).getZ()+.001,"Rail impact packet passed wall or lost shooter");
            ModuleHelper.get().getModuleContainer(gun).removeModule(p.registryAccess(),gun,EquipmentModules.get(GearUpgrade.PIERCING),1);
            first.invulnerableTime=0;check(GearCombat.shoot(p,gun),"Second rail shot failed");
            var stopped=visuals(fixture).getLast();check(stopped.impact()&&stopped.to().z()<wallShot.to().z()-1,"Non-piercing slug visual continued beyond target");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
            try{GearVisuals.Beam.CODEC.encode(buffer,stopped);check(GearVisuals.Beam.CODEC.decode(buffer).equals(stopped),"Weapon effect codec changed endpoint/type/hand");}finally{buffer.release();}
            int sent=visuals(fixture).size();new GearEnergy(gun).setEnergy(0);check(!GearCombat.shoot(p,gun)&&GearCombat.ammo(gun)==8&&visuals(fixture).size()==sent,"Empty weapon consumed a round or displayed a shot");
            var tool=new ItemStack(MekanismItems.MEKA_TOOL.get());module(tool,GearUpgrade.RESONANCE,2,p);StorageUtils.getEnergyContainer(tool,0).setEnergy(100000);
            var neighbor=mob(h,new BlockPos(21,4,25));try{long old=StorageUtils.getEnergyContainer(tool,0).getEnergy();p.setItemSlot(EquipmentSlot.MAINHAND,tool);tool.getItem().hurtEnemy(tool,first,p);check(neighbor.getHealth()<200&&StorageUtils.getEnergyContainer(tool,0).getEnergy()<old,"Native MekaTool hook did not arc/pay");}finally{neighbor.discard();}
            new GearEnergy(blade).setEnergy(GearCombat.joules(100000));module(blade,GearUpgrade.BLADE_FIELD,2,p);p.setItemSlot(EquipmentSlot.MAINHAND,blade);first.invulnerableTime=0;float health=first.getHealth();
            fixture.packets().clear();
            check(GearCombat.burst(p,blade)&&first.getHealth()<health&&behind.getHealth()==200,"Charged blade failed range/occlusion");
            check(visuals(fixture).size()==1&&visuals(fixture).getFirst().kind()==1,"Blade still emitted target-by-target laser beams");
            var roundTrip=ItemStack.parse(p.registryAccess(),gun.save(p.registryAccess())).orElseThrow();check(GearCombat.ammo(roundTrip)==8&&GearEnergy.stored(roundTrip)==0,"Weapon save lost ammo/energy");
            var modules=ModuleHelper.get().getModuleContainer(gun);modules.addModule(p.registryAccess(),gun,MekanismModules.ENERGY_UNIT,1);
            new GearEnergy(gun).setEnergy(GearEnergy.capacity(gun));long retained=GearEnergy.stored(gun);
            ModuleHelper.get().getModuleContainer(gun).removeModule(p.registryAccess(),gun,MekanismModules.ENERGY_UNIT,1);
            check(GearEnergy.stored(gun)==retained&&retained>GearEnergy.capacity(gun)&&new GearEnergy(gun).insert(100,Action.SIMULATE,AutomationType.INTERNAL)==100,"Native energy module removal discarded charge or overfilled");
            h.succeed();
        }finally{first.discard();behind.discard();ThunderWardGameTests.close(fixture);}
    }
    private static java.util.List<GearVisuals.Beam> visuals(ThunderWardGameTests.Fixture fixture){
        return fixture.packets().stream().filter(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket.class::isInstance)
            .map(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket.class::cast).map(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket::payload)
            .filter(GearVisuals.Beam.class::isInstance).map(GearVisuals.Beam.class::cast).toList();
    }
}
