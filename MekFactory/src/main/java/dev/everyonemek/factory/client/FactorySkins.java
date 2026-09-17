package dev.everyonemek.factory.client;

import dev.everyonemek.factory.FactoryAppearance.Snapshot;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Render-worker reads use immutable snapshots; updates and rebuild requests run on the client thread. */
public final class FactorySkins {
    private static final Map<BlockPos, Snapshot> CONTROLLERS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Snapshot> POSITIONS = new ConcurrentHashMap<>();

    public static boolean formed(BlockPos pos) { return POSITIONS.containsKey(pos); }

    public static void receive(Snapshot packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) return;
        var old = CONTROLLERS.remove(packet.controller());
        if (old != null) for (var pos : BlockPos.betweenClosed(old.min(), old.max())) POSITIONS.remove(pos, old);
        if (packet.formed()) {
            CONTROLLERS.put(packet.controller(), packet);
            for (var pos : BlockPos.betweenClosed(packet.min(), packet.max())) if (level.hasChunkAt(pos)) POSITIONS.put(pos.immutable(), packet);
        }
        if (old != null) refresh(old);
        refresh(packet);
    }

    private static void refresh(Snapshot packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var pos : BlockPos.betweenClosed(packet.min(), packet.max())) if (level.hasChunkAt(pos)) {
            var be = level.getBlockEntity(pos);
            if (be != null) level.getModelDataManager().requestRefresh(be);
            var state = level.getBlockState(pos);
            level.setBlocksDirty(pos, state, state);
        }
    }

    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) { CONTROLLERS.clear(); POSITIONS.clear(); }
    }

    public static void chunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        var chunk = event.getChunk().getPos();
        POSITIONS.keySet().removeIf(p -> (p.getX() >> 4) == chunk.x && (p.getZ() >> 4) == chunk.z);
        CONTROLLERS.entrySet().removeIf(e -> !POSITIONS.containsValue(e.getValue()));
    }

    private FactorySkins() {}
}
