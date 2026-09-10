package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.UpgradeTierResult;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public final class Recipes {
    public record Choice(ResourceLocation id, ItemStack icon, Component name, List<Ingredient> materials, boolean upgrade, int tier) { }
    public static List<Holder.Reference<Ritual>> rituals(Level level) {
        return level.registryAccess().registryOrThrow(FARegistries.RITUAL).holders().toList();
    }
    public static boolean exists(Level level, MachineKind kind, ResourceLocation id) {
        if (id == null || level == null) return false;
        return kind.forge() ? level.registryAccess().registryOrThrow(FARegistries.RITUAL).containsKey(id)
              : level.getRecipeManager().getAllRecipesFor(ClibanoMainBlockEntity.RECIPE_TYPE).stream().anyMatch(r -> r.id().equals(id));
    }
    public static List<Choice> choices(Level level, MachineKind kind) {
        var result = new ArrayList<Choice>();
        if (kind.forge()) for (var holder : rituals(level)) {
            Ritual ritual = holder.value();
            boolean upgrade = ritual.result() instanceof UpgradeTierResult;
            ItemStack input = Arrays.stream(ritual.mainIngredient().getItems()).findFirst().orElse(ItemStack.EMPTY);
            ItemStack icon = upgrade ? new ItemStack(ModBlocks.HEPHAESTUS_FORGE_TIER_1.get()) : ritual.result().getResultItem(input);
            int tier = upgrade ? ((UpgradeTierResult) ritual.result()).resultTier() : 0;
            Component name = upgrade ? Component.translatable("gui.forbiddenmekanism.upgrade_to", tier) : icon.getHoverName();
            result.add(new Choice(holder.key().location(), icon, name, ingredients(ritual), upgrade, tier));
        } else for (var recipe : level.getRecipeManager().getAllRecipesFor(ClibanoMainBlockEntity.RECIPE_TYPE)) {
            ItemStack icon = recipe.value().getResultItem(level.registryAccess()).copy();
            result.add(new Choice(recipe.id(), icon, icon.getHoverName(), recipe.value().getIngredients(), false, 0));
        }
        return result;
    }
    public static List<Ingredient> ingredients(Ritual ritual) {
        var needed = new ArrayList<Ingredient>(); needed.add(ritual.mainIngredient());
        for (var input : ritual.inputs()) {
            if (input.amount() > 8 || needed.size() + input.amount() > 9) return List.of();
            for (int i = 0; i < input.amount(); i++) needed.add(input.ingredient());
        }
        return needed;
    }
    /** Maximum bipartite matching of single-item requirements against stack counts, without mutating items. */
    public static int[] allocate(List<Ingredient> needed, List<ItemStack> stock) {
        if (needed.isEmpty() || needed.size() > 9) return null;
        var units = new ArrayList<Integer>();
        for (int i = 0; i < stock.size(); i++) for (int n = 0; n < Math.min(needed.size(), stock.get(i).getCount()); n++) units.add(i);
        int[] owner = new int[units.size()]; Arrays.fill(owner, -1);
        for (int i = 0; i < needed.size(); i++)
            if (!assign(i, needed, stock, units, owner, new boolean[units.size()])) return null;
        int[] result = new int[needed.size()];
        for (int i = 0; i < owner.length; i++) if (owner[i] >= 0) result[owner[i]] = units.get(i);
        return result;
    }
    private static boolean assign(int requirement, List<Ingredient> needed, List<ItemStack> stock,
          List<Integer> units, int[] owner, boolean[] visited) {
        for (int i = 0; i < units.size(); i++) {
            if (visited[i] || !needed.get(requirement).test(stock.get(units.get(i)))) continue;
            visited[i] = true;
            if (owner[i] == -1 || assign(owner[i], needed, stock, units, owner, visited)) { owner[i] = requirement; return true; }
        }
        return false;
    }
    private Recipes() { }
}
