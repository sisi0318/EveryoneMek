package dev.everyonemek.factory;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
public final class FactoryMenu extends MekanismTileContainer<Controller> {
    public static final int PAGE_SIZE=9;
    private BlockPos clicked;public int inputPage,outputPage;private int pageRevision;public boolean awaitingPage;private final int bankStart;
    public FactoryMenu(int id,Inventory inv,Controller c,BlockPos clicked){super(Content.MENU,id,inv,c);this.clicked=clicked;bankStart=slots.size();
        track(SyncableInt.create(()->inputPage,v->inputPage=v));track(SyncableInt.create(()->outputPage,v->outputPage=v));
        track(SyncableInt.create(()->pageRevision,v->{pageRevision=v;awaitingPage=false;}));
        for(boolean out:new boolean[]{false,true})for(int i=0;i<PAGE_SIZE;i++){
            final boolean output=out;final int local=i;var b=out?c.outputs:c.inputs;
            var view=new IItemHandlerModifiable(){private ItemStack displayed=ItemStack.EMPTY;private int index(){return (output?outputPage:inputPage)*PAGE_SIZE+local;}
                // Vanilla slot packets precede Mek property packets. Client slots are page-independent display cells.
                public int getSlots(){return 1;}public ItemStack getStackInSlot(int ignored){return c.getLevel().isClientSide?displayed:b.items[index()];}
                public void setStackInSlot(int ignored,ItemStack stack){if(c.getLevel().isClientSide)displayed=stack;else{b.items[index()]=stack;b.changed();}}
                public ItemStack insertItem(int ignored,ItemStack stack,boolean sim){if(output)return stack;if(!c.getLevel().isClientSide)return b.insert(index(),stack,sim);return stack;}
                public ItemStack extractItem(int ignored,int n,boolean sim){if(!c.getLevel().isClientSide)return b.take(index(),n,sim);n=Math.min(n,displayed.getCount());var result=displayed.copyWithCount(n);if(!sim)displayed=displayed.copyWithCount(displayed.getCount()-n);return result;}
                public int getSlotLimit(int ignored){return index()<b.slots()?64:getStackInSlot(0).getCount();}
                public boolean isItemValid(int ignored,ItemStack stack){return !output&&index()<b.slots();}
            };
            addSlot(new SlotItemHandler(view,0,(out?172:18)+i%3*18,44+i/3*18){
                @Override public boolean mayPickup(Player p){return p.level().isClientSide||canConfigure(p);}
                @Override public boolean mayPlace(ItemStack s){return (inv.player.level().isClientSide||canConfigure(inv.player))&&super.mayPlace(s);}
            });
        }
    }
    public static FactoryMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf buf){var pos=buf.readBlockPos();var clicked=buf.readBlockPos();return new FactoryMenu(id,inv,(Controller)inv.player.level().getBlockEntity(pos),clicked);}
    public static void open(Player player,Controller c,BlockPos clicked){if(player instanceof ServerPlayer server&&c.access(player)&&player.distanceToSqr(clicked.getCenter())<=64&&(clicked.equals(c.getBlockPos())||c.structure.contains(clicked)))
        server.openMenu(new SimpleMenuProvider((id,inv,p)->new FactoryMenu(id,inv,c,clicked),c.getDisplayName()),buf->{buf.writeBlockPos(c.getBlockPos());buf.writeBlockPos(clicked);});}
    @Override protected int getInventoryXOffset(){return 41;}
    @Override protected int getInventoryYOffset(){return 162;}
    @Override public boolean stillValid(Player p){if(clicked==null)return true;if(!super.stillValid(p)||p.distanceToSqr(clicked.getCenter())>64)return false;
        if(p.level().isClientSide)return true;if(!tile.access(p)||!p.level().hasChunkAt(clicked))return false;
        return clicked.equals(tile.getBlockPos())?p.level().getBlockEntity(clicked)==tile:
              p.level().getBlockEntity(clicked) instanceof Part part&&part.controller()==tile||FactoryStructure.inductionController(p.level(),clicked)==tile;
    }
    public boolean canConfigure(Player p){return !p.level().isClientSide&&stillValid(p)&&tile.access(p);}
    @Override public boolean clickMenuButton(Player p,int id){if(!canConfigure(p))return false;
        if(id==1){tile.enabled=!tile.enabled;tile.markForSave();return true;}
        if(id==2){Construction.preview(tile,(ServerPlayer)p);return true;}
        if(id==3)return Construction.build(tile,(ServerPlayer)p);
        if(id>=10&&id<16)return tile.resize((id-10)/2,id%2==0?-1:1);
        if(id==20||id==21||id==23||id==24){tile.parallelLimit=Math.clamp(tile.parallelLimit+(id==20||id==23?-1:1)*(id>=23?16:1),1,512);tile.markForSave();return true;}
        if(id==22&&tile.processing.jobs.isEmpty()){tile.rotaryReverse=!tile.rotaryReverse;tile.markForSave();return true;}
        if(id>=30&&id<=33){if(getCarried().isEmpty()){if(id<32)inputPage=Math.clamp(inputPage+(id==30?-1:1),0,maxPage(tile.inputs));else outputPage=Math.clamp(outputPage+(id==32?-1:1),0,maxPage(tile.outputs));}pageRevision++;broadcastChanges();return true;}
        if(id>=40&&id<46)return PortConfiguration.cycleFace(tile,p,mekanism.api.RelativeSide.values()[id-40]);
        if(id==46){tile.autoEject=!tile.autoEject;tile.markForSave();return true;}
        return false;
    }
    private int maxPage(Buffers b){int last=b.slots();for(int i=Buffers.SLOTS-1;i>=last;i--)if(!b.items[i].isEmpty()){last=i+1;break;}return Math.max(0,(last-1)/PAGE_SIZE);}
    @Override public ItemStack quickMoveStack(Player p,int index){if(!canConfigure(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.mayPickup(p)||!slot.hasItem())return ItemStack.EMPTY;var original=slot.getItem().copy();
        if(index>=bankStart){var left=ItemHandlerHelper.insertItemStacked(new InvWrapper(p.getInventory()),original,false);int moved=original.getCount()-left.getCount();if(moved==0)return ItemStack.EMPTY;slot.remove(moved);slot.setChanged();return original;}
        if(slot.container==p.getInventory()&&Profiles.get(original)==null&&!(original.getItem() instanceof mekanism.common.item.ItemUpgrade)){
            var left=original;for(int i=0;i<tile.inputs.slots()&&!left.isEmpty();i++)left=tile.inputs.insert(i,left,false);if(left.getCount()==original.getCount())return ItemStack.EMPTY;slot.setByPlayer(left);return original;
        }
        return super.quickMoveStack(p,index);
    }
}
