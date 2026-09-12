package dev.everyonemek.botania;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;
import vazkii.botania.api.mana.ManaCollector;

public final class ManaLotus extends GeneratingFlowerBlockEntity {
    private long lastProductionTick = Long.MIN_VALUE;
    public int status;
    public ManaLotus(BlockPos pos, BlockState state) { super(Content.LOTUS_TILE.get(), pos, state); }
    public long lastProductionTick() { return lastProductionTick; }
    public void restoreProductionTick(long tick) { lastProductionTick = tick; }
    @Override public boolean canSelect(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack wand, net.minecraft.core.Direction side) { return Flowers.owns(player, this); }
    @Override public boolean bindTo(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack wand, BlockPos pos, net.minecraft.core.Direction side) {
        return Flowers.owns(player, this) && super.bindTo(player, wand, pos, side);
    }
    @Override public void tickFlower() {
        if (!Flowers.live(this)) return;
        if (level.isClientSide) { super.tickFlower(); return; }
        if (!Flowers.enabled(this) || getBlockState().getValue(BlockStateProperties.POWERED)) { status = 1; return; }
        // The parent performs normal binding and transfers the one real native mana buffer.
        super.tickFlower();
        long tick = level.getGameTime();
        if (lastProductionTick == tick) return;
        lastProductionTick = tick; setChanged();
        ManaCollector collector = findBoundTile();
        if (collector == null) { status = 2; return; }
        if (collector.isFull() || getMana() >= getMaxMana()) { status = 3; return; }
        int amount = Math.min(Balance.LOTUS_RATE.get(), Math.min(getMaxMana() - getMana(), Flowers.storedFE(this) / Balance.FE_PER_MANA.get()));
        if (amount <= 0) { status = 4; return; }
        Flowers.setFE(this, Flowers.storedFE(this) - amount * Balance.FE_PER_MANA.get());
        addMana(amount); status = 0; setChanged();
    }
    @Override public ManaCollector findBindCandidateAt(BlockPos position) {
        return level == null || position == null || !level.hasChunkAt(position)
              || position.distSqr(getBlockPos()) > (long) getBindingRadius() * getBindingRadius() ? null : super.findBindCandidateAt(position);
    }
    @Override public int getMaxMana() { return Balance.LOTUS_CAPACITY; }
    @Override public int getColor() { return 0x4AE1E8; }
    @Override public RadiusDescriptor getRadius() { return RadiusDescriptor.Rectangle.square(getBlockPos(), getBindingRadius()); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        // Native flower updates only carry mana/binding. The wand also selects on the client,
        // which needs the owner for the same permission decision as the server.
        CompoundTag access = new CompoundTag();
        var owner = Flowers.owner(this);
        if (owner != null) access.putUUID("owner", owner);
        tag.put(BotanicalMekanism.ID, access);
        return tag;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); Flowers.saveData(this, tag); tag.putLong("last_production_tick", lastProductionTick);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        Flowers.loadData(this, tag);
        lastProductionTick = tag.contains("last_production_tick") ? tag.getLong("last_production_tick") : Long.MIN_VALUE;
        addMana(Math.clamp(getMana(), 0, getMaxMana()) - getMana());
    }
}
