package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Player data is authoritative; a copied pendant can never create a second energy reserve. */
public final class CoreBinding {
    public static final String SLOT = "overload_core";
    public static final String KEY = "overloadcore_binding";
    public static CompoundTag data(Player player) { return player.getPersistentData().getCompound(KEY); }
    public static void save(Player player, CompoundTag tag) { player.getPersistentData().put(KEY, tag); }
    public static boolean bound(Player player) { return data(player).hasUUID("instance"); }
    public static boolean active(Player player) {
        return (player.level().isClientSide ? CorePackets.clientState.getBoolean("bound") : bound(player)) && player.isAlive() && !player.isSpectator();
    }
    public static boolean matches(Player player, ItemStack stack) {
        var item = stack.get(CoreContent.DATA.get()); var state = data(player);
        return stack.is(CoreContent.CORE) && item != null && item.hasUUID("owner") && player.getUUID().equals(item.getUUID("owner"))
              && item.hasUUID("instance") && state.hasUUID("instance") && item.getUUID("instance").equals(state.getUUID("instance"));
    }
    public static boolean hasEmptySlot(Player player) {
        return CuriosApi.getCuriosInventory(player).flatMap(h -> h.getStacksHandler(SLOT))
              .map(h -> h.getStacks().getSlots() > 0 && h.getStacks().getStackInSlot(0).isEmpty()).orElse(false);
    }
    public static boolean bind(ServerPlayer player, ItemStack source) {
        if (bound(player) || !source.is(CoreContent.CORE) || !hasEmptySlot(player)) return false;
        var previousOwner = source.get(CoreContent.DATA.get());
        if (previousOwner != null && previousOwner.hasUUID("owner") && !previousOwner.getUUID("owner").equals(player.getUUID())) return false;
        var state = new CompoundTag(); state.putUUID("instance", UUID.randomUUID()); save(player, state);
        var stack = source.copyWithCount(1); var item = new CompoundTag(); item.putUUID("owner", player.getUUID()); item.putUUID("instance", state.getUUID("instance"));
        stack.set(CoreContent.DATA.get(), item);
        CuriosApi.getCuriosInventory(player).flatMap(h -> h.getStacksHandler(SLOT)).orElseThrow().getStacks().setStackInSlot(0, stack);
        source.shrink(1); player.displayClientMessage(CoreContent.text("bound"), false); CorePackets.sendStatus(player, false); return true;
    }
    public static void restore(ServerPlayer player) {
        if (!bound(player)) return;
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null); if (inventory == null) return;
        var coreSlots = inventory.getStacksHandler(SLOT).orElse(null); if (coreSlots == null || coreSlots.getStacks().getSlots() == 0) return;
        var slots = coreSlots.getStacks();
        if (matches(player, slots.getStackInSlot(0))) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (matches(player, player.getInventory().getItem(i))) player.getInventory().setItem(i, ItemStack.EMPTY);
            for (var entry : inventory.getCurios().entrySet()) for (int i = 0; i < entry.getValue().getStacks().getSlots(); i++)
                if (!(entry.getKey().equals(SLOT) && i == 0) && matches(player, entry.getValue().getStacks().getStackInSlot(i))) entry.getValue().getStacks().setStackInSlot(i, ItemStack.EMPTY);
            return;
        }
        // Recover the existing item from inventory first; only one canonical equipped instance stays active.
        ItemStack recovered = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var candidate = player.getInventory().getItem(i);
            if (matches(player, candidate)) { if (recovered.isEmpty()) recovered = candidate.copyWithCount(1); candidate.shrink(1); }
        }
        if (recovered.isEmpty()) {
            recovered = new ItemStack(CoreContent.CORE.get()); var item = new CompoundTag(); item.putUUID("owner", player.getUUID()); item.putUUID("instance", data(player).getUUID("instance"));
            recovered.set(CoreContent.DATA.get(), item);
        }
        var previous = slots.getStackInSlot(0);
        if (!previous.isEmpty() && !player.getInventory().add(previous.copy())) player.drop(previous.copy(), false);
        slots.setStackInSlot(0, recovered); player.getInventory().setChanged();
    }
    public static void clear(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(h -> {
            var slots = h.getEquippedCurios();
            for (int i = 0; i < slots.getSlots(); i++) if (matches(player, slots.getStackInSlot(i))) slots.setStackInSlot(i, ItemStack.EMPTY);
        });
        player.getPersistentData().remove(KEY); CorePackets.sendStatus(player, false);
    }
    public static void recover(ServerPlayer player, long extraJoules) {
        if (!active(player) || extraJoules <= 0) return;
        var tag = data(player); long old = Math.max(0, tag.getLong("energy"));
        long capacity = EnergyUnit.FORGE_ENERGY.convertFrom(CoreConfig.BUFFER_FE.get().longValue());
        long quarter = extraJoules / 4; int remainder = (int) (extraJoules % 4) + Math.clamp(tag.getInt("recovery_remainder"), 0, 3);
        long add = quarter + remainder / 4;
        tag.putLong("energy", old + Math.min(add, Math.max(0, capacity - old))); tag.putInt("recovery_remainder", remainder % 4); save(player, tag);
    }
    public static void charge(ServerPlayer player) {
        var tag = data(player); long stored = Math.max(0, tag.getLong("energy")); if (stored == 0) return;
        long remaining = Math.min(stored, EnergyUnit.FORGE_ENERGY.convertFrom(CoreConfig.CHARGE_FE.get().longValue()));
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            var stack = player.getInventory().getItem(i);
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!id.getNamespace().equals("mekanism") || !(id.getPath().equals("meka_tool") || id.getPath().startsWith("mekasuit_"))) continue;
            var handler = mekanism.common.integration.energy.EnergyCompatUtils.getStrictEnergyHandler(stack);
            if (handler == null) continue;
            long accepted = Math.clamp(remaining - handler.insertEnergy(remaining, mekanism.api.Action.EXECUTE), 0, remaining);
            stored -= accepted; remaining -= accepted;
        }
        tag.putLong("energy", stored); save(player, tag);
    }
    private CoreBinding() { }
}
