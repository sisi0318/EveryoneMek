package dev.everyonemek.factory;

import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Native item conversion into the owning hatch; the same transaction also serves legacy automatic auxiliaries. */
public final class ChemicalConversions {
    public static final int STRUCTURE=0, WAITING=1, WORKING=2, FULL=3, STOPPED=4, REDSTONE=5;
    public static boolean accepts(Level level,ItemStack stack){
        if(level==null||stack.isEmpty())return false;
        var output=ChemicalInventorySlot.getPotentialConversion(level,stack);
        return !output.isEmpty()&&!output.isRadioactive();
    }
    public static int convert(Buffers bank,int slot,ItemStackToChemicalRecipe recipe){
        var stack=bank.item(slot);var input=recipe.getInput().getMatchingInstance(stack);if(input.isEmpty())return 0;
        var output=recipe.getOutput(input);if(output.isEmpty()||output.isRadioactive())return 0;
        long space=0;
        for(int i=0;i<bank.chemicalTanks();i++)space=mekanism.api.math.MathUtils.addClamped(space,bank.insertChem(i,output.copyWithAmount(Long.MAX_VALUE),true));
        int count=(int)Math.min(stack.getCount()/input.getCount(),space/output.getAmount());
        if(count<=0)return 0;
        ChemicalStack converted=output.copyWithAmount(Math.multiplyExact(output.getAmount(),count));
        if(!bank.store(List.of(),List.of(),List.of(converted),false))return 0;
        int used=Math.multiplyExact(input.getCount(),count);bank.take(slot,used,false);return used;
    }
    public static void tick(Controller c){
        if(!c.canFunction()||!c.enabled&&c.processing.jobs.isEmpty())return;
        for(var part:c.structure.converters){
            var bank=part.storage();boolean converted=false,full=false;
            for(int slot=0;slot<bank.itemSlots();slot++){
                var stack=bank.item(slot);if(stack.isEmpty())continue;
                var recipe=MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findFirstRecipe(c.getLevel(),stack);
                if(recipe==null||!accepts(c.getLevel(),stack))continue;
                if(convert(bank,slot,recipe)>0)converted=true;else full=true;
            }
            part.conversionStatus=converted?WORKING:full?FULL:WAITING;
        }
    }
    public static int status(Part part){
        var c=part.controller();if(c==null||!c.structure.valid())return STRUCTURE;
        if(!c.canFunction())return REDSTONE;
        if(!c.enabled&&c.processing.jobs.isEmpty())return STOPPED;
        return part.conversionStatus;
    }
    private ChemicalConversions(){}
}
