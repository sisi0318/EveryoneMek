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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/** A warehouse menu owns no inventory; it addresses one hatch, or one explicitly selected legacy bank. */
public final class WarehouseMenu extends MekanismContainer {
    public static final int PAGE_SIZE=27;
    public final BlockEntity host;
    public final Buffers stock;
    public final boolean output,legacy;
    private final BlockPos clicked;
    public int page,visibleSlots,pageCount=1;
    private int pageRevision;
    public boolean awaitingPage;
    public long tankCapacity;
    private int playerStart;

    public WarehouseMenu(int id,Inventory inv,BlockEntity host,BlockPos clicked,boolean output){
        super(Content.WAREHOUSE_MENU,id,inv);this.host=host;this.clicked=clicked;this.output=output;legacy=host instanceof Controller;
        stock=host instanceof Part part?part.storage():output?((Controller)host).outputs:((Controller)host).inputs;
        track(SyncableInt.create(()->page,v->page=v));
        track(SyncableInt.create(this::visibleCount,v->visibleSlots=v));
        track(SyncableInt.create(()->Math.max(1,(visibleCount()+PAGE_SIZE-1)/PAGE_SIZE),v->pageCount=v));
        track(SyncableInt.create(()->pageRevision,v->{pageRevision=v;awaitingPage=false;}));
        track(SyncableLong.create(stock::capacity,v->tankCapacity=v));
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
                public boolean isItemValid(int ignored,ItemStack stack){return !output&&!legacy&&index()<stock.slots();}
                public ItemStack insertItem(int ignored,ItemStack stack,boolean simulate){return output||legacy?stack:stock.insert(index(),stack,simulate);}
                public ItemStack extractItem(int ignored,int count,boolean simulate){
                    if(!getLevel().isClientSide)return stock.take(index(),count,simulate);
                    int n=Math.min(count,displayed.getCount());var result=displayed.copyWithCount(n);if(!simulate)displayed=displayed.copyWithCount(displayed.getCount()-n);return result;
                }
            };
            addSlot(new SlotItemHandler(handler,0,8+i%9*18,30+i/9*18){
                @Override public boolean isActive(){return page*PAGE_SIZE+local<visibleSlots;}
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
        if(host instanceof Part p)return p.isPort()&&p.canOpen(player)&&p.getBlockState().getValue(PartBlock.OUTPUT)==output;
        return FactoryMenu.access(player,(Controller)host,clicked);
    }
    private boolean canExecute(Player p){return !getLevel().isClientSide&&stillValid(p);}

    @Override public boolean clickMenuButton(Player p,int id){
        if(!canExecute(p))return false;
        if(id==1||id==2){if(getCarried().isEmpty())page=Math.clamp(page+(id==1?-1:1),0,Math.max(0,(visibleCount()-1)/PAGE_SIZE));pageRevision++;broadcastChanges();return true;}
        return false;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!canExecute(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var original=slot.getItem().copy();
        if(index<playerStart){var left=ItemHandlerHelper.insertItemStacked(new InvWrapper(p.getInventory()),original,false);int moved=original.getCount()-left.getCount();if(moved<=0)return ItemStack.EMPTY;slot.remove(moved);slot.setChanged();return original;}
        if(output||legacy)return ItemStack.EMPTY;
        var left=original;for(int i=0;i<stock.slots()&&!left.isEmpty();i++)left=stock.insert(i,left,false);
        if(left.getCount()==original.getCount())return ItemStack.EMPTY;slot.setByPlayer(left);return original;
    }

    public static WarehouseMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf buffer){
        var host=inv.player.level().getBlockEntity(buffer.readBlockPos());var clicked=buffer.readBlockPos();boolean output=buffer.readBoolean();
        return new WarehouseMenu(id,inv,host,clicked,output);
    }
    public static void open(Player p,Part part){
        if(p instanceof ServerPlayer server&&part.isPort()&&part.canOpen(p))open(server,part,part.getBlockPos(),part.getBlockState().getValue(PartBlock.OUTPUT));
    }
    public static void openLegacy(Player p,Controller c,BlockPos clicked,boolean output){if(p instanceof ServerPlayer server&&FactoryMenu.access(p,c,clicked))open(server,c,clicked,output);}
    private static void open(ServerPlayer p,BlockEntity host,BlockPos clicked,boolean output){
        var title=Content.text(host instanceof Controller?(output?"legacy_output":"legacy_input"):(output?"output_warehouse":"input_warehouse"));
        p.openMenu(new SimpleMenuProvider((id,inv,player)->new WarehouseMenu(id,inv,host,clicked,output),title),b->{b.writeBlockPos(host.getBlockPos());b.writeBlockPos(clicked);b.writeBoolean(output);});
    }
}
