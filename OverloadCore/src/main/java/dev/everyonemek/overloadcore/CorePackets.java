package dev.everyonemek.overloadcore;

import java.util.*;
import java.util.function.Consumer;
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
    public static Consumer<CompoundTag> onClient = state -> { };
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
        var registrar = event.registrar("1");
        registrar.playToClient(Status.TYPE, Status.CODEC, (packet, context) -> context.enqueueWork(() -> { clientState = packet.data.copy(); onClient.accept(clientState); }));
        registrar.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.action != 0) return;
            var data = CoreBinding.data(player); long tick = player.level().getGameTime();
            if (tick - data.getLong("last_request") < 5) return;
            data.putLong("last_request", tick); CoreBinding.save(player, data); sendStatus(player, true);
        }));
    }
    public static void sendStatus(ServerPlayer player, boolean open) {
        var tag = new CompoundTag(); var data = CoreBinding.data(player);
        tag.putBoolean("bound", CoreBinding.bound(player)); tag.putBoolean("open", open);
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
    }
    private CorePackets() { }
}
