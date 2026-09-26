package dev.everyonemek.gravity.expansion;
import java.util.*;
import dev.everyonemek.gravity.corona.CoronalInventorySlot;
import mekanism.api.*;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.capabilities.holder.fluid.*;
import mekanism.common.capabilities.holder.chemical.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.*;
import mekanism.common.tile.component.config.*;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Native Mek side configuration around the original node containers and save layout. */
public final class NodeModule extends OrbitalModule implements ISideConfiguration {
    public static final TransmissionType[] TYPES={TransmissionType.ITEM,TransmissionType.ENERGY,TransmissionType.FLUID,TransmissionType.CHEMICAL};
    private final TileComponentConfig config;private final TileComponentEjector ejector;
    public UUID frequency;public String frequencyName="";public int peers;private int cursor;
    private long demandTick=Long.MIN_VALUE;private int demandMask;
    private final NodeSnapshot projection=new NodeSnapshot();public int snapshotType=-1;public net.minecraft.resources.ResourceLocation snapshotId;
    public NodeModule(BlockPos pos,BlockState state){super(pos,state);config=new TileComponentConfig(this,EnumSet.of(TYPES[0],TYPES[1],TYPES[2],TYPES[3]));
        setup(TYPES[0],inputs,outputs);setup(TYPES[1],List.of(node.energyIn),List.of(node.energyOut));setup(TYPES[2],node.fluidIn,node.fluidOut);setup(TYPES[3],node.chemicalIn,node.chemicalOut);
        // Fast bounded output below preserves the large existing FE/J and cargo throughput.
        ejector=new TileComponentEjector(this).setCanEject(t->false);
    }
    private void setup(TransmissionType type,List<?> in,List<?> out){var info=config.getConfig(type);var both=new ArrayList<Object>(in);both.addAll(out);
        info.addSlotInfo(DataType.INPUT,TileComponentConfig.createInfo(type,true,false,in));info.addSlotInfo(DataType.OUTPUT,TileComponentConfig.createInfo(type,false,true,out));info.addSlotInfo(DataType.INPUT_OUTPUT,TileComponentConfig.createInfo(type,true,true,both));
        for(var side:RelativeSide.values())info.setDataType(side==RelativeSide.FRONT?DataType.OUTPUT:DataType.INPUT,side);info.setCanEject(true);info.setEjecting(true);config.addConfigChangeListener(type,side->demandTick=Long.MIN_VALUE);
    }
    @Override public TileComponentConfig getConfig(){return config;}
    @Override public TileComponentEjector getEjector(){return ejector;}
    private void storage(IContentsListener listener){if(node==null)node=new NodeStorage(listener);}
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener){storage(listener);var h=EnergyContainerHelper.forSideWithConfig(this);h.addContainer(node.energyIn);h.addContainer(node.energyOut);return h.build();}
    @Override protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener){storage(listener);var h=FluidTankHelper.forSideWithConfig(this);node.fluidIn.forEach(h::addTank);node.fluidOut.forEach(h::addTank);return h.build();}
    @Override public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener){storage(listener);var h=ChemicalTankHelper.forSideWithConfig(this);node.chemicalIn.forEach(h::addTank);node.chemicalOut.forEach(h::addTank);return h.build();}
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener){inputs=new ArrayList<>();outputs=new ArrayList<>();var h=InventorySlotHelper.forSideWithConfig(this);
        for(int i=0;i<9;i++){var slot=new CoronalInventorySlot(false,listener,12+i%3*18,78+i/3*18);inputs.add(slot);h.addSlot(slot);}for(int i=0;i<9;i++){var slot=new CoronalInventorySlot(true,listener,154+i%3*18,78+i/3*18);outputs.add(slot);h.addSlot(slot);}return h.build();
    }
    public List<Direction> outputSides(int resource){var info=config.getConfig(TYPES[resource]);if(!autoEject||!info.isEjecting())return List.of();var result=new ArrayList<Direction>();for(var side:RelativeSide.values()){var type=info.getDataType(side);if(type==DataType.OUTPUT||type==DataType.INPUT_OUTPUT)result.add(side.getDirection(getDirection()));}return result;}
    private int connectedOutputs(){long now=level.getGameTime();if(demandTick!=Long.MIN_VALUE&&now-demandTick<5)return demandMask;demandTick=now;demandMask=0;
        for(int type=0;type<4;type++){if(!channel(type))continue;for(var side:outputSides(type)){var at=worldPosition.relative(side);if(!level.hasChunkAt(at))continue;var face=side.getOpposite();boolean connected=switch(type){
            case 0->{var cap=level.getCapability(Capabilities.ItemHandler.BLOCK,at,face);yield cap!=null&&cap.getSlots()>0;}
            case 1->level.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),at,face)!=null||level.getCapability(Capabilities.EnergyStorage.BLOCK,at,face)!=null;
            case 2->{var cap=level.getCapability(Capabilities.FluidHandler.BLOCK,at,face);yield cap!=null&&cap.getTanks()>0;}
            default->{var cap=level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),at,face);yield cap!=null&&cap.getChemicalTanks()>0;}};
            if(connected){demandMask|=1<<type;break;}
        }}return demandMask;
    }
    @Override public boolean receiveChannel(int resource){if(!super.receiveChannel(resource))return false;var info=config.getConfig(TYPES[resource]);return info.getSideConfig().stream().anyMatch(e->e.getValue()==DataType.OUTPUT||e.getValue()==DataType.INPUT_OUTPUT);}
    @Override protected void eject(){node.eject(this);int budget=36864;for(var side:outputSides(0)){var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))continue;var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side.getOpposite());if(handler==null)continue;
        for(var slot:outputs){if(budget<=0)return;if(slot.isEmpty())continue;var offered=slot.getStack().copyWithCount(Math.min(budget,slot.getCount()));int accepted=offered.getCount()-ItemHandlerHelper.insertItemStacked(handler,offered,true).getCount();if(accepted==0)continue;var taken=slot.extractItem(accepted,Action.EXECUTE,AutomationType.INTERNAL);var remainder=ItemHandlerHelper.insertItemStacked(handler,taken,false);budget-=taken.getCount()-remainder.getCount();if(!remainder.isEmpty())slot.insertItem(remainder,Action.EXECUTE,AutomationType.INTERNAL);}}
    }
    @Override public void onLoad(){super.onLoad();NodeNetwork.add(this);}
    @Override public void setRemoved(){NodeNetwork.remove(this);super.setRemoved();}
    @Override public void snapshot(int type,Object resource){projection.record(level.getGameTime(),type,resource);}
    @Override public CompoundTag getReducedUpdateTag(HolderLookup.Provider r){var tag=super.getReducedUpdateTag(r);projection.write(tag,level==null?0:level.getGameTime());return tag;}
    @Override public void handleUpdateTag(CompoundTag t,HolderLookup.Provider r){super.handleUpdateTag(t,r);snapshotType=t.contains("snapshot_type")?Math.clamp(t.getInt("snapshot_type"),-1,3):-1;snapshotId=net.minecraft.resources.ResourceLocation.tryParse(t.getString("snapshot_id"));}
    public boolean selectFrequency(Player player,UUID id){if(!FieldConnections.access(player,this)||!(level instanceof ServerLevel server))return false;var entry=id==null?null:NodeFrequencies.get(server).get(id);if(id!=null&&(entry==null||!entry.permits(player.getUUID())))return false;
        var owner=NodeNetwork.owner(this);if(owner==null){getSecurity().setOwnerUUID(player.getUUID());owner=player.getUUID();}if(entry!=null&&!entry.permits(owner))return false;
        frequency=id;frequencyName=entry==null?"":entry.name();NodeNetwork.changed(this);markForSave();sendUpdatePacket();return true;
    }
    @Override public FieldSource linked(){if(frequency==null){frequencyName="";peers=0;status="frequency_missing";return null;}
        var entry=NodeFrequencies.get((ServerLevel)level).get(frequency);if(entry==null){frequencyName="";peers=0;status="frequency_missing";return null;}frequencyName=entry.name();if(!entry.permits(NodeNetwork.owner(this))){peers=0;status="access";return null;}
        peers=NodeNetwork.members(this).size();var shared=NodeNetwork.power(this);if(shared==null)status="frequency_power";return shared;
    }
    @Override protected boolean transfer(FieldSource core){if(frequency==null)return false;if(!enabled||!canFunction()){status="paused";return false;}if(!core.hot()){status="cold";return false;}
        var members=NodeNetwork.members(this).stream().filter(n->NodeNetwork.reachable(this,n)&&n.enabled&&n.canFunction()).toList();if(members.isEmpty()){status="frequency_alone";return false;}boolean moved=false;int start=Math.floorMod(cursor++,members.size()),connected=0;for(var target:members)connected|=target.connectedOutputs();
        for(int i=0;i<Math.min(64,members.size());i++){var target=members.get((start+i)%members.size());if(NodeNetwork.reachable(this,target)&&!core.tile().isRemoved()&&core.permitted(this))moved|=moveTo(target,core,target.connectedOutputs()|(~connected&15));}if(moved)status="transferring";return moved;
    }
    @Override protected CompoundTag data(HolderLookup.Provider r){var t=super.data(r);if(frequency!=null)t.putUUID("frequency",frequency);return t;}
    @Override protected void read(CompoundTag t,HolderLookup.Provider r){super.read(t,r);frequency=t.hasUUID("frequency")?t.getUUID("frequency"):null;}
    @Override public void addContainerTrackers(MekanismContainer menu){super.addContainerTrackers(menu);menu.track(SyncableByteArray.create(()->frequencyName.getBytes(java.nio.charset.StandardCharsets.UTF_8),v->frequencyName=new String(v,java.nio.charset.StandardCharsets.UTF_8)));menu.track(SyncableInt.create(()->peers,v->peers=v));}
}
