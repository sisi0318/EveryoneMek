package dev.everyonemek.natures;

import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.blocks.ModBlocks;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.items.ModItems;
import de.ellpeck.naturesaura.recipes.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class RecipeAdapter {
    public record Plan(ResourceLocation id, int[] consume, List<ItemStack> outputs, int ticks, int aura, int batch, CompoundTag signature) { }

    public static boolean accepts(Level level, MachineKind kind, int slot, ItemStack stack) {
        if (level == null) return true;
        return switch (kind) {
            case AURA_GENERATOR, AURA_CONTROLLER -> false;
            case AURA_BOTTLER -> stack.is(ModItems.BOTTLE_TWO_THE_REBOTTLING);
            // Modded animals define their own food predicates; validate against the selected parents at runtime.
            case INDUSTRIAL_BREEDER -> true;
            case ANIMAL_SPAWNER -> recipes(level, ModRecipes.ANIMAL_SPAWNER_TYPE).stream()
                  .anyMatch(h -> h.value().ingredients.stream().anyMatch(i -> i.test(stack)));
            case FOREST_RITUAL -> slot == 9 ? stack.is(ModBlocks.GOLD_POWDER.asItem())
                  : recipes(level, ModRecipes.TREE_RITUAL_TYPE).stream().anyMatch(h -> slot == 8
                        ? h.value().saplingType.test(stack) : h.value().ingredients.stream().anyMatch(i -> i.test(stack)));
            case NATURAL_ALTAR -> recipes(level, ModRecipes.ALTAR_TYPE).stream().anyMatch(h ->
                  (slot == 0 ? h.value().input : h.value().catalyst).test(stack));
            case OFFERING -> recipes(level, ModRecipes.OFFERING_TYPE).stream().anyMatch(h ->
                  (slot == 0 ? h.value().input : h.value().startItem).test(stack));
        };
    }

    private static <R extends ModRecipe> List<RecipeHolder<R>> recipes(Level level, RecipeType<R> type) {
        return level.getRecipeManager().getAllRecipesFor(type).stream()
              .sorted(Comparator.comparing(h -> h.id().toString())).toList();
    }

    public static Plan find(AuraMachine machine, int lockedBatch) {
        Level level = machine.getLevel();
        List<BasicInventorySlot> in = machine.inputs;
        switch (machine.kind()) {
            case AURA_BOTTLER -> {
                if (!in.getFirst().getStack().is(ModItems.BOTTLE_TWO_THE_REBOTTLING)) return null;
                int environment = IAuraChunk.getAuraInArea(level, machine.getBlockPos(), BottlingRules.RANGE);
                ItemStack output = BottlingRules.output(level, environment, machine.bottlingMode(), machine.hasSimulationModule());
                if (output.isEmpty()) return null;
                int cost = output.is(ModItems.VACUUM_BOTTLE) ? 0 : BottlingRules.AURA_PER_BOTTLE;
                Plan result = plan(machine, ResourceLocation.fromNamespaceAndPath(NaturesMekanism.ID, "bottling"),
                      new int[]{1}, output, MachineConfig.BOTTLER_TICKS.get(), cost, 1);
                result.signature().putInt("bottling_mode", machine.bottlingMode().ordinal());
                result.signature().putBoolean("simulated", machine.hasSimulationModule());
                return result;
            }
            case FOREST_RITUAL -> {
                for (var holder : recipes(level, ModRecipes.TREE_RITUAL_TYPE)) {
                    TreeRitualRecipe r = holder.value();
                    if (r.time < 1 || r.ingredients.size() > 8 || r.output.isEmpty() || !r.saplingType.test(in.get(8).getStack())) continue;
                    int gold = machine.hasInfiniteGold() ? 0 : MachineConfig.FOREST_GOLD.get();
                    if (gold > 0 && (!in.get(9).getStack().is(ModBlocks.GOLD_POWDER.asItem()) || in.get(9).getCount() < gold)) continue;
                    boolean[][] accepts = new boolean[r.ingredients.size()][8];
                    int[] counts = new int[8];
                    for (int slot = 0; slot < 8; slot++) {
                        counts[slot] = in.get(slot).getCount();
                        for (int ingredient = 0; ingredient < r.ingredients.size(); ingredient++)
                            accepts[ingredient][slot] = r.ingredients.get(ingredient).test(in.get(slot).getStack());
                    }
                    int[] used = IngredientAssignment.match(accepts, counts);
                    if (used == null) continue;
                    int[] consume = java.util.Arrays.copyOf(used, 10);
                    consume[8] = 1;
                    consume[9] = gold;
                    return plan(machine, holder.id(), consume, r.output.copy(), r.time, 0, 1);
                }
            }
            case NATURAL_ALTAR -> {
                for (var holder : recipes(level, ModRecipes.ALTAR_TYPE)) {
                    AltarRecipe r = holder.value();
                    if (r.time < 1 || r.aura < 0 || r.output.isEmpty() || !r.input.test(in.getFirst().getStack())) continue;
                    if (!r.catalyst.isEmpty() && !r.catalyst.test(in.get(1).getStack())) continue;
                    return plan(machine, holder.id(), new int[]{1, 0}, r.output.copy(), r.time, r.aura, 1);
                }
            }
            case OFFERING -> {
                for (var holder : recipes(level, ModRecipes.OFFERING_TYPE)) {
                    OfferingRecipe r = holder.value();
                    if (r.output.isEmpty() || !r.input.test(in.getFirst().getStack()) || !r.startItem.test(in.get(1).getStack())) continue;
                    int amount = Math.max(1, Helper.getIngredientAmount(r.input));
                    int max = Math.min(16, in.getFirst().getCount() / amount);
                    int batch = lockedBatch > 0 ? lockedBatch : max;
                    if (batch < 1 || batch > max) continue;
                    // One calling spirit starts a batch, matching the original offering table.
                    while (batch > 0) {
                        long count = (long) r.output.getCount() * batch;
                        if (count <= 256) {
                            Plan p = plan(machine, holder.id(), new int[]{amount * batch, 1},
                                  r.output.copyWithCount((int) count), MachineConfig.OFFERING_TICKS.get(), 0, batch);
                            if (machine.canFit(p.outputs(), true) || lockedBatch > 0) return p;
                        }
                        if (lockedBatch > 0) break;
                        batch--;
                    }
                }
            }
            default -> { }
        }
        return null;
    }

    private static Plan plan(AuraMachine machine, ResourceLocation id, int[] consume, ItemStack output, int ticks, int aura, int batch) {
        CompoundTag signature = new CompoundTag();
        signature.putString("recipe", id.toString());
        signature.putInt("ticks", ticks);
        signature.putInt("aura", aura);
        signature.putInt("batch", batch);
        signature.putIntArray("consume", consume);
        signature.put("output", output.save(machine.getLevel().registryAccess()));
        ListTag inputs = new ListTag();
        for (BasicInventorySlot slot : machine.inputs)
            inputs.add(slot.getStack().copyWithCount(1).saveOptional(machine.getLevel().registryAccess()));
        signature.put("inputs", inputs);
        return new Plan(id, consume, List.of(output), Math.min(ticks, 1_000_000), aura, batch, signature);
    }

    private RecipeAdapter() { }
}
