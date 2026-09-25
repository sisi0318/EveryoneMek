package dev.everyonemek.gravity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Menu transfers can mutate an existing ItemStack without invoking the handler's setter. */
public final class FuelInventorySlot extends SlotItemHandler {
    private final BlockEntity owner;
    public FuelInventorySlot(BlockEntity owner,IItemHandler inventory,int index,int x,int y){
        super(inventory,index,x,y);this.owner=owner;
    }
    @Override public void setChanged(){
        super.setChanged();
        // SlotItemHandler's inherited callback only marks its empty backing Container.
        // The real inventory belongs to this block entity and must mark its chunk dirty.
        if(owner.getLevel()!=null&&!owner.getLevel().isClientSide)owner.setChanged();
    }
}
