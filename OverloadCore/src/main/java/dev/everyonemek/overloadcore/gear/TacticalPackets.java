package dev.everyonemek.overloadcore.gear;

import dev.everyonemek.overloadcore.OverloadCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class TacticalPackets {
    public record Input(int action,boolean down,boolean offhand) implements CustomPacketPayload {
        public static final Type<Input> TYPE=new Type<>(id("tactical_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.of((b,p)->{b.writeByte(p.action);b.writeBoolean(p.down);b.writeBoolean(p.offhand);},b->new Input(b.readUnsignedByte(),b.readBoolean(),b.readBoolean()));
        @Override public Type<Input> type(){return TYPE;}
    }
    // Kind 0: directional guard; 1: own target mark; 2: counterattack; 3: extra penetration.
    public record Visual(int entity,java.util.UUID identity,int kind,int ticks,int value) implements CustomPacketPayload {
        public static final Type<Visual> TYPE=new Type<>(id("tactical_visual"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.entity);b.writeUUID(p.identity);b.writeByte(p.kind);b.writeVarInt(p.ticks);b.writeVarInt(p.value);},b->new Visual(b.readVarInt(),b.readUUID(),b.readUnsignedByte(),b.readVarInt(),b.readVarInt()));
        @Override public Type<Visual> type(){return TYPE;}
    }
    public static java.util.function.Consumer<Visual> client=p->{};
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,path);}
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("1");
        r.playToServer(Input.TYPE,Input.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player){if(p.action==0)TacticalCombat.switchForm(player,p.offhand);else if(p.action==1)TacticalCombat.guardInput(player,p.down);}}));
        r.playToClient(Visual.TYPE,Visual.CODEC,(p,c)->c.enqueueWork(()->{if(p.kind>=0&&p.kind<=3&&p.ticks>=0&&p.ticks<=600)client.accept(p);}));
    }
    public static void own(ServerPlayer p,Entity entity,int kind,int ticks,int value){PacketDistributor.sendToPlayer(p,new Visual(entity.getId(),entity.getUUID(),kind,ticks,value));}
    public static void guard(ServerPlayer p,int ticks,int window){PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new Visual(p.getId(),p.getUUID(),0,ticks,window));}
    private TacticalPackets(){}
}
