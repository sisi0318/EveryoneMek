package dev.everyonemek.gravity.expansion;
import java.util.*;
import java.util.function.Consumer;
import dev.everyonemek.gravity.Controller;
import dev.everyonemek.gravity.solar.SolarController;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Small purpose-specific power picker, scoped to the real machine menu. */
public final class ModuleSourceNetwork {
    public static Consumer<Snapshot> clientReceiver=p->{};
    public record Source(BlockPos pos,String name,boolean solar,boolean formed,boolean hot,long available){}
    public record Snapshot(int menu,long session,List<Source> sources,String feedback) implements CustomPacketPayload{
        public static final Type<Snapshot> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("mekgravity","module_sources"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeLong(p.session);b.writeVarInt(p.sources.size());for(var s:p.sources){b.writeBlockPos(s.pos);b.writeUtf(s.name,80);b.writeBoolean(s.solar);b.writeBoolean(s.formed);b.writeBoolean(s.hot);b.writeLong(s.available);}b.writeUtf(p.feedback,48);},b->{int menu=b.readVarInt();long session=b.readLong();int count=b.readVarInt();if(count<0||count>96)throw new IllegalArgumentException("Source list bound");var list=new ArrayList<Source>();for(int i=0;i<count;i++)list.add(new Source(b.readBlockPos(),b.readUtf(80),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readLong()));return new Snapshot(menu,session,List.copyOf(list),b.readUtf(48));});
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Action(int menu,long session,int operation,BlockPos source) implements CustomPacketPayload{
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("mekgravity","module_source_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeLong(p.session);b.writeByte(p.operation);b.writeBoolean(p.source!=null);if(p.source!=null)b.writeBlockPos(p.source);},b->new Action(b.readVarInt(),b.readLong(),b.readUnsignedByte(),b.readBoolean()?b.readBlockPos():null));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static boolean handle(ServerPlayer p,Action request){if(!(p.containerMenu instanceof ModuleMenu menu)||request.menu!=menu.containerId||request.session!=menu.nodeSession||!menu.stillValid(p))return false;var module=menu.getTileEntity();if(!module.access(p)||p.level().getBlockEntity(module.getBlockPos())!=module)return false;
        long now=p.level().getGameTime();if(menu.sourceActionTick!=now){menu.sourceActionTick=now;menu.sourceActions=0;}if(++menu.sourceActions>4)return false;boolean ok=request.operation==0;String feedback="source_choose";
        if(request.operation==0&&(menu.sourceScan==Long.MIN_VALUE||now-menu.sourceScan>=20)){menu.sourceScan=now;int range=ModuleConfig.RANGE.get();var origin=module.getBlockPos();var found=new ArrayList<TileEntityMekanism>();
            for(int x=(origin.getX()-range)>>4;x<=(origin.getX()+range)>>4;x++)for(int z=(origin.getZ()-range)>>4;z<=(origin.getZ()+range)>>4;z++){var chunk=p.serverLevel().getChunkSource().getChunkNow(x,z);if(chunk!=null)for(var be:chunk.getBlockEntities().values())if((be instanceof Controller||be instanceof SolarController)&&FieldConnections.canPower(p,new FieldSource((TileEntityMekanism)be),module))found.add((TileEntityMekanism)be);}
            found.sort(Comparator.comparingDouble(be->be.getBlockPos().distSqr(origin)));var selected=new LinkedHashMap<BlockPos,BlockEntity>();found.stream().limit(96).forEach(be->selected.put(be.getBlockPos(),be));menu.sources=selected;
        }else if(request.operation==1&&request.source!=null&&p.level().hasChunkAt(request.source)){var be=p.level().getBlockEntity(request.source);ok=be==menu.sources.get(request.source)&&(be instanceof Controller||be instanceof SolarController)&&FieldConnections.power(p,new FieldSource((TileEntityMekanism)be),module);feedback=ok?"linked":"link_failed";}
        else if(request.operation==2){module.source=null;module.markForSave();module.sendUpdatePacket();ok=true;feedback="source_unbound";}
        else if(request.operation!=0)feedback="link_failed";
        var list=new ArrayList<Source>();for(var entry:menu.sources.entrySet()){if(!p.level().hasChunkAt(entry.getKey())||p.level().getBlockEntity(entry.getKey())!=entry.getValue())continue;var tile=(TileEntityMekanism)entry.getValue();var core=new FieldSource(tile);if(!FieldConnections.canPower(p,core,module))continue;String name=tile.getCustomName()==null?"":tile.getCustomName().getString();list.add(new Source(tile.getBlockPos(),name.length()>80?name.substring(0,80):name,core.solar(),core.formed(),core.hot(),core.available()));}
        PacketDistributor.sendToPlayer(p,new Snapshot(menu.containerId,menu.nodeSession,List.copyOf(list),feedback));return ok;
    }
    public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("1");r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(p,c)->c.enqueueWork(()->clientReceiver.accept(p)));r.playToServer(Action.TYPE,Action.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer s)handle(s,p);}));}
}
