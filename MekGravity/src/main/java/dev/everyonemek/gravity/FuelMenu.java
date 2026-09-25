package dev.everyonemek.gravity;
import mekanism.common.inventory.container.MekanismContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
public final class FuelMenu extends MekanismContainer {
    public final Part part;
    public FuelMenu(int id,Inventory inv,Part part){super(Content.FUEL_MENU,id,inv);this.part=part;addSlotsAndOpen();}
    @Override protected void addSlots(){for(int i=0;i<18;i++)addSlot(new FuelInventorySlot(part,part.inventory,i,8+i%9*18,33+i/9*18));}
    @Override protected int getInventoryXOffset(){return 8;}
    @Override protected int getInventoryYOffset(){return 121;}
    @Override public boolean stillValid(Player p){return !part.isRemoved()&&(p.level().isClientSide||part.kind()==PartBlock.Kind.FUEL&&part.canOpen(p)&&p.level().getBlockEntity(part.getBlockPos())==part);}
    @Override public boolean canPlayerAccess(Player p){return stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();if(!moveItemStackTo(stack,index<18?18:0,index<18?slots.size():18,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return copy;}
    public static void open(Player p,Part part){if(p instanceof ServerPlayer server&&part.canOpen(p))server.openMenu(new SimpleMenuProvider((id,inv,player)->new FuelMenu(id,inv,part),Content.text("fuel_title")),b->b.writeBlockPos(part.getBlockPos()));}
    public static FuelMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf b){BlockPos pos=b.readBlockPos();return new FuelMenu(id,inv,(Part)inv.player.level().getBlockEntity(pos));}
}
