package dev.everyonemek.oritech;

import java.util.*;
import dev.architectury.fluid.FluidStack;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.*;
import rearth.oritech.util.FluidIngredient;

/** Plans the complete transaction before drawing power or consuming any input. */
final class Engine {
    record Job(ResourceLocation id,int time,List<Ingredient> inputs,List<ItemStack> items,FluidIngredient fluid,List<FluidStack> fluids,
               int duration,long perTick,long budget) {
        CompoundTag signature(Processor p){var t=new CompoundTag();t.putString("recipe",id.toString());t.putInt("time",time);t.putInt("profile",p.profileIndex);
            var a=p.getBaseAddonData();t.putFloat("speed",a.speed());t.putFloat("efficiency",a.efficiency());t.putInt("chambers",a.extraChambers());t.putInt("burst",a.maxBurstTicks());
            t.putLong("budget",budget);t.putInt("power",p.getEnergyPerTick());t.putString("fluidInput",fluid.toString());
            var in=new ListTag();for(int i=0;i<p.profile().inputs();i++)in.add(p.inventory.getItem(i).copyWithCount(1).saveOptional(p.getLevel().registryAccess()));t.put("inputs",in);
            if(fluid.amount()>0)t.putString("inputFluid",p.fluidIn.getStack().copyWithAmount(1).toString());
            var out=new ListTag();for(var s:items)out.add(s.saveOptional(p.getLevel().registryAccess()));t.put("outputs",out);
            var liquid=new ListTag();for(var s:fluids)liquid.add(FluidStack.CODEC.encodeStart(NbtOps.INSTANCE,s).getOrThrow());t.put("fluids",liquid);
            return t;
        }
    }
    static Job find(Processor p){var world=p.getLevel();var profile=p.profile();
        if(profile==Profiles.FURNACE){var input=new SingleRecipeInput(p.inventory.getItem(0));var match=world.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,world);
            if(match.isEmpty())return null;var r=match.get();return make(p,r.id(),r.value().getCookingTime(),List.of(r.value().getIngredients().getFirst()),List.of(r.value().assemble(input,world.registryAccess())),FluidIngredient.EMPTY,List.of());}
        if(profile==Profiles.CENTRIFUGE&&p.fluidAddon){var candidate=findType(p,RecipeContent.CENTRIFUGE_FLUID);if(candidate!=null)return candidate;}
        return profile.recipes==null?null:findType(p,profile.recipes);
    }
    private static Job findType(Processor p,OritechRecipeType type){
        for(var holder:p.getLevel().getRecipeManager().getAllRecipesFor(type).stream().sorted(Comparator.comparingInt((RecipeHolder<OritechRecipe> r)->-r.value().getInputs().size())).toList()){
            var r=holder.value();if(r.getTime()<=0||r.getInputs().size()>4||r.getResults().size()>4||r.getFluidOutputs().size()>3)continue;
            if(assignment(p,r.getInputs())==null||!fluidMatches(p,r.getFluidInput()))continue;
            var items=new ArrayList<ItemStack>();for(int i=0;i<r.getResults().size();i++){
                int multiplier=(p.profile()==Profiles.GRINDER&&p.yieldAddon&&i>0)||(p.profile()==Profiles.REFINERY&&p.modules==0&&r.getFluidOutputs().size()>1)?2:1;
                items.add(r.getResults().get(i).copyWithCount(r.getResults().get(i).getCount()*multiplier));
            }
            var fluids=new ArrayList<FluidStack>();for(var f:r.getFluidOutputs())fluids.add(f.copy());
            if(p.profile()==Profiles.REFINERY&&fluids.size()>1){if(p.modules==0){fluids.getFirst().setAmount(fluids.getFirst().getAmount()*2);fluids.subList(1,fluids.size()).clear();}
                else if(p.modules==1){fluids.get(1).setAmount(fluids.get(1).getAmount()*2);if(fluids.size()>2)fluids.remove(2);}}
            return make(p,holder.id(),r.getTime(),r.getInputs(),items,r.getFluidInput(),fluids);
        }
        return null;
    }
    private static Job make(Processor p,ResourceLocation id,int time,List<Ingredient> input,List<ItemStack> items,FluidIngredient fluid,List<FluidStack> fluids){
        double speed=p.getSpeedMultiplier(),eff=p.getEfficiencyMultiplier();
        if(p.profile()==Profiles.FURNACE)speed*=OritechConfig.processingMachines.furnaceData.speedMultiplier.get();
        if(p.profile()==Profiles.COOLER&&p.getLevel().getBiome(p.getBlockPos()).is(TagContent.CONVENTIONAL_COLD)){speed*=.5;eff*=.5;}
        int ticks=(int)Math.clamp(Math.ceil(time*speed),1,2_000_000_000);
        long rate=positive(p.getEnergyPerTick()*eff/speed/(p.profile()==Profiles.FURNACE?2:1));
        long budget=p.profile()==Profiles.ATOMIC?positive((double)p.getEnergyPerTick()*time*p.getBaseAddonData().efficiency()):0;
        if(p.profile()==Profiles.ATOMIC)rate=positive(p.getEnergyPerTick()/Math.max(.000001,p.getSpeedMultiplier()));
        return new Job(id,time,input,List.copyOf(items),fluid,List.copyOf(fluids),ticks,rate,budget);
    }
    private static long positive(double n){return Double.isFinite(n)?Math.clamp((long)Math.ceil(n),1,Long.MAX_VALUE/4):Long.MAX_VALUE/4;}
    static boolean matches(Processor p,Job j){return assignment(p,j.inputs())!=null&&fluidMatches(p,j.fluid());}
    private static boolean fluidMatches(Processor p,FluidIngredient f){return f.amount()<=0||(p.fluidEnabled()&&f.test(p.fluidIn.getStack()));}
    static int[] assignment(Processor p,List<Ingredient> ingredients){int count=p.profile().inputs();boolean[][] accepts=new boolean[ingredients.size()][count];int[] available=new int[count],needed=new int[ingredients.size()];Arrays.fill(needed,1);
        for(int s=0;s<count;s++){var stack=p.inventory.getItem(s);available[s]=stack.getCount();for(int i=0;i<ingredients.size();i++)accepts[i][s]=ingredients.get(i).test(stack);}
        return IngredientAssignment.matchQuantities(accepts,available,needed);
    }
    private static List<ItemStack> outputs(Processor p,Job j){var stacks=new ArrayList<ItemStack>();for(int i=0;i<p.profile().outputs();i++)stacks.add(p.inventory.getItem(4+i).copy());
        for(var result:j.items()){int left=result.getCount();for(int pass=0;pass<2&&left>0;pass++)for(int i=0;i<stacks.size()&&left>0;i++){
            var slot=stacks.get(i);if(pass==0?(slot.isEmpty()||!ItemStack.isSameItemSameComponents(slot,result)):!slot.isEmpty())continue;
            int moved=Math.min(left,result.getMaxStackSize()-slot.getCount());if(moved<=0)continue;
            if(slot.isEmpty())stacks.set(i,result.copyWithCount(moved));else slot.grow(moved);left-=moved;
        }if(left>0)return null;}return stacks;
    }
    static boolean canOutput(Processor p,Job j){if(outputs(p,j)==null||j.fluids().size()>p.outputTanks())return false;
        for(int i=0;i<j.fluids().size();i++){var f=j.fluids().get(i);if(p.tanks().get(i+1).insert(f,true)!=f.getAmount())return false;}return true;
    }
    static void finish(Processor p,Job j){var used=assignment(p,j.inputs());var output=outputs(p,j);if(used==null||output==null||!canOutput(p,j)||!fluidMatches(p,j.fluid()))return;
        for(int i=0;i<used.length;i++)p.inventory.getItem(i).shrink(used[i]);
        for(int i=0;i<output.size();i++)p.inventory.setItem(4+i,output.get(i));
        if(j.fluid().amount()>0)p.fluidIn.extract(p.fluidIn.getStack().copyWithAmount(j.fluid().amount()),false);
        for(int i=0;i<j.fluids().size();i++)p.tanks().get(i+1).insert(j.fluids().get(i),false);
        if(p.profile()==Profiles.PULVERIZER||p.profile()==Profiles.GRINDER)compact(p);
        p.setChanged();
    }
    private static void compact(Processor p){var small=p.inventory.getItem(5);var main=p.inventory.getItem(4);if(small.getCount()<9||main.isEmpty())return;
        var grid=CraftingInput.of(3,3,java.util.Collections.nCopies(9,small.copyWithCount(1)));
        var recipe=p.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,p.getLevel());if(recipe.isEmpty())return;
        var result=recipe.get().value().assemble(grid,p.getLevel().registryAccess());
        if(result.getCount()==1&&ItemStack.isSameItemSameComponents(main,result)&&main.getCount()<main.getMaxStackSize()){
            var remains=recipe.get().value().getRemainingItems(grid);if(remains.stream().anyMatch(s->!s.isEmpty()))return;
            small.shrink(9);main.grow(1);
        }
    }
    private Engine(){}
}
