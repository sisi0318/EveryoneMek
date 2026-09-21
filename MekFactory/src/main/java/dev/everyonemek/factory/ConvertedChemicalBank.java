package dev.everyonemek.factory;

import mekanism.api.chemical.ChemicalStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** The factory may use converted chemicals, never the dedicated hatch's raw auxiliary items. */
public record ConvertedChemicalBank(Buffers source) implements ResourceBank {
    public int itemSlots(){return 0;}
    public int itemLimit(int i){return 0;}
    public ItemStack item(int i){throw new IndexOutOfBoundsException(i);}
    public void item(int i,ItemStack stack){throw new IndexOutOfBoundsException(i);}
    public int fluidTanks(){return 0;}
    public int fluidCapacity(int i){return 0;}
    public FluidStack fluid(int i){throw new IndexOutOfBoundsException(i);}
    public void fluid(int i,FluidStack stack){throw new IndexOutOfBoundsException(i);}
    public int chemicalTanks(){return source.chemicalTanks();}
    public long chemicalCapacity(int i){return source.chemicalCapacity(i);}
    public ChemicalStack chemical(int i){return source.chemical(i);}
    public void chemical(int i,ChemicalStack stack){source.chemical(i,stack);}
}
