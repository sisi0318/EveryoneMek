package dev.everyonemek.botania;

import java.util.function.Supplier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import vazkii.botania.common.entity.ManaSparkEntity;
import vazkii.botania.common.item.ManaSparkItem;

/** Run native placement, including its validation, offhand dye and item consumption. */
public final class MechanicalSparkItem extends ManaSparkItem {
    private static final ThreadLocal<ItemStack> PLACING = new ThreadLocal<>();
    public final boolean master;
    public MechanicalSparkItem(boolean master) { super(new Properties().stacksTo(master ? 1 : 64)); this.master = master; }
    public static <T> T placing(ItemStack stack, Supplier<T> action) {
        var old = PLACING.get(); PLACING.set(stack.copyWithCount(1));
        try { return action.get(); } finally { if (old == null) PLACING.remove(); else PLACING.set(old); }
    }
    @Override public InteractionResult useOn(UseOnContext context) { return placing(context.getItemInHand(), () -> super.useOn(context)); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var tile = context.getLevel().getBlockEntity(context.getClickedPos());
        if (tile == null || !SparkMeLink.Holder.anchor.test(tile)) return super.onItemUseFirst(stack, context);
        // The early-use hook returns before GameMode restores the creative hand count.
        int count = stack.getCount();
        try { return useOn(context); }
        finally { if (context.getPlayer() != null && context.getPlayer().isCreative()) stack.setCount(count); }
    }
    public static ManaSparkEntity create(Level level) {
        var stack = PLACING.get();
        if (stack == null) return new ManaSparkEntity(level);
        var spark = new MechanicalSparkEntity(MechanicalSparks.ENTITY.get(), level);
        spark.readItem(stack); return spark;
    }
}
