package dev.everyonemek.botania.compat.ae2;

import appeng.api.networking.*;
import appeng.api.networking.pathing.ChannelMode;
import dev.everyonemek.botania.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/** One AE node per loaded ME-mounted spark; it holds no items, fluids or mana. */
public final class SparkMeEndpoint implements SparkMeLink {
    final MechanicalSparkEntity spark;
    IManagedGridNode node;
    IGridNode attached;
    IGridConnection local;
    int capacity = 32, state = NO_ANCHOR;
    double power;
    boolean removed;
    private long lastTick = Long.MIN_VALUE;
    private CompoundTag saved = new CompoundTag();
    private Stats stats = new Stats(0, 0, 0, NO_ANCHOR, 0);
    public SparkMeEndpoint(MechanicalSparkEntity spark) { this.spark = spark; }
    @Override public void tick() { if (!removed) { lastTick = spark.level().getGameTime(); SparkMeNetworks.add(this); } }
    @Override public void invalidate() { if (!removed) SparkMeNetworks.invalidate(this); }
    @Override public void remove() { if (!removed) { removed = true; SparkMeNetworks.remove(this); detach(); } }
    @Override public void load(CompoundTag data) { saved = data.copy(); }
    @Override public CompoundTag save() { var tag = saved.copy(); if (node != null) node.saveToNBT(tag); return tag; }
    @Override public Stats stats() { return stats; }
    IGridNode locate() {
        if (removed || lastTick != spark.level().getGameTime() || !spark.live()) return null;
        var level = spark.level(); var pos = spark.getAttachPos();
        // Honor the actual top-side connection, including device orientation and part changes.
        return GridHelper.getExposedNode(level, pos, Direction.UP);
    }
    void attach(IGridNode target) {
        detach(); attached = target;
        if (target == null) return;
        node = GridHelper.createManagedNode(this, new IGridNodeListener<SparkMeEndpoint>() {
            @Override public void onSaveChanges(SparkMeEndpoint owner, IGridNode gridNode) { }
        }).setInWorldNode(false).setTagName("node").setFlags(GridFlags.DENSE_CAPACITY)
              .setVisualRepresentation(spark.isMaster() ? MechanicalSparks.MASTER.get() : MechanicalSparks.SPARK.get()).setIdlePowerUsage(0);
        node.loadFromNBT(saved); node.setOwningPlayerId(target.getOwningPlayerId()); node.create(spark.level(), null);
        local = SparkMeNetworks.connect(node.getNode(), target, this, null);
    }
    void detach() {
        SparkMeNetworks.unlink(this);
        if (node != null) {
            saved = save();
            if (local != null) SparkMeNetworks.forget(local);
            node.destroy(); node = null;
        }
        local = null; attached = null; power = 0;
    }
    boolean ready() { return !removed && node != null && node.isReady() && attached != null && spark.live(); }
    void setCapacity(int value) {
        if (capacity == value) return;
        capacity = value;
        if (ready()) node.getGrid().getPathingService().repath();
    }
    void setPower(double value) { if (node != null && Double.compare(power, value) != 0) { power = value; node.setIdlePowerUsage(value); } }
    public int maximumChannels() {
        if (!ready()) return capacity;
        var mode = node.getGrid().getPathingService().getChannelMode();
        return mode == ChannelMode.INFINITE ? Integer.MAX_VALUE : capacity * mode.getCableCapacityFactor();
    }
    void report(int displayCapacity, int links, int status, double totalPower) {
        int used = ready() ? node.getNode().getUsedChannels() : 0;
        int maximum = displayCapacity == 0 ? 0 : maximumChannels();
        stats = new Stats(maximum == Integer.MAX_VALUE ? -1 : Math.min(32767, maximum), Math.min(32767, used), links, status, (int) Math.ceil(totalPower));
    }
}
