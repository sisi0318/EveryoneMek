package dev.everyonemek.overloadcore;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Source-owner controls. Extreme mode never bypasses a prohibition or ownership check. */
public final class WardSources {
    private static DeviceScope.Device owned(ServerPlayer owner, BlockEntity tile) {
        var device = tile == null ? null : DeviceScope.powerDevice(tile);
        return device != null && owner.getUUID().equals(device.owner()) ? device : null;
    }
    public static boolean enabled(ServerPlayer owner, BlockEntity tile, boolean enabled) {
        var device = owned(owner, tile); if (device == null) return false;
        var tag = DeviceScope.data(device.anchor()); tag.putBoolean("ward_blocked", !enabled); DeviceScope.save(device.anchor(), tag); return true;
    }
    public static boolean reserve(ServerPlayer owner, BlockEntity tile, int percent) {
        var device = owned(owner, tile); if (device == null || percent < 0 || percent > 100) return false;
        var tag = DeviceScope.data(device.anchor()); tag.putInt("ward_reserve", percent); DeviceScope.save(device.anchor(), tag); return true;
    }
    public static boolean share(ServerPlayer owner, BlockEntity tile, UUID target, boolean allow) {
        var device = owned(owner, tile); if (device == null) return false;
        var tag = DeviceScope.data(device.anchor());
        var shares = tag.getList(tag.contains("ward_shares") ? "ward_shares" : "shares", Tag.TAG_INT_ARRAY).copy();
        shares.removeIf(t -> !(t instanceof IntArrayTag a) || a.getAsIntArray().length != 4 || NbtUtils.loadUUID(t).equals(target));
        if (allow && shares.size() >= 64) return false;
        if (allow) shares.add(NbtUtils.createUUID(target));
        tag.put("ward_shares", shares); DeviceScope.save(device.anchor(), tag); return true;
    }
    public static long reserve(DeviceScope.Device device, long capacity) {
        var tag = DeviceScope.data(device.anchor());
        int percent = tag.contains("ward_reserve") ? Math.clamp(tag.getInt("ward_reserve"),0,100) : CoreConfig.WARD_RESERVE_PERCENT.get();
        // Round up without multiplying a potentially near-Long.MAX_VALUE capacity.
        return capacity / 100 * percent + (capacity % 100 * percent + 99) / 100;
    }
    private WardSources() { }
}
