package dev.everyonemek.forbidden;

import com.mojang.serialization.MapCodec;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.UpgradeTierResult;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import java.util.ArrayList;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** A reference to the native ritual, so data packs remain the source of all nine materials. */
public final class ForgeUpgradeRecipe extends ShapedRecipe {
    private final Holder<Ritual> ritual;
    public ForgeUpgradeRecipe(Holder<Ritual> ritual) {
        super("forbiddenmekanism:forge_tiers", CraftingBookCategory.MISC,
              new ShapedRecipePattern(3, 3, ingredients(ritual.value()), java.util.Optional.empty()), ItemStack.EMPTY);
        this.ritual = ritual;
    }
    public Holder<Ritual> ritual() { return ritual; }
    public int tier() {
        return ritual.value().result() instanceof UpgradeTierResult upgrade && upgrade.resultTier() >= 2 && upgrade.resultTier() <= 5
              ? upgrade.resultTier() : 0;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (tier() == 0 || input.width() != 3 || input.height() != 3 || Recipes.ingredients(ritual.value()).size() != 9) return false;
        var perimeter = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) if (i != 4) perimeter.add(input.getItem(i));
        return ritual.value().checkIngredients(perimeter, input.getItem(4));
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) { return getResultItem(lookup); }
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) {
        return tier() == 0 ? ItemStack.EMPTY : new ItemStack(Content.INSTALLERS.get(tier()).get());
    }
    private static NonNullList<Ingredient> ingredients(Ritual ritual) {
        var nativeIngredients = Recipes.ingredients(ritual);
        var ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        if (nativeIngredients.size() != 9) return ingredients;
        int outer = 1;
        for (int i = 0; i < 9; i++) ingredients.set(i, nativeIngredients.get(i == 4 ? 0 : outer++));
        return ingredients;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width >= 3 && height >= 3; }
    @Override public CraftingBookCategory category() { return CraftingBookCategory.MISC; }
    @Override public RecipeSerializer<?> getSerializer() { return Content.FORGE_UPGRADE_RECIPE.get(); }
    public static final class Serializer implements RecipeSerializer<ForgeUpgradeRecipe> {
        private static final MapCodec<ForgeUpgradeRecipe> CODEC = Ritual.CODEC.fieldOf("ritual").xmap(ForgeUpgradeRecipe::new, ForgeUpgradeRecipe::ritual);
        private static final StreamCodec<RegistryFriendlyByteBuf, ForgeUpgradeRecipe> STREAM =
              ByteBufCodecs.holderRegistry(FARegistries.RITUAL).map(ForgeUpgradeRecipe::new, ForgeUpgradeRecipe::ritual);
        @Override public MapCodec<ForgeUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ForgeUpgradeRecipe> streamCodec() { return STREAM; }
    }
}
