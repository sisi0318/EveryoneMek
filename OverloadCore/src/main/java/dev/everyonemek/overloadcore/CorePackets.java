package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CorePackets {
    public static CompoundTag clientState = new CompoundTag();
    public static CompoundTag clientWardState = new CompoundTag();
    public record WardStatus(CompoundTag data) implements CustomPacketPayload {
        public static final Type<WardStatus> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "ward_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WardStatus> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, WardStatus::data, WardStatus::new);
        @Override public Type<WardStatus> type() { return TYPE; }
    }
    public record WardExtreme(boolean enabled) implements CustomPacketPayload {
        public static final Type<WardExtreme> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "ward_extreme"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WardExtreme> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, WardExtreme::enabled, WardExtreme::new);
        @Override public Type<WardExtreme> type() { return TYPE; }
    }
    // Retain the protocol-2 payload so older servers can connect; it no longer triggers a screen animation.
    public record WardPulse() implements CustomPacketPayload {
        public static final Type<WardPulse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "ward_pulse"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WardPulse> CODEC = StreamCodec.unit(new WardPulse());
        @Override public Type<WardPulse> type() { return TYPE; }
    }
    public record Status(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Status::data, Status::new);
        @Override public Type<Status> type() { return TYPE; }
    }
    public record Request(int action) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Request::action, Request::new);
        @Override public Type<Request> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(WardStatus.TYPE, WardStatus.CODEC, (packet, context) -> context.enqueueWork(() -> clientWardState = packet.data.copy()));
        registrar.playToServer(WardExtreme.TYPE, WardExtreme.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handleWardExtreme(player, packet);
        }));
        registrar.playToClient(WardPulse.TYPE, WardPulse.CODEC, (packet, context) -> { });
        registrar.playToClient(Status.TYPE, Status.CODEC, (packet, context) -> context.enqueueWork(() -> clientState = packet.data.copy()));
        registrar.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.action != 0) return;
            var data = CoreBinding.data(player); long tick = player.level().getGameTime();
            if (tick - data.getLong("last_request") < 5) return;
            data.putLong("last_request", tick); CoreBinding.save(player, data); sendStatus(player, true);
        }));
    }
    public static void handleWardExtreme(ServerPlayer player, WardExtreme packet) { WardRuntime.setExtreme(player, packet.enabled); }
    public static void sendWardStatus(ServerPlayer player, CompoundTag tag) { PacketDistributor.sendToPlayer(player, new WardStatus(tag)); }
    public static void sendStatus(ServerPlayer player, boolean report) {
        var tag = new CompoundTag(); var data = CoreBinding.data(player);
        tag.putBoolean("bound", CoreBinding.bound(player));
        tag.putBoolean("heavy", data.getBoolean("heavy")); tag.putInt("load", data.getInt("load")); tag.putInt("heat", data.getInt("heat")); tag.putInt("noise", data.getInt("noise"));
        tag.putLong("energy", (long) EnergyUnit.FORGE_ENERGY.convertTo(Math.max(0, data.getLong("energy"))));
        var devices = new ListTag(); var seen = new HashSet<net.minecraft.core.BlockPos>();
        if (CoreBinding.active(player)) for (var tile : DeviceTracker.nearby(player.level(), player.position(), CoreConfig.RANGE.get())) {
            var device = DeviceScope.device(tile);
            if (!DeviceScope.allowed(device, player) || !seen.add(device.anchor().getBlockPos()) || devices.size() >= 64) continue;
            var entry = new CompoundTag(); entry.putLong("pos", device.anchor().getBlockPos().asLong());
            entry.putString("name", tile.getBlockState().getBlock().getDescriptionId()); entry.putBoolean("working", DeviceTracker.working(tile) || DeviceTracker.working(device.anchor()));
            String issue = entry.getBoolean("working") ? "working" : "idle"; boolean bonus = false; var jobs = DeviceScope.data(tile).getCompound("jobs");
            for (String key : jobs.getAllKeys()) {
                var job=jobs.getCompound(key); bonus |= job.getBoolean("bonus_recipe"); String next=job.getString("issue");
                if (next.equals("energy") || next.equals("output")) { issue=next; break; }
                if (!next.isEmpty() && (!next.equals("working") || entry.getBoolean("working"))) issue=next;
            }
            entry.putString("issue",issue); entry.putBoolean("bonus",bonus);
            entry.putDouble("heat", Workplace.temperature(tile));
            entry.putDouble("minX", device.bounds().minX); entry.putDouble("minY", device.bounds().minY); entry.putDouble("minZ", device.bounds().minZ);
            entry.putDouble("maxX", device.bounds().maxX); entry.putDouble("maxY", device.bounds().maxY); entry.putDouble("maxZ", device.bounds().maxZ);
            devices.add(entry);
        }
        tag.put("devices", devices); PacketDistributor.sendToPlayer(player, new Status(tag));
        if (report) {
            player.displayClientMessage(CoreContent.text("stored", tag.getLong("energy")), false);
            player.displayClientMessage(CoreContent.text("hud", tag.getInt("load"), tag.getInt("heat")), false);
        }
    }
    private CorePackets() { }
}
