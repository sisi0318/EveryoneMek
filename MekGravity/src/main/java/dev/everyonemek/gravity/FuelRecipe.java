package dev.everyonemek.gravity;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public record FuelRecipe(Ingredient ingredient,int count,long energy) implements Recipe<SingleRecipeInput> {
    public FuelRecipe {if(count<1||count>64||energy<1||energy>1_000_000_000_000_000L)throw new IllegalArgumentException("Invalid matter fuel");}
    public static FuelRecipe find(Level level,ItemStack stack){if(level==null||stack.isEmpty())return null;var recipes=level.getRecipeManager().getAllRecipesFor(Content.FUEL_TYPE.get());for(var holder:recipes)if(holder.value().ingredient.test(stack))return holder.value();return null;}
    @Override public boolean matches(SingleRecipeInput i,Level l){return ingredient.test(i.item())&&i.item().getCount()>=count;}
    @Override public ItemStack assemble(SingleRecipeInput i,HolderLookup.Provider r){return ItemStack.EMPTY;}
    @Override public ItemStack getResultItem(HolderLookup.Provider r){return ItemStack.EMPTY;}
    @Override public boolean canCraftInDimensions(int w,int h){return false;}
    @Override public boolean isSpecial(){return true;}
    @Override public RecipeType<?> getType(){return Content.FUEL_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer(){return Content.FUEL_SERIALIZER.get();}
    @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,ingredient);}
    public static final class Serializer implements RecipeSerializer<FuelRecipe> {
        private static final MapCodec<FuelRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FuelRecipe::ingredient),Codec.intRange(1,64).optionalFieldOf("count",1).forGetter(FuelRecipe::count),Codec.LONG.validate(v->v>0&&v<=1_000_000_000_000_000L?DataResult.success(v):DataResult.error(()->"Fuel energy out of range")).fieldOf("energy").forGetter(FuelRecipe::energy)).apply(i,FuelRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,FuelRecipe> STREAM=StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC,FuelRecipe::ingredient,ByteBufCodecs.VAR_INT,FuelRecipe::count,ByteBufCodecs.VAR_LONG,FuelRecipe::energy,FuelRecipe::new);
        public MapCodec<FuelRecipe> codec(){return CODEC;}public StreamCodec<RegistryFriendlyByteBuf,FuelRecipe> streamCodec(){return STREAM;}
    }
}
