package dev.everyonemek.gravity.corona;
import mekanism.api.*;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.attachments.containers.item.ComponentBackedInventorySlot;
import net.minecraft.world.item.ItemStack;
/** Native Mek oversized serialization; manual/external extraction still yields legal stacks. */
public final class CoronalInventorySlot extends BasicInventorySlot {
    public static final int CAPACITY=4096;
    public CoronalInventorySlot(boolean output,IContentsListener listener,int x,int y){
        super(CAPACITY,(s,a)->output||a!=AutomationType.EXTERNAL,(s,a)->!output||a==AutomationType.INTERNAL,s->true,listener,x,y);
        obeyStackLimit=false;setSlotType(output?ContainerSlotType.OUTPUT:ContainerSlotType.INPUT);
    }
    private static int capacity(ItemStack stack){return stack.isEmpty()?CAPACITY:stack.getMaxStackSize()==1?1:CAPACITY*Math.min(64,stack.getMaxStackSize())/64;}
    @Override public int getLimit(ItemStack stack){return capacity(stack);}
    @Override public ItemStack extractItem(int amount,Action action,AutomationType automation){
        if(automation!=AutomationType.INTERNAL)return super.extractItem(amount,action,automation);
        int take=Math.min(Math.max(0,amount),getCount());if(take==0)return ItemStack.EMPTY;var result=getStack().copyWithCount(take);if(action.execute())shrinkStack(take,Action.EXECUTE);return result;
    }
    public static ComponentBackedInventorySlot attached(ItemStack owner,int index,boolean output){
        return new ComponentBackedInventorySlot(owner,index,(s,a)->output||a!=AutomationType.EXTERNAL,(s,a)->!output||a==AutomationType.INTERNAL,s->true,false,CAPACITY){@Override public int getLimit(ItemStack s){return capacity(s);}};
    }
}
