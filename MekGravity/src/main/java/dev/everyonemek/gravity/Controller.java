package dev.everyonemek.gravity;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.inventory.container.sync.chemical.SyncableChemicalStack;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class Controller extends TileEntityMekanism {
    public final Structure structure=new Structure(this);
    public long stored,fuelRemaining,fuelTotal,gross,selfUse,lastOutput,lastInput;
    public ChemicalStack cold=ChemicalStack.EMPTY,hot=ChemicalStack.EMPTY;
    public boolean enabled,ignited,autoEject=true;
    public int load=100;
    public String status="structure";
    private long clock=Long.MIN_VALUE,exported,received;
    public Controller(BlockPos pos,BlockState state){super(Content.CONTROLLER,pos,state);}
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener){return EnergyContainerHelper.forSide(facingSupplier).build();}
    @Override public boolean persists(ContainerType<?,?,?> type){return type!=ContainerType.ENERGY&&super.persists(type);}
    public boolean access(Player p){return !isRemoved()&&p.level()==level&&mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(p,level,worldPosition,this);}
    public long capacity(){return ReactorConfig.CAPACITY.get();}
    public long reserve(){return Math.min(capacity(),ReactorConfig.RESERVE.get());}
    public long startup(){return ReactorConfig.STARTUP.get();}
    public void clock(){if(level.getGameTime()!=clock){lastOutput=exported;lastInput=received;exported=received=0;clock=level.getGameTime();}}
    public long inputLimit(){return structure.formed?ReactorConfig.PORT_RATE.get()*structure.energyInputs:0;}
    public long outputLimit(){return structure.formed?ReactorConfig.PORT_RATE.get()*structure.energyOutputs:0;}
    public long accept(long n,boolean simulate){if(n<=0||!structure.valid())return 0;clock();long accepted=Math.min(n,Math.min(Math.max(0,capacity()-stored),Math.max(0,inputLimit()-received)));if(!simulate&&accepted>0){stored+=accepted;received+=accepted;markForSave();}return accepted;}
    public long extract(long n,boolean simulate){if(n<=0||!ignited||!structure.valid())return 0;clock();long amount=Math.min(n,Math.min(Math.max(0,stored-reserve()),Math.max(0,outputLimit()-exported)));if(!simulate&&amount>0){stored-=amount;exported+=amount;markForSave();}return amount;}
    public long tankCapacity(){return ReactorConfig.TANK_CAPACITY.get();}
    public boolean fuelAvailable(){if(fuelRemaining>0)return true;for(var h:structure.fuelHatches)for(int i=0;i<18;i++){var stack=h.inventory.getStackInSlot(i);var r=FuelRecipe.find(level,stack);if(r!=null&&stack.getCount()>=r.count())return true;}return false;}
    private void chargeFuel(long need){
        boolean charged=false;
        for(var h:structure.fuelHatches)for(int i=0;i<18&&fuelRemaining<need;i++){
            var stack=h.inventory.getStackInSlot(i);var r=FuelRecipe.find(level,stack);if(r==null)continue;
            int count=(int)Math.min(stack.getCount()/r.count(),1+(need-fuelRemaining-1)/r.energy());
            count=(int)Math.min(count,(Long.MAX_VALUE-fuelRemaining)/r.energy());
            if(count>0){if(!charged){fuelTotal=fuelRemaining;charged=true;}long amount=Math.multiplyExact(r.energy(),count);h.inventory.extractItem(i,r.count()*count,false);fuelRemaining=Math.addExact(fuelRemaining,amount);fuelTotal=Math.addExact(fuelTotal,amount);markForSave();}
        }
    }
    public void react(){
        long previousGross=gross;gross=selfUse=0;
        if(!structure.valid()){status=structure.error;return;}
        if(!enabled){status="stopped";return;}if(!canFunction()){status="redstone";return;}
        if(startup()>capacity()-reserve()){status="startup_config";return;}
        if(!fuelAvailable()){status="fuel_missing";return;}
        if(!ignited){if(stored<startup()+reserve()){status="charging";return;}stored-=startup();ignited=true;markForSave();}
        if(stored<reserve()){status="reserve_low";return;}
        long target=structure.grade.power()*load/100;
        long requested=Math.min(target,previousGross+Math.max(1,structure.grade.power()/20));
        long room=Math.max(0,capacity()-stored),available=Math.min(requested,room);
        if(available<50){status="full";return;}
        chargeFuel(available);available=Math.min(available,fuelRemaining);
        if(available<50){status="fuel_missing";return;}
        gross=available;selfUse=(gross+49)/50;
        fuelRemaining-=gross;stored+=gross-selfUse;
        if(fuelRemaining==0)fuelTotal=0;
        status=gross<requested?(room<=gross?"output_limited":"fuel_limited"):"running";markForSave();
    }
    @Override protected boolean onUpdateServer(){boolean changed=super.onUpdateServer();clock();if(!structure.formed&&level.getGameTime()%20==0)structure.invalidate();react();Ports.eject(this);if(exported>0)lastOutput=exported;if(received>0)lastInput=received;structure.activity(gross>0);setActive(gross>0);return changed;}
    @Override public void setRemoved(){structure.detach();super.setRemoved();}
    private CompoundTag data(HolderLookup.Provider r){var t=new CompoundTag();t.putLong("energy",stored);t.putLong("fuel",fuelRemaining);t.putLong("fuel_total",fuelTotal);t.putBoolean("enabled",enabled);t.putBoolean("ignited",ignited);t.putBoolean("eject",autoEject);t.putInt("load",load);t.put("cold",cold.saveOptional(r));t.put("hot",hot.saveOptional(r));return t;}
    private void read(CompoundTag t,HolderLookup.Provider r){stored=Math.max(0,t.getLong("energy"));fuelRemaining=Math.max(0,t.getLong("fuel"));fuelTotal=Math.max(fuelRemaining,t.getLong("fuel_total"));enabled=t.getBoolean("enabled");ignited=t.getBoolean("ignited");autoEject=!t.contains("eject")||t.getBoolean("eject");load=t.contains("load")?Math.clamp(t.getInt("load"),1,100):100;cold=ChemicalStack.parseOptional(r,t.getCompound("cold"));hot=ChemicalStack.parseOptional(r,t.getCompound("hot"));structure.invalidate();}
    @Override public void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.put("reactor",data(r));}
    @Override public void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);read(t.getCompound("reactor"),r);}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder b){super.collectImplicitComponents(b);b.set(Content.DATA.get(),data(level.registryAccess()));}
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input){super.applyImplicitComponents(input);var t=input.get(Content.DATA.get());if(t!=null)read(t,level.registryAccess());}
    @Override public void addContainerTrackers(MekanismContainer menu){super.addContainerTrackers(menu);
        menu.track(SyncableLong.create(()->stored,v->stored=v));menu.track(SyncableLong.create(()->fuelRemaining,v->fuelRemaining=v));menu.track(SyncableLong.create(()->fuelTotal,v->fuelTotal=v));
        menu.track(SyncableLong.create(()->gross,v->gross=v));menu.track(SyncableLong.create(()->selfUse,v->selfUse=v));menu.track(SyncableLong.create(()->lastOutput,v->lastOutput=v));
        menu.track(SyncableLong.create(()->lastInput,v->lastInput=v));
        menu.track(SyncableInt.create(()->load,v->load=v));menu.track(SyncableBoolean.create(()->enabled,v->enabled=v));menu.track(SyncableBoolean.create(()->ignited,v->ignited=v));menu.track(SyncableBoolean.create(()->autoEject,v->autoEject=v));
        menu.track(SyncableChemicalStack.create(()->cold,v->cold=v));menu.track(SyncableChemicalStack.create(()->hot,v->hot=v));
    }
}
