package dev.everyonemek.factory;

import java.util.*;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Stable slot references for one server operation. Inventory remains in the owning hatch. */
public final class CombinedBank implements ResourceBank {
    private record Slot(ResourceBank bank, int index) {}
    private final List<Slot> items = new ArrayList<>(), fluids = new ArrayList<>(), chemicals = new ArrayList<>();

    public CombinedBank(List<? extends ResourceBank> banks) {
        for (var bank : banks) {
            for (int i = 0; i < bank.itemSlots(); i++) items.add(new Slot(bank, i));
            for (int i = 0; i < bank.fluidTanks(); i++) fluids.add(new Slot(bank, i));
            for (int i = 0; i < bank.chemicalTanks(); i++) chemicals.add(new Slot(bank, i));
        }
    }
    public int itemSlots() { return items.size(); }
    public int itemLimit(int i) { var s = items.get(i); return s.bank.itemLimit(s.index); }
    public ItemStack item(int i) { var s = items.get(i); return s.bank.item(s.index); }
    public void item(int i, ItemStack stack) { var s = items.get(i); s.bank.item(s.index, stack); }
    public int fluidTanks() { return fluids.size(); }
    public int fluidCapacity(int i) { var s = fluids.get(i); return s.bank.fluidCapacity(s.index); }
    public FluidStack fluid(int i) { var s = fluids.get(i); return s.bank.fluid(s.index); }
    public void fluid(int i, FluidStack stack) { var s = fluids.get(i); s.bank.fluid(s.index, stack); }
    public int chemicalTanks() { return chemicals.size(); }
    public long chemicalCapacity(int i) { var s = chemicals.get(i); return s.bank.chemicalCapacity(s.index); }
    public ChemicalStack chemical(int i) { var s = chemicals.get(i); return s.bank.chemical(s.index); }
    public void chemical(int i, ChemicalStack stack) { var s = chemicals.get(i); s.bank.chemical(s.index, stack); }
}
