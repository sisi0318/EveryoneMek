package dev.everyonemek.gravity.expansion;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class LinkPanelNetwork {
    public static final int MAX_DEVICES=192;
    public static Consumer<Snapshot> clientReceiver=packet->{};
    public record Device(BlockPos pos,ResourceLocation item,Component name,int kind,String state,boolean active,boolean enabled,int channels,BlockPos source,BlockPos peer,boolean powerValid,boolean routeValid,boolean receiver){
        public boolean core(){return kind<2;}public boolean node(){return kind==ModuleKind.NODE.ordinal()+2;}
    }
    public record Snapshot(int menuId,long session,int revision,int range,int linkRange,boolean canEditLinkRange,int total,List<Device> devices,String feedback) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(id("link_panel_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->{
            b.writeVarInt(p.menuId);b.writeLong(p.session);b.writeVarInt(p.revision);b.writeVarInt(p.range);b.writeVarInt(p.linkRange);b.writeBoolean(p.canEditLinkRange);b.writeVarInt(p.total);b.writeVarInt(p.devices.size());
            for(var d:p.devices){b.writeBlockPos(d.pos);b.writeResourceLocation(d.item);ComponentSerialization.STREAM_CODEC.encode(b,d.name);b.writeByte(d.kind);b.writeUtf(d.state,64);b.writeBoolean(d.active);b.writeBoolean(d.enabled);b.writeByte(d.channels);writePos(b,d.source);writePos(b,d.peer);b.writeBoolean(d.powerValid);b.writeBoolean(d.routeValid);b.writeBoolean(d.receiver);}b.writeUtf(p.feedback,64);
        },b->{int menu=b.readVarInt();long session=b.readLong();int revision=b.readVarInt(),range=b.readVarInt(),linkRange=b.readVarInt();boolean canEdit=b.readBoolean();int total=b.readVarInt(),count=b.readVarInt();if(count<0||count>MAX_DEVICES||range<ModuleConfig.MIN_RANGE||range>ModuleConfig.MAX_RANGE||linkRange<ModuleConfig.MIN_RANGE||linkRange>ModuleConfig.MAX_RANGE)throw new IllegalArgumentException("Invalid panel snapshot");var devices=new ArrayList<Device>(count);
            for(int i=0;i<count;i++)devices.add(new Device(b.readBlockPos(),b.readResourceLocation(),ComponentSerialization.STREAM_CODEC.decode(b),b.readUnsignedByte(),b.readUtf(64),b.readBoolean(),b.readBoolean(),b.readUnsignedByte(),readPos(b),readPos(b),b.readBoolean(),b.readBoolean(),b.readBoolean()));return new Snapshot(menu,session,revision,range,linkRange,canEdit,total,List.copyOf(devices),b.readUtf(64));});
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** 0 refresh, 1 connect, 2 unlink power, 3 unlink route, 4..7 channels, 8 scan range, 9 world link range. */
    public record Action(int menuId,long session,int revision,int operation,BlockPos from,BlockPos to,int value) implements CustomPacketPayload {
        public Action(int menuId,long session,int revision,int operation,BlockPos from,BlockPos to){this(menuId,session,revision,operation,from,to,0);}
        public static final Type<Action> TYPE=new Type<>(id("link_panel_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menuId);b.writeLong(p.session);b.writeVarInt(p.revision);b.writeByte(p.operation);writePos(b,p.from);writePos(b,p.to);b.writeVarInt(p.value);},b->new Action(b.readVarInt(),b.readLong(),b.readVarInt(),b.readUnsignedByte(),readPos(b),readPos(b),b.readVarInt()));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("mekgravity",path);}
    private static void writePos(RegistryFriendlyByteBuf b,BlockPos p){b.writeBoolean(p!=null);if(p!=null)b.writeBlockPos(p);}
    private static BlockPos readPos(RegistryFriendlyByteBuf b){return b.readBoolean()?b.readBlockPos():null;}
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("2");r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(p,c)->c.enqueueWork(()->clientReceiver.accept(p)));r.playToServer(Action.TYPE,Action.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player&&player.containerMenu instanceof LinkPanelMenu menu)menu.handle(player,p);}));}
    private LinkPanelNetwork(){}
}
