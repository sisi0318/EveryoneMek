package dev.everyonemek.factory;
import java.util.*;
import mekanism.api.Action;
import mekanism.api.chemical.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
/** Fixed retained slots prevent shrinkage from truncating existing contents. Limits govern new insertion only. */
public final class Buffers {
    public static final int SLOTS=432,TANKS=4;
    public final Controller owner; public final boolean output;
    public final ItemStack[] items=new ItemStack[SLOTS];public final FluidStack[] fluids=new FluidStack[TANKS];public final ChemicalStack[] chemicals=new ChemicalStack[TANKS];
    public Buffers(Controller c,boolean out){owner=c;output=out;Arrays.fill(items,ItemStack.EMPTY);Arrays.fill(fluids,FluidStack.EMPTY);Arrays.fill(chemicals,ChemicalStack.EMPTY);}
    public int slots(){return output?owner.structure.outputSlots:owner.structure.inputSlots;}
    public long capacity(){return output?owner.structure.outputCapacity:owner.structure.inputCapacity;}
    public void changed(){owner.markForSave();owner.inputRevision++;}
    public ItemStack insert(int i,ItemStack stack,boolean simulate){
        if(stack.isEmpty()||i<0||i>=slots())return stack;
        var old=items[i];if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,stack))return stack;
        int n=Math.min(stack.getCount(),Math.max(0,Math.min(64,stack.getMaxStackSize())-old.getCount()));
        if(n>0&&!simulate){items[i]=stack.copyWithCount(old.getCount()+n);changed();}
        return n==stack.getCount()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-n);
    }
    public ItemStack take(int i,int amount,boolean simulate){
        if(i<0||i>=SLOTS||amount<=0||items[i].isEmpty())return ItemStack.EMPTY;int n=Math.min(amount,items[i].getCount());var result=items[i].copyWithCount(n);
        if(!simulate){items[i]=items[i].copyWithCount(items[i].getCount()-n);changed();}return result;
    }
    public IItemHandlerModifiable menu(){return new IItemHandlerModifiable(){
        public int getSlots(){return SLOTS;}public ItemStack getStackInSlot(int i){return items[i];}
        public void setStackInSlot(int i,ItemStack stack){items[i]=stack;changed();}
        public ItemStack insertItem(int i,ItemStack s,boolean sim){return output?s:insert(i,s,sim);}
        public ItemStack extractItem(int i,int n,boolean sim){return take(i,n,sim);}
        public int getSlotLimit(int i){return i<slots()?64:items[i].getCount();}
        public boolean isItemValid(int i,ItemStack s){return !output&&i<slots();}
    };}
    public long insertChem(int i,ChemicalStack stack,boolean simulate){
        if(stack.isEmpty()||stack.isRadioactive()||i<0||i>=TANKS)return 0;var old=chemicals[i];if(!old.isEmpty()&&!ChemicalStack.isSameChemical(old,stack))return 0;
        long n=Math.min(stack.getAmount(),Math.max(0,capacity()-old.getAmount()));if(n>0&&!simulate){chemicals[i]=stack.copyWithAmount(old.getAmount()+n);changed();}return n;
    }
    public int insertFluid(int i,FluidStack stack,boolean simulate){
        if(stack.isEmpty()||i<0||i>=TANKS)return 0;var old=fluids[i];if(!old.isEmpty()&&!FluidStack.isSameFluidSameComponents(old,stack))return 0;
        int n=(int)Math.min(stack.getAmount(),Math.max(0,capacity()-old.getAmount()));if(n>0&&!simulate){fluids[i]=stack.copyWithAmount(old.getAmount()+n);changed();}return n;
    }
    public boolean store(List<ItemStack> stacks,List<FluidStack> liquids,List<ChemicalStack> gases,boolean simulate){
        var copyI=Arrays.stream(items).map(ItemStack::copy).toArray(ItemStack[]::new);
        var copyF=Arrays.stream(fluids).map(FluidStack::copy).toArray(FluidStack[]::new);
        var copyC=Arrays.stream(chemicals).map(ChemicalStack::copy).toArray(ChemicalStack[]::new);
        for(var stack:stacks){int left=stack.getCount();for(int pass=0;pass<2;pass++)for(int i=0;i<slots()&&left>0;i++){
            var old=copyI[i];if(pass==0?old.isEmpty():!old.isEmpty())continue;if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,stack))continue;
            int n=Math.min(left,Math.max(0,Math.min(64,stack.getMaxStackSize())-old.getCount()));if(n>0){copyI[i]=stack.copyWithCount(old.getCount()+n);left-=n;}
        }if(left>0)return false;}
        for(var stack:liquids){int left=stack.getAmount();for(int pass=0;pass<2;pass++)for(int i=0;i<TANKS&&left>0;i++){
            var old=copyF[i];if(pass==0?old.isEmpty():!old.isEmpty())continue;if(!old.isEmpty()&&!FluidStack.isSameFluidSameComponents(old,stack))continue;
            int n=(int)Math.min(left,Math.max(0,capacity()-old.getAmount()));if(n>0){copyF[i]=stack.copyWithAmount(old.getAmount()+n);left-=n;}
        }if(left>0)return false;}
        for(var stack:gases){if(stack.isRadioactive())return false;long left=stack.getAmount();for(int pass=0;pass<2;pass++)for(int i=0;i<TANKS&&left>0;i++){
            var old=copyC[i];if(pass==0?old.isEmpty():!old.isEmpty())continue;if(!old.isEmpty()&&!ChemicalStack.isSameChemical(old,stack))continue;
            long n=Math.min(left,Math.max(0,capacity()-old.getAmount()));if(n>0){copyC[i]=stack.copyWithAmount(old.getAmount()+n);left-=n;}
        }if(left>0)return false;}
        if(!simulate){System.arraycopy(copyI,0,items,0,SLOTS);System.arraycopy(copyF,0,fluids,0,TANKS);System.arraycopy(copyC,0,chemicals,0,TANKS);changed();}
        return true;
    }
    public CompoundTag save(HolderLookup.Provider r){var tag=new CompoundTag();var list=new ListTag();for(int i=0;i<SLOTS;i++)if(!items[i].isEmpty()){var v=new CompoundTag();v.putInt("slot",i);v.put("stack",items[i].save(r));list.add(v);}tag.put("items",list);
        var f=new ListTag();var c=new ListTag();for(int i=0;i<TANKS;i++){f.add(fluids[i].saveOptional(r));c.add(chemicals[i].saveOptional(r));}tag.put("fluids",f);tag.put("chemicals",c);return tag;}
    public void load(CompoundTag tag,HolderLookup.Provider r){Arrays.fill(items,ItemStack.EMPTY);Arrays.fill(fluids,FluidStack.EMPTY);Arrays.fill(chemicals,ChemicalStack.EMPTY);
        for(var value:tag.getList("items",Tag.TAG_COMPOUND)){var v=(CompoundTag)value;int i=v.getInt("slot");if(i>=0&&i<SLOTS)items[i]=ItemStack.parseOptional(r,v.getCompound("stack"));}
        var f=tag.getList("fluids",Tag.TAG_COMPOUND);var c=tag.getList("chemicals",Tag.TAG_COMPOUND);for(int i=0;i<TANKS;i++){if(i<f.size())fluids[i]=FluidStack.parseOptional(r,f.getCompound(i));if(i<c.size())chemicals[i]=ChemicalStack.parseOptional(r,c.getCompound(i));}
    }
}
