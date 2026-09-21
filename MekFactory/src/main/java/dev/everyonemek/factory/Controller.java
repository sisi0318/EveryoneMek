package dev.everyonemek.factory;
import mekanism.api.*;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.inventory.container.sync.chemical.SyncableChemicalStack;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class Controller extends TileEntityMekanism {
    private static final java.util.List<String> STATES=java.util.List.of("structure","frame","shell","interior","tier","unloaded","occupied","ports","port_limit","induction","ready","idle","working","materials","machine","template_not_empty","paused","conditions","energy","output","output_or_energy","redstone","draining","secondary","converter_limit");
    public BasicInventorySlot template;private FactoryEnergy energy;
    public final FactoryStructure structure=new FactoryStructure(this);
    public final Buffers inputs=new Buffers(this,false),outputs=new Buffers(this,true);
    public final Processing processing=new Processing();
    public int sizeX=3,sizeY=3,sizeZ=3,parallelLimit=FactoryConfig.MAX_PARALLEL,parallel,running,progress,inputRevision;
    public long powerUsed,clientEnergy,clientCapacity;public String status="structure";
    public boolean enabled=true,autoEject=true,rotaryReverse,preview;
    public final int[] portInputs=new int[6],portOutputs=new int[6],portConverters=new int[6];
    public FactoryAppearance.Snapshot publishedAppearance;
    public Controller(BlockPos pos,BlockState state){super(Content.CONTROLLERS.get(((ControllerBlock)state.getBlock()).grade),pos,state);}
    public Grade grade(){return ((ControllerBlock)getBlockState().getBlock()).grade;}
    public FactoryEnergy energy(){return energy;}
    public ResourceBank inputBank(){var banks=new java.util.ArrayList<ResourceBank>();if(inputs.hasContents())banks.add(inputs);for(var p:structure.ports)if(!p.getBlockState().getValue(PartBlock.OUTPUT))banks.add(p.storage());for(var p:structure.converters)banks.add(new ConvertedChemicalBank(p.storage()));return new GroupedInputBank(new CombinedBank(banks));}
    public ResourceBank warehouseBank(boolean output){var banks=new java.util.ArrayList<ResourceBank>();for(var p:structure.ports)if(p.getBlockState().getValue(PartBlock.OUTPUT)==output)banks.add(p.storage());return new CombinedBank(banks);}
    public ResourceBank outputBank(){return warehouseBank(true);}
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener){var b=EnergyContainerHelper.forSide(facingSupplier);b.addContainer(energy=new FactoryEnergy(this));return b.build();}
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener){var b=InventorySlotHelper.forSide(facingSupplier);
        template=BasicInventorySlot.at((s,a)->a!=AutomationType.EXTERNAL&&(processing==null||processing.jobs.isEmpty()),
              (s,a)->a!=AutomationType.EXTERNAL&&(processing==null||processing.jobs.isEmpty()||template!=null&&ItemStack.isSameItemSameComponents(s,template.getStack())),
              s->Profiles.get(s)!=null,listener,18,30);
        b.addSlot(template);return b.build();}
    @Override public boolean persists(ContainerType<?,?,?> type){return type!=ContainerType.ENERGY&&super.persists(type);}
    public boolean access(Player p){return !isRemoved()&&p.level()==level&&mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(p,level,worldPosition,this);}
    @Override protected boolean onUpdateServer(){boolean changed=super.onUpdateServer();
        if(!structure.formed&&level.getGameTime()%20==0)structure.invalidate();
        if(structure.valid()){LegacyMigration.transfer(this);ChemicalConversions.tick(this);processing.tick(this);}else{running=0;powerUsed=0;status=structure.error;}
        Ports.eject(this);Ports.ejectConverters(this);if(level.getGameTime()%5==0)PortConfiguration.refresh(this);setActive(running>0);FactoryAppearance.sync(this);return changed;}
    @Override public void setRemoved(){FactoryAppearance.clear(this);structure.detach();super.setRemoved();}
    public boolean resize(int axis,int delta){if(!processing.jobs.isEmpty())return false;int n=(axis==0?sizeX:axis==1?sizeY:sizeZ)+delta;if(n<3||n>grade().size())return false;structure.detach();if(axis==0)sizeX=n;else if(axis==1)sizeY=n;else sizeZ=n;markForSave();return true;}
    private CompoundTag data(HolderLookup.Provider r){var t=new CompoundTag();t.putInt("x",sizeX);t.putInt("y",sizeY);t.putInt("z",sizeZ);t.putInt("parallel",parallelLimit);t.putInt("parallel_revision",1);t.putBoolean("enabled",enabled);t.putBoolean("auto_eject",autoEject);t.putBoolean("rotary",rotaryReverse);t.put("inputs",inputs.save(r));t.put("outputs",outputs.save(r));t.put("jobs",processing.save(r));return t;}
    public static int readParallelLimit(CompoundTag t){int stored=t.contains("parallel")?t.getInt("parallel"):FactoryConfig.MAX_PARALLEL;return !t.contains("parallel_revision")&&stored==512?FactoryConfig.MAX_PARALLEL:Math.clamp(stored,1,FactoryConfig.MAX_PARALLEL);}
    private void read(CompoundTag t,HolderLookup.Provider r){sizeX=Math.clamp(t.getInt("x"),3,11);sizeY=Math.clamp(t.getInt("y"),3,11);sizeZ=Math.clamp(t.getInt("z"),3,11);if(!t.contains("x"))sizeX=sizeY=sizeZ=3;parallelLimit=readParallelLimit(t);enabled=!t.contains("enabled")||t.getBoolean("enabled");autoEject=!t.contains("auto_eject")||t.getBoolean("auto_eject");rotaryReverse=t.getBoolean("rotary");inputs.load(t.getCompound("inputs"),r);outputs.load(t.getCompound("outputs"),r);processing.load(t.getList("jobs",Tag.TAG_COMPOUND),r);structure.invalidate();}
    @Override public void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.put("factory",data(r));}
    @Override public void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);read(t.getCompound("factory"),r);}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder b){super.collectImplicitComponents(b);b.set(Content.DATA.get(),data(level.registryAccess()));}
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input){super.applyImplicitComponents(input);var t=input.get(Content.DATA.get());if(t!=null)read(t,level.registryAccess());}
    @Override public void addContainerTrackers(MekanismContainer menu){super.addContainerTrackers(menu);
        menu.track(SyncableInt.create(()->sizeX,v->sizeX=v));menu.track(SyncableInt.create(()->sizeY,v->sizeY=v));menu.track(SyncableInt.create(()->sizeZ,v->sizeZ=v));
        menu.track(SyncableInt.create(()->parallelLimit,v->parallelLimit=v));menu.track(SyncableInt.create(()->parallel,v->parallel=v));menu.track(SyncableInt.create(()->running,v->running=v));menu.track(SyncableInt.create(()->progress,v->progress=v));
        menu.track(SyncableBoolean.create(()->enabled,v->enabled=v));menu.track(SyncableBoolean.create(()->rotaryReverse,v->rotaryReverse=v));
        menu.track(SyncableBoolean.create(()->autoEject,v->autoEject=v));
        for(int i=0;i<6;i++){final int n=i;menu.track(SyncableInt.create(()->portInputs[n],v->portInputs[n]=v));menu.track(SyncableInt.create(()->portOutputs[n],v->portOutputs[n]=v));menu.track(SyncableInt.create(()->portConverters[n],v->portConverters[n]=v));}
        menu.track(SyncableInt.create(()->Math.max(0,STATES.indexOf(status)),v->status=STATES.get(Math.clamp(v,0,STATES.size()-1))));menu.track(SyncableLong.create(energy::getEnergy,v->clientEnergy=v));menu.track(SyncableLong.create(energy::getMaxEnergy,v->clientCapacity=v));menu.track(SyncableLong.create(()->powerUsed,v->powerUsed=v));
        menu.track(SyncableInt.create(()->structure.inputSlots,v->structure.inputSlots=v));menu.track(SyncableInt.create(()->structure.outputSlots,v->structure.outputSlots=v));
        menu.track(SyncableLong.create(()->structure.inputCapacity,v->structure.inputCapacity=v));menu.track(SyncableLong.create(()->structure.outputCapacity,v->structure.outputCapacity=v));
        menu.track(SyncableLong.create(()->structure.transfer,v->structure.transfer=v));

    }
}
