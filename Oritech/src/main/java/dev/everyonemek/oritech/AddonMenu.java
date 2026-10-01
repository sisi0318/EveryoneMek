package dev.everyonemek.oritech;

import java.util.Objects;
import dev.architectury.registry.menu.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.api.networking.SyncType;

public final class AddonMenu extends AbstractContainerMenu {
    public static final int PLAYER_START=2,PLAYER_END=38,HIDDEN_START=38,LOAD_X=250,LOAD_Y=36,UNLOAD_Y=112,INVENTORY_X=56,INVENTORY_Y=161;
    public final Processor processor;
    public final Inventory playerInventory;
    public AddonMenu(int id,Inventory inventory,FriendlyByteBuf buf){this(id,inventory,read(inventory,buf));}
    private static Processor read(Inventory inventory,FriendlyByteBuf buf){var p=(Processor)Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos()));p.tier=Processor.clampTier(buf.readVarInt());return p;}
    public AddonMenu(int id,Inventory inventory,Processor p){super(Content.ADDON_MENU.get(),id);processor=p;playerInventory=inventory;
        addSlot(new Slot(p.inventory,Processor.LOAD_SLOT,LOAD_X,LOAD_Y){@Override public boolean mayPlace(ItemStack stack){return Processor.validAddon(stack);}});
        addSlot(new Slot(p.inventory,Processor.UNLOAD_SLOT,LOAD_X,UNLOAD_Y){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+y*9+x,INVENTORY_X+x*18,INVENTORY_Y+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,INVENTORY_X+x*18,INVENTORY_Y+58));
        // Synchronize the sole installed inventory; these slots can never be clicked, dragged, or shift-moved.
        for(int i=Processor.ADDON_START;i<Processor.ADDON_END;i++)addSlot(new Slot(p.inventory,i,-10000,-10000){
            @Override public boolean isActive(){return false;}
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return false;}
        });
    }
    public static void open(ServerPlayer player,Processor processor){MenuRegistry.openExtendedMenu(player,new ExtendedMenuProvider(){
        @Override public Component getDisplayName(){return Component.translatable("gui.oritechmekanism.upgrades");}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player p){return new AddonMenu(id,inventory,processor);}
        @Override public void saveExtraData(FriendlyByteBuf buf){processor.sendUpdate(SyncType.GUI_OPEN);buf.writeBlockPos(processor.getBlockPos());buf.writeVarInt(processor.tier);}
    });}
    @Override public boolean stillValid(Player player){return !processor.isRemoved()&&player.level()==processor.getLevel()&&player.distanceToSqr(Vec3.atCenterOf(processor.getBlockPos()))<=64
        &&player.level().hasChunkAt(processor.getBlockPos())&&player.level().getBlockEntity(processor.getBlockPos())==processor;}
    @Override public boolean canDragTo(Slot slot){return slot.isActive()&&super.canDragTo(slot);}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.isActive()&&super.canTakeItemForPickAll(stack,slot);}
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(player.isSpectator()||player.containerMenu!=this||!stillValid(player)||slot>=HIDDEN_START)return;super.clicked(slot,button,type,player);}
    @Override public boolean clickMenuButton(Player player,int action){if(action!=0||player.containerMenu!=this||!stillValid(player)||!getCarried().isEmpty())return false;
        player.closeContainer();MenuRegistry.openExtendedMenu((ServerPlayer)player,processor);return true;}
    public int unload(Player player,ItemStack template,boolean all){if(player.isSpectator()||player.containerMenu!=this||!stillValid(player)||!getCarried().isEmpty())return 0;
        int count=processor.unloadAddon(template,all);broadcastChanges();return count;}
    @Override public ItemStack quickMoveStack(Player player,int index){if(player.isSpectator()||player.containerMenu!=this||!stillValid(player)||index<0||index>=HIDDEN_START)return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index>=PLAYER_START&&stack.getItem() instanceof ProcessorUpgrade upgrade){if(!upgrade.apply(processor,player,stack))return ItemStack.EMPTY;}
        else if(index<PLAYER_START){if(!moveItemStackTo(stack,PLAYER_START,PLAYER_END,true))return ItemStack.EMPTY;}
        else if(Processor.validAddon(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
    @Override public void broadcastChanges(){processor.sendUpdate(SyncType.GUI_TICK,(ServerPlayer)playerInventory.player);super.broadcastChanges();}
}
