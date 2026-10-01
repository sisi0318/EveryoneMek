package dev.everyonemek.oritech;

import java.util.function.IntSupplier;
import dev.architectury.fluid.FluidStack;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.fluid.containers.SimpleInOutFluidStorage;

/** Keep native NBT/network fluid data while deriving capacity from installed addons. */
final class ProcessorTank extends SimpleFluidStorage {
    private final int index;
    private final IntSupplier count;
    ProcessorTank(int index,IntSupplier count,Runnable changed){super(FluidCapacityAddon.capacity(index,0),changed);this.index=index;this.count=count;}
    @Override public long getCapacity(){return FluidCapacityAddon.capacity(index,count.getAsInt());}
    @Override public long insert(FluidStack stack,boolean simulate){
        return SimpleInOutFluidStorage.insertTo(stack,simulate,getCapacity(),getStack(),this::setStack);
    }
}
