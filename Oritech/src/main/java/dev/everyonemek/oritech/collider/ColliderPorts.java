package dev.everyonemek.oritech.collider;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HopperBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.*;

public final class ColliderPorts {
    private static boolean valid(Collider p){return p.getLevel()!=null&&!p.isRemoved()&&p.getLevel().hasChunkAt(p.getBlockPos())&&p.getLevel().getBlockEntity(p.getBlockPos())==p;}
    public static final class Items implements IItemHandler {
        private final Collider p;private final Direction side;
        public Items(Collider p,Direction side){this.p=p;this.side=side;}
        private boolean visible(int slot){if(!valid(p)||slot<0||slot>2)return false;int mode=p.mode(side);return slot==2?mode==Collider.EJECT:mode==Collider.FEED_BOTH||mode==slot+1;}
        @Override public int getSlots(){return 3;}
        @Override public int getSlotLimit(int slot){return slot>=0&&slot<3?64:0;}
        @Override public ItemStack getStackInSlot(int slot){return visible(slot)?p.inventory.getItem(slot):ItemStack.EMPTY;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot<2&&visible(slot)&&p.accepts(slot,stack);}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(stack.isEmpty()||!isItemValid(slot,stack))return stack;
            var current=p.inventory.getItem(slot);if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,stack))return stack;
            int count=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize())-current.getCount());
            var other=p.inventory.getItem(1-slot);
            if(p.mode(side)==Collider.FEED_BOTH&&p.sameInputs(stack)&&(other.isEmpty()||ItemStack.isSameItemSameComponents(other,stack)))
                count=Math.min(count,Math.max(0,other.getCount()+(stack.getCount()+1)/2-current.getCount()));
            if(count<=0)return stack;
            if(!simulate){p.inventory.setItem(slot,stack.copyWithCount(current.getCount()+count));p.setChanged();}return stack.copyWithCount(stack.getCount()-count);
        }
        @Override public ItemStack extractItem(int slot,int count,boolean simulate){if(slot!=2||!visible(slot)||count<=0)return ItemStack.EMPTY;var current=p.inventory.getItem(slot);var result=current.copyWithCount(Math.min(count,current.getCount()));
            if(!simulate&&!result.isEmpty()){current.shrink(result.getCount());p.setChanged();}return result;}
    }
    public static final class Energy implements IEnergyStorage {
        private final Collider p;
        public Energy(Collider p){this.p=p;}
        @Override public int receiveEnergy(int max,boolean simulate){if(!valid(p)||max<=0)return 0;int received=(int)Math.min(max,Math.min(Collider.RECEIVE,Math.max(0,Collider.CAPACITY-p.energy)));if(!simulate&&received>0){p.energy+=received;p.setChanged();}return received;}
        @Override public int extractEnergy(int max,boolean simulate){return 0;}
        @Override public int getEnergyStored(){return valid(p)?(int)Math.min(Integer.MAX_VALUE,p.energy):0;}
        @Override public int getMaxEnergyStored(){return valid(p)?(int)Collider.CAPACITY:0;}
        @Override public boolean canReceive(){return valid(p);}
        @Override public boolean canExtract(){return false;}
    }
    static void eject(Collider p){if(!valid(p))return;var world=p.getLevel();for(var side:Direction.values()){
        if(p.mode(side)!=Collider.EJECT)continue;var pos=p.getBlockPos().relative(side);if(!world.hasChunkAt(pos))continue;
        var state=world.getBlockState(pos);if(state.getBlock() instanceof HopperBlock&&state.getValue(HopperBlock.FACING)==side.getOpposite())continue;
        var target=world.getCapability(Capabilities.ItemHandler.BLOCK,pos,side.getOpposite());if(target==null)continue;
        var stack=p.inventory.getItem(Collider.OUTPUT);if(stack.isEmpty())break;var remaining=ItemHandlerHelper.insertItemStacked(target,stack.copy(),false);
        if(remaining.getCount()!=stack.getCount()){p.inventory.setItem(Collider.OUTPUT,remaining);p.setChanged();}
    }}
    private ColliderPorts(){}
}
