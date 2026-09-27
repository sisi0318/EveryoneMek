package dev.everyonemek.overloadcore.gear;

import java.util.List;
import dev.everyonemek.overloadcore.*;
import mekanism.api.gear.IModuleHelper;
import mekanism.common.content.gear.IModuleContainerItem;
import mekanism.common.item.ItemEnergized;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public final class WeaponItem extends ItemEnergized implements IModuleContainerItem {
    public final boolean rail;
    public WeaponItem(boolean rail) {
        super(IModuleHelper.INSTANCE.applyModuleContainerProperties(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
              .attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID,rail?1:3,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_ID,rail?-3:-2.4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build())));
        this.rail = rail;
    }
    public int chargeTicks(ItemStack stack) { return Math.max(6,(rail?CoreConfig.RAIL_CHARGE.get():CoreConfig.BLADE_CHARGE.get())-5*GearEffects.level(stack,GearUpgrade.ACCELERATOR)); }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return rail?UseAnim.BOW:UseAnim.SPEAR;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server && rail && (player.isShiftKeyDown()||GearCombat.ammo(stack)==0)){
            GearCombat.reload(server,stack);if(player.isShiftKeyDown()||GearCombat.ammo(stack)==0)return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        if(GearEnergy.stored(stack)<GearCombat.joules(rail?CoreConfig.RAIL_COST.get():CoreConfig.BLADE_BURST_COST.get())){
            if(player instanceof ServerPlayer server)server.displayClientMessage(CoreContent.text("weapon.no_energy"),true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
        if(entity instanceof ServerPlayer player && getUseDuration(stack,entity)-remaining>=chargeTicks(stack) && !player.getCooldowns().isOnCooldown(this)){
            boolean fired=rail?GearCombat.shoot(player,stack):GearCombat.burst(player,stack);
            if(fired)player.getCooldowns().addCooldown(this,rail?10:16);
        }
    }
    @Override public void adjustAttributes(net.neoforged.neoforge.event.ItemAttributeModifierEvent event){
        IModuleContainerItem.super.adjustAttributes(event);
        if(!rail && GearEnergy.stored(event.getItemStack())>=GearCombat.joules(CoreConfig.BLADE_COST.get()))
            event.replaceModifier(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID,CoreConfig.BLADE_DAMAGE.get()-1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!rail && attacker instanceof ServerPlayer player){GearCombat.pay(stack,CoreConfig.BLADE_COST.get());GearCombat.resonate(player,stack,target);}
        return true;
    }
    @Override public boolean isBarVisible(ItemStack stack){return true;}
    @Override public void onDestroyed(net.minecraft.world.entity.item.ItemEntity entity,net.minecraft.world.damagesource.DamageSource source){IModuleHelper.INSTANCE.dropModuleContainerContents(entity,source);}
    @Override public int getBarWidth(ItemStack stack){return GearEnergy.bar(stack);}
    @Override public int getBarColor(ItemStack stack){return rail?0x72DFC0:0x66DF88;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        GearEnergy.tooltip(stack,lines);lines.add(CoreContent.text(rail?"weapon.rail_hint":"weapon.blade_hint"));
        if(rail)lines.add(CoreContent.text("weapon.magazine",GearCombat.ammo(stack),GearCombat.magazine(stack)));
        lines.add(CoreContent.text("weapon.charge",chargeTicks(stack)/20D));addModuleDetails(stack,lines);
    }
}
