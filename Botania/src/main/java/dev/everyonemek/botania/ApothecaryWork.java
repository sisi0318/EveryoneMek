package dev.everyonemek.botania;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import vazkii.botania.api.recipe.PetalApothecaryRecipe;
import vazkii.botania.api.recipe.ProcessingRecipeInput;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;

public final class ApothecaryWork {
    public record Option(ResourceLocation id, Recipe<ProcessingRecipeInput> recipe, Ingredient reagent, int ticks, int fePerTick) { }
    public record Plan(Option option, int[] consume, List<ItemStack> products, CompoundTag signature) { }
    public static boolean acceptsMaterial(Level level, ItemStack stack) {
        return options(level).stream().anyMatch(o -> o.recipe().getIngredients().stream().anyMatch(i -> i.test(stack)));
    }
    public static boolean acceptsReagent(Level level, ItemStack stack) {
        return options(level).stream().anyMatch(o -> o.reagent().test(stack));
    }
    public static List<Option> options(Level level) {
        if (level == null) return List.of();
        List<Option> choices = new ArrayList<>();
        for (var recipe : level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.PETAL_APOTHECARY_TYPE))
            choices.add(new Option(recipe.id(), recipe.value(), recipe.value().getReagent(), 100, 50));
        for (var recipe : level.getRecipeManager().getAllRecipesFor(ApothecaryContent.RECIPE_TYPE.get())) {
            MechanicalFlowerRecipe r = recipe.value(); choices.add(new Option(recipe.id(), r, r.reagent(), r.ticks(), r.fePerTick()));
        }
        choices.sort(Comparator.comparing(o -> o.id().toString())); return choices;
    }
    public static int[] assign(List<Ingredient> ingredients, List<ItemStack> stacks) {
        if (stacks.stream().mapToInt(ItemStack::getCount).sum() < ingredients.size()) return null;
        boolean[][] accepts = new boolean[ingredients.size()][stacks.size()];
        int[] required = new int[ingredients.size()], available = new int[stacks.size()]; Arrays.fill(required, 1);
        for (int slot = 0; slot < stacks.size(); slot++) {
            available[slot] = stacks.get(slot).getCount();
            for (int row = 0; row < ingredients.size(); row++) accepts[row][slot] = ingredients.get(row).test(stacks.get(slot));
        }
        for (boolean[] row : accepts) { boolean any = false; for (boolean match : row) any |= match; if (!any) return null; }
        return IngredientAssignment.matchQuantities(accepts, available, required);
    }
    public static Plan find(MechanicalApothecary tile) {
        var stacks = tile.inputs.stream().map(slot -> slot.getStack().copy()).toList();
        if (stacks.stream().allMatch(ItemStack::isEmpty)) return null;
        Plan missingReagent = null;
        if (tile.lastRecipe != null) {
            var holder = tile.getLevel().getRecipeManager().byKey(tile.lastRecipe).orElse(null);
            Option cached = null;
            if (holder != null && holder.value() instanceof PetalApothecaryRecipe recipe)
                cached = new Option(holder.id(), recipe, recipe.getReagent(), 100, 50);
            else if (holder != null && holder.value() instanceof MechanicalFlowerRecipe recipe)
                cached = new Option(holder.id(), recipe, recipe.reagent(), recipe.ticks(), recipe.fePerTick());
            if (cached != null) {
                Plan plan = plan(tile, cached, stacks);
                if (plan != null && cached.reagent().test(tile.reagent.getStack())) return plan;
                missingReagent = plan;
            }
        }
        for (Option option : options(tile.getLevel())) {
            Plan plan = plan(tile, option, stacks);
            if (plan != null) {
                if (option.reagent().test(tile.reagent.getStack())) { tile.lastRecipe = option.id(); return plan; }
                if (missingReagent == null) missingReagent = plan;
            }
        }
        return missingReagent;
    }
    private static Plan plan(MechanicalApothecary tile, Option option, List<ItemStack> stacks) {
        var ingredients = option.recipe().getIngredients();
        if (ingredients.isEmpty() || ingredients.size() > 16) return null;
        // Stacks may hold multiple batches, but unrelated materials must never be silently
        // ignored: bionic flower petals must not trigger a cheaper native-flower recipe.
        for (ItemStack stack : stacks) if (!stack.isEmpty() && ingredients.stream().noneMatch(i -> i.test(stack))) return null;
        int[] used = assign(ingredients, stacks); if (used == null) return null;
        List<ItemStack> consumed = new ArrayList<>();
        for (int i = 0; i < used.length; i++) for (int n = 0; n < used[i]; n++) consumed.add(stacks.get(i).copyWithCount(1));
        var input = new Input(consumed);
        if (!option.recipe().matches(input, tile.getLevel())) return null;
        var registries = tile.getLevel().registryAccess();
        ItemStack result = option.recipe().assemble(new Input(consumed), registries);
        if (result.isEmpty()) return null;
        List<ItemStack> products = new ArrayList<>(); products.add(result.copy());
        for (ItemStack remainder : option.recipe().getRemainingItems(new Input(consumed))) if (!remainder.isEmpty()) products.add(remainder.copy());
        ItemStack reagent = tile.reagent.getStack().copyWithCount(1);
        if (!reagent.isEmpty()) {
            ItemStack remainder = reagent.getCraftingRemainingItem();
            if (!remainder.isEmpty()) products.add(remainder.copy());
        }
        CompoundTag signature = new CompoundTag(); signature.putString("recipe", option.id().toString());
        signature.putInt("ticks", option.ticks()); signature.putInt("fe_per_tick", option.fePerTick());
        signature.putIntArray("consume", used);
        ListTag items = new ListTag(); for (ItemStack stack : consumed) items.add(stack.saveOptional(registries)); signature.put("inputs", items);
        signature.put("reagent", reagent.saveOptional(registries));
        ListTag outputs = new ListTag(); for (ItemStack stack : products) outputs.add(stack.saveOptional(registries)); signature.put("outputs", outputs);
        return new Plan(option, used, List.copyOf(products), signature);
    }
    /** One item per consumed ingredient, as in the native basin. Caller-owned stacks never escape. */
    public static final class Input implements ProcessingRecipeInput {
        private final List<ItemStack> items;
        private final StackedContents contents = new StackedContents();
        public Input(List<ItemStack> stacks) { items = stacks.stream().map(ItemStack::copy).toList(); items.forEach(contents::accountStack); }
        @Override public ItemStack getItem(int slot) { return items.get(slot); }
        @Override public int size() { return items.size(); }
        @Override public List<ItemStack> getItems() { return items; }
        @Override public StackedContents getStackedContents() { return contents; }
        @Override public ProcessingRecipeInput getSubset(int start, int end) { return new Input(items.subList(start, end)); }
    }
    private ApothecaryWork() { }
}
