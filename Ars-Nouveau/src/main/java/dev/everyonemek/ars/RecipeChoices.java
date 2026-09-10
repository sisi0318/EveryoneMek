package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantmentRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/** The same supported recipes used by server processing, with independent display stacks. */
public final class RecipeChoices {
    public record Choice(ResourceLocation id, ItemStack icon, Component name, Ingredient input,
                         List<Ingredient> materials, int source, int experience, int tier) { }

    public static List<Choice> available(Level level, MachineKind kind, int mode) {
        var choices = new ArrayList<Choice>();
        if (level == null) return choices;
        switch (kind) {
            case IMBUEMENT_CHAMBER -> RecipeAdapter.imbuements(level).forEach(h -> {
                var r = h.value();
                add(choices, h.id(), r.output.copy(), null, r.input, r.pedestalItems, r.source, 0, 0);
            });
            case ENCHANTING_APPARATUS -> RecipeAdapter.apparatus(level, mode).forEach(h -> {
                var r = h.value();
                if (r instanceof EnchantmentRecipe enchantment) {
                    var icon = enchantment.assemble(new ApparatusRecipeInput(new ItemStack(Items.BOOK), List.of(), null), level.registryAccess());
                    add(choices, h.id(), icon, Enchantment.getFullname(enchantment.holderFor(level), enchantment.enchantLevel),
                          Ingredient.of(Items.BOOK), r.pedestalItems(), r.sourceCost(), 0, 0);
                } else add(choices, h.id(), r.getResultItem(level.registryAccess()).copy(), null,
                      r.reagent(), r.pedestalItems(), r.sourceCost(), 0, 0);
            });
            case MAGIC_CRUSHER -> AdvancedRecipes.crushes(level).forEach(h -> {
                var r = h.value();
                r.outputs().stream().filter(o -> !o.stack().isEmpty()).findFirst().ifPresent(o ->
                      add(choices, h.id(), o.stack().copy(), null, r.input(), List.of(), 0, 0, 0));
            });
            case GLYPH_SCRIBE -> AdvancedRecipes.glyphs(level).forEach(h -> {
                var r = h.value();
                add(choices, h.id(), r.output.copy(), null, Ingredient.EMPTY, r.inputs, 0, r.exp,
                      r.getSpellPart().getConfigTier().value);
            });
            default -> { }
        }
        return List.copyOf(choices);
    }

    private static void add(List<Choice> choices, ResourceLocation id, ItemStack icon, Component name,
          Ingredient input, List<Ingredient> materials, int source, int experience, int tier) {
        if (!icon.isEmpty() && source >= 0 && experience >= 0)
            choices.add(new Choice(id, icon, name == null ? icon.getHoverName() : name,
                  input, List.copyOf(materials), source, experience, tier));
    }

    private RecipeChoices() { }
}
