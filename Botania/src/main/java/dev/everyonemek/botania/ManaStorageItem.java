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
    public static boolean isCell(ItemStack stack) { return stack.getItem() instanceof ManaStorageItem item && item.tier != null; }
    public static long capacity(ItemStack stack) { return isCell(stack) ? ((ManaStorageItem) stack.getItem()).tier.capacity : 0; }
    public static double idleDrain(ItemStack stack) { return isCell(stack) ? ((ManaStorageItem) stack.getItem()).tier.idleDrain : 0; }
    public static long stored(ItemStack stack) {
        long amount = Math.max(0, stack.getOrDefault(Content.STORED_MANA.get(), 0L));
        return isCell(stack) ? Math.min(capacity(stack), amount) : amount;
    }
    public static void store(ItemStack stack, long amount) { stack.set(Content.STORED_MANA.get(), Math.max(0, amount)); }
    public static ItemStack recovery(long amount) { var stack = new ItemStack(Content.MANA_PACKET.get()); store(stack, amount); return stack; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tier != null ? "tooltip.botanicalmekanism.mana_cell" : "tooltip.botanicalmekanism.mana_packet", stored(stack), capacity(stack)));
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
