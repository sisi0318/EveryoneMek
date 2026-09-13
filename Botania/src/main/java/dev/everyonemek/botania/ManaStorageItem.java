package dev.everyonemek.botania;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Uses an addon component, so removing the optional AE2 integration never erases stored mana. */
public final class ManaStorageItem extends Item {
    public static final long CELL_CAPACITY = 1_000_000;
    private final boolean cell;
    public ManaStorageItem(boolean cell) { super(new Properties().stacksTo(1)); this.cell = cell; }
    public static long stored(ItemStack stack) {
        long amount = Math.max(0, stack.getOrDefault(Content.STORED_MANA.get(), 0L));
        return stack.is(Content.MANA_CELL.get()) ? Math.min(CELL_CAPACITY, amount) : amount;
    }
    public static void store(ItemStack stack, long amount) { stack.set(Content.STORED_MANA.get(), Math.max(0, amount)); }
    public static ItemStack recovery(long amount) { var stack = new ItemStack(Content.MANA_PACKET.get()); store(stack, amount); return stack; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(cell ? "tooltip.botanicalmekanism.mana_cell" : "tooltip.botanicalmekanism.mana_packet", stored(stack), CELL_CAPACITY));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (cell) return super.useOn(context);
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
