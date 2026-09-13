package dev.everyonemek.botania.compat.ae2;

import java.util.*;
import java.util.concurrent.*;
import com.google.common.collect.ImmutableSet;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.StorageHelper;
import dev.everyonemek.botania.corporea.CorporeaFlower;
import net.minecraft.nbt.*;
import vazkii.botania.api.corporea.CorporeaRequestMatcher;

/** Bounded restocking jobs. Results return to ME; native requestors/retainers perform delivery. */
final class BridgeCrafting implements ICraftingRequester {
    private static final int MAX_JOBS = 4, MAX_AMOUNT = 4096;
    private final AeBridge bridge;
    private final CorporeaFlower flower;
    private final Map<AEItemKey, ICraftingLink> links = new HashMap<>();
    private Future<ICraftingPlan> calculation;
    private AEItemKey pending;
    private long nextRequest;
    private String status = "idle";
    BridgeCrafting(AeBridge bridge, CorporeaFlower flower) { this.bridge = bridge; this.flower = flower; }
    private boolean ready() { return flower.autocraft && flower.exposesME() && bridge.craftingReady(); }
    void request(CorporeaRequestMatcher matcher, int missing) {
        if (!ready() || missing <= 0 || calculation != null || links.size() >= MAX_JOBS
              || flower.getLevel().getGameTime() < nextRequest) return;
        // A transfer limit or an insertion restriction is not a shortage of items.
        long available = bridge.availableForCrafting(matcher);
        if (available >= missing) return;
        var service = getActionableNode().getGrid().getCraftingService();
        var matches = service.getCraftables(key -> key instanceof AEItemKey item && flower.filter.allows(item.toStack())
              && matcher.test(item.toStack())).stream().filter(AEItemKey.class::isInstance).map(AEItemKey.class::cast).limit(2).toList();
        nextRequest = flower.getLevel().getGameTime() + 20;
        if (matches.size() != 1) { status = matches.isEmpty() ? "no_pattern" : "ambiguous"; return; }
        var key = matches.getFirst();
        if (links.containsKey(key)) return; // Repeated retainer/redstone requests share the accepted job.
        pending = key; status = "calculating";
        getActionableNode().getGrid().getStorageService().invalidateCache();
        calculation = service.beginCraftingCalculation(flower.getLevel(), () -> IActionSource.ofMachine(this),
              key, Math.min(MAX_AMOUNT, missing - available), CalculationStrategy.REPORT_MISSING_ITEMS);
    }
    void tick() {
        if (links.entrySet().removeIf(e -> e.getValue().isDone() || e.getValue().isCanceled())) flower.setChanged();
        if (!ready()) { stopCalculation(); return; }
        if (calculation == null || !calculation.isDone()) return;
        try {
            var plan = calculation.get();
            if (plan.simulation()) status = "missing_materials";
            else {
                var result = getActionableNode().getGrid().getCraftingService().submitJob(plan, this, null, false, IActionSource.ofMachine(this));
                if (result.successful() && result.link() != null) { links.put(pending, result.link()); flower.setChanged(); status = "running"; }
                else status = "no_cpu";
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); status = "failed"; }
        catch (ExecutionException | CancellationException e) { status = "failed"; }
        finally { calculation = null; pending = null; }
    }
    void stopCalculation() {
        if (calculation != null) { calculation.cancel(true); calculation = null; pending = null; status = "idle"; }
    }
    void cancelAll() { stopCalculation(); for (var link : List.copyOf(links.values())) link.cancel(); links.clear(); flower.setChanged(); }
    void save(CompoundTag tag) {
        var jobs = new ListTag();
        for (var entry : links.entrySet()) {
            var job = new CompoundTag(); job.put("item", entry.getKey().toTag(flower.getLevel().registryAccess()));
            var link = new CompoundTag(); entry.getValue().writeToNBT(link); job.put("link", link); jobs.add(job);
        }
        tag.put("crafting_jobs", jobs);
    }
    void load(CompoundTag tag) {
        var jobs = tag.getList("crafting_jobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_JOBS, jobs.size()); i++) {
            var job = jobs.getCompound(i); var key = AEItemKey.fromTag(flower.getLevel().registryAccess(), job.getCompound("item"));
            if (key != null && !job.getCompound("link").isEmpty()) links.put(key, StorageHelper.loadCraftingLink(job.getCompound("link"), this));
        }
    }
    void describe(CompoundTag tag) {
        tag.putInt("craft_jobs", links.size() + (calculation == null ? 0 : 1));
        tag.putString("craft_status", !flower.autocraft ? "off" : !links.isEmpty() ? "running" : status.equals("running") ? "idle" : status);
    }
    @Override public IGridNode getActionableNode() { return bridge.getActionableNode(); }
    @Override public ImmutableSet<ICraftingLink> getRequestedJobs() { return ImmutableSet.copyOf(links.values()); }
    @Override public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        if (!(what instanceof AEItemKey item) || links.get(item) != link || amount <= 0) return 0;
        // Disabling new orders must not strand already paid results in a private buffer.
        return bridge.returnCrafted(item, amount, mode);
    }
    @Override public void jobStateChange(ICraftingLink link) {
        links.values().removeIf(value -> value == link); flower.setChanged();
        status = link.isCanceled() ? "canceled" : "idle";
    }
}
