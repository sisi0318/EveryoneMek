package dev.everyonemek.gravity.client;
import dev.everyonemek.gravity.*;
import mekanism.api.*;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.chemical.*;
public final class Views {
    public record Chemical(Controller c,ReactorMenu menu,boolean heated) implements IChemicalTank {
        public ChemicalStack getStack(){return heated?c.hot:c.cold;}
        public long getCapacity(){return menu.tankCapacity;}
        public boolean isValid(ChemicalStack s){return false;}
        public ChemicalStack insert(ChemicalStack s,Action a,AutomationType t){return s;}
        public ChemicalStack extract(long n,Action a,AutomationType t){return ChemicalStack.EMPTY;}
        public void setStack(ChemicalStack s){throw new UnsupportedOperationException("Display only");}
        public void setStackUnchecked(ChemicalStack s){setStack(s);}
        public void onContentsChanged(){}
    }
    public record Energy(Controller c,ReactorMenu menu) implements IEnergyContainer {
        public long getEnergy(){return c.stored;}public long getMaxEnergy(){return menu.capacity;}
        public void setEnergy(long n){throw new UnsupportedOperationException("Display only");}
        public long insert(long n,Action a,AutomationType t){return n;}public long extract(long n,Action a,AutomationType t){return 0;}
        public void onContentsChanged(){}
        public net.minecraft.nbt.CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider r){return new net.minecraft.nbt.CompoundTag();}
        public void deserializeNBT(net.minecraft.core.HolderLookup.Provider r,net.minecraft.nbt.CompoundTag t){}
    }
    private Views(){}
}
