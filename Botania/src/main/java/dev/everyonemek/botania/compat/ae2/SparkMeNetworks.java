package dev.everyonemek.botania.compat.ae2;

import java.util.*;
import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.pathing.ControllerState;
import appeng.blockentity.networking.ControllerBlockEntity;
import dev.everyonemek.botania.*;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Only owns the virtual connections it creates. Vanilla wiring and resource inventories stay with AE. */
public final class SparkMeNetworks {
    private static final int MAX_ENDPOINTS = 128;
    private static final Map<ServerLevel, State> LEVELS = new IdentityHashMap<>();
    private record Limit(SparkMeEndpoint a, SparkMeEndpoint b) { int maximum() { return b == null ? a.maximumChannels() : Math.min(a.maximumChannels(), b.maximumChannels()); } }
    private static final Map<IGridConnection, Limit> LIMITS = new IdentityHashMap<>();
    private record Pair(SparkMeEndpoint parent, SparkMeEndpoint child) { }
    private record Wire(SparkMeEndpoint root, Pair pair, IGridConnection connection) { }
    private static final class State {
        final ServerLevel level;
        final Set<SparkMeEndpoint> endpoints = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Pair, Wire> wires = new HashMap<>();
        final Map<SparkMeEndpoint, Long> retry = new IdentityHashMap<>();
        boolean dirty = true; long rebuilt = Long.MIN_VALUE;
        State(ServerLevel level) { this.level = level; }
    }
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerTickEvent.Post event) -> {
            for (var state : List.copyOf(LEVELS.values())) update(state);
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                var state = LEVELS.get(level);
                if (state != null) { removeWires(state, wire -> true); for (var endpoint : List.copyOf(state.endpoints)) endpoint.detach(); }
                LEVELS.remove(level);
            }
        });
    }
    public static void add(SparkMeEndpoint endpoint) {
        if (endpoint.spark.level() instanceof ServerLevel level && LEVELS.computeIfAbsent(level, State::new).endpoints.add(endpoint)) LEVELS.get(level).dirty = true;
    }
    public static void invalidate(SparkMeEndpoint endpoint) {
        var state = LEVELS.get(endpoint.spark.level());
        if (state != null) { removeWires(state, wire -> wire.root == endpoint || wire.pair.parent == endpoint || wire.pair.child == endpoint); state.dirty = true; }
    }
    public static void remove(SparkMeEndpoint endpoint) {
        var state = LEVELS.get(endpoint.spark.level());
        if (state != null) { unlink(endpoint); state.endpoints.remove(endpoint); state.retry.remove(endpoint); state.dirty = true; }
    }
    static void unlink(SparkMeEndpoint endpoint) {
        var state = LEVELS.get(endpoint.spark.level());
        if (state != null) removeWires(state, wire -> wire.root == endpoint || wire.pair.parent == endpoint || wire.pair.child == endpoint);
    }
    private static void removeWires(State state, java.util.function.Predicate<Wire> predicate) {
        for (var wire : List.copyOf(state.wires.values())) if (predicate.test(wire)) {
            state.wires.remove(wire.pair); destroy(wire.connection); state.dirty = true;
        }
    }
    private static void destroy(IGridConnection connection) {
        LIMITS.remove(connection);
        if (connection.a().getConnections().contains(connection)) connection.destroy();
    }
    static void forget(IGridConnection connection) { LIMITS.remove(connection); }
    static IGridConnection connect(IGridNode a, IGridNode b, SparkMeEndpoint owner, SparkMeEndpoint other) {
        var connection = GridHelper.createConnection(a, b); LIMITS.put(connection, new Limit(owner, other)); return connection;
    }
    public static int connectionCapacity(IGridConnection connection, int original) { var limit = LIMITS.get(connection); return limit == null ? original : limit.maximum(); }
    private static boolean near(SparkMeEndpoint a, SparkMeEndpoint b, int range) {
        return Math.abs(a.spark.getX() - b.spark.getX()) <= range && Math.abs(a.spark.getY() - b.spark.getY()) <= range && Math.abs(a.spark.getZ() - b.spark.getZ()) <= range;
    }
    private static boolean group(SparkMeEndpoint endpoint, SparkMeEndpoint root) {
        var network = MechanicalSparkNetworks.network(endpoint.spark);
        return !network.conflict() && network.master() == root.spark;
    }
    private static boolean controllers(IGrid grid) { return grid.getMachines(ControllerBlockEntity.class).iterator().hasNext(); }
    private static boolean powered(SparkMeEndpoint root) { return root.ready() && root.node.getGrid().getEnergyService().isNetworkPowered(); }
    private static void update(State state) {
        long now = state.level.getGameTime();
        for (var endpoint : List.copyOf(state.endpoints)) {
            if (endpoint.removed || !endpoint.spark.isAlive()) { endpoint.remove(); continue; }
            var target = endpoint.locate();
            if (target != endpoint.attached || target != null && (endpoint.local == null || !target.getConnections().contains(endpoint.local))) {
                endpoint.attach(target); state.dirty = true;
            }
        }
        removeWires(state, wire -> !wire.root.ready() || wire.root.spark.channelCapacity() == 0 || !wire.pair.parent.ready() || !wire.pair.child.ready()
              || !group(wire.pair.parent, wire.root) || !group(wire.pair.child, wire.root)
              || !near(wire.pair.parent, wire.pair.child, MechanicalSparkNetworks.range(wire.root.spark)));
        for (var root : state.endpoints) if (root.spark.isMaster() && root.ready() && (!powered(root)
              || root.node.getGrid().getPathingService().getControllerState() == ControllerState.CONTROLLER_CONFLICT)) {
            boolean hadWires = state.wires.values().stream().anyMatch(wire -> wire.root == root);
            if (hadWires) { removeWires(state, wire -> wire.root == root); state.retry.put(root, now + 20); }
        }
        if (!state.dirty && state.rebuilt != Long.MIN_VALUE && now - state.rebuilt < 20) { report(state); return; }
        state.dirty = false; state.rebuilt = now;
        var ordered = state.endpoints.stream().sorted(Comparator.comparing((SparkMeEndpoint e) -> e.spark.getAttachPos()).thenComparing(e -> e.spark.getUUID())).toList();
        Map<SparkMeEndpoint, Double> power = new IdentityHashMap<>();
        for (var endpoint : ordered) {
            var network = MechanicalSparkNetworks.network(endpoint.spark);
            var master = network.master();
            endpoint.setCapacity(master == null || master.channelCapacity() == 0 ? 32 : master.channelCapacity());
            endpoint.state = !endpoint.ready() ? SparkMeLink.NO_ANCHOR : network.conflict() ? SparkMeLink.CONFLICT
                  : master == null ? SparkMeLink.NO_MASTER : master.channelCapacity() == 0 ? SparkMeLink.OFF : SparkMeLink.OUT_OF_RANGE;
        }
        for (var root : ordered) if (root.spark.isMaster() && root.ready() && root.spark.channelCapacity() > 0 && group(root, root)) {
            power.put(root, 4.0 + root.capacity / 32.0);
            if (!powered(root)) { root.state = SparkMeLink.NO_POWER; continue; }
            if (state.retry.getOrDefault(root, 0L) > now) { root.state = SparkMeLink.CONNECTING; continue; }
            if (root.node.getGrid().getPathingService().getControllerState() == ControllerState.CONTROLLER_CONFLICT) { root.state = SparkMeLink.INVALID_CONTROLLER; continue; }
            var members = ordered.stream().filter(e -> e.ready() && group(e, root)).toList();
            if (members.size() > MAX_ENDPOINTS) { removeWires(state, wire -> wire.root == root); root.state = SparkMeLink.TOO_MANY; continue; }
            int range = MechanicalSparkNetworks.range(root.spark);
            Set<SparkMeEndpoint> visited = Collections.newSetFromMap(new IdentityHashMap<>()); visited.add(root);
            var queue = new ArrayDeque<SparkMeEndpoint>(); queue.add(root);
            List<Pair> desired = new ArrayList<>();
            while (!queue.isEmpty()) {
                var parent = queue.remove();
                for (var child : members) if (!visited.contains(child) && near(parent, child, range)) {
                    // New connections cannot join two independently controlled ME networks.
                    if (root.node.getGrid() != child.node.getGrid() && controllers(root.node.getGrid()) && controllers(child.node.getGrid())) {
                        child.state = SparkMeLink.CONTROLLERS; continue;
                    }
                    visited.add(child); queue.add(child); desired.add(new Pair(parent, child));
                }
            }
            removeWires(state, wire -> wire.root == root && !desired.contains(wire.pair));
            root.state = SparkMeLink.ONLINE;
            Set<SparkMeEndpoint> reached = Collections.newSetFromMap(new IdentityHashMap<>()); reached.add(root);
            for (var pair : desired) {
                if (!reached.contains(pair.parent)) continue;
                var child = pair.child;
                var wire = state.wires.get(pair);
                if (wire == null && pair.parent.node.getGrid() != child.node.getGrid()) {
                    if (controllers(root.node.getGrid()) && controllers(child.node.getGrid())) { child.state = SparkMeLink.CONTROLLERS; continue; }
                    double cost = 8 + Math.ceil(pair.parent.spark.distanceTo(child.spark) / 16) + root.capacity / 32.0;
                    var energy = root.node.getGrid().getEnergyService();
                    if (energy.extractAEPower(cost, Actionable.SIMULATE, PowerMultiplier.CONFIG) + 0.0001 < cost) { child.state = SparkMeLink.NO_POWER; continue; }
                    wire = new Wire(root, pair, connect(pair.parent.node.getNode(), child.node.getNode(), pair.parent, child)); state.wires.put(pair, wire);
                }
                reached.add(child); child.state = wire == null ? SparkMeLink.WIRED : SparkMeLink.ONLINE;
                if (wire != null) power.put(child, 8 + Math.ceil(pair.parent.spark.distanceTo(child.spark) / 16) + root.capacity / 32.0);
            }
            if (members.stream().anyMatch(e -> e.state == SparkMeLink.CONTROLLERS)) root.state = SparkMeLink.CONTROLLERS;
        }
        for (var endpoint : ordered) endpoint.setPower(power.getOrDefault(endpoint, 0.0));
        report(state);
    }
    private static void report(State state) {
        Map<SparkMeEndpoint, Integer> linksByRoot = new IdentityHashMap<>();
        Map<MechanicalSparkEntity, Double> powerByRoot = new IdentityHashMap<>();
        for (var wire : state.wires.values()) linksByRoot.merge(wire.root, 1, Integer::sum);
        for (var endpoint : state.endpoints) {
            var network = MechanicalSparkNetworks.network(endpoint.spark);
            if (!network.conflict() && network.master() != null) powerByRoot.merge(network.master(), endpoint.power, Double::sum);
        }
        for (var endpoint : state.endpoints) {
            int status = endpoint.state;
            if (status == SparkMeLink.ONLINE && endpoint.ready() && !endpoint.node.hasGridBooted()) status = SparkMeLink.CONNECTING;
            int links = linksByRoot.getOrDefault(endpoint, 0);
            double power = endpoint.spark.isMaster() ? powerByRoot.getOrDefault(endpoint.spark, 0.0) : endpoint.power;
            endpoint.report(endpoint.spark.isMaster() ? endpoint.spark.channelCapacity() : groupCapacity(endpoint), links, status, power);
        }
    }
    private static int groupCapacity(SparkMeEndpoint endpoint) {
        var net = MechanicalSparkNetworks.network(endpoint.spark); return net.master() == null || net.conflict() ? 0 : net.master().channelCapacity();
    }
    private SparkMeNetworks() { }
}
