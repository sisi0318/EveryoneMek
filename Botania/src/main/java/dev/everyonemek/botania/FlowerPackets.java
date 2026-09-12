package dev.everyonemek.botania;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class FlowerPackets {
    public record Settings(int menuId, int action, String value) implements CustomPacketPayload {
        public static final Type<Settings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Settings> CODEC = StreamCodec.of(
              (buffer, packet) -> { buffer.writeVarInt(packet.menuId); buffer.writeVarInt(packet.action); buffer.writeUtf(packet.value, 128); },
              buffer -> new Settings(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Snapshot(int menuId, CompoundTag values) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of(
              (buffer, packet) -> { buffer.writeVarInt(packet.menuId); buffer.writeNbt(packet.values); },
              buffer -> new Snapshot(buffer.readVarInt(), buffer.readNbt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        // The joining screen requires acknowledged settings and connect-as support on both ends.
        var registrar = event.registrar("3");
        registrar.playToServer(Settings.TYPE, Settings.CODEC, FlowerPackets::handleSettings);
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof FlowerMenu menu && menu.containerId == packet.menuId && packet.values != null) menu.state = packet.values;
            else if (context.player().containerMenu instanceof ManaMachineMenu menu && menu.containerId == packet.menuId && packet.values != null) menu.state = packet.values;
        }));
    }
    public static void handleSettings(Settings packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof FlowerMenu menu && menu.containerId == packet.menuId) {
                menu.handleSettings(player, packet.action, packet.value);
            }
            else if (player.containerMenu instanceof ManaMachineMenu menu && menu.containerId == packet.menuId) menu.handleSettings(player, packet.action, packet.value);
        });
    }
    private FlowerPackets() { }
}
