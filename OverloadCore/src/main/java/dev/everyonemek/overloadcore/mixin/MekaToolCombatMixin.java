package dev.everyonemek.overloadcore.mixin;

import dev.everyonemek.overloadcore.gear.GearCombat;
import dev.everyonemek.overloadcore.gear.MekaCombat;
import mekanism.common.item.gear.ItemMekaTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemMekaTool.class)
public abstract class MekaToolCombatMixin extends mekanism.common.item.ItemEnergized {
    protected MekaToolCombatMixin(net.minecraft.world.item.Item.Properties properties){super(properties);}
    @Inject(method="use",at=@At("HEAD"),cancellable=true,remap=false)
    private void overload$combatUse(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,
        CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<ItemStack>> result){
        var combat=MekaCombat.begin(level,player,hand);if(combat!=null)result.setReturnValue(combat);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return MekaCombat.form(stack)!=null?MekaCombat.USE_DURATION:super.getUseDuration(stack,entity);}
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack){return MekaCombat.form(stack)!=null?net.minecraft.world.item.UseAnim.NONE:super.getUseAnimation(stack);}
    @Override public void releaseUsing(ItemStack stack,net.minecraft.world.level.Level level,LivingEntity entity,int remaining){
        MekaCombat.release(stack,level,entity,remaining);super.releaseUsing(stack,level,entity,remaining);
    }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="hurtEnemy",remap=false)
    private boolean overload$resonance(ItemStack stack,LivingEntity target,LivingEntity attacker,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        long before=MekaCombat.energy(stack);boolean result=original.call(stack,target,attacker);
        if(attacker instanceof ServerPlayer player){GearCombat.resonate(player,stack,target);dev.everyonemek.overloadcore.training.TrainingTarget.power(target,player,Math.max(0,before-MekaCombat.energy(stack)));}
        return result;
    }
}
