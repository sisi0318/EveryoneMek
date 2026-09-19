package dev.everyonemek.factory;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Appearance is a transient view of a validated structure, never a replacement block or inventory. */
public final class FactoryAppearance {
    public static Consumer<Snapshot> clientReceiver = packet -> {};

    public record Snapshot(ResourceLocation dimension, BlockPos controller, BlockPos min, BlockPos max, boolean formed)
          implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MekFactory.ID, "appearance"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of(
              (b, p) -> { b.writeResourceLocation(p.dimension); b.writeBlockPos(p.controller); b.writeBlockPos(p.min); b.writeBlockPos(p.max); b.writeBoolean(p.formed); },
              b -> new Snapshot(b.readResourceLocation(), b.readBlockPos(), b.readBlockPos(), b.readBlockPos(), b.readBoolean()));

        public Snapshot {
            controller = controller.immutable(); min = min.immutable(); max = max.immutable();
            if (max.getX() < min.getX() || max.getY() < min.getY() || max.getZ() < min.getZ()
                  || (long)max.getX() - min.getX() > 10 || (long)max.getY() - min.getY() > 10 || (long)max.getZ() - min.getZ() > 10)
                throw new IllegalArgumentException("Invalid factory appearance bounds");
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        public boolean contains(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                  && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
        }
        public Snapshot cleared() { return new Snapshot(dimension, controller, min, max, false); }
    }

    public static Snapshot snapshot(Controller c) {
        var a = c.structure.at(0, 0, 0);
        var b = c.structure.at(c.sizeX - 1, c.sizeY - 1, c.sizeZ - 1);
        return new Snapshot(c.getLevel().dimension().location(), c.getBlockPos(),
              new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
              new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())), c.structure.formed);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("4").playToClient(Snapshot.TYPE, Snapshot.CODEC,
              (packet, context) -> context.enqueueWork(() -> clientReceiver.accept(packet)));
    }

    public static void sync(Controller c) {
        if (!(c.getLevel() instanceof ServerLevel level)) return;
        var next = snapshot(c);
        if (!next.equals(c.publishedAppearance)) {
            if (c.publishedAppearance != null && (!c.publishedAppearance.min().equals(next.min()) || !c.publishedAppearance.max().equals(next.max()))) broadcast(level, c.publishedAppearance.cleared());
            c.publishedAppearance = next;
            broadcast(level, next);
        }
    }

    private static void broadcast(ServerLevel level, Snapshot packet) {
        // A saved larger factory may cross a chunk boundary; every visible section receives its state.
        for (int x = packet.min.getX() >> 4; x <= packet.max.getX() >> 4; x++)
            for (int z = packet.min.getZ() >> 4; z <= packet.max.getZ() >> 4; z++)
                PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(x, z), packet);
    }

    public static void clear(Controller c) {
        if (c.getLevel() instanceof ServerLevel level && (c.publishedAppearance != null || c.structure.formed)) {
            broadcast(level, (c.publishedAppearance == null ? snapshot(c) : c.publishedAppearance).cleared());
            c.publishedAppearance = null;
        }
    }

    public static void chunkSent(ChunkWatchEvent.Sent event) {
        for (var controller : FactoryStructure.controllersInChunk(event.getLevel(), event.getPos())) {
            if (!controller.isRemoved())
                PacketDistributor.sendToPlayer(event.getPlayer(), snapshot(controller));
        }
    }

    private FactoryAppearance() {}
}
