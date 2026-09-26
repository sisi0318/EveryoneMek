package dev.everyonemek.gravity.expansion;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NodeFrequencyNetwork {
    public static Consumer<Snapshot> clientReceiver=p->{};
    public record Snapshot(int menu,long session,BlockPos pos,UUID current,List<NodeFrequencies.Entry> entries,String feedback) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("mekgravity","node_frequencies"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeLong(p.session);b.writeBlockPos(p.pos);uuid(b,p.current);b.writeVarInt(p.entries.size());for(var e:p.entries){b.writeUUID(e.id());b.writeUUID(e.owner());b.writeUtf(e.name(),32);b.writeBoolean(e.shared());}b.writeUtf(p.feedback,48);},b->{int menu=b.readVarInt();long session=b.readLong();var pos=b.readBlockPos();var current=uuid(b);int count=b.readVarInt();if(count<0||count>128)throw new IllegalArgumentException("Frequency list too large");var entries=new ArrayList<NodeFrequencies.Entry>();for(int i=0;i<count;i++)entries.add(new NodeFrequencies.Entry(b.readUUID(),b.readUUID(),b.readUtf(32),b.readBoolean()));return new Snapshot(menu,session,pos,current,List.copyOf(entries),b.readUtf(48));});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** 0 list, 1 create/join by name, 2 join selected, 3 leave, 4 delete owned frequency. */
    public record Action(int menu,long session,BlockPos pos,int operation,UUID id,String name,boolean shared) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("mekgravity","node_frequency_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeLong(p.session);b.writeBlockPos(p.pos);b.writeByte(p.operation);uuid(b,p.id);b.writeUtf(p.name,32);b.writeBoolean(p.shared);},b->new Action(b.readVarInt(),b.readLong(),b.readBlockPos(),b.readUnsignedByte(),uuid(b),b.readUtf(32),b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static void uuid(RegistryFriendlyByteBuf b,UUID id){b.writeBoolean(id!=null);if(id!=null)b.writeUUID(id);}
    private static UUID uuid(RegistryFriendlyByteBuf b){return b.readBoolean()?b.readUUID():null;}
    public static boolean handle(ServerPlayer player,Action request){if(!(player.containerMenu instanceof ModuleMenu menu)||menu.containerId!=request.menu||!(menu.getTileEntity() instanceof NodeModule node)||!node.getBlockPos().equals(request.pos)||!menu.stillValid(player)||!node.access(player)||player.level().getBlockEntity(request.pos)!=node)return false;
        if(request.operation!=0&&request.session!=menu.nodeSession)return false;long now=player.level().getGameTime();if(menu.frequencyActionTick!=now){menu.frequencyActionTick=now;menu.frequencyActions=0;}if(++menu.frequencyActions>4)return false;
        var data=NodeFrequencies.get(player.serverLevel());boolean ok=request.operation==0;
        if(request.operation==1){var entry=data.create(player.getUUID(),request.name,request.shared);ok=entry!=null&&node.selectFrequency(player,entry.id());}
        else if(request.operation==2)ok=request.id!=null&&node.selectFrequency(player,request.id);
        else if(request.operation==3)ok=node.selectFrequency(player,null);
        else if(request.operation==4&&request.id!=null){ok=data.remove(request.id,player.getUUID());if(ok&&request.id.equals(node.frequency))node.selectFrequency(player,null);}
        PacketDistributor.sendToPlayer(player,new Snapshot(menu.containerId,menu.nodeSession,node.getBlockPos(),node.frequency,data.visible(player.getUUID()),request.operation==0?"frequency_help":ok?"frequency_saved":"frequency_denied"));return ok;
    }
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("1");r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(p,c)->c.enqueueWork(()->clientReceiver.accept(p)));r.playToServer(Action.TYPE,Action.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer s)handle(s,p);}));}
    private NodeFrequencyNetwork(){}
}
