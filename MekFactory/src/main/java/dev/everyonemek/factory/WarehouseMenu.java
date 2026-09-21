package dev.everyonemek.factory;

import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.inventory.container.sync.chemical.SyncableChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import java.util.Optional;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

/** A warehouse menu owns no inventory; it addresses one hatch, or one explicitly selected legacy bank. */
public final class WarehouseMenu extends MekanismContainer {
    public static final int PAGE_SIZE=27;
    public final BlockEntity host;
    public final Buffers stock;
    public final boolean output,legacy,conversion;
    public int conversionStatus;
    private final BlockPos clicked;
    public int page,visibleSlots,pageCount=1;
    private int pageRevision;
    public boolean awaitingPage;
    public long tankCapacity;
    private int playerStart;

    public WarehouseMenu(int id,Inventory inv,BlockEntity host,BlockPos clicked,boolean output){
        super(Content.WAREHOUSE_MENU,id,inv);this.host=host;this.clicked=clicked;this.output=output;legacy=host instanceof Controller;conversion=host instanceof Part part&&part.isConverter();
        stock=host instanceof Part part?part.storage():output?((Controller)host).outputs:((Controller)host).inputs;
        track(SyncableInt.create(()->page,v->page=v));
        track(SyncableInt.create(this::visibleCount,v->visibleSlots=v));
        track(SyncableInt.create(()->Math.max(1,(visibleCount()+PAGE_SIZE-1)/PAGE_SIZE),v->pageCount=v));
        track(SyncableInt.create(()->pageRevision,v->{pageRevision=v;awaitingPage=false;}));
        track(SyncableLong.create(stock::capacity,v->tankCapacity=v));
        track(SyncableInt.create(()->conversion?ChemicalConversions.status((Part)host):0,v->conversionStatus=v));
        for(int i=0;i<Buffers.TANKS;i++){final int n=i;
            track(SyncableFluidStack.create(()->stock.fluids[n],v->stock.fluids[n]=v));
            track(SyncableChemicalStack.create(()->stock.chemicals[n],v->stock.chemicals[n]=v));
        }
        visibleSlots=visibleCount();addSlotsAndOpen();
    }

