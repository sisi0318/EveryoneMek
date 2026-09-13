package dev.everyonemek.botania.compat.ae2;

import java.util.*;
import java.util.function.Supplier;
import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.*;
import appeng.api.networking.security.*;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import appeng.api.util.AECableType;
import dev.everyonemek.botania.*;
import dev.everyonemek.botania.corporea.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import vazkii.botania.api.corporea.*;

public final class AeBridge implements BridgeBackend, IInWorldGridNodeHost, IActionHost, IStorageProvider {
    private static final ThreadLocal<Set<UUID>> EXCLUDED_CORPOREA = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Boolean> EXPORTING = ThreadLocal.withInitial(() -> false);
    private static final int NODE_LIMIT = 128, SLOT_LIMIT = 8192;
    private final CorporeaFlower flower;
    private final IManagedGridNode node;
    private final BridgeCrafting crafting;
    private final ExportStorage storage = new ExportStorage();
    private long budgetTick = Long.MIN_VALUE;
    private long displayTick = Long.MIN_VALUE;
    private int spent, lastMoved, displayNodes, displayTypes, displayMeTypes;
    private long displayItems, displayMeItems;
    private String status = "connecting";
    private boolean scheduled, destroyed;

    public AeBridge(CorporeaFlower flower) {
        this.flower = flower;
        crafting = new BridgeCrafting(this, flower);
        node = GridHelper.createManagedNode(this, new IGridNodeListener<AeBridge>() {
            @Override public void onSaveChanges(AeBridge bridge, IGridNode gridNode) { flower.setChanged(); }
            @Override public void onStateChanged(AeBridge bridge, IGridNode gridNode, State state) { bridge.settingsChanged(); }
            @Override public void onGridChanged(AeBridge bridge, IGridNode gridNode) { flower.setChanged(); }
        }).setInWorldNode(true).setTagName("ae_node").setFlags(GridFlags.REQUIRE_CHANNEL)
              .setIdlePowerUsage(4).setVisualRepresentation(Content.CORPOREA.get()).addService(IStorageProvider.class, this)
              .addService(appeng.api.networking.crafting.ICraftingRequester.class, crafting);
    }
    @Override public void loaded() {
        if (scheduled || destroyed) return;
        scheduled = true;
        GridHelper.onFirstTick(flower, tile -> {
            scheduled = false;
            if (destroyed || !Flowers.live(tile) || !PlantSupport.areaLoaded(tile.getLevel(), tile.getBlockPos(), 1)) return;
            if (Flowers.owner(tile) != null) node.setOwningPlayerId(IPlayerRegistry.getMapping(tile.getLevel()).getPlayerId(Flowers.owner(tile)));
            node.create(tile.getLevel(), tile.getBlockPos());
        });
    }
    @Override public void load(CompoundTag tag) { node.loadFromNBT(tag); crafting.load(tag); }
    @Override public CompoundTag save() { var tag = new CompoundTag(); node.saveToNBT(tag); crafting.save(tag); return tag; }
    @Override public void destroy() { destroyed = true; crafting.stopCalculation(); node.destroy(); }
    @Override public void removed() { crafting.cancelAll(); }
    @Override public IGridNode getGridNode(Direction direction) { return destroyed ? null : node.getNode(); }
    @Override public IGridNode getActionableNode() { return getGridNode(Direction.DOWN); }
    @Override public AECableType getCableConnectionType(Direction direction) { return AECableType.SMART; }
    @Override public boolean connected() { return !destroyed && node.isActive(); }
    @Override public void mountInventories(IStorageMounts mounts) { mounts.mount(storage, 0); }
    @Override public void settingsChanged() {
        if (node.getGrid() != null) node.getGrid().getStorageService().invalidateCache();
        flower.setChanged();
    }
    public IManagedGridNode managedNode() { return node; }
    private static <T> T withoutCorporea(CorporeaSpark master, Supplier<T> operation) {
        var excluded = EXCLUDED_CORPOREA.get(); UUID id = master.entity().getUUID(); boolean added = excluded.add(id);
        try { return operation.get(); } finally { if (added) excluded.remove(id); if (excluded.isEmpty()) EXCLUDED_CORPOREA.remove(); }
    }
    private static <T> T exporting(Supplier<T> operation, T refused) {
        if (EXPORTING.get()) return refused;
        EXPORTING.set(true); try { return operation.get(); } finally { EXPORTING.set(false); }
    }
    private int remaining() {
        long tick = flower.getLevel().getGameTime();
        return Math.max(0, Balance.CORPOREA_TRANSFER.get() - (budgetTick == tick ? spent : 0));
    }
    private void advanceBudget() {
        long tick = flower.getLevel().getGameTime();
        if (budgetTick != tick) { budgetTick = tick; lastMoved = spent; spent = 0; }
    }
    private void record(int moved) { if (moved > 0) { advanceBudget(); spent += moved; settingsChanged(); } }
    private boolean basicReady() { return connected() && flower.enabled(); }
    private boolean liveSpark(CorporeaSpark spark) {
        return spark != null && spark.entity().isAlive() && spark.entity().level() == flower.getLevel()
              && flower.getLevel().hasChunkAt(spark.getAttachPos());
    }
    private record Port(AeBridge bridge, BlockPos pos, BlockEntity tile, CorporeaSpark spark) {
        IItemHandler handler() {
            var level = bridge.flower.getLevel();
            if (!bridge.liveSpark(spark) || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != tile || tile.isRemoved()) return null;
            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null, tile, Direction.UP);
            return handler != null ? handler : level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null, tile, null);
        }
    }
    private record Topology(CorporeaSpark master, List<Port> ports, String problem) { }
    private Topology topology() {
        var level = flower.getLevel();
        if (!Flowers.live(flower) || !PlantSupport.areaLoaded(level, flower.getBlockPos(), 1)) return new Topology(null, List.of(), "unloaded");
        var spark = CorporeaHelper.instance().getSparkForBlock(level, flower.getBlockPos());
        if (!liveSpark(spark)) return new Topology(null, List.of(), "missing_spark");
        if (spark.isMaster() || spark.isCreative()) return new Topology(null, List.of(), "ordinary_spark");
        var master = spark.getMaster();
        if (!liveSpark(master) || !master.isMaster() || !master.getConnections().contains(spark)) return new Topology(null, List.of(), "missing_master");
        if (master.getConnections().size() > NODE_LIMIT) return new Topology(master, List.of(), "too_many_nodes");
        var members = master.getConnections().stream().filter(this::liveSpark).filter(s -> s.getMaster() == master)
              .sorted(Comparator.comparing(CorporeaSpark::getAttachPos)).toList();
        // One bridge per Corporea network, even when several separate ME grids touch it.
        for (var member : members) if (level.getBlockEntity(member.getAttachPos()) instanceof CorporeaFlower other
              && other.enabled() && other.backend().connected()) {
            if (other != flower) return new Topology(master, List.of(), "duplicate_bridge");
            break;
        }
        List<Port> ports = new ArrayList<>(); Set<BlockPos> seen = new HashSet<>(); int slots = 0;
        for (var member : members) {
            var pos = member.getAttachPos(); var tile = level.getBlockEntity(pos);
            if (tile == null || tile instanceof CorporeaFlower || member.isCreative()) continue;
            // Never advertise another ME view back to ME. These are not Corporea-owned physical stores.
            if (level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null
                  || level.getCapability(AECapabilities.ME_STORAGE, pos, Direction.UP) != null) continue;
            BlockPos identity = pos, partner = null;
            var state = tile.getBlockState();
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                partner = pos.relative(ChestBlock.getConnectedDirection(state));
                if (!level.hasChunkAt(partner)) continue;
                if (partner.compareTo(identity) < 0) identity = partner;
            }
            if (!seen.add(identity)) return new Topology(master, List.of(), "duplicate_chest");
            var port = new Port(this, pos, tile, member); var handler = port.handler();
            if (handler == null) continue;
            if (hasDuplicateStorageBus(pos) || partner != null && hasDuplicateStorageBus(partner)) return new Topology(master, List.of(), "duplicate_storage");
            slots += handler.getSlots(); if (slots > SLOT_LIMIT) return new Topology(master, List.of(), "too_many_slots");
            ports.add(port);
        }
        return new Topology(master, ports, "");
    }
    private boolean hasDuplicateStorageBus(BlockPos inventory) {
        var level = flower.getLevel();
        for (var side : Direction.values()) {
            var neighbor = inventory.relative(side); if (!level.hasChunkAt(neighbor)) continue;
            var part = PartHelper.getPart(level, neighbor, side.getOpposite());
            if (part != null && BuiltInRegistries.ITEM.getKey(part.getPartItem().asItem()).toString().equals("ae2:storage_bus")
                  && part.getGridNode() != null && part.getGridNode().getGrid() == node.getGrid()) return true;
        }
        return false;
    }
    private Topology usable() {
        if (!basicReady()) return null;
        var topology = topology(); return topology.problem().isEmpty() ? topology : null;
    }
    @Override public void tick() {
        if (!node.isReady()) loaded();
        advanceBudget(); crafting.tick();
        if (!flower.enabled()) status = Flowers.enabled(flower) ? "redstone" : "paused";
        else if (!node.isReady()) status = "connecting";
        else if (!node.isPowered()) status = "no_power";
        else if (!node.isActive()) status = "no_channel";
        else status = topology().problem();
        if (status.isEmpty()) status = "ready";
    }
    private void refreshDisplay() {
        long now = flower.getLevel().getGameTime();
        if (displayTick != Long.MIN_VALUE && now - displayTick < 20) return;
        displayTick = now;
        displayItems = displayMeItems = 0; displayTypes = displayMeTypes = displayNodes = 0;
        var topology = usable();
        if (topology != null) {
            displayNodes = topology.ports().size();
            var physical = scan(topology); displayTypes = physical.size();
            for (long amount : physical.values()) displayItems = saturatedAdd(displayItems, amount);
            var me = withoutCorporea(topology.master(), () -> node.getGrid().getStorageService().getInventory().getAvailableStacks());
            for (var entry : me) if (entry.getKey() instanceof AEItemKey key && flower.filter.allows(key.toStack()) && entry.getLongValue() > 0) {
                displayMeTypes++; displayMeItems = saturatedAdd(displayMeItems, entry.getLongValue());
            }
        }
    }
    @Override public void describe(CompoundTag state) {
        refreshDisplay();
        crafting.describe(state); state.putString("status", status); state.putBoolean("ae_connected", connected());
        state.putInt("nodes", displayNodes); state.putInt("types", displayTypes); state.putInt("me_types", displayMeTypes);
        state.putLong("items", displayItems); state.putLong("me_items", displayMeItems); state.putInt("moved", lastMoved);
        state.putInt("limit", Balance.CORPOREA_TRANSFER.get());
    }
    private static long saturatedAdd(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    private Map<AEItemKey, Long> scan(Topology topology) {
        Map<AEItemKey, Long> counts = new HashMap<>();
        for (var port : topology.ports()) {
            var handler = port.handler(); if (handler == null) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.getStackInSlot(slot); var key = AEItemKey.of(stack);
                if (key != null && flower.filter.allows(stack) && key.matches(handler.extractItem(slot, 1, true))) counts.merge(key, (long) stack.getCount(), AeBridge::saturatedAdd);
            }
        }
        return counts;
    }
    @Override public List<ItemStack> request(CorporeaRequest request, boolean execute) {
        if (EXPORTING.get() || !flower.exposesME()) return List.of();
        var topology = usable();
        if (topology == null || EXCLUDED_CORPOREA.get().contains(topology.master().entity().getUUID())) return List.of();
        return withoutCorporea(topology.master(), () -> {
            var grid = node.getGrid(); var inventory = grid.getStorageService().getInventory();
            IActionSource source = request.getEntity() instanceof Player player ? IActionSource.ofPlayer(player, this) : IActionSource.ofMachine(this);
            List<ItemStack> found = new ArrayList<>();
            for (var entry : inventory.getAvailableStacks()) {
                if (!(entry.getKey() instanceof AEItemKey key) || entry.getLongValue() <= 0 || !flower.filter.allows(key.toStack()) || !request.getMatcher().test(key.toStack())) continue;
                int available = (int) Math.min(Integer.MAX_VALUE, entry.getLongValue());
                request.trackFound(Math.min(available, Integer.MAX_VALUE - Math.max(0, request.getFound())));
                int wanted = request.getStillNeeded() < 0 ? available : Math.min(available, request.getStillNeeded());
                if (execute) wanted = Math.min(wanted, remaining());
                if (wanted <= 0) continue;
                int moved = (int) StorageHelper.poweredExtraction(grid.getEnergyService(), inventory, key, wanted, source,
                      execute ? Actionable.MODULATE : Actionable.SIMULATE);
                if (moved <= 0) continue;
                request.trackSatisfied(moved);
                if (execute) {
                    request.trackExtracted(moved); record(moved);
                    var spark = CorporeaHelper.instance().getSparkForBlock(flower.getLevel(), flower.getBlockPos());
                    if (liveSpark(spark)) spark.onItemExtracted(key.toStack(Math.min(moved, key.getMaxStackSize())));
                    for (int left = moved; left > 0;) { int part = Math.min(left, key.getMaxStackSize()); found.add(key.toStack(part)); left -= part; }
                } else found.add(key.toStack(moved));
            }
            return found;
        });
    }
    boolean craftingReady() { return usable() != null; }
    long availableForCrafting(CorporeaRequestMatcher matcher) {
        var topology = usable(); if (topology == null) return Long.MAX_VALUE;
        // All real ME-visible storage is counted once here, including this network's physical stocks.
        long amount = 0;
        for (var entry : node.getGrid().getStorageService().getInventory().getAvailableStacks())
            if (entry.getKey() instanceof AEItemKey key && flower.filter.allows(key.toStack()) && matcher.test(key.toStack()))
                amount = saturatedAdd(amount, Math.max(0, entry.getLongValue()));
        // In receive-only mode the physical stocks are not mounted in ME.
        if (!flower.exportsCorporea()) for (var entry : scan(topology).entrySet())
            if (matcher.test(entry.getKey().toStack())) amount = saturatedAdd(amount, entry.getValue());
        return amount;
    }
    @Override public void requestCraft(CorporeaRequestMatcher matcher, int missing) { crafting.request(matcher, missing); }
    long returnCrafted(AEItemKey key, long amount, Actionable mode) {
        var topology = usable(); if (topology == null) return 0;
        long moved = withoutCorporea(topology.master(), () -> node.getGrid().getStorageService().getInventory()
              .insert(key, Math.min(amount, remaining()), mode, IActionSource.ofMachine(this)));
        if (mode == Actionable.MODULATE) record((int) moved); return moved;
    }
    private final class ExportStorage implements MEStorage {
        @Override public Component getDescription() { return Content.CORPOREA.get().getName(); }
        @Override public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
            if (!(what instanceof AEItemKey key) || !flower.exportsCorporea()) return false;
            return exporting(() -> {
                var topology = usable();
                return topology != null && !EXCLUDED_CORPOREA.get().contains(topology.master().entity().getUUID()) && scan(topology).containsKey(key);
            }, false);
        }
        @Override public void getAvailableStacks(KeyCounter out) {
            exporting(() -> {
                var topology = flower.exportsCorporea() ? usable() : null;
                if (topology != null && !EXCLUDED_CORPOREA.get().contains(topology.master().entity().getUUID())) scan(topology).forEach(out::add); return true;
            }, false);
        }
        @Override public long extract(AEKey what, long amount, Actionable action, IActionSource source) {
            if (!(what instanceof AEItemKey key) || !flower.filter.allows(key.toStack()) || amount <= 0 || !flower.exportsCorporea()) return 0;
            return exporting(() -> transfer(key, amount, action, false), 0L);
        }
        @Override public long insert(AEKey what, long amount, Actionable action, IActionSource source) {
            if (!(what instanceof AEItemKey key) || !flower.filter.allows(key.toStack()) || amount <= 0 || !flower.exportsCorporea()) return 0;
            return exporting(() -> transfer(key, amount, action, true), 0L);
        }
        private long transfer(AEItemKey key, long amount, Actionable action, boolean insert) {
            var topology = usable();
            if (topology == null || EXCLUDED_CORPOREA.get().contains(topology.master().entity().getUUID())) return 0;
            int wanted = (int) Math.min(amount, remaining()), moved = 0;
            for (int pass = 0; pass < (insert ? 2 : 1); pass++) for (var port : topology.ports()) {
                var handler = port.handler(); if (handler == null) continue;
                for (int slot = 0; slot < handler.getSlots() && moved < wanted; slot++) {
                    var current = handler.getStackInSlot(slot);
                    if (insert) {
                        if (pass == 0 ? current.isEmpty() : !current.isEmpty()) continue;
                        if (!current.isEmpty() && !key.matches(current)) continue;
                        int count = Math.min(wanted - moved, key.getMaxStackSize());
                        var remainder = handler.insertItem(slot, key.toStack(count), action == Actionable.SIMULATE);
                        moved += Math.max(0, count - remainder.getCount());
                    } else if (key.matches(current)) {
                        var extracted = handler.extractItem(slot, wanted - moved, action == Actionable.SIMULATE);
                        if (!extracted.isEmpty() && !key.matches(extracted)) {
                            if (action == Actionable.MODULATE) {
                                var rest = handler.insertItem(slot, extracted, false);
                                if (!rest.isEmpty()) net.minecraft.world.Containers.dropItemStack(flower.getLevel(), flower.getBlockPos().getX()+.5,
                                      flower.getBlockPos().getY()+.5, flower.getBlockPos().getZ()+.5, rest);
                            }
                            continue;
                        }
                        moved += extracted.getCount();
                        if (action == Actionable.MODULATE && !extracted.isEmpty()) port.spark().onItemExtracted(extracted.copy());
                    }
                }
            }
            if (action == Actionable.MODULATE) record(moved); return moved;
        }
    }
}
