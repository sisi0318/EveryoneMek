package dev.everyonemek.overloadcore.gear;

import dev.everyonemek.overloadcore.OverloadCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GearVisuals {
    public record Beam(Vec3 from,Vec3 to,int kind,int shooter,boolean rightHand,boolean impact) implements CustomPacketPayload {
        public static final Type<Beam> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"gear_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Beam> CODEC=StreamCodec.of((b,p)->{b.writeDouble(p.from.x);b.writeDouble(p.from.y);b.writeDouble(p.from.z);b.writeDouble(p.to.x);b.writeDouble(p.to.y);b.writeDouble(p.to.z);b.writeByte(p.kind);b.writeVarInt(p.shooter);b.writeBoolean(p.rightHand);b.writeBoolean(p.impact);},b->new Beam(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readUnsignedByte(),b.readVarInt(),b.readBoolean(),b.readBoolean()));
        public Type<Beam> type(){return TYPE;}
    }
    public static java.util.function.Consumer<Beam> client=p->{};
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("2").playToClient(Beam.TYPE,Beam.CODEC,(p,c)->c.enqueueWork(()->client.accept(p)));}
    public static void send(ServerPlayer player,Vec3 from,Vec3 to,int kind){
        send(player,from,to,kind,false);
    }
    public static void send(ServerPlayer player,Vec3 from,Vec3 to,int kind,boolean impact){
        var arm=player.getUsedItemHand()==net.minecraft.world.InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        var packet=new Beam(from,to,kind,player.getId(),arm==net.minecraft.world.entity.HumanoidArm.RIGHT,impact);
        for(var p:player.serverLevel().players())if(p.distanceToSqr(player)<=96*96)PacketDistributor.sendToPlayer(p,packet);
    }
    private GearVisuals(){}
}
