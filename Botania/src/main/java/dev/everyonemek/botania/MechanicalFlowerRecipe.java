package dev.everyonemek.botania;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import vazkii.botania.api.recipe.ProcessingRecipeInput;

/** Separate recipe type: a native petal apothecary never queries these recipes. */
public record MechanicalFlowerRecipe(List<Ingredient> materials, Ingredient reagent, ItemStack output, int ticks, int fePerTick)
      implements Recipe<ProcessingRecipeInput> {
    public MechanicalFlowerRecipe {
        materials = List.copyOf(materials);
        if (materials.isEmpty() || materials.size() > 16 || output.isEmpty() || ticks < 1 || ticks > 10000 || fePerTick < 1 || fePerTick > 100000)
            throw new IllegalArgumentException("Invalid mechanical flower recipe");
    }
    @Override public boolean matches(ProcessingRecipeInput input, Level level) {
        return input.size() == materials.size() && ApothecaryWork.assign(materials, input.getItems()) != null;
    }
    @Override public ItemStack assemble(ProcessingRecipeInput input, HolderLookup.Provider registries) { return output.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return output; }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= materials.size(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, materials.toArray(Ingredient[]::new)); }
    @Override public RecipeType<?> getType() { return ApothecaryContent.RECIPE_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return ApothecaryContent.SERIALIZER.get(); }
    @Override public ItemStack getToastSymbol() { return new ItemStack(ApothecaryContent.BLOCK); }
    public static final class Serializer implements RecipeSerializer<MechanicalFlowerRecipe> {
        private static final MapCodec<MechanicalFlowerRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              Ingredient.CODEC_NONEMPTY.listOf(1, 16).fieldOf("ingredients").forGetter(MechanicalFlowerRecipe::materials),
              Ingredient.CODEC_NONEMPTY.fieldOf("reagent").forGetter(MechanicalFlowerRecipe::reagent),
              ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MechanicalFlowerRecipe::output),
              com.mojang.serialization.Codec.intRange(1, 10000).fieldOf("ticks").forGetter(MechanicalFlowerRecipe::ticks),
              com.mojang.serialization.Codec.intRange(1, 100000).fieldOf("fe_per_tick").forGetter(MechanicalFlowerRecipe::fePerTick)
        ).apply(i, MechanicalFlowerRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, MechanicalFlowerRecipe> STREAM = StreamCodec.composite(
              Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), MechanicalFlowerRecipe::materials,
              Ingredient.CONTENTS_STREAM_CODEC, MechanicalFlowerRecipe::reagent,
              ItemStack.STREAM_CODEC, MechanicalFlowerRecipe::output,
              ByteBufCodecs.VAR_INT, MechanicalFlowerRecipe::ticks,
              ByteBufCodecs.VAR_INT, MechanicalFlowerRecipe::fePerTick, MechanicalFlowerRecipe::new);
        @Override public MapCodec<MechanicalFlowerRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MechanicalFlowerRecipe> streamCodec() { return STREAM; }
    }
}
