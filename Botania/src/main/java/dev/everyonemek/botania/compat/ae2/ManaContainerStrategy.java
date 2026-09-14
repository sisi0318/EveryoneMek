package dev.everyonemek.botania.compat.ae2;

import java.util.function.Supplier;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import dev.everyonemek.botania.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

final class ManaContainerStrategy implements ContainerItemStrategy<ManaKey, ManaContainerStrategy.Context> {
    record Context(Supplier<ItemStack> current, ItemStack original, Runnable changed) {
        boolean valid() { return current.get() == original && original.getCount() == 1; }
    }
    private static vazkii.botania.api.mana.ManaItem manaItem(ItemStack stack) { return vazkii.botania.api.mana.ManaItem.LOOKUP.find(stack); }
    private boolean accepts(ItemStack stack) { return stack.getCount() == 1 && manaItem(stack) != null; }
    private static boolean local(ItemStack stack) { return ManaStorageItem.isCell(stack) || stack.is(Content.MANA_PACKET.get()); }
    @Override public GenericStack getContainedStack(ItemStack stack) {
        return accepts(stack) ? new GenericStack(ManaKeys.current(), local(stack) ? ManaStorageItem.stored(stack) : Math.max(0, manaItem(stack).getMana())) : null;
    }
    @Override public Context findCarriedContext(Player player, AbstractContainerMenu menu) {
        return accepts(menu.getCarried()) ? new Context(menu::getCarried, menu.getCarried(), menu::broadcastChanges) : null;
    }
    @Override public Context findPlayerSlotContext(Player player, int slot) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize() || !accepts(player.getInventory().getItem(slot))) return null;
        return new Context(() -> player.getInventory().getItem(slot), player.getInventory().getItem(slot), player.getInventory()::setChanged);
    }
    @Override public long extract(Context context, ManaKey key, long amount, Actionable mode) {
        if (!context.valid() || amount <= 0) return 0;
        var stack = context.original;
        if (!local(stack)) {
            var item = manaItem(stack); if (item == null) return 0;
            int stored = Math.max(0, item.getMana()); int moved = (int) Math.min(amount, stored);
            if (mode == Actionable.MODULATE && moved > 0) { item.addMana(-moved); context.changed.run(); return Math.clamp((long) stored - item.getMana(), 0, moved); }
            return moved;
        }
        long moved = Math.min(amount, ManaStorageItem.stored(stack));
        if (mode == Actionable.MODULATE && moved > 0) {
            ManaStorageItem.store(stack, ManaStorageItem.stored(stack) - moved);
            if (stack.is(Content.MANA_PACKET.get()) && ManaStorageItem.stored(stack) == 0) stack.shrink(1);
            context.changed.run();
        }
        return moved;
    }
    @Override public long insert(Context context, ManaKey key, long amount, Actionable mode) {
        if (!context.valid() || amount <= 0) return 0;
        if (!local(context.original)) {
            var item = manaItem(context.original); if (item == null) return 0;
            int stored = Math.max(0, item.getMana()); int moved = (int) Math.min(amount, Math.max(0L, (long) item.getMaxMana() - stored));
            if (mode == Actionable.MODULATE && moved > 0) { item.addMana(moved); context.changed.run(); return Math.clamp((long) item.getMana() - stored, 0, moved); }
            return moved;
        }
        if (!ManaStorageItem.isCell(context.original)) return 0;
        long moved = Math.min(amount, Math.max(0, ManaStorageItem.capacity(context.original) - ManaStorageItem.stored(context.original)));
        if (mode == Actionable.MODULATE && moved > 0) { ManaStorageItem.store(context.original, ManaStorageItem.stored(context.original) + moved); context.changed.run(); }
        return moved;
    }
    @Override public GenericStack getExtractableContent(Context context) { return context.valid() ? getContainedStack(context.original) : null; }
    @Override public void playFillSound(Player player, ManaKey key) { player.playNotifySound(vazkii.botania.common.handler.BotaniaSounds.MANA_POOL_CRAFT, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1); }
    @Override public void playEmptySound(Player player, ManaKey key) { player.playNotifySound(vazkii.botania.common.handler.BotaniaSounds.BLACK_LOTUS, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1); }
}
