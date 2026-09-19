package dev.everyonemek.factory;
import java.util.*;
import mekanism.api.chemical.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
/** Owned storage: a hatch owns new materials; controllers retain only pre-alpha.6 legacy buffers. */
public final class Buffers implements ResourceBank {
    public static final int SLOTS=432,TANKS=4;
    public final Controller owner; public final Part hatch; public final boolean output;
    public final ItemStack[] items;
    public final FluidStack[] fluids=new FluidStack[TANKS];public final ChemicalStack[] chemicals=new ChemicalStack[TANKS];
    public Buffers(Controller owner,boolean output){this.owner=owner;this.output=output;hatch=null;items=new ItemStack[SLOTS];initialize();}
    public Buffers(Part hatch){this.hatch=hatch;owner=null;output=false;items=new ItemStack[54];initialize();}
    private void initialize(){Arrays.fill(items,ItemStack.EMPTY);Arrays.fill(fluids,FluidStack.EMPTY);Arrays.fill(chemicals,ChemicalStack.EMPTY);}
    public int slots(){return hatch==null?(output?owner.structure.outputSlots:owner.structure.inputSlots):hatch.grade().slots;}
    public long capacity(){return hatch==null?(output?owner.structure.outputCapacity:owner.structure.inputCapacity):hatch.grade().capacity;}
    public int itemSlots(){return items.length;}
    public int itemLimit(int i){return i<slots()?64:items[i].getCount();}
    public ItemStack item(int i){return items[i];}
    public void item(int i,ItemStack stack){items[i]=stack;changed();}
    public int fluidTanks(){return TANKS;}
    public int fluidCapacity(int i){return (int)Math.min(Integer.MAX_VALUE,capacity());}
    public FluidStack fluid(int i){return fluids[i];}
    public void fluid(int i,FluidStack stack){fluids[i]=stack;changed();}
    public int chemicalTanks(){return TANKS;}
    public long chemicalCapacity(int i){return capacity();}
    public ChemicalStack chemical(int i){return chemicals[i];}
    public void chemical(int i,ChemicalStack stack){chemicals[i]=stack;changed();}
    public void changed(){if(hatch!=null)hatch.setChanged();var c=owner!=null?owner:hatch.controller();if(c!=null){c.markForSave();c.inputRevision++;}}
    public CompoundTag save(HolderLookup.Provider r){var tag=new CompoundTag();var list=new ListTag();for(int i=0;i<items.length;i++)if(!items[i].isEmpty()){var v=new CompoundTag();v.putInt("slot",i);v.put("stack",items[i].save(r));list.add(v);}tag.put("items",list);
        var f=new ListTag();var c=new ListTag();for(int i=0;i<TANKS;i++){f.add(fluids[i].saveOptional(r));c.add(chemicals[i].saveOptional(r));}tag.put("fluids",f);tag.put("chemicals",c);return tag;}
    public void load(CompoundTag tag,HolderLookup.Provider r){Arrays.fill(items,ItemStack.EMPTY);Arrays.fill(fluids,FluidStack.EMPTY);Arrays.fill(chemicals,ChemicalStack.EMPTY);
        for(var value:tag.getList("items",Tag.TAG_COMPOUND)){var v=(CompoundTag)value;int i=v.getInt("slot");if(i>=0&&i<items.length)items[i]=ItemStack.parseOptional(r,v.getCompound("stack"));}
        var f=tag.getList("fluids",Tag.TAG_COMPOUND);var c=tag.getList("chemicals",Tag.TAG_COMPOUND);for(int i=0;i<TANKS;i++){if(i<f.size())fluids[i]=FluidStack.parseOptional(r,f.getCompound(i));if(i<c.size())chemicals[i]=ChemicalStack.parseOptional(r,c.getCompound(i));}
    }
}
