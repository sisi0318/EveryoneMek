package dev.everyonemek.oritech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class AddonPackets {
    public record Unload(int menu,ItemStack template,boolean all) implements CustomPacketPayload {
        public static final Type<Unload> TYPE=new Type<>(Content.id("unload_addon"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Unload> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);ItemStack.STREAM_CODEC.encode(b,p.template);b.writeBoolean(p.all);},
            b->new Unload(b.readVarInt(),ItemStack.STREAM_CODEC.decode(b),b.readBoolean()));
        @Override public Type<Unload> type(){return TYPE;}
    }
    public static int handle(Unload packet,Player player){return player.containerMenu instanceof AddonMenu menu&&menu.containerId==packet.menu?menu.unload(player,packet.template,packet.all):0;}
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToServer(Unload.TYPE,Unload.CODEC,(packet,context)->context.enqueueWork(()->handle(packet,context.player())));}
    private AddonPackets(){}
}
