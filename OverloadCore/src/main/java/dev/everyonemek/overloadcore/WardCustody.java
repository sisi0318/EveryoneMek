package dev.everyonemek.overloadcore;

import com.google.common.collect.MapMaker;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncCurios;

/** Server-authoritative custody, with a narrow exception for a validated player menu transaction. */
public final class WardCustody {
    private static final Map<ServerPlayer, Boolean> REPAIRING = new MapMaker().weakKeys().makeMap();
    private static final Map<ItemStack, WeakReference<ServerPlayer>> LIVE = new MapMaker().weakKeys().makeMap();
    private static final Map<ServerPlayer, Transfer> TRANSFERS = new MapMaker().weakKeys().makeMap();
    private static final class Transfer {
        final WardLedger.Entry entry;
        final List<ItemEntity> drops = new ArrayList<>();
        Transfer(WardLedger.Entry entry) { this.entry = entry; }
    }
    public static boolean locked(SlotContext ctx) {
        return ctx.index() == 0 && !ctx.cosmetic() && ctx.identifier().equals(ThunderWardItem.SLOT)
              && ctx.entity() instanceof ServerPlayer p && locked(p);
    }
    private static boolean locked(ServerPlayer p) {
        return ThunderWard.tracked(p) && !REPAIRING.containsKey(p) && !TRANSFERS.containsKey(p)
              && WardLedger.get(p).worn.containsKey(p.getUUID());
    }
    public static boolean locked(ItemStack stack) {
        var ref = LIVE.get(stack);
        var p = ref == null ? null : ref.get();
        return p != null && locked(p) && WardLedger.get(p).worn.get(p.getUUID()).live == stack;
    }
    public static void changed(SlotContext ctx) {
        if (ctx.index() == 0 && !ctx.cosmetic() && ctx.identifier().equals(ThunderWardItem.SLOT)
              && ctx.entity() instanceof ServerPlayer p) ensure(p);
    }
    private static boolean sealed(ItemStack stack, WardLedger.Entry entry) {
        return entry.token.equals(stack.get(CoreContent.WARD_SEAL));
    }
    /** Returns actual recovered equipment, never a synthetic always-equipped flag. */
    public static boolean ensure(ServerPlayer p) {
        if (!ThunderWard.tracked(p) || REPAIRING.containsKey(p) || TRANSFERS.containsKey(p)) return false;
        var inventory = CuriosApi.getCuriosInventory(p).orElse(null);
        if (inventory == null) return false;
        REPAIRING.put(p, true);
        try {
            var ledger = WardLedger.get(p);
            var entry = ledger.worn.get(p.getUUID());
            var slot = inventory.getStacksHandler(ThunderWardItem.SLOT).orElse(null);
            boolean rebuild = slot == null || slot.getStacks().getSlots() == 0 || slot.getActiveStates().isEmpty();
            if (entry == null) {
                if (rebuild || !slot.getActiveStates().get(0)) return false;
                var stack = slot.getStacks().getStackInSlot(0);
                if (!stack.is(CoreContent.WARD)) return false;
                // Only an unsealed physical item may start a new custody. Old copies never mint a new one.
                if (stack.has(CoreContent.WARD_SEAL)) {
                    // A missing world record can be a partial backup restore, not proof of duplication.
                    WardRuntime.quarantined(p);
                    return false;
                }
                if (stack.getCount() != 1) return false;
                stack.set(CoreContent.WARD_SEAL, UUID.randomUUID());
                entry = new WardLedger.Entry(stack, p.registryAccess());
                ledger.worn.put(p.getUUID(), entry);
                ledger.setDirty();
            }
            if (rebuild) {
                // Preserve unrelated slots and any remaining items instead of resetting the whole capability.
                boolean render = slot == null || slot.getRenders().isEmpty() || slot.getRenders().get(0);
                if (slot != null) {
                    for (var contents : List.of(slot.getStacks(), slot.getCosmeticStacks())) {
                        for (int i = 0; i < contents.getSlots(); i++) {
                            var displaced = contents.getStackInSlot(i);
                            if (!displaced.isEmpty() && !displaced.is(CoreContent.WARD) && !sealed(displaced, entry))
                                p.getInventory().placeItemBackInInventory(displaced.copy());
                        }
                    }
                }
                slot = new CurioStacksHandler(inventory, ThunderWardItem.SLOT);
                slot.getRenders().set(0, render);
                var slots = new TreeMap<>(inventory.getCurios());
                slots.put(ThunderWardItem.SLOT, slot);
                inventory.setCurios(slots);
                p.closeContainer(); // An open menu still holds references to the removed handler.
            }
            boolean repaired = rebuild || !slot.getActiveStates().get(0);
            slot.getActiveStates().set(0, true);
            var current = slot.getStacks().getStackInSlot(0);
            if (!entry.matches(current)) {
                // An unsealed ward in a protected slot may be the original with its marker stripped.
                // A deliberate ward swap is settled by click() before reaching this repair path.
                if (!current.isEmpty() && !current.is(CoreContent.WARD) && current != entry.live && !sealed(current, entry))
                    p.getInventory().placeItemBackInInventory(current.copy());
                current = entry.restore(p.registryAccess());
                slot.getStacks().setStackInSlot(0, current);
                repaired = true;
            }
            if (entry.live != current) {
                LIVE.remove(entry.live);
                entry.live = current;
                LIVE.put(current, new WeakReference<>(p));
            }
            // Eliminate transferred aliases without mutating a stack still referenced by the true slot.
            for (int i = 0; i < p.getInventory().getContainerSize(); i++)
                if (sealed(p.getInventory().getItem(i), entry)) p.getInventory().setItem(i, ItemStack.EMPTY);
            if (sealed(p.containerMenu.getCarried(), entry)) p.containerMenu.setCarried(ItemStack.EMPTY);
            if (repaired) PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new SPacketSyncCurios(p.getId(), inventory.getCurios()));
            return true;
        } finally { REPAIRING.remove(p); }
    }
    /** Called only around the clicked invocation after vanilla validates menu, slot and player. */
    public static void click(ServerPlayer p, AbstractContainerMenu menu, int index, ClickType type, Runnable action) {
        ensure(p);
        var entry = WardLedger.get(p).worn.get(p.getUUID());
        boolean removableClick = type == ClickType.PICKUP || type == ClickType.QUICK_MOVE || type == ClickType.SWAP || type == ClickType.THROW;
        if (entry == null || !removableClick || menu != p.containerMenu || index < 0 || index >= menu.slots.size()
              || !(menu.slots.get(index) instanceof CurioSlot slot) || slot.isCosmetic()
              || !slot.getIdentifier().equals(ThunderWardItem.SLOT) || slot.getSlotIndex() != 0) {
            action.run();
            ensure(p);
            return;
        }
        var transfer = new Transfer(entry);
        TRANSFERS.put(p, transfer);
        try { action.run(); }
        finally {
            try {
                var actual = CuriosApi.getCuriosInventory(p).flatMap(h -> h.getStacksHandler(ThunderWardItem.SLOT));
                boolean stillWorn = actual.isPresent() && actual.get().getStacks().getSlots() > 0
                      && sealed(actual.get().getStacks().getStackInSlot(0), entry)
                      && !actual.get().getStacks().getStackInSlot(0).isEmpty();
                if (!stillWorn && releaseOne(p, menu, transfer)) {
                    WardLedger.get(p).release(p.getUUID());
                    LIVE.remove(entry.live);
                    WardRuntime.clearShield(p);
                }
            } finally {
                TRANSFERS.remove(p);
                ensure(p);
            }
        }
    }
    private static ItemStack released(ServerPlayer p, WardLedger.Entry entry) {
        var stack = entry.restore(p.registryAccess());
        stack.remove(CoreContent.WARD_SEAL);
        return stack;
    }
    private static boolean releaseOne(ServerPlayer p, AbstractContainerMenu menu, Transfer transfer) {
        var entry = transfer.entry;
        if (!menu.getCarried().isEmpty() && sealed(menu.getCarried(), entry)) {
            menu.setCarried(released(p, entry)); return true;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && sealed(stack, entry)) {
                p.getInventory().setItem(i, released(p, entry)); return true;
            }
        }
        for (var drop : transfer.drops) if (!drop.isRemoved() && sealed(drop.getItem(), entry)) {
            drop.setItem(released(p, entry)); return true;
        }
        return false; // A failed/full-inventory move cannot release the recovery record.
    }
    public static boolean rejectDrop(ItemEntity item) {
        var token = item.getItem().get(CoreContent.WARD_SEAL);
        if (token == null) return false;
        for (var transfer : TRANSFERS.values()) if (transfer.entry.token.equals(token)) {
            transfer.drops.add(item); return false;
        }
        // Keep ambiguous/stale items inert, not destroyed: player and world backups may be from different times.
        return false;
    }
    public static boolean pending(ServerPlayer p) {
        return !WardLedger.get(p).worn.containsKey(p.getUUID()) && CuriosApi.getCuriosInventory(p)
              .flatMap(h -> h.getStacksHandler(ThunderWardItem.SLOT)).map(h -> h.getStacks().getSlots() > 0
                    && h.getStacks().getStackInSlot(0).has(CoreContent.WARD_SEAL)).orElse(false);
    }
    /** Operator action: adopt the extant item only when no active/retired identity conflicts. Never mints an item. */
    public static boolean repair(ServerPlayer p) {
        if (ensure(p)) return true;
        var slot = CuriosApi.getCuriosInventory(p).flatMap(h -> h.getStacksHandler(ThunderWardItem.SLOT)).orElse(null);
        if (slot == null || slot.getStacks().getSlots() == 0) return false;
        var stack = slot.getStacks().getStackInSlot(0);
        boolean fromHand = stack.isEmpty();
        if (fromHand) stack = p.getMainHandItem();
        var token = stack.get(CoreContent.WARD_SEAL);
        var ledger = WardLedger.get(p);
        if (!stack.is(CoreContent.WARD) || stack.getCount() != 1 || token == null || ledger.known(token)) return false;
        ledger.worn.put(p.getUUID(), new WardLedger.Entry(stack, p.registryAccess()));
        ledger.setDirty();
        if (fromHand) {
            REPAIRING.put(p, true);
            try {
                slot.getStacks().setStackInSlot(0, ledger.worn.get(p.getUUID()).restore(p.registryAccess()));
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            } finally { REPAIRING.remove(p); }
        }
        return ensure(p);
    }
    /** Operator release returns the registered original; it does not delete equipment or reset player data. */
    public static boolean release(ServerPlayer p) {
        if (!ensure(p)) return false;
        var ledger = WardLedger.get(p); var entry = ledger.worn.get(p.getUUID());
        var stack = released(p, entry);
        REPAIRING.put(p, true);
        try {
            ledger.release(p.getUUID()); LIVE.remove(entry.live);
            CuriosApi.getCuriosInventory(p).flatMap(h -> h.getStacksHandler(ThunderWardItem.SLOT))
                  .ifPresent(h -> h.getStacks().setStackInSlot(0, ItemStack.EMPTY));
            p.getInventory().placeItemBackInInventory(stack);
            WardRuntime.clearShield(p);
            return true;
        } finally { REPAIRING.remove(p); }
    }
    public static void forget(ServerPlayer p) {
        var entry = WardLedger.get(p).worn.get(p.getUUID());
        if (entry != null) { LIVE.remove(entry.live); entry.live = ItemStack.EMPTY; }
        REPAIRING.remove(p);
        TRANSFERS.remove(p);
    }
    private WardCustody() { }
}
