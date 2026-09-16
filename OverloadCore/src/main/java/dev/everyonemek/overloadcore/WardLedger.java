package dev.everyonemek.overloadcore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Equipped-item custody lives outside player NBT, so clearing inventory/attachments cannot erase it. */
public final class WardLedger extends SavedData {
    static final class Entry {
        final UUID token;
        private final CompoundTag saved;
        private final ItemStack comparison;
        ItemStack live = ItemStack.EMPTY;
        Entry(ItemStack stack, HolderLookup.Provider registry) {
            token = stack.get(CoreContent.WARD_SEAL);
            saved = ((CompoundTag)stack.save(registry)).copy();
            comparison = ItemStack.parse(registry, saved.copy()).orElseThrow();
        }
        ItemStack restore(HolderLookup.Provider registry) { return ItemStack.parse(registry, saved.copy()).orElseThrow(); }
        boolean matches(ItemStack stack) { return ItemStack.matches(comparison, stack); }
    }
    final Map<UUID, Entry> worn = new HashMap<>();
    private final Set<UUID> extreme = new HashSet<>();
    private final Set<UUID> retired = new HashSet<>();
    public static WardLedger get(ServerPlayer player) {
        return get(player.serverLevel());
    }
    public static WardLedger get(net.minecraft.server.level.ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
              new Factory<>(WardLedger::new, WardLedger::load, null), "overloadcore_ward_custody");
    }
    public boolean extreme(ServerPlayer p) { return extreme.contains(p.getUUID()); }
    public void extreme(ServerPlayer p, boolean value) {
        if (value ? extreme.add(p.getUUID()) : extreme.remove(p.getUUID())) setDirty();
    }
    boolean retired(UUID token) { return retired.contains(token); }
    boolean known(UUID token) { return token != null && (retired.contains(token) || worn.values().stream().anyMatch(e -> token.equals(e.token))); }
    void release(UUID owner) {
        var entry = worn.remove(owner);
        if (entry != null) { retired.add(entry.token); setDirty(); }
    }
    public static WardLedger load(CompoundTag tag, HolderLookup.Provider registry) {
        var ledger = new WardLedger();
        for (Tag row : tag.getList("Extreme", Tag.TAG_INT_ARRAY))
            if (((IntArrayTag)row).getAsIntArray().length == 4) ledger.extreme.add(NbtUtils.loadUUID(row));
        for (Tag row : tag.getList("Retired", Tag.TAG_INT_ARRAY))
            if (((IntArrayTag)row).getAsIntArray().length == 4) ledger.retired.add(NbtUtils.loadUUID(row));
        for (Tag value : tag.getList("Worn", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)value;
            if (!row.hasUUID("Owner")) continue;
            ItemStack.parse(registry, row.getCompound("Stack")).filter(s -> s.is(CoreContent.WARD)
                  && s.getCount() == 1 && s.has(CoreContent.WARD_SEAL)).ifPresent(s ->
                  ledger.worn.put(row.getUUID("Owner"), new Entry(s, registry)));
        }
        return ledger;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registry) {
        var list = new ListTag();
        worn.forEach((owner, entry) -> {
            var row = new CompoundTag();
            row.putUUID("Owner", owner);
            row.put("Stack", entry.saved.copy());
            list.add(row);
        });
        tag.put("Worn", list);
        var modes = new ListTag(); extreme.forEach(id -> modes.add(NbtUtils.createUUID(id))); tag.put("Extreme", modes);
        var old = new ListTag(); retired.forEach(id -> old.add(NbtUtils.createUUID(id))); tag.put("Retired", old);
        return tag;
    }
}
