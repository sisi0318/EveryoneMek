package dev.everyonemek.factory;

import java.util.*;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** A transactional view of real inventories. Aggregates never own or serialize another copy. */
public interface ResourceBank {
    int itemSlots();
    int itemLimit(int slot);
    default int itemLimit(int slot,ItemStack stack){return stack.isEmpty()?itemLimit(slot):Math.min(itemLimit(slot),stack.getMaxStackSize());}
    ItemStack item(int slot);
    void item(int slot, ItemStack stack);
    int fluidTanks();
    int fluidCapacity(int tank);
    FluidStack fluid(int tank);
    void fluid(int tank, FluidStack stack);
    int chemicalTanks();
    long chemicalCapacity(int tank);
    ChemicalStack chemical(int tank);
    void chemical(int tank, ChemicalStack stack);

    default ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= itemSlots()) return stack;
        var old = item(slot);
        if (!old.isEmpty() && !ItemStack.isSameItemSameComponents(old, stack)) return stack;
        int count = Math.min(stack.getCount(), Math.max(0, itemLimit(slot,stack) - old.getCount()));
        if (count > 0 && !simulate) item(slot, stack.copyWithCount(old.getCount() + count));
        return stack.copyWithCount(stack.getCount() - count);
    }

    default ItemStack take(int slot, int count, boolean simulate) {
        if (slot < 0 || slot >= itemSlots() || count <= 0) return ItemStack.EMPTY;
        var old = item(slot); count = Math.min(count, old.getCount());
        var result = old.copyWithCount(count);
        if (count > 0 && !simulate) item(slot, old.copyWithCount(old.getCount() - count));
        return result;
    }

    default int insertFluid(int tank, FluidStack stack, boolean simulate) {
        if (stack.isEmpty() || tank < 0 || tank >= fluidTanks()) return 0;
        var old = fluid(tank);
        if (!old.isEmpty() && !FluidStack.isSameFluidSameComponents(old, stack)) return 0;
        int count = (int)Math.min(stack.getAmount(), Math.max(0L, (long)fluidCapacity(tank) - old.getAmount()));
        if (count > 0 && !simulate) fluid(tank, stack.copyWithAmount(old.getAmount() + count));
        return count;
    }

    default long insertChem(int tank, ChemicalStack stack, boolean simulate) {
        if (stack.isEmpty() || stack.isRadioactive() || tank < 0 || tank >= chemicalTanks()) return 0;
        var old = chemical(tank);
        if (!old.isEmpty() && !ChemicalStack.isSameChemical(old, stack)) return 0;
        long count = Math.min(stack.getAmount(), Math.max(0, chemicalCapacity(tank) - old.getAmount()));
        if (count > 0 && !simulate) chemical(tank, stack.copyWithAmount(old.getAmount() + count));
        return count;
    }

    default boolean hasContents() {
        for (int i = 0; i < itemSlots(); i++) if (!item(i).isEmpty()) return true;
        for (int i = 0; i < fluidTanks(); i++) if (!fluid(i).isEmpty()) return true;
        for (int i = 0; i < chemicalTanks(); i++) if (!chemical(i).isEmpty()) return true;
        return false;
    }

    default boolean store(List<ItemStack> stacks, List<FluidStack> liquids, List<ChemicalStack> gases, boolean simulate) {
        // Only snapshot resource kinds produced by this recipe. A common item recipe need not copy every tank.
        var items = new ItemStack[stacks.isEmpty()?0:itemSlots()];
        var fluids = new FluidStack[liquids.isEmpty()?0:fluidTanks()];
        var chemicals = new ChemicalStack[gases.isEmpty()?0:chemicalTanks()];
        for (int i = 0; i < items.length; i++) items[i] = item(i).copy();
        for (int i = 0; i < fluids.length; i++) fluids[i] = fluid(i).copy();
        for (int i = 0; i < chemicals.length; i++) chemicals[i] = chemical(i).copy();
        for (var stack : stacks) {
            int left = stack.getCount();
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < items.length && left > 0; i++) {
                var old = items[i]; if (pass == 0 ? old.isEmpty() : !old.isEmpty()) continue;
                if (!old.isEmpty() && !ItemStack.isSameItemSameComponents(old, stack)) continue;
                int n = Math.min(left, Math.max(0, itemLimit(i,stack) - old.getCount()));
                if (n > 0) { items[i] = stack.copyWithCount(old.getCount() + n); left -= n; }
            }
            if (left > 0) return false;
        }
        for (var stack : liquids) {
            int left = stack.getAmount();
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < fluids.length && left > 0; i++) {
                var old = fluids[i]; if (pass == 0 ? old.isEmpty() : !old.isEmpty()) continue;
                if (!old.isEmpty() && !FluidStack.isSameFluidSameComponents(old, stack)) continue;
                int n = (int)Math.min(left, Math.max(0L, (long)fluidCapacity(i) - old.getAmount()));
                if (n > 0) { fluids[i] = stack.copyWithAmount(old.getAmount() + n); left -= n; }
            }
            if (left > 0) return false;
        }
        for (var stack : gases) {
            if (stack.isRadioactive()) return false;
            long left = stack.getAmount();
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < chemicals.length && left > 0; i++) {
                var old = chemicals[i]; if (pass == 0 ? old.isEmpty() : !old.isEmpty()) continue;
                if (!old.isEmpty() && !ChemicalStack.isSameChemical(old, stack)) continue;
                long n = Math.min(left, Math.max(0, chemicalCapacity(i) - old.getAmount()));
                if (n > 0) { chemicals[i] = stack.copyWithAmount(old.getAmount() + n); left -= n; }
            }
            if (left > 0) return false;
        }
        if (!simulate) {
            for (int i = 0; i < items.length; i++) if (!ItemStack.matches(items[i], item(i))) item(i, items[i]);
            for (int i = 0; i < fluids.length; i++) if (!FluidStack.isSameFluidSameComponents(fluids[i], fluid(i)) || fluids[i].getAmount()!=fluid(i).getAmount()) fluid(i, fluids[i]);
            for (int i = 0; i < chemicals.length; i++) if (!ChemicalStack.isSameChemical(chemicals[i], chemical(i)) || chemicals[i].getAmount()!=chemical(i).getAmount()) chemical(i, chemicals[i]);
        }
        return true;
    }
}
