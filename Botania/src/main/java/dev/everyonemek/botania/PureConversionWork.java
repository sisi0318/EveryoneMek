package dev.everyonemek.botania;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import vazkii.botania.api.recipe.PureDaisyRecipe;
import vazkii.botania.api.recipe.StateIngredient;

/** Eight native flower positions represented by one atomic machine batch. */
final class PureConversionWork {
    record Roll(ResourceLocation id, StateIngredient output, int count) { }

    static ManaWork.Plan find(ManaMachine tile, List<ItemStack> inputs, List<RecipeHolder<?>> recipes) {
        var matches = new ArrayList<RecipeHolder<?>>(Collections.nCopies(inputs.size(), null));
        for (int slot = 0; slot < inputs.size(); slot++) {
            var state = ManaWork.itemState(inputs.get(slot)); if (state == null) continue;
            for (var holder : recipes) {
                if (!tile.recipeLock().isEmpty() && !holder.id().toString().equals(tile.recipeLock())) continue;
                var recipe = (PureDaisyRecipe) holder.value();
                if (ManaWork.safeStateRecipe(recipe) && ManaWork.safeOutput(recipe.getOutput()) && recipe.matches(tile.getLevel(), tile.getBlockPos(), state)) {
                    matches.set(slot, holder); break;
                }
            }
        }
        // Visit every occupied slot before taking a second item from a stack.
        int[] used = new int[inputs.size()]; int count = 0;
        for (int round = 0; round < ManaWork.PURE_BATCH_SIZE && count < ManaWork.PURE_BATCH_SIZE; round++) {
            for (int slot = 0; slot < inputs.size() && count < ManaWork.PURE_BATCH_SIZE; slot++) {
                if (matches.get(slot) != null && inputs.get(slot).getCount() > used[slot]) { used[slot]++; count++; }
            }
        }
        if (count == 0) return null;
        var products = new ArrayList<ItemStack>(); var consumed = new ArrayList<ItemStack>(); var rolls = new ArrayList<Roll>();
        ResourceLocation first = null; long ticks = 1;
        for (int slot = 0; slot < inputs.size(); slot++) {
            if (used[slot] == 0) continue;
            var holder = matches.get(slot); var recipe = (PureDaisyRecipe) holder.value();
            if (first == null || holder.id().compareTo(first) < 0) first = holder.id();
            ticks = Math.max(ticks, (long) Math.max(1, recipe.getTime()) * ManaWork.PURE_BATCH_SIZE);
            var states = recipe.getOutput().getDisplayed();
            for (int item = 0; item < used[slot]; item++) {
                consumed.add(inputs.get(slot).copyWithCount(1));
                if (states.size() == 1) products.add(new ItemStack(states.getFirst().getBlock()));
                else rolls.add(new Roll(holder.id(), recipe.getOutput(), 1));
            }
        }
        rolls.sort(Comparator.comparing(Roll::id));
        return new ManaWork.Plan(used, new int[0], Map.of(), true, (int) Math.min(2_000_000L, ticks),
              List.of(new ManaWork.Result(first, 0, 1, products, null, rolls)), consumed, List.of());
    }

    /** Reserve for every possible combination; only recipes with variable outputs need this path. */
    static boolean canOutput(ManaMachine tile, ManaWork.Result result) {
        var merged = tile.mergeOutputs(result.products()); if (merged == null) return false;
        int empty = (int) merged.stream().filter(ItemStack::isEmpty).count();
        if (empty >= result.batchRolls().stream().mapToInt(Roll::count).sum()) return true;
        Map<Item, Integer> choices = new HashMap<>(); int count = 0;
        for (var roll : result.batchRolls()) {
            int mask = ((1 << roll.count()) - 1) << count; count += roll.count();
            for (var state : roll.output().getDisplayed()) choices.merge(state.getBlock().asItem(), mask, (a, b) -> a | b);
        }
        int all = (1 << count) - 1;
        int[] required = new int[all + 1]; Arrays.fill(required, -1); required[0] = 0;
        for (var choice : choices.entrySet()) {
            var sample = new ItemStack(choice.getKey()); int free = 0, limit = sample.getMaxStackSize();
            for (int slot = 0; slot < merged.size(); slot++) {
                var stack = merged.get(slot); int slotLimit = Math.min(sample.getMaxStackSize(), tile.outputs.get(slot).getLimit(sample));
                if (stack.isEmpty()) limit = Math.min(limit, slotLimit);
                else if (ItemStack.isSameItemSameComponents(stack, sample)) free += Math.max(0, slotLimit - stack.getCount());
            }
            if (limit <= 0) return false;
            int[] next = required.clone();
            for (int assigned = 0; assigned <= all; assigned++) if (required[assigned] >= 0) {
                int available = choice.getValue() & ~assigned;
                for (int subset = available; subset != 0; subset = (subset - 1) & available) {
                    int slots = (Math.max(0, Integer.bitCount(subset) - free) + limit - 1) / limit;
                    next[assigned | subset] = Math.max(next[assigned | subset], required[assigned] + slots);
                }
            }
            required = next;
            if (required[all] > empty) return false;
        }
        return required[all] >= 0 && required[all] <= empty;
    }
    private PureConversionWork() { }
}
