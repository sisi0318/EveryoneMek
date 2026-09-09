package dev.everyonemek.natures;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Numeric settings are accepted only for the sender's currently open, accessible machine menu. */
public record SetMachineSettingPayload(int containerId, int setting, int value) implements CustomPacketPayload {
    public static final int CONTROL_LOWER = 0, CONTROL_UPPER = 1;
    public static final Type<SetMachineSettingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "machine_setting"));
    public static final StreamCodec<ByteBuf, SetMachineSettingPayload> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, SetMachineSettingPayload::containerId,
          ByteBufCodecs.VAR_INT, SetMachineSettingPayload::setting,
          ByteBufCodecs.VAR_INT, SetMachineSettingPayload::value, SetMachineSettingPayload::new);

    @Override public Type<SetMachineSettingPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (message, context) -> {
            if (context.player().containerMenu instanceof MachineMenu menu && menu.containerId == message.containerId())
                menu.applySetting(context.player(), message.setting(), message.value());
        });
    }
}
