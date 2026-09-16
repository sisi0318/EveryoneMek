package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class DeviceScope {
    public static final String KEY = "overloadcore_machine";
    public record Device(BlockEntity anchor, AABB bounds, UUID owner) { }
    private record MultiSnapshot(long tick, UUID id, Device device) { }
    private static final Map<MultiblockData, MultiSnapshot> MULTIS = new WeakHashMap<>();
    public static boolean live(BlockEntity tile) {
        var level = tile.getLevel(); return level != null && !tile.isRemoved() && level.hasChunkAt(tile.getBlockPos()) && level.getBlockEntity(tile.getBlockPos()) == tile;
    }
    public static boolean supported(BlockEntity tile) {
        String namespace = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(tile.getType()).getNamespace();
        return tile instanceof mekanism.common.tile.base.CapabilityTileEntity && (namespace.equals("mekanism") || namespace.equals("mekanismgenerators"));
    }
    public static CompoundTag data(BlockEntity tile) { return tile.getPersistentData().getCompound(KEY); }
    public static void save(BlockEntity tile, CompoundTag data) { tile.getPersistentData().put(KEY, data); tile.setChanged(); }
    private static UUID owner(BlockEntity tile) {
        var owner = IBlockSecurityUtils.INSTANCE.ownerCapability(tile.getLevel(), tile.getBlockPos(), tile);
        if (owner != null && owner.getOwnerUUID() != null) return owner.getOwnerUUID();
        var data = data(tile); return data.hasUUID("placed_by") ? data.getUUID("placed_by") : null;
    }
    public static Device device(BlockEntity tile) {
        if (!supported(tile) || !live(tile)) return null;
        if (tile instanceof TileEntityMultiblock<?> multi && multi.getMultiblock().isFormed()) return device(multi.getMultiblock());
        return new Device(tile, new AABB(tile.getBlockPos()), owner(tile));
    }
    public static Device device(MultiblockData multi) {
        var level = multi.getLevel(); var min = multi.getMinPos(); var max = multi.getMaxPos();
        if (!multi.isFormed() || level == null) return null;
        var cached = MULTIS.get(multi);
        if (cached != null && cached.tick == level.getGameTime() && Objects.equals(cached.id, multi.inventoryID) && live(cached.device.anchor)) return cached.device;
        BlockEntity anchor = null; UUID structureOwner = null; boolean unknown = false, conflict = false;
        for (var pos : multi.locations.stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList()) {
            if (!level.hasChunkAt(pos)) return null;
            var part = level.getBlockEntity(pos); if (part == null) continue;
            if (anchor == null) anchor = part;
            UUID owner = owner(part);
            if (owner == null) unknown = true;
            else if (structureOwner == null) structureOwner = owner;
            else if (!structureOwner.equals(owner)) conflict = true;
        }
        if (anchor == null || !live(anchor)) return null;
        var explicit = data(anchor);
        if (explicit.hasUUID("structure_owner") && explicit.hasUUID("structure_id") && explicit.getUUID("structure_id").equals(multi.inventoryID))
            structureOwner = explicit.getUUID("structure_owner");
        else if (unknown || conflict) structureOwner = null;
        var device = new Device(anchor, new AABB(min.getX(), min.getY(), min.getZ(), max.getX()+1, max.getY()+1, max.getZ()+1), structureOwner);
        MULTIS.put(multi, new MultiSnapshot(level.getGameTime(), multi.inventoryID, device)); return device;
    }
    public static boolean allowed(Device device, ServerPlayer player) {
        return CoreBinding.active(player) && permitted(device, player);
    }
    /** Consent and distance without requiring a cursed core or a currently positive health value. */
    public static boolean permitted(Device device, ServerPlayer player) {
        if (device == null || device.owner == null || player.isRemoved() || player.isSpectator()
              || player.level() != device.anchor.getLevel() || !live(device.anchor)) return false;
        if (!device.owner.equals(player.getUUID())) {
            var shares = data(device.anchor).getList("shares", Tag.TAG_INT_ARRAY);
            if (shares.stream().limit(64).noneMatch(t -> t instanceof IntArrayTag a && a.getAsIntArray().length == 4 && NbtUtils.loadUUID(t).equals(player.getUUID()))) return false;
        }
        var p = player.position(); double x = Math.clamp(p.x, device.bounds.minX, device.bounds.maxX), y = Math.clamp(p.y, device.bounds.minY, device.bounds.maxY), z = Math.clamp(p.z, device.bounds.minZ, device.bounds.maxZ);
        double range = CoreConfig.RANGE.get(); return player.distanceToSqr(x, y, z) <= range * range;
    }
    public static Device powerDevice(BlockEntity tile) {
        if (!(tile instanceof TileEntityMekanism) || !live(tile)) return null;
        if (tile instanceof TileEntityMultiblock<?> multi && multi.getMultiblock().isFormed()) return device(multi.getMultiblock());
        return new Device(tile, new AABB(tile.getBlockPos()), owner(tile));
    }
    public static ServerPlayer bearer(BlockEntity tile) { return bearer(device(tile)); }
    public static ServerPlayer bearer(MultiblockData multi) { return bearer(device(multi)); }
    public static ServerPlayer bearer(Device device) {
        if (device == null || device.anchor.getLevel().isClientSide || device.owner == null) return null;
        return ((net.minecraft.server.level.ServerLevel)device.anchor.getLevel()).players().stream().filter(p -> allowed(device, p))
              .min(Comparator.<ServerPlayer>comparingDouble(p -> p.distanceToSqr(device.bounds.getCenter())).thenComparing(ServerPlayer::getUUID)).orElse(null);
    }
    public static void placed(BlockEntity tile, UUID player) {
        if (tile == null) return;
        var data = data(tile); data.putUUID("placed_by", player); save(tile, data); MULTIS.clear();
    }
    public static void claim(BlockEntity tile, UUID owner) {
        if (tile instanceof TileEntityMultiblock<?> multi && multi.getMultiblock().isFormed()) {
            var device = device(multi.getMultiblock()); if (device == null || multi.getMultiblock().inventoryID == null) return;
            var data = data(device.anchor); data.putUUID("structure_owner", owner); data.putUUID("structure_id", multi.getMultiblock().inventoryID);
            save(device.anchor, data); MULTIS.clear();
        } else placed(tile, owner);
    }
    public static void unload(Level level) { MULTIS.entrySet().removeIf(e -> e.getKey().getLevel() == level); }
    public static boolean cargoOwned(Level level, long position, UUID owner) {
        if (position == Long.MAX_VALUE || owner == null) return false;
        var pos = BlockPos.of(position); if (!level.hasChunkAt(pos)) return false;
        var tile = level.getBlockEntity(pos); return tile != null && owner.equals(owner(tile));
    }
    public static boolean share(ServerPlayer owner, BlockEntity tile, UUID target, boolean allow) {
        var device = device(tile); if (device == null) device = powerDevice(tile);
        if (device == null || !owner.getUUID().equals(device.owner)) return false;
        var data = data(device.anchor); var shares = data.getList("shares", Tag.TAG_INT_ARRAY);
        shares.removeIf(t -> !(t instanceof IntArrayTag a) || a.getAsIntArray().length != 4 || NbtUtils.loadUUID(t).equals(target)); if (allow && shares.size() < 64) shares.add(NbtUtils.createUUID(target));
        data.put("shares", shares); save(device.anchor, data); return true;
    }
    private DeviceScope() { }
}
