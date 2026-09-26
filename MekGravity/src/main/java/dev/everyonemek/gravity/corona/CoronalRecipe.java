package dev.everyonemek.gravity.corona;
import java.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record CoronalRecipe(List<Input> inputs,ItemStack result,int ticks,long energy,int tier) implements Recipe<CoronalRecipe.Inventory> {
    public record Input(Ingredient ingredient,int count){
        public static final Codec<Input> CODEC=RecordCodecBuilder.create(i->i.group(Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Input::ingredient),Codec.intRange(1,64).fieldOf("count").forGetter(Input::count)).apply(i,Input::new));
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> STREAM=StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC,Input::ingredient,ByteBufCodecs.VAR_INT,Input::count,Input::new);
    }
    public record Inventory(List<ItemStack> items) implements RecipeInput {public ItemStack getItem(int index){return items.get(index);}public int size(){return items.size();}}
    public CoronalRecipe{if(inputs.isEmpty()||inputs.size()>4||result.isEmpty()||result.getCount()>64||ticks<1||ticks>72000||energy<1||energy>1_000_000_000_000L||tier<0||tier>3)throw new IllegalArgumentException("Invalid coronal recipe");inputs=List.copyOf(inputs);result=result.copy();}
    /** Tiny max-flow allocator handles overlapping tags and split stacks without greedy false negatives. */
    public int[] allocate(Inventory inventory,int batches){return allocate(inventory,inputs,batches);}
    public static int[] allocate(Inventory inventory,List<Input> inputs,int batches){
        int slots=inventory.size(),ingredients=inputs.size(),sink=slots+ingredients+1,n=sink+1;int[][] capacity=new int[n][n];int wanted=0;
        for(int i=0;i<slots;i++){capacity[0][i+1]=inventory.getItem(i).getCount();for(int j=0;j<ingredients;j++)if(inputs.get(j).ingredient.test(inventory.getItem(i)))capacity[i+1][slots+j+1]=capacity[0][i+1];}
        for(int j=0;j<ingredients;j++){capacity[slots+j+1][sink]=inputs.get(j).count*batches;wanted+=inputs.get(j).count*batches;}
        int flow=0;while(flow<wanted){int[] previous=new int[n];Arrays.fill(previous,-1);previous[0]=0;var queue=new ArrayDeque<Integer>();queue.add(0);
            while(!queue.isEmpty()&&previous[sink]<0){int a=queue.remove();for(int b=1;b<n;b++)if(capacity[a][b]>0&&previous[b]<0){previous[b]=a;queue.add(b);}}
            if(previous[sink]<0)return null;int amount=wanted-flow;for(int b=sink;b!=0;b=previous[b])amount=Math.min(amount,capacity[previous[b]][b]);
            for(int b=sink;b!=0;b=previous[b]){int a=previous[b];capacity[a][b]-=amount;capacity[b][a]+=amount;}flow+=amount;
        }
        int[] consumed=new int[slots];for(int i=0;i<slots;i++)consumed[i]=inventory.getItem(i).getCount()-capacity[0][i+1];return consumed;
    }
    @Override public boolean matches(Inventory i,Level l){return allocate(i,1)!=null;}
    @Override public ItemStack assemble(Inventory i,HolderLookup.Provider r){return result.copy();}
    @Override public ItemStack getResultItem(HolderLookup.Provider r){return result.copy();}
    @Override public boolean canCraftInDimensions(int w,int h){return true;}
    @Override public boolean isSpecial(){return true;}
    @Override public RecipeType<?> getType(){return CoronalContent.TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer(){return CoronalContent.SERIALIZER.get();}
    @Override public NonNullList<Ingredient> getIngredients(){var result=NonNullList.<Ingredient>create();inputs.forEach(i->result.add(i.ingredient));return result;}
    public static final class Serializer implements RecipeSerializer<CoronalRecipe>{
        private static final MapCodec<CoronalRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Input.CODEC.listOf().fieldOf("inputs").forGetter(CoronalRecipe::inputs),ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CoronalRecipe::result),Codec.intRange(1,72000).fieldOf("ticks").forGetter(CoronalRecipe::ticks),Codec.LONG.validate(n->n>=1&&n<=1_000_000_000_000L?DataResult.success(n):DataResult.error(()->"Energy out of range")).fieldOf("energy").forGetter(CoronalRecipe::energy),Codec.intRange(0,3).optionalFieldOf("tier",0).forGetter(CoronalRecipe::tier)).apply(i,CoronalRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,CoronalRecipe> STREAM=StreamCodec.composite(Input.STREAM.apply(ByteBufCodecs.list(4)),CoronalRecipe::inputs,ItemStack.STREAM_CODEC,CoronalRecipe::result,ByteBufCodecs.VAR_INT,CoronalRecipe::ticks,ByteBufCodecs.VAR_LONG,CoronalRecipe::energy,ByteBufCodecs.VAR_INT,CoronalRecipe::tier,CoronalRecipe::new);
        public MapCodec<CoronalRecipe> codec(){return CODEC;}public StreamCodec<RegistryFriendlyByteBuf,CoronalRecipe> streamCodec(){return STREAM;}
    }
}
