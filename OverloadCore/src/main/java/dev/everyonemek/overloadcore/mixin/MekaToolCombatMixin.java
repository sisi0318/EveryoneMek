package dev.everyonemek.overloadcore.mixin;

import dev.everyonemek.overloadcore.gear.GearCombat;
import mekanism.common.item.gear.ItemMekaTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemMekaTool.class)
public abstract class MekaToolCombatMixin {
    @Inject(method="hurtEnemy",at=@At("TAIL"),remap=false)
    private void overload$resonance(ItemStack stack,LivingEntity target,LivingEntity attacker,CallbackInfoReturnable<Boolean> result){
        if(attacker instanceof ServerPlayer player)GearCombat.resonate(player,stack,target);
    }
}