    @Override protected void addSlots(){
        for(int i=0;i<PAGE_SIZE;i++){
            final int local=i;
            var handler=new IItemHandlerModifiable(){
                private ItemStack displayed=ItemStack.EMPTY;
                private int index(){return page*PAGE_SIZE+local;}
                public int getSlots(){return 1;}
                public ItemStack getStackInSlot(int ignored){return getLevel().isClientSide?displayed:stock.item(index());}
                public void setStackInSlot(int ignored,ItemStack value){if(getLevel().isClientSide)displayed=value;else stock.item(index(),value);}
                public int getSlotLimit(int ignored){return legacy?getStackInSlot(0).getCount():index()<visibleSlots?stock.itemLimit(index()):0;}
                public boolean isItemValid(int ignored,ItemStack stack){return !output&&!legacy&&index()<stock.slots()&&stock.acceptsItem(stack);}
                public ItemStack insertItem(int ignored,ItemStack stack,boolean simulate){return output||legacy?stack:stock.insert(index(),stack,simulate);}
                public ItemStack extractItem(int ignored,int count,boolean simulate){
                    if(!getLevel().isClientSide)return stock.take(index(),Math.min(count,stock.item(index()).getMaxStackSize()),simulate);
                    int n=Math.min(count,Math.min(displayed.getCount(),displayed.getMaxStackSize()));var result=displayed.copyWithCount(n);if(!simulate)displayed=displayed.copyWithCount(displayed.getCount()-n);return result;
                }
            };
            addSlot(new SlotItemHandler(handler,0,8+i%9*18,30+i/9*18){
                @Override public boolean isActive(){return page*PAGE_SIZE+local<visibleSlots;}
                @Override public int getMaxStackSize(ItemStack stack){int index=page*PAGE_SIZE+local;return legacy||index>=stock.slots()?getItem().getCount():stock.itemLimit(index,stack);}
                @Override public ItemStack safeInsert(ItemStack stack,int amount){
                    if(stack.isEmpty()||!mayPlace(stack))return stack;
                    var old=getItem();if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,stack))return stack;
                    int moved=Math.min(Math.min(Math.max(0,amount),stack.getCount()),Math.max(0,getMaxStackSize(stack)-old.getCount()));
                    if(moved>0){setByPlayer(stack.copyWithCount(old.getCount()+moved));stack.shrink(moved);}return stack;
                }
                @Override public Optional<ItemStack> tryRemove(int count,int decrement,Player player){
                    if(!isActive()||!mayPickup(player))return Optional.empty();
                    var removed=remove(Math.min(count,decrement));return removed.isEmpty()?Optional.empty():Optional.of(removed);
                }
                @Override public boolean mayPickup(Player p){return getLevel().isClientSide||canExecute(p);}
                @Override public boolean mayPlace(ItemStack stack){return (getLevel().isClientSide||canExecute(inv.player))&&super.mayPlace(stack);}
            });
        }
        playerStart=slots.size();
    }
    @Override protected int getInventoryXOffset(){return 8;}
    @Override protected int getInventoryYOffset(){return 138;}

    private int visibleCount(){int n=legacy?0:stock.slots();for(int i=stock.items.length-1;i>=n;i--)if(!stock.items[i].isEmpty()){n=i+1;break;}return Math.max(1,n);}
    @Override public boolean canPlayerAccess(Player player){return getLevel().isClientSide||stillValid(player);}
    @Override public boolean stillValid(Player player){
        if(getLevel().isClientSide)return !host.isRemoved();
        if(host.isRemoved()||!getLevel().hasChunkAt(host.getBlockPos())||getLevel().getBlockEntity(host.getBlockPos())!=host)return false;
        if(host instanceof Part p)return p.hasStorage()&&p.canOpen(player)&&p.isOutput()==output;
        return FactoryMenu.access(player,(Controller)host,clicked);
    }
    private boolean canExecute(Player p){return !getLevel().isClientSide&&stillValid(p);}

    @Override public boolean clickMenuButton(Player p,int id){
        if(!canExecute(p))return false;
        if(id==1||id==2){if(getCarried().isEmpty())page=Math.clamp(page+(id==1?-1:1),0,Math.max(0,(visibleCount()-1)/PAGE_SIZE));pageRevision++;broadcastChanges();return true;}
        return false;
    }
    /** Large stacks stay in the warehouse. Cursors, hotbars, drops and normal inventories get normal stacks. */
    @Override public void clicked(int index,int button,ClickType type,Player player){
        if(!getLevel().isClientSide&&!canExecute(player))return;
        if(index<0||index>=playerStart){super.clicked(index,button,type,player);return;}
        var slot=slots.get(index);if(!slot.isActive())return;
        var stored=slot.getItem();var carried=getCarried();
        if(stored.isEmpty()||stored.getCount()<=stored.getMaxStackSize()){super.clicked(index,button,type,player);return;}
        if(type==ClickType.PICKUP&&(button==0||button==1)){
            if(!slot.mayPickup(player))return;
            if(carried.isEmpty()){
                if(stored.isEmpty())return;int size=Math.min(stored.getCount(),stored.getMaxStackSize());
                var taken=slot.remove(button==0?size:(size+1)/2);setCarried(taken);slot.onTake(player,taken);
            }else if(slot.mayPlace(carried)){
                if(stored.isEmpty()||ItemStack.isSameItemSameComponents(stored,carried))setCarried(slot.safeInsert(carried,button==0?carried.getCount():1));
                else if(stored.getCount()<=stored.getMaxStackSize()&&carried.getCount()<=slot.getMaxStackSize(carried)){setCarried(stored.copy());slot.setByPlayer(carried);}
            }else if(!stored.isEmpty()&&ItemStack.isSameItemSameComponents(stored,carried)){
                int room=carried.getMaxStackSize()-carried.getCount();var taken=slot.remove(Math.min(room,button==0?room:1));carried.grow(taken.getCount());setCarried(carried);slot.onTake(player,taken);
            }
            return;
        }
        if(type==ClickType.SWAP&&(button>=0&&button<9||button==40)){
            var inventory=player.getInventory();var incoming=inventory.getItem(button);
            if(stored.isEmpty()){
                if(slot.mayPlace(incoming))inventory.setItem(button,slot.safeInsert(incoming.copy()));
            }else if(slot.mayPickup(player)){
                if(incoming.isEmpty()){var taken=slot.remove(stored.getMaxStackSize());inventory.setItem(button,taken);slot.onTake(player,taken);}
                else if(slot.mayPlace(incoming)){
                    if(stored.getCount()<=stored.getMaxStackSize()&&incoming.getCount()<=slot.getMaxStackSize(incoming)){inventory.setItem(button,stored.copy());slot.setByPlayer(incoming);}
                    else if(ItemStack.isSameItemSameComponents(stored,incoming)&&incoming.getCount()<=Math.max(0,slot.getMaxStackSize(incoming)-(stored.getCount()-stored.getMaxStackSize()))){
                        var taken=slot.remove(stored.getMaxStackSize());slot.safeInsert(incoming.copy());inventory.setItem(button,taken);slot.onTake(player,taken);
                    }
                }
            }
            inventory.setChanged();return;
        }
        super.clicked(index,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!canExecute(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var original=slot.getItem().copy();
        if(index<playerStart){var left=ItemHandlerHelper.insertItemStacked(new RangedWrapper(new InvWrapper(p.getInventory()),0,36),original,false);int moved=original.getCount()-left.getCount();if(moved<=0)return ItemStack.EMPTY;stock.take(page*PAGE_SIZE+index,moved,false);slot.setChanged();return original;}
        if(output||legacy)return ItemStack.EMPTY;
        var left=original;for(int pass=0;pass<2;pass++)for(int i=0;i<stock.slots()&&!left.isEmpty();i++){if(pass==0?stock.item(i).isEmpty():!stock.item(i).isEmpty())continue;left=stock.insert(i,left,false);}
        if(left.getCount()==original.getCount())return ItemStack.EMPTY;slot.setByPlayer(left);return original;
    }

    public static WarehouseMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf buffer){
        var host=inv.player.level().getBlockEntity(buffer.readBlockPos());var clicked=buffer.readBlockPos();boolean output=buffer.readBoolean();
        return new WarehouseMenu(id,inv,host,clicked,output);
    }
    public static void open(Player p,Part part){
        if(p instanceof ServerPlayer server&&part.hasStorage()&&part.canOpen(p))open(server,part,part.getBlockPos(),part.isOutput());
    }
    public static void openLegacy(Player p,Controller c,BlockPos clicked,boolean output){if(p instanceof ServerPlayer server&&FactoryMenu.access(p,c,clicked))open(server,c,clicked,output);}
    private static void open(ServerPlayer p,BlockEntity host,BlockPos clicked,boolean output){
        var title=Content.text(host instanceof Controller?(output?"legacy_output":"legacy_input"):host instanceof Part part&&part.isConverter()?"conversion_warehouse":(output?"output_warehouse":"input_warehouse"));
        p.openMenu(new SimpleMenuProvider((id,inv,player)->new WarehouseMenu(id,inv,host,clicked,output),title),b->{b.writeBlockPos(host.getBlockPos());b.writeBlockPos(clicked);b.writeBoolean(output);});
    }
}
