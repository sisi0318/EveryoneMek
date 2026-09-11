package dev.everyonemek.forbidden.mixin;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BasicInventorySlot.class, remap = false)
public interface UpgradeSlotAccess {
    @Accessor("validator") Predicate<ItemStack> forbiddenmekanism$validator();
    @Mutable @Accessor("validator") void forbiddenmekanism$validator(Predicate<ItemStack> value);
    @Accessor("canInsert") BiPredicate<ItemStack, AutomationType> forbiddenmekanism$canInsert();
    @Mutable @Accessor("canInsert") void forbiddenmekanism$canInsert(BiPredicate<ItemStack, AutomationType> value);
}
