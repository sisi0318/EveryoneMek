package dev.everyonemek.botania;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import vazkii.botania.api.recipe.ManaInfusionRecipe;

final class InfusionBatchWork {
    static final int BATCH_SIZE = 8, TICKS = 20;
    private record Match(ResourceLocation id, int mana, ItemStack output) { }

    static ManaWork.Plan find(ManaMachine tile, List<ItemStack> inputs, List<RecipeHolder<?>> recipes) {
        Match[] matches = new Match[inputs.size()];
        for (int slot = 0; slot < inputs.size(); slot++) {
            if (inputs.get(slot).isEmpty()) continue;
            var input = inputs.get(slot).copyWithCount(1);
            for (var holder : recipes) {
                if (!tile.recipeLock().isEmpty() && !holder.id().toString().equals(tile.recipeLock())) continue;
                var recipe = (ManaInfusionRecipe) holder.value();
                if (!recipe.matches(input.copy()) || !ManaWork.catalystMatches(tile, recipe) || !ManaWork.validMana(recipe.getManaToConsume())) continue;
                var output = recipe.getRecipeOutput(tile.getLevel().registryAccess(), input.copy());
                if (!output.isEmpty()) { matches[slot] = new Match(holder.id(), recipe.getManaToConsume(), output.copy()); break; }
            }
        }
        int[] used = new int[inputs.size()]; int count = 0, mana = 0; ResourceLocation first = null;
        var products = new ArrayList<ItemStack>(); var consumed = new ArrayList<ItemStack>();
        for (int round = 0; round < BATCH_SIZE && count < BATCH_SIZE; round++) {
            for (int slot = 0; slot < inputs.size() && count < BATCH_SIZE; slot++) {
                var match = matches[slot];
                if (match == null || used[slot] >= inputs.get(slot).getCount() || match.mana() > ManaMachine.MANA_CAPACITY - mana) continue;
                used[slot]++; count++; mana += match.mana(); products.add(match.output().copy());
                if (first == null || match.id().compareTo(first) < 0) first = match.id();
            }
        }
        if (count == 0) return null;
        for (int slot = 0; slot < used.length; slot++) for (int item = 0; item < used[slot]; item++) consumed.add(inputs.get(slot).copyWithCount(1));
        return new ManaWork.Plan(used, new int[tile.extras.size()], Map.of(), true, TICKS,
              List.of(new ManaWork.Result(first, mana, 1, products, null)), consumed,
              tile.extras.stream().map(slot -> slot.getStack().copyWithCount(1)).toList());
    }
    private InfusionBatchWork() { }
}
