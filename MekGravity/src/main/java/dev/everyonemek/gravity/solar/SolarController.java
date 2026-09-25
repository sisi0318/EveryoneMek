package dev.everyonemek.gravity.solar;
import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class SolarController extends TileEntityMekanism {
    public final SolarStructure structure=new SolarStructure(this);
    private final SolarHeat heat=new SolarHeat(this);
    public long stored,fuelRemaining,fuelTotal,gross,selfUse,lastOutput,lastInput;
    public boolean enabled,ignited,autoEject=true,automatic=true,refill=true;
    public int load=100,buildTier,outputCursor;public String status="structure";
    private long ioTick=Long.MIN_VALUE,exported,received;private int lastAlarm=-1;
    public SolarController(BlockPos pos,BlockState state){super(SolarContent.CONTROLLER,pos,state);}
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener){return EnergyContainerHelper.forSide(facingSupplier).build();}
    @Override public boolean persists(ContainerType<?,?,?> type){return type!=ContainerType.ENERGY&&super.persists(type);}
    public boolean access(Player p){return !isRemoved()&&p.level()==level&&mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(p,level,worldPosition,this);}
    public long capacity(){return SolarConfig.CAPACITY.get();}public long reserve(){return Math.min(capacity(),SolarConfig.RESERVE.get());}public long startup(){return SolarConfig.STARTUP.get();}
    public long inputLimit(){return structure.formed?SolarConfig.PORT_RATE.get()*structure.inputs:0;}public long outputLimit(){return structure.formed?SolarConfig.PORT_RATE.get()*structure.outputs:0;}
    public void clock(){long now=level.getGameTime();if(ioTick!=now){lastOutput=exported;lastInput=received;exported=received=0;ioTick=now;}}
    public long accept(long n,boolean simulate){if(n<=0||!structure.valid())return 0;clock();long take=Math.min(n,Math.min(Math.max(0,capacity()-stored),Math.max(0,inputLimit()-received)));if(!simulate&&take>0){stored+=take;received+=take;markForSave();}return take;}
    public long extract(long n,boolean simulate){if(n<=0||!ignited||!structure.valid())return 0;clock();long take=Math.min(n,Math.min(Math.max(0,stored-reserve()),Math.max(0,outputLimit()-exported)));if(!simulate&&take>0){stored-=take;exported+=take;markForSave();}return take;}
    public boolean fuelAvailable(){if(fuelRemaining>0)return true;for(var hatch:structure.fuelHatches)for(int i=0;i<18;i++){var stack=hatch.inventory.getStackInSlot(i);var fuel=SolarFuelRecipe.fuel(level,stack);if(fuel!=null&&stack.getCount()>=fuel.count())return true;}return false;}
    public dev.everyonemek.gravity.FuelStock fuelStock(){var result=new dev.everyonemek.gravity.FuelStock();for(var hatch:structure.fuelHatches)for(int i=0;i<18;i++){var stack=hatch.inventory.getStackInSlot(i);var fuel=SolarFuelRecipe.fuel(level,stack);if(fuel!=null)result.add(stack.getCount()/fuel.count(),fuel.remaining());}return result;}
    // "ignited" remembers the paid startup charge; it is not proof that a stopped core is hot.
    public boolean isCoreHot(){return enabled&&ignited&&structure.formed&&(gross>0||fuelRemaining>0||refill&&fuelAvailable());}
    private boolean chargeFuel(){if(fuelRemaining>0)return true;if(!refill)return false;
        for(var hatch:structure.fuelHatches)for(int i=0;i<18;i++){var stack=hatch.inventory.getStackInSlot(i);var fuel=SolarFuelRecipe.fuel(level,stack);if(fuel==null||stack.getCount()<fuel.count())continue;
            hatch.inventory.extractItem(i,fuel.count(),false);fuelRemaining=fuel.remaining();fuelTotal=fuel.total();markForSave();return true;}
        return false;
    }
    public boolean recover(Player player){if(enabled||!access(player)||!structure.valid()||fuelRemaining<=0)return false;int slot=player.getInventory().getFreeSlot();if(slot<0)return false;
        var item=new ItemStack(SolarContent.CAPSULE.get());var data=new CompoundTag();data.putLong("remaining",fuelRemaining);data.putLong("total",fuelTotal);if(!SolarFuelRecipe.validReserve(data))return false;
        item.set(SolarContent.FUEL_DATA.get(),data);player.getInventory().setItem(slot,item);player.getInventory().setChanged();fuelRemaining=fuelTotal=0;markForSave();return true;
    }
    public void react(){long previous=gross;gross=selfUse=0;
        if(!structure.valid()){status=structure.error;return;}if(!enabled){status="stopped";return;}if(!canFunction()){status="redstone";return;}
        if(startup()>capacity()-reserve()){status="startup_config";return;}
        if(!fuelAvailable()){status="fuel_missing";return;}if(fuelRemaining==0&&!refill){status="refill_off";return;}
        if(!ignited){if(stored<startup()+reserve()){status="charging";return;}stored-=startup();ignited=true;markForSave();}
        if(stored<reserve()){status="reserve_low";return;}
        long limit=structure.power()*load/100,room=Math.max(0,capacity()-stored);if(room<=0){status="full";return;}
        long requested=Math.min(limit,previous+Math.max(1,structure.power()/20));
        // Always refill toward actual capacity. Automatic mode can also respond immediately
        // to real exported energy, instead of ramping up again after every full-buffer pause.
        if(automatic)requested=Math.max(requested,fuelForNet(Math.min(Math.max(0,lastOutput),netEnergy(limit))));
        // Capacity is NET energy, fuel is GROSS energy. Using room directly as fuel causes
        // an asymptotic tail and eventually burns 1 J/t forever without filling the last joule.
        long amount=netEnergy(requested)>room?fuelForNet(room):requested;
        if(netEnergy(amount)<=0){status="limited";return;}
        if(!chargeFuel()){status=refill?"fuel_missing":"refill_off";return;}
        gross=Math.min(amount,fuelRemaining);selfUse=gross/20+(gross%20==0?0:1);fuelRemaining-=gross;stored+=gross-selfUse;
        if(fuelRemaining==0)fuelTotal=0;status=gross<requested?"limited":"running";markForSave();
    }
    private static long netEnergy(long fuel){return fuel-fuel/20-(fuel%20==0?0:1);}
    // ceil(20 * net / 19), without the overflowing multiplication. Callers bound net by
    // the configured per-tick generation limit, never by the entire stored fuel budget.
    private static long fuelForNet(long net){return net+net/19+(net%19==0?0:1);}
    @Override protected boolean onUpdateServer(){boolean changed=super.onUpdateServer();clock();react();SolarPorts.eject(this);if(exported>0)lastOutput=exported;if(received>0)lastInput=received;structure.activity(gross>0);setActive(gross>0);heat.tick();
        int alarm=structure.formed&&!fuelAvailable()?15:0;if(alarm!=lastAlarm){lastAlarm=alarm;for(var hatch:structure.fuelHatches)if(!hatch.isRemoved()&&level.hasChunkAt(hatch.getBlockPos()))level.updateNeighbourForOutputSignal(hatch.getBlockPos(),hatch.getBlockState().getBlock());}return changed;}
    @Override public void onLoad(){super.onLoad();structure.watch();structure.invalidate();}
    @Override public void setRemoved(){structure.detach();super.setRemoved();}
    private CompoundTag data(){var t=new CompoundTag();t.putLong("energy",stored);t.putLong("fuel",fuelRemaining);t.putLong("fuel_total",fuelTotal);t.putBoolean("enabled",enabled);t.putBoolean("ignited",ignited);t.putBoolean("automatic",automatic);t.putBoolean("refill",refill);t.putBoolean("eject",autoEject);t.putInt("load",load);t.putInt("build_tier",buildTier);return t;}
    private void read(CompoundTag t){stored=Math.max(0,t.getLong("energy"));fuelRemaining=Math.clamp(t.getLong("fuel"),0,1_000_000_000_000_000_000L);fuelTotal=Math.max(fuelRemaining,Math.clamp(t.getLong("fuel_total"),0,1_000_000_000_000_000_000L));enabled=t.getBoolean("enabled");ignited=t.getBoolean("ignited");automatic=!t.contains("automatic")||t.getBoolean("automatic");refill=!t.contains("refill")||t.getBoolean("refill");autoEject=!t.contains("eject")||t.getBoolean("eject");buildTier=Math.clamp(t.getInt("build_tier"),0,3);load=t.contains("load")?Math.clamp(t.getInt("load"),1,100):100;gross=selfUse=0;structure.invalidate();}
    @Override public void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.put("solar",data());}
    @Override public void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);read(t.getCompound("solar"));}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder b){super.collectImplicitComponents(b);b.set(SolarContent.DATA.get(),data());}
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput in){super.applyImplicitComponents(in);var t=in.get(SolarContent.DATA.get());if(t!=null)read(t);}
    @Override public void addContainerTrackers(MekanismContainer menu){super.addContainerTrackers(menu);
        menu.track(SyncableLong.create(()->stored,v->stored=v));menu.track(SyncableLong.create(()->fuelRemaining,v->fuelRemaining=v));menu.track(SyncableLong.create(()->fuelTotal,v->fuelTotal=v));menu.track(SyncableLong.create(()->gross,v->gross=v));menu.track(SyncableLong.create(()->selfUse,v->selfUse=v));menu.track(SyncableLong.create(()->lastOutput,v->lastOutput=v));menu.track(SyncableLong.create(()->lastInput,v->lastInput=v));
        menu.track(SyncableInt.create(()->load,v->load=v));menu.track(SyncableBoolean.create(()->enabled,v->enabled=v));menu.track(SyncableBoolean.create(()->ignited,v->ignited=v));menu.track(SyncableBoolean.create(()->automatic,v->automatic=v));menu.track(SyncableBoolean.create(()->refill,v->refill=v));menu.track(SyncableBoolean.create(()->autoEject,v->autoEject=v));
    }
}
