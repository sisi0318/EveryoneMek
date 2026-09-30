package dev.everyonemek.overloadcore;

import dev.everyonemek.overloadcore.gear.*;
import mekanism.api.gear.config.ModuleConfig;
import mekanism.common.content.gear.*;
import mekanism.common.registries.*;
import mekanism.common.tile.TileEntityModificationStation;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class MekaCombatGameTests {
    private static void check(boolean yes,String text){CoreGameTests.check(yes,text);}
    private static void install(ItemStack stack,net.minecraft.world.entity.player.Player p){ModuleHelper.get().getModuleContainer(stack).addModule(p.registryAccess(),stack,EquipmentModules.COMBAT,1);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeStationInstallsCombatAndRadialPersistsBothForms(GameTestHelper h){
        var p=CoreGameTests.player(h,new BlockPos(20,4,20));var tool=new ItemStack(MekanismItems.MEKA_TOOL.get());
        var pos=new BlockPos(20,4,22);h.setBlock(pos,MekanismBlocks.MODIFICATION_STATION.get());var station=(TileEntityModificationStation)h.getBlockEntity(pos);
        station.getEnergyContainer().setEnergy(station.getEnergyContainer().getMaxEnergy());long before=station.getEnergyContainer().getEnergy();
        check(station.containerSlot.insertItem(tool,mekanism.api.Action.EXECUTE,mekanism.api.AutomationType.MANUAL).isEmpty(),"Native equipment slot rejected Meka-Tool");
        var modules=station.getInventorySlots(null).getFirst();check(modules.insertItem(new ItemStack(CoreContent.COMBAT_MODULE.get()),mekanism.api.Action.EXECUTE,mekanism.api.AutomationType.MANUAL).isEmpty(),"Native module slot rejected combat module");
        h.startSequence().thenWaitUntil(()->check(MekaCombat.form(station.containerSlot.getStack())!=null,"Native station has not installed combat module"))
            .thenExecute(()->{try{
                var stack=station.containerSlot.getStack();var item=(IRadialModuleContainerItem)stack.getItem();var radial=CombatModule.RADIAL.get();
                check(modules.isEmpty()&&station.getEnergyContainer().getEnergy()<before,"Station did not consume chip/power");
                check(item.getRadialData(stack)!=null&&item.getMode(stack,radial)==CombatModule.Form.MELEE,"Combat forms absent from native radial");
                item.setMode(stack,p,radial,CombatModule.Form.RANGED);check(MekaCombat.form(stack)==CombatModule.Form.RANGED,"Native radial did not select ranged");
                var restored=ItemStack.parse(p.registryAccess(),stack.save(p.registryAccess())).orElseThrow();check(MekaCombat.form(restored)==CombatModule.Form.RANGED,"Form did not persist");
                ModuleHelper.get().getModuleContainer(stack).addModule(p.registryAccess(),stack,MekanismModules.EXCAVATION_ESCALATION_UNIT,1);
                check(item.getRadialData(stack).getModes().size()==2,"Combat replaced native excavation radial");
                station.removeModule(p,EquipmentModules.COMBAT,false);check(MekaCombat.form(stack)==null&&p.getInventory().countItem(CoreContent.COMBAT_MODULE.get())==1,"Native removal did not restore original tool/refund module");
            }finally{CoreGameTests.remove(p);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void actualToolChargeUsesNativeEnergyAndModeChangesCancel(GameTestHelper h){
        var f=ThunderWardGameTests.player(h,new BlockPos(20,4,20));var p=f.player();var stack=new ItemStack(MekanismItems.MEKA_TOOL.get());install(stack,p);
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.getInventory().setItem(1,new ItemStack(Items.IRON_NUGGET,8));
        var item=(IRadialModuleContainerItem)stack.getItem();item.setMode(stack,p,CombatModule.RADIAL.get(),CombatModule.Form.RANGED);
        long initial=GearCombat.joules(300000);StorageUtils.getEnergyContainer(stack,0).setEnergy(initial);
        stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        check(p.isUsingItem()&&stack.getUseDuration(p)==72000&&GearCombat.ammo(stack)==2,"Native use hook/duration/automatic reload failed");
        h.startSequence().thenIdle(2).thenExecute(()->{
            p.releaseUsingItem();check(MekaCombat.energy(stack)==initial&&GearCombat.ammo(stack)==2,"Short charge paid or fired");
            stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        }).thenIdle(35).thenExecute(()->{
            p.releaseUsingItem();check(MekaCombat.energy(stack)==initial-GearCombat.joules(CoreConfig.RAIL_COST.get())&&GearCombat.ammo(stack)==1,"Ranged release did not use native energy exactly once");
        }).thenIdle(11).thenExecute(()->{
            item.setMode(stack,p,CombatModule.RADIAL.get(),CombatModule.Form.MELEE);stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        }).thenIdle(26).thenExecute(()->{
            p.releaseUsingItem();check(MekaCombat.energy(stack)==initial-GearCombat.joules(CoreConfig.RAIL_COST.get()+CoreConfig.BLADE_BURST_COST.get()),"Melee release paid wrong amount");
            check(stack.getDestroySpeed(Blocks.STONE.defaultBlockState())>1,"Combat form disabled native mining");
        }).thenIdle(17).thenExecute(()->{
            stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);item.setMode(stack,p,CombatModule.RADIAL.get(),CombatModule.Form.RANGED);check(!p.isUsingItem(),"Changing forms did not cancel charge");
        }).thenIdle(35).thenExecute(()->{try{
            stack.getItem().releaseUsing(stack,h.getLevel(),p,0);check(GearCombat.ammo(stack)==1,"Canceled charge fired ranged attack");
            p.setShiftKeyDown(true);var result=stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);check(result.getResult()==InteractionResult.PASS&&!p.isUsingItem(),"Sneak-use did not pass to original Meka-Tool");
            var container=ModuleHelper.get().getModuleContainer(stack);var mod=container.get(EquipmentModules.COMBAT);
            container.replaceModuleConfig(p.registryAccess(),stack,EquipmentModules.COMBAT,mod.<Boolean>getConfigOrThrow(ModuleConfig.ENABLED_KEY).with(false));
            p.setShiftKeyDown(false);check(MekaCombat.form(stack)==null&&stack.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.PASS,"Disabled module still consumed use");
        }finally{ThunderWardGameTests.close(f);}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void legacyWeaponsBecomeNativeToolWithoutLosingChargeOrModules(GameTestHelper h){
        var p=CoreGameTests.player(h,new BlockPos(20,4,20));try{
            var old=new ItemStack(CoreContent.RAILGUN.get());ModuleContainer.EMPTY.addModule(p.registryAccess(),old,EquipmentModules.get(GearUpgrade.MAGAZINE),2);
            old.set(CoreContent.GEAR_ENERGY,37000000000L);old.set(CoreContent.RAIL_AMMO,7);old.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Preserved"));
            p.setItemInHand(InteractionHand.OFF_HAND,old);old.getItem().inventoryTick(old,h.getLevel(),p,0,false);
            var tool=p.getOffhandItem();check(tool.is(MekanismItems.MEKA_TOOL)&&tool.getCount()==1&&MekaCombat.form(tool)==CombatModule.Form.RANGED,"Old gun did not convert in offhand");
            check(GearCombat.ammo(tool)==7&&GearEffects.level(tool,GearUpgrade.MAGAZINE)==2&&tool.getHoverName().getString().equals("Preserved"),"Conversion lost module/ammo/name");
            check(MekaCombat.energy(tool)+tool.getOrDefault(CoreContent.GEAR_ENERGY,0L)==37000000000L,"Conversion discarded excess native-capacity charge");
            check(LegacyWeaponItem.convert(tool,p)==tool,"Conversion ran again");
            var restored=ItemStack.parse(p.registryAccess(),tool.save(p.registryAccess())).orElseThrow();
            check(GearCombat.pay(restored,1000),"Converted native tool could not pay");LegacyWeaponItem.feedReserve(restored);
            check(MekaCombat.energy(restored)+restored.getOrDefault(CoreContent.GEAR_ENERGY,0L)==37000000000L-GearCombat.joules(1000),"Deferred charge duplicated or vanished");
            var blade=LegacyWeaponItem.convert(new ItemStack(CoreContent.BLADE.get()),p);check(MekaCombat.form(blade)==CombatModule.Form.MELEE,"Old sword became wrong form");h.succeed();
        }finally{CoreGameTests.remove(p);}
    }
}
