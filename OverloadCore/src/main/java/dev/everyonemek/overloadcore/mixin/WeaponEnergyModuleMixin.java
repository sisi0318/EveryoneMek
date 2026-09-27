package dev.everyonemek.overloadcore.mixin;

import dev.everyonemek.overloadcore.gear.WeaponItem;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.common.content.gear.shared.ModuleEnergyUnit;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only our weapons retain an above-capacity balance after a native energy-module removal. */
@Mixin(ModuleEnergyUnit.class)
public abstract class WeaponEnergyModuleMixin {
    @Inject(method="onRemoved",at=@At("HEAD"),cancellable=true,remap=false)
    private void overload$keepExistingCharge(IModule<ModuleEnergyUnit> module,IModuleContainer container,ItemStack stack,boolean last,CallbackInfo ci){
        if(stack.getItem() instanceof WeaponItem)ci.cancel();
    }
}
