package dev.everyonemek.oritech;

import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.util.ScreenProvider;

public final class ProcessorMenu extends OritechScreenHandler {
    public static final int HOST_X=-29,OPEN_ADDONS=7;
    public final Processor processor;
    public final Profiles layout;
    public final int mainSlots,sideStart,layoutModules;
    public final boolean layoutFluid;
    public ProcessorMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,read(inv,buf));}
    private static Processor read(Inventory inv,FriendlyByteBuf buf){var p=(Processor)Objects.requireNonNull(inv.player.level().getBlockEntity(buf.readBlockPos()));p.profileIndex=buf.readVarInt();p.modules=buf.readVarInt();p.fluidAddon=buf.readBoolean();p.tier=Math.clamp(buf.readVarInt(),1,Processor.MAX_TIER);return p;}
    public ProcessorMenu(int id,Inventory inv,Processor p){super(id,inv,p);processor=p;layout=p.profile();layoutModules=p.modules;layoutFluid=p.fluidAddon;mainSlots=layout.slots().size();sideStart=slots.size();
        addSlot(new Slot(p.inventory,Processor.HOST,HOST_X,20){@Override public int getMaxStackSize(){return 1;}
            @Override public boolean mayPlace(ItemStack s){return p.canInstallHost(s);}
            @Override public boolean mayPickup(Player player){return p.canChangeHost();}
            @Override public void setChanged(){super.setChanged();if(!p.getLevel().isClientSide)p.refreshEquipment();}});
        bindFluids();
    }
    @Override public void addMachineSlot(int inventorySlot,int x,int y,boolean output){
        if(output){super.addMachineSlot(inventorySlot,x,y,true);return;}
        var p=(Processor)blockEntity;var host=p.profile();addSlot(new Slot(inventory,inventorySlot,x,y){
            @Override public boolean mayPlace(ItemStack s){return host!=Profiles.EMPTY&&Profiles.of(p.inventory.getItem(Processor.HOST))==host&&p.profile()==host&&inventorySlot<p.profile().inputs();}
        });
    }
    @Override public void addFluidDisplay(){var p=(Processor)blockEntity;if(!p.fluidEnabled())return;
        var bars=fluidBars(p);for(int i=0;i<bars.size();i++){var data=DisplayDataSource.CreateFluid(p.tanks().get(i),bars.get(i),p);getDataDisplays().add(data);}
    }
    private static List<ScreenProvider.BarConfiguration> fluidBars(Processor p){
        if(p.profile()==Profiles.REFINERY){var list=new ArrayList<ScreenProvider.BarConfiguration>();list.add(new ScreenProvider.BarConfiguration(30,6,21,74));for(int i=0;i<=p.modules;i++)list.add(new ScreenProvider.BarConfiguration(92+27*i,6,21,74));return list;}
        if(p.profile()==Profiles.CENTRIFUGE)return List.of(new ScreenProvider.BarConfiguration(28,6,21,74),new ScreenProvider.BarConfiguration(147,6,21,74));
        return List.of(p.profile().metadata().getFluidConfiguration());
    }
    /** Native fluid clicks use these guarded views, never an unrestricted tank reference. */
    private void bindFluids(){fluidStorages.clear();var p=processor;if(!p.fluidEnabled())return;int count=1+p.outputTanks();
        for(int i=0;i<count;i++){final var tank=p.tanks().get(i);final int index=i;
            fluidStorages.add(new FluidApi.SingleSlotStorage(){
                private boolean valid(){return playerInventory.player.containerMenu==ProcessorMenu.this&&stillValid(playerInventory.player)&&p.profile()==layout&&p.modules==layoutModules&&p.fluidAddon==layoutFluid&&p.fluidEnabled()&&index<=p.outputTanks();}
                @Override public long insert(dev.architectury.fluid.FluidStack f,boolean simulate){if(!valid()||index!=0)return 0;long moved=tank.insert(f,simulate);if(!simulate&&moved>0)p.setChanged();return moved;}
                @Override public long extract(dev.architectury.fluid.FluidStack f,boolean simulate){if(!valid())return 0;long moved=tank.extract(f,simulate);if(!simulate&&moved>0)p.setChanged();return moved;}
                @Override public java.util.List<dev.architectury.fluid.FluidStack> getContent(){return tank.getContent();}
                @Override public long getCapacity(){return tank.getCapacity();}
                @Override public void update(){if(valid())p.setChanged();}
                @Override public dev.architectury.fluid.FluidStack getStack(){return tank.getStack();}
                @Override public void setStack(dev.architectury.fluid.FluidStack f){if(valid())tank.setStack(f);}
            });
        }
        int i=0;for(var source:getDataDisplays())if(source instanceof DisplayDataSource.FluidDataSource data)data.setTankIndex(i++);
    }
    @Override public void broadcastChanges(){if(fluidStorages.isEmpty()&&processor.fluidEnabled())bindFluids();
        if(!playerInventory.player.level().isClientSide&&getCarried().isEmpty()&&(layout!=processor.profile()||layoutModules!=processor.modules||layoutFluid!=processor.fluidAddon)){
            playerInventory.player.closeContainer();dev.architectury.registry.menu.MenuRegistry.openExtendedMenu((net.minecraft.server.level.ServerPlayer)playerInventory.player,processor);return;}
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player p){return !processor.isRemoved()&&p.level()==processor.getLevel()&&p.level().getBlockEntity(blockPos)==processor&&p.distanceToSqr(Vec3.atCenterOf(blockPos))<=64;}
    @Override public boolean clickMenuButton(Player p,int button){
        if(!stillValid(p)||p.containerMenu!=this)return false;
        if(button==6){if(p.isSpectator())return false;processor.eject=!processor.eject;processor.setChanged();return true;}
        if(button!=OPEN_ADDONS||!getCarried().isEmpty()||!(p instanceof net.minecraft.server.level.ServerPlayer server))return false;
        AddonMenu.open(server,processor);return true;
    }
    @Override public boolean canDragTo(Slot slot){return slot.isActive()&&super.canDragTo(slot);}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.isActive()&&super.canTakeItemForPickAll(stack,slot);}
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(p.isSpectator()||!stillValid(p)||p.containerMenu!=this||(slot>=0&&slot<slots.size()&&!slots.get(slot).isActive()))return;
        super.clicked(slot,button,type,p);
    }
    @Override public int getPlayerInvStartSlot(ItemStack stack){return mainSlots;}
    @Override public int getPlayerInvEndSlot(ItemStack stack){return mainSlots+36;}
    @Override public int getMachineInvEndSlot(ItemStack stack){return mainSlots;}
    @Override public ItemStack quickMoveStack(Player p,int index){if(p.isSpectator()||!stillValid(p)||p.containerMenu!=this||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();boolean moved;
        if(index>=mainSlots&&index<mainSlots+36&&stack.getItem() instanceof ProcessorUpgrade upgrade){
            if(!upgrade.apply(processor,p,stack))return ItemStack.EMPTY;
            if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
        }
        if(index<mainSlots||index>=sideStart)moved=moveItemStackTo(stack,mainSlots,mainSlots+36,true);
        else if(Profiles.of(stack)!=Profiles.EMPTY)moved=moveItemStackTo(stack,sideStart,sideStart+1,false);
        else if(Processor.validAddon(stack)){
            if(!getCarried().isEmpty()||!(p instanceof net.minecraft.server.level.ServerPlayer server))return ItemStack.EMPTY;
            int playerSlot=slot.getContainerSlot();AddonMenu.open(server,processor);
            return p.containerMenu.quickMoveStack(p,AddonMenu.PLAYER_START+(playerSlot<9?27+playerSlot:playerSlot-9));
        }
        else moved=moveItemStackTo(stack,0,mainSlots,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
