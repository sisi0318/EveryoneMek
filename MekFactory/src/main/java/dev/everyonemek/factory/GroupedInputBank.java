package dev.everyonemek.factory;

import java.util.*;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Count matching ingredients across hatches, but debit their original slots. Never persisted or exposed to pipes. */
public final class GroupedInputBank implements ResourceBank {
    private static final class Group<T> {
        final T sample; final List<Integer> slots=new ArrayList<>(); long amount;
        Group(T sample){this.sample=sample;}
        void add(int slot,long amount){slots.add(slot);this.amount=mekanism.api.math.MathUtils.addClamped(this.amount,amount);}
    }
    private final ResourceBank source;
    private final List<Group<ItemStack>> items=new ArrayList<>();
    private final List<Group<FluidStack>> fluids=new ArrayList<>();
    private final List<Group<ChemicalStack>> chemicals=new ArrayList<>();
    public GroupedInputBank(ResourceBank source){
        this.source=source;
        var itemHashes=new HashMap<Integer,List<Group<ItemStack>>>();
        for(int i=0;i<source.itemSlots();i++){
            var stack=source.item(i);if(stack.isEmpty())continue;
            var bucket=itemHashes.computeIfAbsent(ItemStack.hashItemAndComponents(stack),key->new ArrayList<>());
            var group=bucket.stream().filter(g->ItemStack.isSameItemSameComponents(g.sample,stack)).findFirst().orElse(null);
            if(group==null){group=new Group<>(stack.copy());bucket.add(group);items.add(group);}group.add(i,stack.getCount());
        }
        for(int i=0;i<source.fluidTanks();i++){
            var stack=source.fluid(i);if(stack.isEmpty())continue;
            var group=fluids.stream().filter(g->FluidStack.isSameFluidSameComponents(g.sample,stack)).findFirst().orElse(null);
            if(group==null){group=new Group<>(stack.copy());fluids.add(group);}group.add(i,stack.getAmount());
        }
        var byChemical=new HashMap<Chemical,Group<ChemicalStack>>();
        for(int i=0;i<source.chemicalTanks();i++){
            var stack=source.chemical(i);if(stack.isEmpty())continue;
            var group=byChemical.get(stack.getChemical());if(group==null){group=new Group<>(stack.copy());byChemical.put(stack.getChemical(),group);chemicals.add(group);}group.add(i,stack.getAmount());
        }
    }
    public int itemSlots(){return items.size();}
    public int itemLimit(int i){return item(i).getCount();}
    public ItemStack item(int i){var g=items.get(i);return g.sample.copyWithCount((int)Math.min(Integer.MAX_VALUE,g.amount));}
    public void item(int i,ItemStack stack){
        var g=items.get(i);int n=item(i).getCount()-stack.getCount();if(n<0||!stack.isEmpty()&&!ItemStack.isSameItemSameComponents(g.sample,stack))throw new IllegalArgumentException("Input view only permits consumption");
        int left=n;for(int slot:g.slots){var old=source.item(slot);int used=Math.min(left,old.getCount());if(used>0)source.item(slot,old.copyWithCount(old.getCount()-used));left-=used;if(left==0)break;}
        if(left!=0)throw new IllegalStateException("Input changed during reservation");g.amount-=n;
    }
    public int fluidTanks(){return fluids.size();}
    public int fluidCapacity(int i){return fluid(i).getAmount();}
    public FluidStack fluid(int i){var g=fluids.get(i);return g.sample.copyWithAmount((int)Math.min(Integer.MAX_VALUE,g.amount));}
    public void fluid(int i,FluidStack stack){
        var g=fluids.get(i);int n=fluid(i).getAmount()-stack.getAmount();if(n<0||!stack.isEmpty()&&!FluidStack.isSameFluidSameComponents(g.sample,stack))throw new IllegalArgumentException("Input view only permits consumption");
        int left=n;for(int slot:g.slots){var old=source.fluid(slot);int used=Math.min(left,old.getAmount());if(used>0)source.fluid(slot,old.copyWithAmount(old.getAmount()-used));left-=used;if(left==0)break;}
        if(left!=0)throw new IllegalStateException("Fluid changed during reservation");g.amount-=n;
    }
    public int chemicalTanks(){return chemicals.size();}
    public long chemicalCapacity(int i){return chemicals.get(i).amount;}
    public ChemicalStack chemical(int i){var g=chemicals.get(i);return g.sample.copyWithAmount(g.amount);}
    public void chemical(int i,ChemicalStack stack){
        var g=chemicals.get(i);long n=g.amount-stack.getAmount();if(n<0||!stack.isEmpty()&&!ChemicalStack.isSameChemical(g.sample,stack))throw new IllegalArgumentException("Input view only permits consumption");
        long left=n;for(int slot:g.slots){var old=source.chemical(slot);long used=Math.min(left,old.getAmount());if(used>0)source.chemical(slot,old.copyWithAmount(old.getAmount()-used));left-=used;if(left==0)break;}
        if(left!=0)throw new IllegalStateException("Chemical changed during reservation");g.amount-=n;
    }
    @Override public ItemStack insert(int i,ItemStack stack,boolean simulate){return stack;}
    @Override public int insertFluid(int i,FluidStack stack,boolean simulate){return 0;}
    @Override public long insertChem(int i,ChemicalStack stack,boolean simulate){return 0;}
    @Override public boolean store(List<ItemStack> items,List<FluidStack> fluids,List<ChemicalStack> chemicals,boolean simulate){return items.isEmpty()&&fluids.isEmpty()&&chemicals.isEmpty();}
}
