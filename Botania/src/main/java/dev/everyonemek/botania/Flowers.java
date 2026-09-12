package dev.everyonemek.botania;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.energy.IEnergyStorage;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;
import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;

public final class Flowers {
    private static final String DATA = BotanicalMekanism.ID;
    public static CompoundTag data(BlockEntity tile) {
        CompoundTag persistent = tile.getPersistentData();
        if (!persistent.contains(DATA)) persistent.put(DATA, new CompoundTag());
        return persistent.getCompound(DATA);
    }
    public static boolean isAmaranthus(BlockEntity tile) { return tile.getBlockState().is(Content.AMARANTHUS.get()); }
    public static boolean live(BlockEntity tile) {
        return tile.getLevel() != null && !tile.isRemoved() && tile.getLevel().hasChunkAt(tile.getBlockPos())
              && tile.getLevel().getBlockEntity(tile.getBlockPos()) == tile;
    }
    private static CompoundTag readData(BlockEntity tile) { return tile.getPersistentData().getCompound(DATA); }
    // The pinned Botania flower base does not call BlockEntity.save/loadAdditional.
    // Persist our namespace explicitly instead of relying on NeoForgeData being inherited.
    public static void saveData(BlockEntity tile, CompoundTag tag) { tag.put(DATA, readData(tile).copy()); }
    public static void loadData(BlockEntity tile, CompoundTag tag) {
        CompoundTag source = tag.contains(DATA, net.minecraft.nbt.Tag.TAG_COMPOUND) ? tag : tag.getCompound("NeoForgeData");
        if (source.contains(DATA, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag common = source.getCompound(DATA).copy();
            if (common.contains("fe")) common.putInt("fe", Math.clamp(common.getInt("fe"), 0, Balance.ENERGY_CAPACITY));
            tile.getPersistentData().put(DATA, common);
        }
    }
    public static boolean enabled(BlockEntity tile) { return !readData(tile).getBoolean("paused"); }
    public static int storedFE(BlockEntity tile) { return Math.clamp(readData(tile).getInt("fe"), 0, Balance.ENERGY_CAPACITY); }
    public static void setFE(BlockEntity tile, int energy) { data(tile).putInt("fe", Math.clamp(energy, 0, Balance.ENERGY_CAPACITY)); tile.setChanged(); }
    public static UUID owner(BlockEntity tile) { return readData(tile).hasUUID("owner") ? readData(tile).getUUID("owner") : null; }
    public static boolean owns(Player player, BlockEntity tile) { return owner(tile) != null && owner(tile).equals(player.getUUID()); }
    public static void claim(BlockEntity tile, LivingEntity placer) {
        if (owner(tile) == null && placer instanceof Player player) {
            data(tile).putUUID("owner", player.getUUID()); tile.setChanged();
            if (tile instanceof ManaLotus lotus) lotus.markForImmediateSync();
        }
    }
    public static IEnergyStorage energy(BlockEntity tile) {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) {
                if (!live(tile) || tile.getLevel().isClientSide || amount <= 0) return 0;
                int accepted = Math.min(amount, Balance.ENERGY_CAPACITY - storedFE(tile));
                if (!simulate && accepted > 0) setFE(tile, storedFE(tile) + accepted);
                return accepted;
            }
            @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return live(tile) ? storedFE(tile) : 0; }
            @Override public int getMaxEnergyStored() { return Balance.ENERGY_CAPACITY; }
            @Override public boolean canExtract() { return false; }
            @Override public boolean canReceive() { return live(tile); }
        };
    }
    public static void supplyAmaranthus(FunctionalFlowerBlockEntity tile) {
        if (!live(tile) || tile.getLevel().isClientSide || !enabled(tile)
              || tile.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
              || !PlantSupport.areaLoaded(tile.getLevel(), tile.getBlockPos(), 5)) return;
        if (tile.getBindingPos() != null) tile.setBindingPos(null);
        int units = Math.min(tile.getMaxMana() - tile.getMana(), storedFE(tile) / Balance.FE_PER_MANA.get());
        if (units > 0) { setFE(tile, storedFE(tile) - units * Balance.FE_PER_MANA.get()); tile.addMana(units); tile.setChanged(); }
    }
    public static List<ItemStack> withState(List<ItemStack> drops, LootParams.Builder builder, net.minecraft.world.level.block.Block block) {
        BlockEntity tile = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tile == null) return drops;
        CompoundTag state = readData(tile).copy();
        state.putString("kind", BuiltInRegistries.BLOCK.getKey(block).toString());
        if (tile instanceof GeneratingFlowerBlockEntity flower) {
            state.putInt("mana", flower.getMana());
            if (flower.getBindingPos() != null) state.putLong("binding", flower.getBindingPos().asLong());
        } else if (tile instanceof FunctionalFlowerBlockEntity flower) state.putInt("mana", flower.getMana());
        if (tile instanceof NetworkPlant plant) plant.saveItem(state);
        if (tile instanceof ManaLotus lotus) state.putLong("last_production_tick", lotus.lastProductionTick());
        for (ItemStack drop : drops) if (drop.is(block.asItem())) drop.set(Content.STATE.get(), CustomData.of(state));
        return drops;
    }
    public static void placed(BlockEntity tile, LivingEntity placer, ItemStack stack) {
        CustomData component = stack.get(Content.STATE.get());
        if (component != null) {
            CompoundTag state = component.copyTag();
            if (state.getString("kind").equals(BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()).toString())) {
                CompoundTag common = new CompoundTag();
                if (state.hasUUID("owner")) common.putUUID("owner", state.getUUID("owner"));
                common.putBoolean("paused", state.getBoolean("paused"));
                tile.getPersistentData().put(DATA, common);
                setFE(tile, state.getInt("fe"));
                if (tile instanceof GeneratingFlowerBlockEntity flower) {
                    flower.addMana(Math.clamp(state.getInt("mana"), 0, flower.getMaxMana()) - flower.getMana());
                    if (state.contains("binding")) flower.setBindingPos(BlockPos.of(state.getLong("binding")));
                } else if (tile instanceof FunctionalFlowerBlockEntity flower)
                    flower.addMana(Math.clamp(state.getInt("mana"), 0, flower.getMaxMana()) - flower.getMana());
                if (tile instanceof NetworkPlant plant) plant.loadItem(state);
                if (tile instanceof ManaLotus lotus && state.contains("last_production_tick")) lotus.restoreProductionTick(state.getLong("last_production_tick"));
                if (owner(tile) != null && (!(placer instanceof Player player) || !owns(player, tile))) data(tile).putBoolean("paused", true);
            }
        }
        claim(tile, placer); tile.setChanged();
    }
    public static void open(Player player, BlockEntity tile) {
        if (!(player instanceof ServerPlayer serverPlayer) || tile == null) return;
        claim(tile, player);
        if (!owns(player, tile)) { player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.botanicalmekanism.denied"), true); return; }
        serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new FlowerMenu(id, inventory, tile.getBlockPos()),
              tile.getBlockState().getBlock().getName()), buffer -> buffer.writeBlockPos(tile.getBlockPos()));
    }
    private Flowers() { }
}
