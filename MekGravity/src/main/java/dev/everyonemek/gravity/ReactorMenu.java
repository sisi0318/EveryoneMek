package dev.everyonemek.gravity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.sync.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
public final class ReactorMenu extends MekanismTileContainer<Controller> {
    private static final java.util.List<String> STATES=java.util.List.of("structure","unloaded","occupied","shell","frame","interior","core","coil","coil_facing","ports","port_limit","ready","stopped","redstone","fuel_missing","cold_missing","hot_blocked","charging","reserve_low","full","coolant_invalid","output_limited","cold_limited","running","startup_config","fuel_port","cold_port","hot_port","excitation_port","output_port","fuel_limited");
    public int reserveFuelCount=-1;public long reserveFuelEnergy;
    public int portIndex,portCount;public BlockPos portPos;public long portInput,portOutput;public boolean portIsOutput;
    private BlockPos clicked;public BlockPos errorPos;public int grade,coils,outputs;public boolean formed;public long capacity,startup,reserve,tankCapacity,inputLimit,outputLimit;
    public ReactorMenu(int id,Inventory inv,Controller c,BlockPos clicked){super(Content.MENU,id,inv,c);this.clicked=clicked;
        track(SyncableInt.create(()->c.structure.formed?c.fuelStock().portions:-1,v->reserveFuelCount=v));
        track(SyncableLong.create(()->c.structure.formed?c.fuelStock().energy:0,v->reserveFuelEnergy=v));
        track(SyncableInt.create(()->energyPorts().size(),v->portCount=v));
        track(SyncableInt.create(()->Math.floorMod(portIndex,Math.max(1,energyPorts().size())),v->portIndex=v));
        track(SyncableLong.create(()->selectedPort()==null?Long.MIN_VALUE:selectedPort().getBlockPos().asLong(),v->portPos=v==Long.MIN_VALUE?null:BlockPos.of(v)));
        track(SyncableLong.create(()->selectedPort()==null?0:selectedPort().inputRate(),v->portInput=v));
        track(SyncableLong.create(()->selectedPort()==null?0:selectedPort().outputRate(),v->portOutput=v));
        track(SyncableBoolean.create(()->selectedPort()!=null&&selectedPort().output(),v->portIsOutput=v));
        track(SyncableInt.create(()->c.buildTier,v->c.buildTier=v));
        track(SyncableBoolean.create(()->c.structure.formed,v->formed=v));track(SyncableInt.create(()->c.structure.grade.ordinal(),v->grade=v));track(SyncableInt.create(()->c.structure.coils.size(),v->coils=v));track(SyncableInt.create(()->(int)c.structure.ports.stream().filter(p->p.kind()==PartBlock.Kind.ENERGY&&p.output()).count(),v->outputs=v));
        track(SyncableLong.create(c::capacity,v->capacity=v));track(SyncableLong.create(c::startup,v->startup=v));track(SyncableLong.create(c::reserve,v->reserve=v));track(SyncableLong.create(c::tankCapacity,v->tankCapacity=v));
        track(SyncableLong.create(c::inputLimit,v->inputLimit=v));track(SyncableLong.create(c::outputLimit,v->outputLimit=v));
        track(SyncableLong.create(()->c.structure.errorPos==null?Long.MIN_VALUE:c.structure.errorPos.asLong(),v->errorPos=v==Long.MIN_VALUE?null:BlockPos.of(v)));
        track(SyncableInt.create(()->Math.max(0,STATES.indexOf(c.status)),v->c.status=STATES.get(Math.clamp(v,0,STATES.size()-1))));
    }
    private java.util.List<Part> energyPorts(){return !tile.structure.formed?java.util.List.of():tile.structure.ports.stream().filter(p->p.kind()==PartBlock.Kind.ENERGY&&!p.isRemoved()).toList();}
    private Part selectedPort(){var ports=energyPorts();return ports.isEmpty()?null:ports.get(Math.floorMod(portIndex,ports.size()));}
    public static boolean access(Player p,Controller c,BlockPos pos){if(p.level()!=c.getLevel()||!c.access(p)||!p.level().hasChunkAt(pos)||p.distanceToSqr(pos.getCenter())>64)return false;return pos.equals(c.getBlockPos())?p.level().getBlockEntity(pos)==c:p.level().getBlockEntity(pos) instanceof Part part&&part.controller()==c;}
    public static void open(Player p,Controller c,BlockPos pos){if(p instanceof ServerPlayer server&&access(p,c,pos))server.openMenu(new SimpleMenuProvider((id,inv,player)->new ReactorMenu(id,inv,c,pos),c.getDisplayName()),b->{b.writeBlockPos(c.getBlockPos());b.writeBlockPos(pos);});}
    public static ReactorMenu fromNetwork(int id,Inventory inv,RegistryFriendlyByteBuf b){var pos=b.readBlockPos();return new ReactorMenu(id,inv,(Controller)inv.player.level().getBlockEntity(pos),b.readBlockPos());}
    @Override protected int getInventoryXOffset(){return 35;}
    @Override protected int getInventoryYOffset(){return 159;}
    @Override public boolean stillValid(Player p){return clicked==null||!tile.isRemoved()&&(p.level().isClientSide||access(p,tile,clicked));}
    @Override public boolean clickMenuButton(Player p,int id){if(p.level().isClientSide||!stillValid(p)||!tile.access(p))return false;
        if(id==1)tile.enabled=!tile.enabled;
        else if(id==2){Construction.preview(tile,(ServerPlayer)p);return true;}
        else if(id==3)return Construction.build(tile,(ServerPlayer)p);
        else if(id==4)tile.autoEject=!tile.autoEject;
        else if(id==20)tile.buildTier=(tile.buildTier+1)%4;
        else if(id==21)return Construction.upgrade(tile,(ServerPlayer)p);
        else if(id==30||id==31){portIndex=energyPorts().isEmpty()?0:Math.floorMod(portIndex+(id==30?-1:1),energyPorts().size());return true;}
        else if(id>=101&&id<=200)tile.load=id-100;
        else return false;tile.markForSave();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int i){return stillValid(p)?super.quickMoveStack(p,i):ItemStack.EMPTY;}
}
