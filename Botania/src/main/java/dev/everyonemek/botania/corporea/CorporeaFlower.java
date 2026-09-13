package dev.everyonemek.botania.corporea;

import dev.everyonemek.botania.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CorporeaFlower extends BlockEntity implements vazkii.botania.api.corporea.CorporeaInterceptor {
    public final BridgeFilter filter = new BridgeFilter();
    public boolean autocraft;
    private BridgeBackend backend;
    private CompoundTag connection = new CompoundTag();
    private boolean disconnected;
    public int mode; // 0 both, 1 Corporea visible to ME only, 2 ME visible to Corporea only.
    public CorporeaFlower(BlockPos pos, BlockState state) { super(Content.CORPOREA_TILE.get(), pos, state); }
    public BridgeBackend backend() {
        if (backend == null) { backend = CorporeaIntegration.factory.apply(this); backend.load(connection); backend.loaded(); }
        return backend;
    }
    @Override public void onLoad() { super.onLoad(); backend(); }
    public void tick() { if (Flowers.live(this) && !level.isClientSide) backend().tick(); }
    public boolean enabled() { return !disconnected && Flowers.live(this) && Flowers.enabled(this) && PlantSupport.areaLoaded(level, worldPosition, 1) && !level.hasNeighborSignal(worldPosition); }
    public boolean exportsCorporea() { return mode != 2; }
    public boolean exposesME() { return mode != 1; }
    private void disconnect() {
        if (backend != null && !disconnected) { connection = backend.save(); disconnected = true; backend.destroy(); }
    }
    @Override public void setRemoved() { disconnect(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { disconnect(); super.onChunkUnloaded(); }
    @Override public void clearRemoved() {
        super.clearRemoved(); if (disconnected) { backend = null; disconnected = false; }
    }
    public void saveItem(CompoundTag tag) {
        tag.putInt("bridge_mode", mode); tag.putBoolean("autocraft", autocraft);
        filter.save(tag, level.registryAccess());
    }
    public void loadItem(CompoundTag tag) {
        mode = Math.clamp(tag.getInt("bridge_mode"), 0, 2); connection = new CompoundTag();
        autocraft = tag.getBoolean("autocraft"); filter.load(tag, level.registryAccess());
        if (backend != null) { backend.destroy(); backend = null; }
        disconnected = false;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); tag.putInt("bridge_mode", mode); tag.putBoolean("autocraft", autocraft); filter.save(tag, provider);
        tag.put("me_connection", backend == null || disconnected ? connection.copy() : backend.save());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (backend != null) { backend.destroy(); backend = null; }
        disconnected = false;
        mode = Math.clamp(tag.getInt("bridge_mode"), 0, 2); autocraft = tag.getBoolean("autocraft");
        filter.load(tag, provider); connection = tag.getCompound("me_connection").copy();
    }
    @Override public void interceptRequest(vazkii.botania.api.corporea.CorporeaRequestMatcher request, int count,
          vazkii.botania.api.corporea.CorporeaSpark spark, vazkii.botania.api.corporea.CorporeaSpark source,
          java.util.List<net.minecraft.world.item.ItemStack> stacks, java.util.Set<vazkii.botania.api.corporea.CorporeaNode> nodes, boolean doit) { }
    @Override public void interceptRequestLast(vazkii.botania.api.corporea.CorporeaRequestMatcher request, int count,
          vazkii.botania.api.corporea.CorporeaSpark spark, vazkii.botania.api.corporea.CorporeaSpark source,
          java.util.List<net.minecraft.world.item.ItemStack> stacks, java.util.Set<vazkii.botania.api.corporea.CorporeaNode> nodes, boolean doit) {
        if (!doit || count <= 0 || !autocraft || !enabled() || !exposesME()) return;
        long found = stacks.stream().filter(request::test).mapToLong(net.minecraft.world.item.ItemStack::getCount).sum();
        if (found < count) backend().requestCraft(request, (int) (count - found));
    }
}
