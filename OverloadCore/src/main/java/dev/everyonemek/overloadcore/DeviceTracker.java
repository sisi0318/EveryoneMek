package dev.everyonemek.overloadcore;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Tick-fed index, never a world-volume scan or a chunk loader. */
public final class DeviceTracker {
    private record Seen(BlockEntity tile, long tick) { }
    private static final Map<Level, Map<Long, Map<BlockPos, Seen>>> SEEN = new WeakHashMap<>();
    private static final Map<BlockEntity, Long> WORKED = new WeakHashMap<>();
    private static final ThreadLocal<BlockEntity> TICK = new ThreadLocal<>();
    public static BlockEntity ticking() { return TICK.get(); }
    public static BlockEntity begin(BlockEntity tile) {
        var previous = TICK.get(); TICK.set(tile);
        if (DeviceScope.supported(tile)) SEEN.computeIfAbsent(tile.getLevel(), unused -> new HashMap<>())
              .computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(tile.getBlockPos()),unused -> new HashMap<>())
              .put(tile.getBlockPos(), new Seen(tile, tile.getLevel().getGameTime()));
        return previous;
    }
    public static void end(BlockEntity previous) { if (previous == null) TICK.remove(); else TICK.set(previous); }
    public static void worked(BlockEntity tile) { if (DeviceScope.live(tile)) WORKED.put(tile, tile.getLevel().getGameTime()); }
    public static boolean working(BlockEntity tile) {
        return DeviceScope.live(tile) && (tile.getLevel().getGameTime() - WORKED.getOrDefault(tile, Long.MIN_VALUE / 2) <= 2
              || tile instanceof mekanism.common.tile.base.TileEntityMekanism machine && machine.isActivatable() && machine.getActive());
    }
    public static List<BlockEntity> nearby(Level level, net.minecraft.world.phys.Vec3 point, double range) {
        var chunks = SEEN.get(level); if (chunks == null) return List.of();
        var result = new ArrayList<BlockEntity>(); double radius=range+24;
        int minX=net.minecraft.util.Mth.floor(point.x-radius)>>4,maxX=net.minecraft.util.Mth.floor(point.x+radius)>>4;
        int minZ=net.minecraft.util.Mth.floor(point.z-radius)>>4,maxZ=net.minecraft.util.Mth.floor(point.z+radius)>>4;
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
            var map=chunks.get(net.minecraft.world.level.ChunkPos.asLong(x,z));if(map==null)continue;
            if(level.getGameTime()%100==0)map.values().removeIf(s -> level.getGameTime()-s.tick>40||!DeviceScope.live(s.tile));
            for(var seen:map.values())if(level.getGameTime()-seen.tick<=2&&DeviceScope.live(seen.tile)&&seen.tile.getBlockPos().getCenter().distanceToSqr(point)<=radius*radius)result.add(seen.tile);
        }
        result.sort(Comparator.comparingDouble(t -> t.getBlockPos().getCenter().distanceToSqr(point)));return result;
    }
    public static void unloadChunk(Level level,long chunk) { var chunks=SEEN.get(level);if(chunks!=null)chunks.remove(chunk); }
    public static void unload(Level level) { SEEN.remove(level); WORKED.keySet().removeIf(t -> t.getLevel() == level); }
    private DeviceTracker() { }
}
