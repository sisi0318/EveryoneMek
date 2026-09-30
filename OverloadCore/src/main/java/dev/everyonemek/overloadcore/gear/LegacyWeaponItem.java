package dev.everyonemek.overloadcore.gear;

import dev.everyonemek.overloadcore.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.registries.MekanismItems;
import mekanism.common.util.StorageUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Decode-only old IDs: no weapon behavior, recipe, creative entry or custom model. */
public final class LegacyWeaponItem extends Item {
    public final boolean rail;
    public LegacyWeaponItem(boolean rail){super(new Properties().stacksTo(1).fireResistant());this.rail=rail;}
    @Override public void onDestroyed(net.minecraft.world.entity.item.ItemEntity entity,net.minecraft.world.damagesource.DamageSource source){
        mekanism.api.gear.IModuleHelper.INSTANCE.dropModuleContainerContents(entity,source);
    }
    public static ItemStack convert(ItemStack old,Player player){
        if(!(old.getItem() instanceof LegacyWeaponItem legacy))return old;
        ItemStack tool=new ItemStack(MekanismItems.MEKA_TOOL.get(),old.getCount());tool.applyComponents(old.getComponentsPatch());
        var container=ModuleHelper.get().getModuleContainer(tool);
        if(!container.has(EquipmentModules.COMBAT)){container.addModule(player.registryAccess(),tool,EquipmentModules.COMBAT,1);container=ModuleHelper.get().getModuleContainer(tool);}
        var module=container.get(EquipmentModules.COMBAT);
        container.replaceModuleConfig(player.registryAccess(),tool,EquipmentModules.COMBAT,module.<CombatModule.Form>getConfigOrThrow(CombatModule.FORM).with(legacy.rail?CombatModule.Form.RANGED:CombatModule.Form.MELEE));
        // Use the old GEAR_ENERGY only as a one-time migration reserve if native capacity is lower.
        tool.set(CoreContent.GEAR_ENERGY,GearEnergy.stored(old));feedReserve(tool);return tool;
    }
    public static void feedReserve(ItemStack stack){
        if(!stack.is(MekanismItems.MEKA_TOOL))return;
        long reserve=Math.max(0,stack.getOrDefault(CoreContent.GEAR_ENERGY,0L));if(reserve==0)return;
        var tank=StorageUtils.getEnergyContainer(stack,0);if(tank==null)return;
        long left=tank.insert(reserve,Action.EXECUTE,AutomationType.INTERNAL);
        if(left==0)stack.remove(CoreContent.GEAR_ENERGY);else if(left!=reserve)stack.set(CoreContent.GEAR_ENERGY,left);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        if(!level.isClientSide&&holder instanceof Player player)for(int i=0;i<player.getInventory().getContainerSize();i++){
            if(player.getInventory().getItem(i)==stack){player.getInventory().setItem(i,convert(stack,player));break;}
        }
    }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,Player player,net.minecraft.world.InteractionHand hand){
        if(!level.isClientSide)player.setItemInHand(hand,convert(player.getItemInHand(hand),player));
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
