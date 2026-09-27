package dev.everyonemek.overloadcore.gear;

import java.util.List;
import dev.everyonemek.overloadcore.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** One stack-owned J balance. A sealed ward commits legitimate changes to its custody record. */
public final class GearEnergy implements IMekanismStrictEnergyHandler, IEnergyContainer {
    private final ItemStack stack;
    public GearEnergy(ItemStack stack) { this.stack = stack; }
    public static void register(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerItem(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.item(), (stack, ignored) -> new GearEnergy(stack), CoreContent.WARD.get(), CoreContent.RAILGUN.get(), CoreContent.BLADE.get());
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM,
              (stack, ignored) -> new mekanism.common.integration.energy.forgeenergy.ForgeEnergyIntegration(new GearEnergy(stack)), CoreContent.WARD.get(), CoreContent.RAILGUN.get(), CoreContent.BLADE.get());
    }
    public static long stored(ItemStack stack) { return Math.clamp(stack.getOrDefault(CoreContent.GEAR_ENERGY, 0L), 0, Long.MAX_VALUE / 4); }
    public static long capacity(ItemStack stack) {
        if (stack.is(CoreContent.WARD)) {
            var c = mekanism.api.gear.IModuleHelper.INSTANCE.getModuleContainer(stack);
            int n = c == null ? 0 : c.installedCount(EquipmentModules.get(GearUpgrade.CAPACITOR));
            if (n == 0) return 0;
            return EnergyUnit.FORGE_ENERGY.convertFrom((long) CoreConfig.WARD_COST_FE.get() * n * CoreConfig.CAPACITOR_CHARGES.get());
        }
        if (stack.getItem() instanceof WeaponItem weapon) {
            int n = energyUnits(stack);
            return EnergyUnit.FORGE_ENERGY.convertFrom((weapon.rail ? 2000000L : 1000000L) << n);
        }
        return 0;
    }
    private static int energyUnits(ItemStack stack) { var c=mekanism.api.gear.IModuleHelper.INSTANCE.getModuleContainer(stack);return c==null?0:Math.min(8,c.installedCount(mekanism.common.registries.MekanismModules.ENERGY_UNIT)); }
    public static int bar(ItemStack stack) { return (int)Math.clamp(Math.round(13D * stored(stack) / Math.max(1, capacity(stack))), 0, 13); }
    public static void tooltip(ItemStack stack, List<Component> lines) {
        if (capacity(stack) > 0 || stored(stack) > 0) lines.add(CoreContent.text("gear.energy",
              mekanism.common.util.text.EnergyDisplay.of(stored(stack)), mekanism.common.util.text.EnergyDisplay.of(capacity(stack))));
    }
    private boolean available() { return !stack.isEmpty() && (!stack.is(CoreContent.WARD) || WardCustody.energyAccess(stack)); }
    private boolean write(long amount) {
        if (!available()) return false;
        long value = Math.clamp(amount, 0, Math.max(getEnergy(), getMaxEnergy()));
        if (stack.is(CoreContent.WARD)) return WardCustody.energy(stack, value);
        stack.set(CoreContent.GEAR_ENERGY, value); return true;
    }
    @Override public long getEnergy() { return stored(stack); }
    @Override public long getMaxEnergy() { return capacity(stack); }
    @Override public long getNeeded() { return Math.max(0, getMaxEnergy() - getEnergy()); }
    @Override public void setEnergy(long energy) { write(energy); }
    @Override public long insert(long amount, Action action, AutomationType type) {
        if (amount <= 0 || !available()) return amount;
        long rate = CoreConfig.GEAR_CHARGE_RATE.get().longValue() << (stack.getItem() instanceof WeaponItem ? energyUnits(stack) : 0);
        long accepted = Math.min(amount, Math.min(getNeeded(), EnergyUnit.FORGE_ENERGY.convertFrom(rate)));
        return accepted > 0 && (!action.execute() || write(getEnergy() + accepted)) ? amount - accepted : amount;
    }
    @Override public long extract(long amount, Action action, AutomationType type) {
        if (amount <= 0 || !available() || type == AutomationType.EXTERNAL) return 0;
        long removed = Math.min(amount, getEnergy());
        return removed > 0 && (!action.execute() || write(getEnergy() - removed)) ? removed : 0;
    }
    @Override public Direction getEnergySideFor() { return null; }
    @Override public List<IEnergyContainer> getEnergyContainers(Direction side) { return List.of(this); }
    @Override public void onContentsChanged() { }
    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) { setEnergy(tag.getLong("stored")); }
}
