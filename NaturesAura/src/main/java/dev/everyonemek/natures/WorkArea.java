package dev.everyonemek.natures;

import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** World-axis offsets; range modules extend the maximum horizontal radius without loading chunks. */
public final class WorkArea {
    private final AuraMachine machine;
    private int x, y = 1, z, radius = 2, cap = 16;
    public WorkArea(AuraMachine machine) { this.machine = machine; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public int radius() { return Math.min(radius, maxRadius()); }
    public int maxRadius() { return 4 + machine.rangeModules() * 2; }
    public int cap() { return cap; }
    public BlockPos center() { return machine.getBlockPos().offset(x, y, z); }
    public AABB bounds() { return new AABB(center()).inflate(radius(), 4, radius()); }
    public int count(EntityType<?> target) {
        return machine.getLevel().getEntities((Entity) null, bounds(), e -> e.isAlive() && !(e instanceof Player)
              && (e instanceof LivingEntity || e.getType() == target)).size();
    }
    public boolean set(int setting, int value) {
        switch (setting) {
            case SetMachineSettingPayload.AREA_X -> x = Math.clamp(value, -16, 16);
            case SetMachineSettingPayload.AREA_Y -> y = Math.clamp(value, -8, 8);
            case SetMachineSettingPayload.AREA_Z -> z = Math.clamp(value, -16, 16);
            case SetMachineSettingPayload.AREA_RADIUS -> radius = Math.clamp(value, 0, maxRadius());
            case SetMachineSettingPayload.AREA_CAP -> cap = Math.clamp(value, 1, 128);
            default -> { return false; }
        }
        machine.markForSave();
        return true;
    }
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("x", x); tag.putInt("y", y); tag.putInt("z", z);
        tag.putInt("radius", radius); tag.putInt("cap", cap);
        return tag;
    }
    public void load(CompoundTag tag) {
        x = Math.clamp(tag.getInt("x"), -16, 16);
        y = Math.clamp(tag.contains("y") ? tag.getInt("y") : 1, -8, 8);
        z = Math.clamp(tag.getInt("z"), -16, 16);
        radius = Math.clamp(tag.contains("radius") ? tag.getInt("radius") : 2, 0, 12);
        cap = Math.clamp(tag.contains("cap") ? tag.getInt("cap") : 16, 1, 128);
    }
    public void track(MekanismContainer c) {
        c.track(SyncableInt.create(() -> x, v -> x = v));
        c.track(SyncableInt.create(() -> y, v -> y = v));
        c.track(SyncableInt.create(() -> z, v -> z = v));
        c.track(SyncableInt.create(() -> radius, v -> radius = v));
        c.track(SyncableInt.create(() -> cap, v -> cap = v));
    }
}
