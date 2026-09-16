package dev.everyonemek.overloadcore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    public static WardLedger get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(
              new Factory<>(WardLedger::new, WardLedger::load, null), "overloadcore_ward_custody");
    }
    public static WardLedger load(CompoundTag tag, HolderLookup.Provider registry) {
        var ledger = new WardLedger();
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
        return tag;
    }
}
