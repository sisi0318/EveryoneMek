package dev.everyonemek.botania;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import vazkii.botania.common.block.mana.ManaPoolBlock;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

public final class ManaNetworks extends SavedData {
    public static final class Network {
        public final UUID id, owner;
        public String name;
        public BlockPos core;
        public final Set<BlockPos> nodes = new HashSet<>();
        public final Map<UUID, String> members = new HashMap<>();
        public int credit, cursor, sourceCursor, lastDelivered, lastFee;
        private long lastBatch = Long.MIN_VALUE;
        public Network(UUID id, UUID owner, String name, BlockPos core) { this.id = id; this.owner = owner; this.name = name; this.core = core; }
        public boolean permits(UUID player) { return player != null && (owner.equals(player) || members.containsKey(player)); }
    }
    private final Map<UUID, Network> networks = new HashMap<>();
    private final Map<BlockPos, BlockPos> targetOwners = new HashMap<>();
    private long targetOwnerBatch = Long.MIN_VALUE;
    public void invalidate() { targetOwnerBatch = Long.MIN_VALUE; setDirty(); }
    public static ManaNetworks get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(new Factory<>(ManaNetworks::new, ManaNetworks::load, null), "botanical_mana_networks"); }
    public Network find(UUID id) { return networks.get(id); }
    public List<Network> accessible(UUID player) {
        return networks.values().stream().filter(n -> n.permits(player)).sorted(Comparator.comparing((Network n) -> n.name).thenComparing(n -> n.id)).toList();
    }
    public boolean activate(NetworkPlant core) {
        UUID owner = Flowers.owner(core);
        if (owner == null) { core.status = "unowned"; return false; }
        if (core.network == null) { core.network = UUID.randomUUID(); core.setChanged(); }
        Network network = networks.get(core.network);
        if (network == null) {
            network = new Network(core.network, owner, "Mana " + (networks.size() + 1), core.getBlockPos());
            networks.put(network.id, network); invalidate();
        }
        if (!network.owner.equals(owner)) { core.status = "denied"; return false; }
        if (network.core == null) { network.core = core.getBlockPos(); invalidate(); }
        if (!network.core.equals(core.getBlockPos())) { core.status = "duplicate_core"; return false; }
        return true;
    }
    public boolean join(NetworkPlant node, UUID id, UUID player) {
        Network target = networks.get(id);
        if (node.core() || !Objects.equals(Flowers.owner(node), player) || target == null || !target.permits(player)) return false;
        if (!target.nodes.contains(node.getBlockPos()) && target.nodes.size() >= Balance.NODE_LIMIT) return false;
        Network previous = networks.get(node.network);
        if (previous != null) previous.nodes.remove(node.getBlockPos());
        node.network = id; target.nodes.add(node.getBlockPos()); node.status = "waiting"; node.setChanged(); invalidate(); return true;
    }
    public void removed(NetworkPlant plant) {
        Network network = networks.get(plant.network);
        if (network == null) return;
        if (plant.core() && Objects.equals(network.core, plant.getBlockPos())) network.core = null;
        else network.nodes.remove(plant.getBlockPos());
        invalidate();
    }
    private static NetworkPlant at(ServerLevel level, BlockPos pos) {
        return pos != null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof NetworkPlant plant && Flowers.live(plant) ? plant : null;
    }
    private static boolean within(BlockPos a, BlockPos b) { return a.distSqr(b) <= (long) Balance.RANGE * Balance.RANGE; }
    private static ManaPoolBlockEntity pool(ServerLevel level, NetworkPlant node) {
        BlockPos position = node.targetPos();
        if (!level.hasChunkAt(position) || !(level.getBlockEntity(position) instanceof ManaPoolBlockEntity pool)
              || pool.getClass() != ManaPoolBlockEntity.class || !(pool.getBlockState().getBlock() instanceof ManaPoolBlock block) || block.isCreative()) return null;
        return pool;
    }
    private record Endpoint(NetworkPlant node, ManaPoolBlockEntity pool, List<BlockPos> path) { }
    private static List<BlockPos> path(NetworkPlant node, BlockPos core, List<NetworkPlant> relays) {
        if (within(core, node.getBlockPos())) return List.of(core, node.getBlockPos());
        return relays.stream().filter(relay -> within(core, relay.getBlockPos()) && within(relay.getBlockPos(), node.getBlockPos()))
              .min(Comparator.comparingDouble((NetworkPlant relay) -> relay.getBlockPos().distSqr(node.getBlockPos())).thenComparing(NetworkPlant::getBlockPos))
              .map(relay -> List.of(core, relay.getBlockPos(), node.getBlockPos())).orElse(List.of());
    }
    private static int hops(List<BlockPos> left, List<BlockPos> right) {
        int shared = 0;
        while (shared < Math.min(left.size(), right.size()) && left.get(shared).equals(right.get(shared))) shared++;
        return left.size() + right.size() - 2 * shared;
    }
    private static Set<BlockPos> relays(Endpoint source, Endpoint target) {
        Set<BlockPos> result = new HashSet<>();
        if (source.path.size() == 3) result.add(source.path.get(1));
        if (target.path.size() == 3) result.add(target.path.get(1));
        return result;
    }
    private void claimTargets(ServerLevel level, long batch) {
        if (targetOwnerBatch == batch) return;
        targetOwnerBatch = batch; targetOwners.clear();
        for (Network network : networks.values().stream().sorted(Comparator.comparing((Network n) -> n.id)).toList()) {
            NetworkPlant core = at(level, network.core);
            if (core == null || !core.core() || !network.id.equals(core.network) || !Flowers.enabled(core)
                  || !network.owner.equals(Flowers.owner(core))) continue;
            List<NetworkPlant> nodes = network.nodes.stream().sorted().map(pos -> at(level, pos))
                  .filter(Objects::nonNull).filter(node -> !node.core() && network.id.equals(node.network)
                        && network.permits(Flowers.owner(node)) && Flowers.enabled(node)).toList();
            List<NetworkPlant> relays = nodes.stream().filter(node -> node.mode == NetworkPlant.RELAY && within(network.core, node.getBlockPos())).toList();
            for (NetworkPlant node : nodes) {
                if (node.mode == NetworkPlant.RELAY || path(node, network.core, relays).isEmpty()) continue;
                ManaPoolBlockEntity pool = pool(level, node);
                if (pool != null) targetOwners.putIfAbsent(pool.getBlockPos(), node.getBlockPos());
            }
        }
    }
    public void tick(NetworkPlant core) {
        if (!(core.getLevel() instanceof ServerLevel level) || !activate(core)) return;
        Network network = networks.get(core.network);
        if (!Flowers.enabled(core)) { core.status = "paused"; return; }
        long batch = level.getGameTime() / Balance.BATCH_TICKS;
        if (network.lastBatch == batch) return;
        network.lastBatch = batch; network.lastDelivered = 0; network.lastFee = 0;
        claimTargets(level, batch);
        List<NetworkPlant> nodes = new ArrayList<>();
        for (BlockPos pos : network.nodes.stream().sorted().toList()) {
            NetworkPlant node = at(level, pos);
            if (node == null || node.core() || !network.id.equals(node.network)) continue;
            node.moved = 0; node.hops = 0;
            if (!network.permits(Flowers.owner(node))) { node.status = "denied"; continue; }
            if (!Flowers.enabled(node)) { node.status = "paused"; continue; }
            nodes.add(node);
        }
        List<NetworkPlant> relays = nodes.stream().filter(n -> n.mode == NetworkPlant.RELAY && within(core.getBlockPos(), n.getBlockPos())).toList();
        List<Endpoint> sources = new ArrayList<>(), receivers = new ArrayList<>();
        for (NetworkPlant node : nodes) {
            List<BlockPos> route = path(node, core.getBlockPos(), relays);
            if (node.mode == NetworkPlant.RELAY) { node.status = within(core.getBlockPos(), node.getBlockPos()) ? "relay" : "out_of_range"; continue; }
            if (route.isEmpty()) { node.status = "out_of_range"; continue; }
            ManaPoolBlockEntity pool = pool(level, node);
            if (pool == null) { node.status = "missing_pool"; continue; }
            if (!node.getBlockPos().equals(targetOwners.get(pool.getBlockPos()))) { node.status = "duplicate_target"; continue; }
            node.hops = route.size() - 1;
            Endpoint endpoint = new Endpoint(node, pool, route);
            if (node.mode == NetworkPlant.SUPPLY) { sources.add(endpoint); node.status = pool.getCurrentMana() > node.reserve ? "waiting" : "reserve"; }
            else { receivers.add(endpoint); node.status = pool.getCurrentMana() >= Math.min(node.target, pool.getMaxMana()) ? "full" : "waiting"; }
        }
        // Interleave weighted tickets, so the high-priority consumer cannot take the entire batch first.
        List<Endpoint> schedule = new ArrayList<>();
        for (int pass = 0; pass < 4; pass++) for (Endpoint receiver : receivers)
            if (pass < (1 << receiver.node.priority)) schedule.add(receiver);
        Map<BlockPos, Integer> endpointSpent = new HashMap<>(), relaySpent = new HashMap<>();
        int budget = Balance.NETWORK_BUDGET, misses = 0;
        while (budget > 0 && !sources.isEmpty() && !schedule.isEmpty() && misses < schedule.size()) {
            Endpoint receiver = schedule.get(Math.floorMod(network.cursor++, schedule.size()));
            boolean transferred = false;
            for (int i = 0; i < sources.size(); i++) {
                Endpoint source = sources.get(Math.floorMod(network.sourceCursor++, sources.size()));
                int count = transfer(level, network, source, receiver, Math.min(8, budget), endpointSpent, relaySpent);
                if (count > 0) { budget -= count; transferred = true; break; }
            }
            misses = transferred ? 0 : misses + 1;
        }
        core.moved = network.lastDelivered;
        core.status = network.lastDelivered > 0 ? "working" : sources.isEmpty() ? "no_supply" : "waiting";
        setDirty();
    }
    private int transfer(ServerLevel level, Network network, Endpoint source, Endpoint target, int max,
          Map<BlockPos, Integer> endpointSpent, Map<BlockPos, Integer> relaySpent) {
        if (source.pool == target.pool || source.node.mode != NetworkPlant.SUPPLY || target.node.mode != NetworkPlant.RECEIVE
              || !valid(level, network, source) || !valid(level, network, target)) return 0;
        Set<BlockPos> routeRelays = relays(source, target);
        int available = source.pool.getCurrentMana() - source.node.reserve;
        int space = Math.min(target.node.target, target.pool.getMaxMana()) - target.pool.getCurrentMana();
        int count = Math.min(max, Math.min(space, Balance.ENDPOINT_BUDGET - endpointSpent.getOrDefault(target.node.getBlockPos(), 0)));
        count = Math.min(count, Balance.ENDPOINT_BUDGET - endpointSpent.getOrDefault(source.node.getBlockPos(), 0));
        for (BlockPos relay : routeRelays) count = Math.min(count, Balance.RELAY_BUDGET - relaySpent.getOrDefault(relay, 0));
        int hops = hops(source.path, target.path);
        while (count > 0 && count + WirelessFee.forDelivery(count, hops, network.credit).fee() > available) count--;
        if (count <= 0) return 0;
        int beforeSource = source.pool.getCurrentMana(), beforeTarget = target.pool.getCurrentMana();
        WirelessFee planned = WirelessFee.forDelivery(count, hops, network.credit);
        source.pool.receiveMana(-count - planned.fee());
        if (source.pool.getCurrentMana() != beforeSource - count - planned.fee()) {
            source.pool.receiveMana(beforeSource - source.pool.getCurrentMana()); return 0;
        }
        target.pool.receiveMana(count);
        int accepted = Math.clamp(target.pool.getCurrentMana() - beforeTarget, 0, count);
        WirelessFee actual = WirelessFee.forDelivery(accepted, hops, network.credit);
        source.pool.receiveMana(count + planned.fee() - accepted - actual.fee());
        if (accepted == 0) return 0;
        network.credit = actual.remainingCredit(); network.lastFee += actual.fee(); network.lastDelivered += accepted;
        for (Endpoint endpoint : List.of(source, target)) {
            endpointSpent.merge(endpoint.node.getBlockPos(), accepted, Integer::sum);
            endpoint.node.moved += accepted; endpoint.node.status = "working";
        }
        for (BlockPos pos : routeRelays) {
            relaySpent.merge(pos, accepted, Integer::sum);
            NetworkPlant relay = at(level, pos);
            if (relay != null) { relay.moved += accepted; relay.status = "working"; }
        }
        source.pool.setChanged(); target.pool.setChanged(); return accepted;
    }
    private static boolean valid(ServerLevel level, Network network, Endpoint endpoint) {
        if (!Flowers.live(endpoint.node) || !Flowers.enabled(endpoint.node) || !network.id.equals(endpoint.node.network)
              || !network.permits(Flowers.owner(endpoint.node)) || pool(level, endpoint.node) != endpoint.pool) return false;
        for (int i = 0; i < endpoint.path.size(); i++) {
            NetworkPlant plant = at(level, endpoint.path.get(i));
            if (plant == null || !Flowers.enabled(plant) || !network.id.equals(plant.network) || !network.permits(Flowers.owner(plant))) return false;
            if (i > 0 && !within(endpoint.path.get(i - 1), endpoint.path.get(i))) return false;
            if (i > 0 && i < endpoint.path.size() - 1 && plant.mode != NetworkPlant.RELAY) return false;
        }
        return true;
    }
    static ManaNetworks load(CompoundTag tag, HolderLookup.Provider provider) {
        ManaNetworks data = new ManaNetworks();
        for (Tag value : tag.getList("networks", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) value;
            if (!saved.hasUUID("id") || !saved.hasUUID("owner")) continue;
            Network network = new Network(saved.getUUID("id"), saved.getUUID("owner"), saved.getString("name"), saved.contains("core") ? BlockPos.of(saved.getLong("core")) : null);
            network.credit = Math.clamp(saved.getInt("credit"), 0, 49); network.cursor = Math.max(0, saved.getInt("cursor"));
            for (long pos : saved.getLongArray("nodes")) if (network.nodes.size() < Balance.NODE_LIMIT) network.nodes.add(BlockPos.of(pos));
            for (Tag memberTag : saved.getList("members", Tag.TAG_COMPOUND)) {
                CompoundTag member = (CompoundTag) memberTag;
                if (member.hasUUID("id") && network.members.size() < 32) network.members.put(member.getUUID("id"), member.getString("name"));
            }
            data.networks.put(network.id, network);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Network network : networks.values()) {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("id", network.id); saved.putUUID("owner", network.owner); saved.putString("name", network.name);
            if (network.core != null) saved.putLong("core", network.core.asLong());
            saved.putInt("credit", network.credit); saved.putInt("cursor", network.cursor);
            saved.putLongArray("nodes", network.nodes.stream().mapToLong(BlockPos::asLong).toArray());
            ListTag members = new ListTag();
            network.members.forEach((id, name) -> { CompoundTag member = new CompoundTag(); member.putUUID("id", id); member.putString("name", name); members.add(member); });
            saved.put("members", members); list.add(saved);
        }
        tag.put("networks", list); return tag;
    }
}
