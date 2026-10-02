package dev.everyonemek.oritech.collider;

import java.util.*;
import dev.everyonemek.oritech.Content;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ColliderMenu extends AbstractContainerMenu {
    public static final int PLAYER_START=5,PLAYER_END=41,INVENTORY_X=83,INVENTORY_Y=154,INPUT_A_X=239,INPUT_B_X=281,INPUT_Y=43,OUTPUT_X=260,OUTPUT_Y=86,PART_Y=165;
    public final Collider collider;
    public final Inventory playerInventory;
    public CompoundTag view=new CompoundTag();
    public final Map<Integer,Track.Node> layout=new TreeMap<>();
    private int sentRevision=Integer.MIN_VALUE;
    private CompoundTag lastView;
    public ColliderMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,(Collider)Objects.requireNonNull(inv.player.level().getBlockEntity(buf.readBlockPos())));}
    public ColliderMenu(int id,Inventory inv,Collider collider){super(Content.COLLIDER_MENU.get(),id);this.collider=collider;playerInventory=inv;
        addSlot(input(Collider.INPUT_A,INPUT_A_X,INPUT_Y));addSlot(input(Collider.INPUT_B,INPUT_B_X,INPUT_Y));
        addSlot(new Slot(collider.inventory,Collider.OUTPUT,OUTPUT_X,OUTPUT_Y){@Override public boolean mayPlace(ItemStack stack){return false;}});
        addSlot(new Slot(collider.inventory,Collider.PART_IN,12,PART_Y){@Override public boolean mayPlace(ItemStack stack){return Collider.partKind(stack)>0;}});
        addSlot(new Slot(collider.inventory,Collider.PART_OUT,47,PART_Y){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,INVENTORY_X+x*18,INVENTORY_Y+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,INVENTORY_X+x*18,INVENTORY_Y+58));
        readView(collider.view(true));
    }
    private Slot input(int index,int x,int y){return new Slot(collider.inventory,index,x,y){@Override public boolean mayPlace(ItemStack stack){return collider.accepts(index,stack);}};}
    public void readView(CompoundTag tag){view=tag;
        if(tag.contains("layout")){layout.clear();for(int packed:tag.getIntArray("layout")){try{layout.put(packed&4095,Track.unpack(packed));}catch(IllegalArgumentException ignored){}}}
        // Predicates for client-side ghost previews; server still validates every insertion.
        if(collider.getLevel().isClientSide){var id=tag.getString("locked");collider.lockedRecipe=id.isEmpty()?null:net.minecraft.resources.ResourceLocation.tryParse(id);collider.lockedSwapped=tag.getBoolean("swapped");}
    }
    @Override public boolean stillValid(Player player){return player.level()==collider.getLevel()&&!collider.isRemoved()&&player.level().hasChunkAt(collider.getBlockPos())
        &&player.level().getBlockEntity(collider.getBlockPos())==collider&&player.distanceToSqr(Vec3.atCenterOf(collider.getBlockPos()))<=64;}
    public boolean canControl(Player player){return player.containerMenu==this&&stillValid(player)&&!player.isSpectator()&&player.mayBuild()&&player.level().mayInteract(player,collider.getBlockPos());}
    @Override public void broadcastChanges(){super.broadcastChanges();if(playerInventory.player instanceof ServerPlayer player){
        boolean full=sentRevision!=collider.revision;var next=collider.view(full);var comparison=collider.view(false);
        if(full||!comparison.equals(lastView)){PacketDistributor.sendToPlayer(player,new ColliderPackets.Snapshot(containerId,next));sentRevision=collider.revision;lastView=comparison;}
    }}
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(!canControl(player))return;super.clicked(slot,button,type,player);}
    @Override public ItemStack quickMoveStack(Player player,int index){if(!canControl(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();boolean moved;
        if(index<PLAYER_START)moved=moveItemStackTo(stack,PLAYER_START,PLAYER_END,true);
        else if(Collider.partKind(stack)>0)moved=moveItemStackTo(stack,3,4,false);
        else if(collider.sameInputs(stack)){
            moved=false;for(int count=stack.getCount();count>0&&!stack.isEmpty();count--){int first=collider.inventory.getItem(0).getCount()<=collider.inventory.getItem(1).getCount()?0:1;boolean inserted=false;
                for(int pass=0;pass<2&&!inserted;pass++){int target=pass==0?first:1-first;var dest=slots.get(target);var current=dest.getItem();if(dest.mayPlace(stack)&&(current.isEmpty()||ItemStack.isSameItemSameComponents(current,stack))&&current.getCount()<Math.min(64,stack.getMaxStackSize())){dest.setByPlayer(stack.copyWithCount(current.getCount()+1));stack.shrink(1);inserted=true;moved=true;}}
                if(!inserted)break;
            }
        }
        else {
            // Same-ingredient recipes refill both inputs, rather than filling A forever.
            int first=collider.inventory.getItem(0).getCount()<=collider.inventory.getItem(1).getCount()?0:1;
            moved=moveItemStackTo(stack,first,first+1,false);if(!stack.isEmpty())moved|=moveItemStackTo(stack,1-first,2-first,false);
        }
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
