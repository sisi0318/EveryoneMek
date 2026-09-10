package dev.everyonemek.ars;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SetRecipeLockPayload(int containerId, String recipe) implements CustomPacketPayload {
    public static final Type<SetRecipeLockPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsMekanism.ID, "recipe_lock"));
    public static final StreamCodec<ByteBuf, SetRecipeLockPayload> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, SetRecipeLockPayload::containerId,
          ByteBufCodecs.stringUtf8(256), SetRecipeLockPayload::recipe, SetRecipeLockPayload::new);
    @Override public Type<SetRecipeLockPayload> type() { return TYPE; }
    public void handle(net.minecraft.world.entity.player.Player player) {
        if (player.containerMenu instanceof MachineMenu menu && menu.containerId == containerId)
            menu.setRecipeLock(player, recipe);
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (message, context) -> message.handle(context.player()));
    }
}
