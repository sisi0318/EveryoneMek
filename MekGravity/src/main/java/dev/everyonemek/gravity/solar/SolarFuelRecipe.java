package dev.everyonemek.gravity.solar;
import net.minecraft.nbt.CompoundTag;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public record SolarFuelRecipe(Ingredient ingredient,int count,long energy) implements Recipe<SingleRecipeInput> {
    public SolarFuelRecipe {if(count<1||count>64||energy<1||energy>1_000_000_000_000_000_000L)throw new IllegalArgumentException("Invalid matter fuel");}
    public static SolarFuelRecipe find(Level level,ItemStack stack){if(level==null||stack.isEmpty())return null;var recipes=level.getRecipeManager().getAllRecipesFor(SolarContent.FUEL_TYPE.get());for(var holder:recipes)if(holder.value().ingredient.test(stack))return holder.value();return null;}
    public static boolean validReserve(CompoundTag t){return t.getLong("remaining")>0&&t.getLong("remaining")<=t.getLong("total")&&t.getLong("total")<=1_000_000_000_000_000_000L;}
    public record Fuel(int count,long remaining,long total){}
    public static Fuel fuel(Level level,ItemStack stack){
        if(stack.is(SolarContent.CAPSULE.get())){var t=stack.get(SolarContent.FUEL_DATA.get());return t!=null&&validReserve(t)?new Fuel(1,t.getLong("remaining"),t.getLong("total")):null;}
        var recipe=find(level,stack);return recipe==null?null:new Fuel(recipe.count(),recipe.energy(),recipe.energy());
    }
    @Override public boolean matches(SingleRecipeInput i,Level l){return ingredient.test(i.item())&&i.item().getCount()>=count;}
    @Override public ItemStack assemble(SingleRecipeInput i,HolderLookup.Provider r){return ItemStack.EMPTY;}
    @Override public ItemStack getResultItem(HolderLookup.Provider r){return ItemStack.EMPTY;}
    @Override public boolean canCraftInDimensions(int w,int h){return false;}
    @Override public boolean isSpecial(){return true;}
    @Override public RecipeType<?> getType(){return SolarContent.FUEL_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer(){return SolarContent.SERIALIZER.get();}
    @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,ingredient);}
    public static final class Serializer implements RecipeSerializer<SolarFuelRecipe> {
        private static final MapCodec<SolarFuelRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SolarFuelRecipe::ingredient),Codec.intRange(1,64).optionalFieldOf("count",1).forGetter(SolarFuelRecipe::count),Codec.LONG.validate(v->v>0&&v<=1_000_000_000_000_000_000L?DataResult.success(v):DataResult.error(()->"Fuel energy out of range")).fieldOf("energy").forGetter(SolarFuelRecipe::energy)).apply(i,SolarFuelRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,SolarFuelRecipe> STREAM=StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC,SolarFuelRecipe::ingredient,ByteBufCodecs.VAR_INT,SolarFuelRecipe::count,ByteBufCodecs.VAR_LONG,SolarFuelRecipe::energy,SolarFuelRecipe::new);
        public MapCodec<SolarFuelRecipe> codec(){return CODEC;}public StreamCodec<RegistryFriendlyByteBuf,SolarFuelRecipe> streamCodec(){return STREAM;}
    }
}
