package dev.everyonemek.overloadcore;

import java.util.*;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.everyonemek.overloadcore.mixin.CachedRecipeAccess;

/** Scope exists only around a real native recipe operation; global recipe objects stay untouched. */
public final class RecipeHooks {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    private static final Map<Level, IdentityHashMap<Recipe<?>, ResourceLocation>> IDS = new WeakHashMap<>();
    public static final class Context {
        public final BlockEntity tile;
        public final ServerPlayer bearer;
        public final CompoundTag job;
        public final int index;
        public final Context previous;
        public final CachedRecipe<?> recipe;
        public final boolean affected;
        public boolean eligible, paying, worked;
        private Context(BlockEntity tile, CachedRecipe<?> recipe, int index) {
            this.tile = tile; this.recipe = recipe; this.index = index; previous = CURRENT.get();
            bearer = CoreConfig.WORK.get() ? DeviceScope.bearer(tile) : null; affected = bearer != null;
            int ticks = ((CachedRecipeAccess) recipe).overload$ticks();
            String id = recipeId(tile.getLevel(), recipe.getRecipe()).toString();
            job = DeviceScope.data(tile).getCompound("jobs").getCompound(Integer.toString(index)).copy();
            boolean same = job.getString("recipe").equals(id) && job.getInt("ticks") == ticks;
            boolean audited = BonusRecipes.allowed(tile.getLevel(), recipe.getRecipe(), id);
            eligible = (ticks == 0 || same && job.getBoolean("eligible")) && affected && audited;
            job.putBoolean("bonus_recipe", audited);
            if (job.getBoolean("affected") != affected) recipe.unpauseErrors();
            job.putBoolean("affected", affected); job.putString("recipe", id);
        }
    }
    public static Context current() { return CURRENT.get(); }
    public static Context begin(BlockEntity tile, CachedRecipe<?> recipe, int index) {
        if (!DeviceScope.supported(tile)) return null;
        var context = new Context(tile, recipe, index); CURRENT.set(context); return context;
    }
    public static void end(Context context) {
        if (context == null) return;
        try {
            int ticks = ((CachedRecipeAccess) context.recipe).overload$ticks();
            context.job.putInt("ticks", ticks); context.job.putBoolean("eligible", context.eligible && ticks > 0);
            var errors = ((CachedRecipeAccess)context.recipe).overload$errors();
            var energyError = CachedRecipe.OperationTracker.RecipeError.NOT_ENOUGH_ENERGY;
            context.job.putString("issue", errors.contains(energyError) ? "energy"
                  : errors.contains(CachedRecipe.OperationTracker.RecipeError.NOT_ENOUGH_OUTPUT_SPACE) ? "output"
                  : !errors.isEmpty() ? "input" : context.worked ? "working" : "idle");
            var data = DeviceScope.data(context.tile); var jobs = data.getCompound("jobs");
            if (!context.job.equals(jobs.getCompound(Integer.toString(context.index)))) {
                jobs.put(Integer.toString(context.index), context.job); data.put("jobs", jobs); DeviceScope.save(context.tile, data);
            }
        } finally { if (context.previous == null) CURRENT.remove(); else CURRENT.set(context.previous); }
    }
    public static boolean bonus() { var context = current(); return context != null && context.eligible && context.affected; }
    public static ResourceLocation recipeId(Level level, Recipe<?> recipe) {
        var ids = IDS.computeIfAbsent(level, unused -> new IdentityHashMap<>());
        if (!ids.containsKey(recipe)) { ids.clear(); for (var holder : level.getRecipeManager().getRecipes()) ids.put(holder.value(), holder.id()); }
        return ids.getOrDefault(recipe, ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, "unknown"));
    }
    public static <T> IOutputHandler<T> output(IOutputHandler<T> delegate) {
        return new IOutputHandler<>() {
            @SuppressWarnings("unchecked") private T amount(T original) {
                if (!bonus()) return original;
                if (original instanceof ItemStack item) return (T) item.copyWithCount(Math.multiplyExact(item.getCount(), 2));
                if (original instanceof ChemicalStack chemical) return (T) chemical.copyWithAmount(Math.multiplyExact(chemical.getAmount(), 2));
                return original;
            }
            @Override public void handleOutput(T output, int operations) { delegate.handleOutput(amount(output), operations); }
            @Override public void calculateOperationsCanSupport(CachedRecipe.OperationTracker tracker, T output) { delegate.calculateOperationsCanSupport(tracker, amount(output)); }
        };
    }
    public static void paid(long amount) {
        var context = current(); if (context == null) return;
        if (amount <= 0) { context.eligible = false; return; }
        context.worked = true; DeviceTracker.worked(context.tile);
        if (context.affected) CoreBinding.recover(context.bearer, amount / 2); else context.eligible = false;
    }
    private RecipeHooks() { }
}
