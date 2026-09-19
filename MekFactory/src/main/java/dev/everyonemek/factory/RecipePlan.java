package dev.everyonemek.factory;
import java.util.*;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
/** Immutable per-operation outputs and exact reservations; no copied world block entities. */
public final class RecipePlan {
    public final String id;public int ticks=200,baseTicks=200;public long energy,baseEnergy;public int operations=1;public boolean exponential,fixedEnergy;
    public final Map<Integer,Integer> items=new HashMap<>(),fluids=new HashMap<>();public final Map<Integer,Long> chemicals=new HashMap<>();
    public final List<ItemStack> outItems=new ArrayList<>();public final List<FluidStack> outFluids=new ArrayList<>();public final List<ChemicalStack> outChemicals=new ArrayList<>();
    public RecipePlan(String id){this.id=id;}
    public RecipePlan item(int i,int n){if(n>0)items.merge(i,n,Math::addExact);return this;}
    public RecipePlan fluid(int i,int n){if(n>0)fluids.merge(i,n,Math::addExact);return this;}
    public RecipePlan chemical(int i,long n){if(n>0)chemicals.merge(i,n,Math::addExact);return this;}
    public RecipePlan out(ItemStack s){if(!s.isEmpty())outItems.add(s.copy());return this;}
    public RecipePlan out(FluidStack s){if(!s.isEmpty())outFluids.add(s.copy());return this;}
    public RecipePlan out(ChemicalStack s){if(!s.isEmpty())outChemicals.add(s.copy());return this;}
    public int available(ResourceBank b,int limit){
        for(var e:items.entrySet())limit=Math.min(limit,b.item(e.getKey()).getCount()/e.getValue());
        for(var e:fluids.entrySet())limit=Math.min(limit,b.fluid(e.getKey()).getAmount()/e.getValue());
        for(var e:chemicals.entrySet())limit=(int)Math.min(limit,b.chemical(e.getKey()).getAmount()/e.getValue());
        return Math.max(0,limit);
    }
    public void consume(ResourceBank b,int n){for(var e:items.entrySet())b.take(e.getKey(),Math.multiplyExact(n,e.getValue()),false);
        for(var e:fluids.entrySet()){int i=e.getKey();b.fluid(i,b.fluid(i).copyWithAmount(b.fluid(i).getAmount()-Math.multiplyExact(n,e.getValue())));}
        for(var e:chemicals.entrySet()){int i=e.getKey();b.chemical(i,b.chemical(i).copyWithAmount(b.chemical(i).getAmount()-Math.multiplyExact(n,e.getValue())));}}
}
