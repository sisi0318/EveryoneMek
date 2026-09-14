package dev.everyonemek.botania;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Uses an addon component, so removing the optional AE2 integration never erases stored mana. */
public final class ManaStorageItem extends Item {
    private final ManaCellTier tier;
    public ManaStorageItem(ManaCellTier tier) { super(new Properties().stacksTo(1)); this.tier = tier; }
    public static Item preferredCell(ManaCellTier tier) {
        return AppliedBotanics.loaded() ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
              net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("appbot", "mana_cell_" + tier.kilobytes + "k")) : Content.MANA_CELLS.get(tier).get();
    }
    public static boolean isCell(ItemStack stack) { return stack.getItem() instanceof ManaStorageItem item && item.tier != null; }
    public static long capacity(ItemStack stack) { return isCell(stack) ? ((ManaStorageItem) stack.getItem()).tier.capacity : 0; }
    public static double idleDrain(ItemStack stack) { return isCell(stack) ? ((ManaStorageItem) stack.getItem()).tier.idleDrain : 0; }
    public static long stored(ItemStack stack) {
        // A capacity reduction must never hide or erase mana from an existing cell.
        return Math.max(0, stack.getOrDefault(Content.STORED_MANA.get(), 0L));
    }
    public static void store(ItemStack stack, long amount) { stack.set(Content.STORED_MANA.get(), Math.max(0, amount)); }
    public static ItemStack recovery(long amount) { var stack = new ItemStack(Content.MANA_PACKET.get()); store(stack, amount); return stack; }
    public record ManaView(ItemStack stack) implements vazkii.botania.api.mana.ManaItem {
        @Override public int getMana() { return (int) Math.min(Integer.MAX_VALUE, stored(stack)); }
        @Override public int getMaxMana() { return isCell(stack) ? (int) Math.min(Integer.MAX_VALUE, Math.max(capacity(stack), stored(stack))) : getMana(); }
        @Override public void addMana(int amount) {
            if (stack.isEmpty() || amount > 0 && !isCell(stack)) return;
            long current = stored(stack);
            long next = amount > 0 ? current + Math.min(amount, Math.max(0, capacity(stack) - current)) : Math.max(0, current + amount);
            store(stack, next); if (!isCell(stack) && next == 0) stack.shrink(1);
        }
        @Override public boolean canReceiveManaFromPool(net.minecraft.world.level.block.entity.BlockEntity pool) { return isCell(stack); }
        @Override public boolean canDrainManaToPool(net.minecraft.world.level.block.entity.BlockEntity pool) { return true; }
        @Override public boolean acceptDispatchedManaFromItem(ItemStack other) { return false; }
        @Override public boolean refuseRequestedManaFromItem(ItemStack other) { return true; }
        @Override public boolean canSendRequestedManaToItem(ItemStack other) { return false; }
        @Override public boolean isNoExport() { return false; }
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tier != null ? "tooltip.botanicalmekanism.mana_cell" : "tooltip.botanicalmekanism.mana_packet", stored(stack), capacity(stack)));
        if (tier != null && stored(stack) > capacity(stack)) lines.add(Component.translatable("tooltip.botanicalmekanism.mana_cell_overfull"));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (tier != null) return super.useOn(context);
        var level = context.getLevel(); var pos = context.getClickedPos(); var player = context.getPlayer();
        if (player == null || !mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(player, level, pos, level.getBlockEntity(pos))) return InteractionResult.FAIL;
        var access = ManaAccess.at(level, pos, context.getClickedFace()); if (access == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            var stack = context.getItemInHand(); long remaining = stored(stack) - access.insert(stored(stack), false);
            if (remaining == 0) stack.shrink(1); else store(stack, remaining);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
