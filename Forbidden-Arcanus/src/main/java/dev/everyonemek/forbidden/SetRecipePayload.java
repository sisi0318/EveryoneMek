package dev.everyonemek.forbidden;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SetRecipePayload(int containerId, String recipe) implements CustomPacketPayload {
    public static final Type<SetRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ForbiddenMekanism.ID, "recipe"));
    public static final StreamCodec<ByteBuf, SetRecipePayload> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, SetRecipePayload::containerId, ByteBufCodecs.stringUtf8(256), SetRecipePayload::recipe, SetRecipePayload::new);
    @Override public Type<SetRecipePayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (message, context) -> {
            if (context.player().containerMenu instanceof MachineMenu menu && menu.containerId == message.containerId())
                menu.setRecipe(context.player(), message.recipe());
        });
    }
}
