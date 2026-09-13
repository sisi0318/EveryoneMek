package dev.everyonemek.botania;

import java.util.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import vazkii.botania.api.mana.spark.ManaSpark;

/** Loaded mechanical sparks form color-matched components; only the master stores upgrades. */
public final class MechanicalSparkNetworks {
    public static final int MAX_RANGE = 76;
    public record Network(MechanicalSparkEntity master, int members, boolean conflict) {
        public String status() { return conflict ? "conflict" : master == null ? "no_master" : "connected"; }
        public int range() { return master != null && master.live() && !conflict ? 12 + master.upgrade(0) * 8 : 12; }
        public int multiplier() { return master != null && master.live() && !conflict ? 1 + master.upgrade(1) : 1; }
    }
    private static final Network NONE = new Network(null, 0, false);
    private static final Map<Level, State> LEVELS = new IdentityHashMap<>();
    private static final class State {
        final Set<MechanicalSparkEntity> nodes = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<MechanicalSparkEntity, Network> networks = Map.of();
        boolean dirty = true; long checked = Long.MIN_VALUE; int generation;
    }
    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> { if (event.getEntity() instanceof MechanicalSparkEntity spark && !event.getLevel().isClientSide) add(spark); });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> { if (event.getEntity() instanceof MechanicalSparkEntity spark) remove(spark); });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> LEVELS.remove(event.getLevel()));
    }
    public static void add(MechanicalSparkEntity spark) { if (!spark.level().isClientSide && LEVELS.computeIfAbsent(spark.level(), ignored -> new State()).nodes.add(spark)) invalidate(spark.level()); }
    public static void remove(MechanicalSparkEntity spark) { var state = LEVELS.get(spark.level()); if (state != null && state.nodes.remove(spark)) state.dirty = true; }
    public static void invalidate(Level level) { var state = LEVELS.get(level); if (state != null) state.dirty = true; }
    private static State state(Level level) {
        var state = LEVELS.computeIfAbsent(level, ignored -> new State());
        if (state.dirty || level.getGameTime() - state.checked >= 20) rebuild(state, level.getGameTime());
        return state;
    }
    public static int generation(Level level) { return state(level).generation; }
    public static Network network(MechanicalSparkEntity spark) { return spark.level().isClientSide ? NONE : state(spark.level()).networks.getOrDefault(spark, NONE); }
    public static int range(ManaSpark spark) { return spark instanceof MechanicalSparkEntity mechanical ? network(mechanical).range() : 12; }
    public static int rate(ManaSpark spark) { return spark instanceof MechanicalSparkEntity mechanical ? 1000 * network(mechanical).multiplier() : 1000; }
    public static int sharedRange(ManaSpark a, ManaSpark b) {
        if (!(a instanceof MechanicalSparkEntity first) || !(b instanceof MechanicalSparkEntity second)) return 12;
        var left = network(first); var right = network(second);
        return left.master() != null && left.master() == right.master() ? Math.min(left.range(), right.range()) : 12;
    }
    private static boolean near(MechanicalSparkEntity a, MechanicalSparkEntity b, int range) {
        return a.getNetwork() == b.getNetwork() && Math.abs(a.getX() - b.getX()) <= range && Math.abs(a.getY() - b.getY()) <= range && Math.abs(a.getZ() - b.getZ()) <= range;
    }
    private static void rebuild(State state, long time) {
        state.nodes.removeIf(spark -> !spark.isAlive());
        var nodes = state.nodes.stream().filter(MechanicalSparkEntity::live).toList();
        Map<Long, List<MechanicalSparkEntity>> buckets = new HashMap<>();
        for (var node : nodes) buckets.computeIfAbsent(ChunkPos.asLong(node.blockPosition()), ignored -> new ArrayList<>()).add(node);
        Map<MechanicalSparkEntity, Set<MechanicalSparkEntity>> groups = new IdentityHashMap<>();
        Set<MechanicalSparkEntity> conflicts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var master : nodes) if (master.isMaster()) {
            int range = 12 + master.upgrade(0) * 8, reach = (range + 15) / 16 + 1;
            Set<MechanicalSparkEntity> found = Collections.newSetFromMap(new IdentityHashMap<>()); found.add(master);
            var queue = new ArrayDeque<MechanicalSparkEntity>(); queue.add(master);
            while (!queue.isEmpty()) {
                var node = queue.remove(); int cx = node.blockPosition().getX() >> 4, cz = node.blockPosition().getZ() >> 4;
                for (int x = cx - reach; x <= cx + reach; x++) for (int z = cz - reach; z <= cz + reach; z++) {
                    for (var other : buckets.getOrDefault(ChunkPos.asLong(x, z), List.of())) if (!found.contains(other) && near(node, other, range)) {
                        found.add(other); queue.add(other);
                        if (other.isMaster()) { conflicts.add(master); conflicts.add(other); }
                    }
                }
            }
            groups.put(master, found);
        }
        // Different controller ranges can meet at a member before either search reaches the other master.
        Map<MechanicalSparkEntity, MechanicalSparkEntity> owners = new IdentityHashMap<>();
        for (var entry : groups.entrySet()) for (var node : entry.getValue()) {
            var previous = owners.putIfAbsent(node, entry.getKey());
            if (previous != null && previous != entry.getKey()) { conflicts.add(previous); conflicts.add(entry.getKey()); }
        }
        Map<MechanicalSparkEntity, Network> result = new HashMap<>();
        for (var entry : groups.entrySet()) {
            var network = new Network(entry.getKey(), entry.getValue().size(), conflicts.contains(entry.getKey()));
            for (var node : entry.getValue()) {
                var previous = result.get(node);
                result.put(node, previous == null ? network : new Network(null, Math.max(previous.members(), network.members()), true));
            }
        }
        if (state.dirty || !result.equals(state.networks)) state.generation++;
        state.networks = result; state.dirty = false; state.checked = time;
    }
    private MechanicalSparkNetworks() { }
}
