package dev.everyonemek.factory;
import mekanism.api.*;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.MathUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
/** A view over native induction cells, never another persistent battery. */
public final class FactoryEnergy implements IEnergyContainer {
    private final Controller c;private long tick=Long.MIN_VALUE,in,out;
    public FactoryEnergy(Controller c){this.c=c;}
    private void reset(){long t=c.getLevel().getGameTime();if(t!=tick){tick=t;in=out=0;}}
    @Override public long getEnergy(){if(c.getLevel()!=null&&c.getLevel().isClientSide)return c.clientEnergy;if(c.getLevel()==null||c.structure==null||!c.structure.valid())return 0;long sum=0;for(var cell:c.structure.cells)sum=MathUtils.addClamped(sum,cell.getEnergyContainer().getEnergy());return sum;}
    @Override public long getMaxEnergy(){if(c.getLevel()!=null&&c.getLevel().isClientSide)return c.clientCapacity;if(c.getLevel()==null||c.structure==null||!c.structure.valid())return 0;long sum=0;for(var cell:c.structure.cells)sum=MathUtils.addClamped(sum,cell.getEnergyContainer().getMaxEnergy());return sum;}
    public long available(){if(c.getLevel()==null||c.structure==null||!c.structure.valid())return 0;reset();return Math.min(getEnergy(),Math.max(0,c.structure.transfer-out));}
    @Override public long insert(long amount,Action action,AutomationType automation){
        if(amount<=0||automation==AutomationType.EXTERNAL||!c.structure.valid())return amount;reset();
        long budget=Math.min(Math.max(0,c.structure.transfer-in),amount),left=budget;
        for(var cell:c.structure.cells){var tank=cell.getEnergyContainer();long n=Math.min(left,tank.getNeeded());if(action.execute()&&n>0)tank.setEnergy(tank.getEnergy()+n);left-=n;if(left==0)break;}
        if(action.execute())in+=budget-left;return amount-budget+left;
    }
    @Override public long extract(long amount,Action action,AutomationType automation){
        if(amount<=0||automation==AutomationType.EXTERNAL||!c.structure.valid())return 0;reset();long n=Math.min(amount,available());
        if(action.execute()){long left=n;for(var cell:c.structure.cells){var tank=cell.getEnergyContainer();long used=Math.min(left,tank.getEnergy());if(used>0)tank.setEnergy(tank.getEnergy()-used);left-=used;if(left==0)break;}out+=n;}
        return n;
    }
    @Override public void setEnergy(long value){if(c.getLevel()!=null&&c.getLevel().isClientSide)c.clientEnergy=value;else throw new UnsupportedOperationException("Energy belongs to induction cells");}
    @Override public void onContentsChanged(){}
    @Override public CompoundTag serializeNBT(HolderLookup.Provider r){return new CompoundTag();}
    @Override public void deserializeNBT(HolderLookup.Provider r,CompoundTag tag){}
}
