package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantmentRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import com.hollingsworth.arsnouveau.common.datagen.ItemTagProvider;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

public final class RecipeAdapter {
    public record Plan(ResourceLocation id, int[] consume, List<ItemStack> outputs, int source, int ticks, CompoundTag signature) { }

    public static List<RecipeHolder<ImbuementRecipe>> imbuements(Level level) {
        return level.getRecipeManager().getAllRecipesFor(RecipeRegistry.IMBUEMENT_TYPE.get()).stream()
              .filter(h -> h.value().getClass() == ImbuementRecipe.class)
              .sorted(Comparator.comparing(h -> h.id().toString())).toList();
    }

    public static List<RecipeHolder<? extends EnchantingApparatusRecipe>> apparatus(Level level, int mode) {
        List<RecipeHolder<? extends EnchantingApparatusRecipe>> recipes = new ArrayList<>();
        if (mode == 0) recipes.addAll(level.getRecipeManager().getAllRecipesFor(RecipeRegistry.APPARATUS_TYPE.get()).stream()
              .filter(h -> h.value().getClass() == EnchantingApparatusRecipe.class).toList());
        else recipes.addAll(level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ENCHANTMENT_TYPE.get()).stream()
              .filter(h -> h.value().getClass() == EnchantmentRecipe.class).toList());
        recipes.sort(Comparator.comparing(h -> h.id().toString()));
        return recipes;
    }

    public static boolean accepts(Level level, MachineKind kind, int slot, ItemStack stack) {
        if (level == null) return true;
        if (kind == MachineKind.IMBUEMENT_CHAMBER)
            return imbuements(level).stream().anyMatch(h -> slot == 0 ? h.value().input.test(stack)
                  : h.value().pedestalItems.stream().anyMatch(i -> i.test(stack)));
        if (kind == MachineKind.ENCHANTING_APPARATUS) {
            // Enchantable targets vary with their current components. The running recipe validates them.
            if (slot == 0) return true;
            for (int mode = 0; mode <= 1; mode++)
                if (apparatus(level, mode).stream().anyMatch(h -> h.value().pedestalItems().stream().anyMatch(i -> i.test(stack)))) return true;
        }
        return false;
    }

    public static boolean isSupportedRecipe(SourceMachine machine, ResourceLocation id) {
        if (machine.kind() == MachineKind.IMBUEMENT_CHAMBER)
            return imbuements(machine.getLevel()).stream().anyMatch(h -> h.id().equals(id));
        return apparatus(machine.getLevel(), machine.mode()).stream().anyMatch(h -> h.id().equals(id));
    }

    private static boolean allowed(SourceMachine machine, ResourceLocation id) {
        return machine.recipeLock().isEmpty() || id.toString().equals(machine.recipeLock());
    }

    private static int[] match(SourceMachine machine, List<Ingredient> ingredients) {
        // Bound data-pack work independently of stack sizes and avoid exponential tag matching.
        if (ingredients.size() > 64) return null;
        boolean[][] accepts = new boolean[ingredients.size()][8];
        int[] available = new int[8], required = new int[ingredients.size()];
        Arrays.fill(required, 1);
        for (int s = 0; s < 8; s++) {
            ItemStack stack = machine.inputs.get(s + 1).getStack();
            available[s] = stack.getCount();
            for (int i = 0; i < ingredients.size(); i++) accepts[i][s] = ingredients.get(i).test(stack);
        }
        return IngredientAssignment.matchQuantities(accepts, available, required);
    }

    private static List<ItemStack> pedestals(SourceMachine machine, int[] used) {
        var stacks = new ArrayList<ItemStack>();
        for (int s = 0; s < used.length; s++)
            for (int i = 0; i < used[s]; i++) stacks.add(machine.inputs.get(s + 1).getStack().copyWithCount(1));
        return stacks;
    }

    public static Plan find(SourceMachine machine) {
        ItemStack center = machine.inputs.getFirst().getStack();
        if (center.isEmpty()) return null;
        if (machine.kind() == MachineKind.IMBUEMENT_CHAMBER) {
            for (var holder : imbuements(machine.getLevel())) {
                ImbuementRecipe recipe = holder.value();
                if (!allowed(machine, holder.id()) || !recipe.input.test(center) || recipe.source < 0) continue;
                int[] used = match(machine, recipe.pedestalItems);
                if (used == null || recipe.output.isEmpty()) continue;
                int[] consume = new int[machine.inputs.size()];
                consume[0] = 1;
                return plan(machine, holder.id(), consume, List.of(recipe.output.copy()), recipe.source,
                      MachineConfig.IMBUEMENT_TICKS.get(), used);
            }
        } else {
            for (var holder : apparatus(machine.getLevel(), machine.mode())) {
                EnchantingApparatusRecipe recipe = holder.value();
                if (!allowed(machine, holder.id()) || recipe.sourceCost() < 0) continue;
                int[] used = match(machine, recipe.pedestalItems());
                if (used == null) continue;
                var input = new ApparatusRecipeInput(center.copyWithCount(1), pedestals(machine, used), null);
                if (!recipe.matches(input, machine.getLevel(), null)) continue;
                // Ars assembly is allowed to mutate inputs, so previews only receive independent copies.
                ItemStack result = recipe.assemble(input, machine.getLevel().registryAccess());
                if (result.isEmpty()) continue;
                int[] consume = new int[machine.inputs.size()];
                consume[0] = 1;
                List<ItemStack> outputs = new ArrayList<>();
                outputs.add(result.copy());
                for (int s = 0; s < used.length; s++) {
                    ItemStack stack = machine.inputs.get(s + 1).getStack();
                    if (used[s] == 0 || stack.is(ItemTagProvider.APPARATUS_PRESERVES)) continue;
                    consume[s + 1] = used[s];
                    ItemStack remainder = stack.copyWithCount(1).getCraftingRemainingItem();
                    if (!remainder.isEmpty()) outputs.add(remainder.copyWithCount(remainder.getCount() * used[s]));
                }
                return plan(machine, holder.id(), consume, outputs, recipe.sourceCost(), MachineConfig.APPARATUS_TICKS.get(), used);
            }
        }
        return null;
    }

    private static Plan plan(SourceMachine machine, ResourceLocation id, int[] consume, List<ItemStack> outputs,
          int source, int ticks, int[] used) {
        var signature = new CompoundTag();
        signature.putString("recipe", id.toString());
        signature.putIntArray("consume", consume);
        signature.putIntArray("pedestals", used);
        signature.putInt("source", source);
        signature.putInt("ticks", ticks);
        signature.putInt("mode", machine.mode());
        ListTag inputs = new ListTag(), products = new ListTag();
        for (var slot : machine.inputs) inputs.add(slot.isEmpty() ? new CompoundTag()
              : slot.getStack().copyWithCount(1).save(machine.getLevel().registryAccess()));
        for (ItemStack output : outputs) products.add(output.save(machine.getLevel().registryAccess()));
        signature.put("inputs", inputs);
        signature.put("outputs", products);
        return new Plan(id, consume, outputs, source, ticks, signature);
    }

    public static int missingStatus(SourceMachine machine) {
        if (!machine.recipeLock().isEmpty() && !isSupportedRecipe(machine, ResourceLocation.parse(machine.recipeLock()))) return SourceMachine.BAD_LOCK;
        ItemStack center = machine.inputs.getFirst().getStack();
        if (center.isEmpty()) return SourceMachine.MISSING_INPUT;
        if (machine.kind() == MachineKind.IMBUEMENT_CHAMBER) {
            boolean centerMatches = imbuements(machine.getLevel()).stream()
                  .anyMatch(h -> allowed(machine, h.id()) && h.value().input.test(center));
            return centerMatches ? SourceMachine.MISSING_MATERIALS : SourceMachine.NO_RECIPE;
        }
        for (var h : apparatus(machine.getLevel(), machine.mode())) {
            if (allowed(machine, h.id()) && h.value().doesReagentMatch(
                  new ApparatusRecipeInput(center.copyWithCount(1), List.of(), null), machine.getLevel(), null)) return SourceMachine.MISSING_MATERIALS;
        }
        return SourceMachine.NO_RECIPE;
    }

    private RecipeAdapter() { }
}
