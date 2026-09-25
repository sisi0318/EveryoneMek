package dev.everyonemek.gravity.solar;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.sync.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
public final class SolarMenu extends MekanismTileContainer<SolarController> {
    private static final java.util.List<String> STATES=java.util.List.of("structure","unloaded","occupied","interior","component","facing","wing_mixed","energy_ports","ready","stopped","redstone","startup_config","fuel_missing","refill_off","charging","reserve_low","full","standby","limited","running");
    public boolean coreHot;public int reserveFuelCount=-1;public long reserveFuelEnergy;
    public int portIndex,portCount;public BlockPos portPos;public long portInput,portOutput;public boolean portIsOutput;
    private BlockPos clicked;public BlockPos errorPos;public boolean formed;public int tier,outputs;public final int[] wings=new int[4];
    public long capacity,startup,reserve,power,constraint,collector,inputLimit,outputLimit;
    public SolarMenu(int id,Inventory inv,SolarController c,BlockPos clicked){super(SolarContent.MENU,id,inv,c);this.clicked=clicked;
        track(SyncableBoolean.create(c::isCoreHot,v->coreHot=v));
        track(SyncableInt.create(()->c.structure.formed?c.fuelStock().portions:-1,v->reserveFuelCount=v));
        track(SyncableLong.create(()->c.structure.formed?c.fuelStock().energy:0,v->reserveFuelEnergy=v));
        track(SyncableInt.create(()->energyPorts().size(),v->portCount=v));
        track(SyncableInt.create(()->Math.floorMod(portIndex,Math.max(1,energyPorts().size())),v->portIndex=v));
        track(SyncableLong.create(()->selectedPort()==null?Long.MIN_VALUE:selectedPort().getBlockPos().asLong(),v->portPos=v==Long.MIN_VALUE?null:BlockPos.of(v)));
        track(SyncableLong.create(()->selectedPort()==null?0:selectedPort().inputRate(),v->portInput=v));
        track(SyncableLong.create(()->selectedPort()==null?0:selectedPort().outputRate(),v->portOutput=v));
        track(SyncableBoolean.create(()->selectedPort()!=null&&selectedPort().output(),v->portIsOutput=v));
        track(SyncableInt.create(()->c.buildTier,v->c.buildTier=v));
        track(SyncableBoolean.create(()->c.structure.formed,v->formed=v));track(SyncableInt.create(()->c.structure.tier,v->tier=v));track(SyncableInt.create(()->c.structure.outputs,v->outputs=v));
        for(int i=0;i<4;i++){int wing=i;track(SyncableInt.create(()->c.structure.wings[wing],v->wings[wing]=v));}
        track(SyncableLong.create(c::capacity,v->capacity=v));track(SyncableLong.create(c::startup,v->startup=v));track(SyncableLong.create(c::reserve,v->reserve=v));
        track(SyncableLong.create(c.structure::power,v->power=v));track(SyncableLong.create(c.structure::constraintLimit,v->constraint=v));track(SyncableLong.create(c.structure::collectorLimit,v->collector=v));
        track(SyncableLong.create(c::inputLimit,v->inputLimit=v));track(SyncableLong.create(c::outputLimit,v->outputLimit=v));
        track(SyncableLong.create(()->c.structure.errorPos==null?Long.MIN_VALUE:c.structure.errorPos.asLong(),v->errorPos=v==Long.MIN_VALUE?null:BlockPos.of(v)));
        track(SyncableInt.create(()->Math.max(0,STATES.indexOf(c.status)),v->c.status=STATES.get(Math.clamp(v,0,STATES.size()-1))));
    }
    private java.util.List<SolarPart> energyPorts(){return !tile.structure.formed?java.util.List.of():tile.structure.ports.stream().filter(p->p.kind()==SolarBlock.Kind.ENERGY&&!p.isRemoved()).toList();}
    private SolarPart selectedPort(){var ports=energyPorts();return ports.isEmpty()?null:ports.get(Math.floorMod(portIndex,ports.size()));}
    public static boolean access(Player p,SolarController c,BlockPos pos){if(p.level()!=c.getLevel()||!c.access(p)||!p.level().hasChunkAt(pos)||p.distanceToSqr(pos.getCenter())>64)return false;return pos.equals(c.getBlockPos())?p.level().getBlockEntity(pos)==c:p.level().getBlockEntity(pos) instanceof SolarPart part&&part.controller()==c;}
    public static void open(Player p,SolarController c,BlockPos pos){if(p instanceof ServerPlayer s&&access(p,c,pos))s.openMenu(new SimpleMenuProvider((id,inv,player)->new SolarMenu(id,inv,c,pos),c.getDisplayName()),b->{b.writeBlockPos(c.getBlockPos());b.writeBlockPos(pos);});}
    public static SolarMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf b){var pos=b.readBlockPos();return new SolarMenu(id,inv,(SolarController)inv.player.level().getBlockEntity(pos),b.readBlockPos());}
    @Override protected int getInventoryXOffset(){return 39;}
    @Override protected int getInventoryYOffset(){return 176;}
    @Override public boolean stillValid(Player p){return clicked==null||!tile.isRemoved()&&(p.level().isClientSide||access(p,tile,clicked));}
    @Override public boolean clickMenuButton(Player p,int id){if(p.level().isClientSide||!stillValid(p)||!tile.access(p))return false;
        if(id==1)tile.enabled=!tile.enabled;
        else if(id==2){SolarConstruction.preview(tile,(ServerPlayer)p);return true;}
        else if(id==3)return SolarConstruction.build(tile,(ServerPlayer)p);
        else if(id==4)tile.autoEject=!tile.autoEject;
        else if(id==5)tile.automatic=!tile.automatic;
        else if(id==6)tile.refill=!tile.refill;
        else if(id==7){boolean ok=tile.recover(p);p.displayClientMessage(SolarContent.text(ok?"recovered":"recover_failed"),true);return ok;}
        else if(id==20)tile.buildTier=(tile.buildTier+1)%4;
        else if(id==21)return SolarConstruction.upgrade(tile,(ServerPlayer)p);
        else if(id==30||id==31){portIndex=energyPorts().isEmpty()?0:Math.floorMod(portIndex+(id==30?-1:1),energyPorts().size());return true;}
        else if(id>=101&&id<=200)tile.load=id-100;
        else return false;tile.markForSave();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int i){return stillValid(p)?super.quickMoveStack(p,i):ItemStack.EMPTY;}
}
