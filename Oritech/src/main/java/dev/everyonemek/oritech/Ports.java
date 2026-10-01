package dev.everyonemek.oritech;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

final class Ports {
    private static Processor valid(Supplier<Processor> supplier){var p=supplier.get();return p!=null&&!p.isRemoved()&&p.getLevel()!=null&&p.getLevel().hasChunkAt(p.getBlockPos())&&p.getLevel().getBlockEntity(p.getBlockPos())==p?p:null;}
    static final class Items implements IItemHandler {
        private final Supplier<Processor> source;private final Direction side;
        Items(Supplier<Processor> p,Direction side){source=p;this.side=side;}
        private Processor p(){var p=valid(source);return p!=null&&p.complete()?p:null;}
        @Override public int getSlots(){return 8;}
        @Override public ItemStack getStackInSlot(int slot){var p=p();return p==null||slot<0||slot>=8?ItemStack.EMPTY:p.inventory.getItem(slot);}
        @Override public int getSlotLimit(int slot){return 64;}
        @Override public boolean isItemValid(int slot,ItemStack stack){var p=p();return p!=null&&p.input(side)&&slot>=0&&slot<p.profile().inputs();}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(stack.isEmpty()||!isItemValid(slot,stack))return stack;var p=p();var current=p.inventory.getItem(slot);
            if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,stack))return stack;int count=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize())-current.getCount());if(count<=0)return stack;
            if(!simulate){p.inventory.setItem(slot,stack.copyWithCount(current.getCount()+count));p.setChanged();}return stack.copyWithCount(stack.getCount()-count);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){var p=p();if(p==null||!p.output(side)||slot<4||slot>=4+p.profile().outputs()||amount<=0)return ItemStack.EMPTY;
            var current=p.inventory.getItem(slot);var out=current.copyWithCount(Math.min(amount,current.getCount()));if(!simulate&&!out.isEmpty()){current.shrink(out.getCount());p.setChanged();}return out;}
    }
    static final class Energy implements IEnergyStorage {
        private final Supplier<Processor> source;private final Direction side;
        Energy(Supplier<Processor> p,Direction side){source=p;this.side=side;}
        @Override public int receiveEnergy(int max,boolean simulate){var p=valid(source);if(p==null||!p.input(side)||max<=0)return 0;
            int amount=(int)Math.min(max,Math.min(p.energyStorage.maxInsert,Math.max(0,p.energyStorage.capacity-p.energyStorage.amount)));
            if(!simulate&&amount>0){p.energyStorage.amount+=amount;p.setChanged();}return amount;}
        @Override public int extractEnergy(int max,boolean simulate){return 0;}
        @Override public int getEnergyStored(){var p=valid(source);return p==null?0:(int)Math.min(Integer.MAX_VALUE,p.energyStorage.amount);}
        @Override public int getMaxEnergyStored(){var p=valid(source);return p==null?0:(int)Math.min(Integer.MAX_VALUE,p.energyStorage.capacity);}
        @Override public boolean canExtract(){return false;}
        @Override public boolean canReceive(){var p=valid(source);return p!=null&&p.input(side);}
    }
    static FluidStack neo(dev.architectury.fluid.FluidStack f){return new FluidStack(BuiltInRegistriesShim.holder(f.getFluid()),(int)Math.min(Integer.MAX_VALUE,f.getAmount()),f.getPatch());}
    static dev.architectury.fluid.FluidStack ori(FluidStack f){return dev.architectury.fluid.FluidStack.create(f.getFluid(),f.getAmount(),f.getComponentsPatch());}
    // Holder construction keeps component patches intact across the two public fluid APIs.
    private static final class BuiltInRegistriesShim {static net.minecraft.core.Holder<net.minecraft.world.level.material.Fluid> holder(net.minecraft.world.level.material.Fluid f){return net.minecraft.core.registries.BuiltInRegistries.FLUID.wrapAsHolder(f);}}
    static final class Fluids implements IFluidHandler {
        private final Supplier<Processor> source;private final Direction side;
        Fluids(Supplier<Processor> p,Direction side){source=p;this.side=side;}
        private Processor p(){var p=valid(source);return p!=null&&p.complete()&&p.fluidEnabled()?p:null;}
        @Override public int getTanks(){return 4;}
        @Override public FluidStack getFluidInTank(int tank){var p=p();return p==null||tank<0||tank>3?FluidStack.EMPTY:neo(p.tanks().get(tank).getStack());}
        @Override public int getTankCapacity(int tank){var p=p();return p==null||tank<0||tank>3?0:(int)p.tanks().get(tank).getCapacity();}
        @Override public boolean isFluidValid(int tank,FluidStack f){var p=p();return p!=null&&p.input(side)&&tank==0;}
        @Override public int fill(FluidStack f,FluidAction action){var p=p();if(p==null||!p.input(side)||f.isEmpty())return 0;int amount=(int)p.fluidIn.insert(ori(f),action.simulate());if(action.execute()&&amount>0)p.setChanged();return amount;}
        @Override public FluidStack drain(FluidStack f,FluidAction action){var p=p();if(p==null||!p.output(side)||f.isEmpty())return FluidStack.EMPTY;
            for(int i=1;i<=p.outputTanks();i++){var tank=p.tanks().get(i);long amount=tank.extract(ori(f),action.simulate());if(amount>0){if(action.execute())p.setChanged();return f.copyWithAmount((int)amount);}}return FluidStack.EMPTY;}
        @Override public FluidStack drain(int max,FluidAction action){var p=p();if(p==null||max<=0)return FluidStack.EMPTY;for(int i=1;i<=p.outputTanks();i++){var f=neo(p.tanks().get(i).getStack());if(!f.isEmpty())return drain(f.copyWithAmount(Math.min(max,f.getAmount())),action);}return FluidStack.EMPTY;}
    }
    static void eject(Processor p){if(!p.complete())return;var cells=new ArrayList<BlockPos>();cells.add(p.getBlockPos());cells.addAll(p.positions());var occupied=new HashSet<>(cells);var world=p.getLevel();
        for(var pos:cells)for(var side:Direction.values()){
            var target=pos.relative(side);if(occupied.contains(target)||!p.output(side)||!world.hasChunkAt(target))continue;
            var handler=world.getCapability(Capabilities.ItemHandler.BLOCK,target,side.getOpposite());if(handler!=null)for(int i=4;i<4+p.profile().outputs();i++){
                var stack=p.inventory.getItem(i);if(stack.isEmpty())continue;var remaining=ItemHandlerHelper.insertItemStacked(handler,stack.copy(),false);int moved=stack.getCount()-remaining.getCount();if(moved>0){stack.shrink(moved);p.setChanged();}}
            var fluid=world.getCapability(Capabilities.FluidHandler.BLOCK,target,side.getOpposite());if(fluid!=null)for(int i=1;i<=p.outputTanks();i++){
                var tank=p.tanks().get(i);var f=neo(tank.getStack());if(f.isEmpty())continue;int moved=fluid.fill(f,IFluidHandler.FluidAction.EXECUTE);if(moved>0){tank.extract(tank.getStack().copyWithAmount(moved),false);p.setChanged();}}
        }
    }
}
