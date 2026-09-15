package dev.everyonemek.botania;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import vazkii.botania.api.recipe.ProcessingRecipeInput;

/** Fixed datapack recipes and native formula recipes use the same machine executor. */
public record GreenhouseRecipe(Ingredient flower, String formula, List<Ingredient> materials,
      FluidStack fluid, int mana, int ticks, int cooldown) implements Recipe<ProcessingRecipeInput> {
    public GreenhouseRecipe {
        materials = List.copyOf(materials); fluid = fluid.copy();
        if ((!GreenhouseRules.isFixed(formula) && GreenhouseRules.get(formula) == null) || materials.size() > 16 || mana < 0 || mana > ManaMachine.MANA_CAPACITY
              || ticks < 1 || ticks > 2_000_000 || cooldown < 0 || cooldown > 2_000_000
              || fluid.getAmount() > GreenhouseWork.FLUID_CAPACITY
              || GreenhouseRules.isFixed(formula) && (mana == 0 || materials.isEmpty() && fluid.isEmpty()))
            throw new IllegalArgumentException("Invalid mana greenhouse recipe");
    }
    public boolean fixed() { return GreenhouseRules.isFixed(formula); }
    public GreenhouseFlowerRule rule() { return GreenhouseRules.get(formula); }
    @Override public boolean matches(ProcessingRecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(ProcessingRecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeType<?> getType() { return GreenhouseWork.TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return GreenhouseWork.SERIALIZER.get(); }
    public static final class Serializer implements RecipeSerializer<GreenhouseRecipe> {
        private static final MapCodec<GreenhouseRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              Ingredient.CODEC_NONEMPTY.fieldOf("flower").forGetter(GreenhouseRecipe::flower),
              Codec.STRING.optionalFieldOf("formula", "fixed").forGetter(GreenhouseRecipe::formula),
              Ingredient.CODEC_NONEMPTY.listOf(0, 16).optionalFieldOf("ingredients", List.of()).forGetter(GreenhouseRecipe::materials),
              FluidStack.CODEC.optionalFieldOf("fluid", FluidStack.EMPTY).forGetter(GreenhouseRecipe::fluid),
              Codec.intRange(0, ManaMachine.MANA_CAPACITY).optionalFieldOf("mana", 0).forGetter(GreenhouseRecipe::mana),
              Codec.intRange(1, 2_000_000).optionalFieldOf("ticks", 1).forGetter(GreenhouseRecipe::ticks),
              Codec.intRange(0, 2_000_000).optionalFieldOf("cooldown", 0).forGetter(GreenhouseRecipe::cooldown)
        ).apply(i, GreenhouseRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, GreenhouseRecipe> STREAM = new StreamCodec<>() {
            @Override public GreenhouseRecipe decode(RegistryFriendlyByteBuf buf) {
                return new GreenhouseRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), buf.readUtf(GreenhouseRules.MAX_ID_LENGTH),
                      Ingredient.CONTENTS_STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list(16)).decode(buf), FluidStack.OPTIONAL_STREAM_CODEC.decode(buf),
                      buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            }
            @Override public void encode(RegistryFriendlyByteBuf buf, GreenhouseRecipe r) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.flower); buf.writeUtf(r.formula, GreenhouseRules.MAX_ID_LENGTH);
                Ingredient.CONTENTS_STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list(16)).encode(buf, r.materials);
                FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, r.fluid); buf.writeVarInt(r.mana); buf.writeVarInt(r.ticks); buf.writeVarInt(r.cooldown);
            }
        };
        @Override public MapCodec<GreenhouseRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GreenhouseRecipe> streamCodec() { return STREAM; }
    }
}
