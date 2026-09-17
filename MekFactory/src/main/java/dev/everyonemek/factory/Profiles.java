package dev.everyonemek.factory;
import java.util.*;
import java.util.function.*;
import mekanism.api.Upgrade;
import mekanism.api.recipes.*;
import mekanism.common.recipe.*;
import mekanism.common.block.attribute.*;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.registries.*;
import mekanism.common.config.MekanismConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
/** Explicit machine profiles allow recipe additions without pretending arbitrary world machines are recipes. */
public final class Profiles {
    public enum Kind {ITEM,OXIDIZE,CRYSTAL,INFUSER,WASHER,SEPARATOR,REACTION,ROTARY}
    public record Settings(int ticks,long energy,int operations){}
    public static Settings settings(Controller c,int baseTicks,long baseEnergy,boolean exponential,boolean fixed){
        int speed=upgrades(c,Upgrade.SPEED),efficiency=upgrades(c,Upgrade.ENERGY);
        double factor=Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(),speed/(double)Upgrade.SPEED.getMax());
        return new Settings(Math.max(1,(int)(baseTicks/factor)),Math.max(1,fixed?baseEnergy:mekanism.api.math.MathUtils.ceilToLong(baseEnergy*Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(),2*speed/(double)Upgrade.SPEED.getMax()-efficiency/(double)Upgrade.ENERGY.getMax()))),
              exponential?1<<speed:Math.clamp((int)(factor/baseTicks),1,65536));
    }
    public record Profile(IMekanismRecipeTypeProvider<?,?,?> recipes,Kind kind,int baseTicks,LongSupplier energy,Predicate<Controller> condition) {
        public RecipePlan find(Controller c){
            if(!condition.test(c))return null;var b=c.inputs;
            for(var holder:recipes.getRecipes(c.getLevel())){
                var r=holder.value();if(r.isIncomplete())continue;RecipePlan found=null;String id=holder.id().toString();
                switch(kind){
                    case ITEM -> {if(r instanceof ItemStackToItemStackRecipe v)for(int i=0;i<b.slots();i++)if(v.test(b.items[i])){found=new RecipePlan(id).item(i,v.getInput().getMatchingInstance(b.items[i]).getCount()).out(v.getOutput(b.items[i]));break;}}
                    case OXIDIZE -> {if(r instanceof ItemStackToChemicalRecipe v)for(int i=0;i<b.slots();i++)if(v.test(b.items[i])){found=new RecipePlan(id).item(i,v.getInput().getMatchingInstance(b.items[i]).getCount()).out(v.getOutput(b.items[i]));break;}}
                    case CRYSTAL -> {if(r instanceof ChemicalCrystallizerRecipe v)for(int i=0;i<Buffers.TANKS;i++)if(v.test(b.chemicals[i])){found=new RecipePlan(id).chemical(i,v.getInput().getMatchingInstance(b.chemicals[i]).getAmount()).out(v.getOutput(b.chemicals[i]));break;}}
                    case INFUSER -> {if(r instanceof ChemicalChemicalToChemicalRecipe v)outer:for(int i=0;i<Buffers.TANKS;i++)for(int j=0;j<Buffers.TANKS;j++)if(v.getLeftInput().test(b.chemicals[i])&&v.getRightInput().test(b.chemicals[j])&&v.test(b.chemicals[i],b.chemicals[j])){
                        var p=new RecipePlan(id).chemical(i,v.getLeftInput().getMatchingInstance(b.chemicals[i]).getAmount()).chemical(j,v.getRightInput().getMatchingInstance(b.chemicals[j]).getAmount()).out(v.getOutput(b.chemicals[i],b.chemicals[j]));
                        if(p.available(b,1)>0){found=p;break outer;}
                    }}
                    case WASHER -> {if(r instanceof FluidChemicalToChemicalRecipe v)outer:for(int i=0;i<Buffers.TANKS;i++)for(int j=0;j<Buffers.TANKS;j++)if(v.test(b.fluids[i],b.chemicals[j])){
                        found=new RecipePlan(id).fluid(i,v.getFluidInput().getMatchingInstance(b.fluids[i]).getAmount()).chemical(j,v.getChemicalInput().getMatchingInstance(b.chemicals[j]).getAmount()).out(v.getOutput(b.fluids[i],b.chemicals[j]));break outer;
                    }}
                    case SEPARATOR -> {if(r instanceof ElectrolysisRecipe v)for(int i=0;i<Buffers.TANKS;i++)if(v.test(b.fluids[i])){
                        var result=v.getOutput(b.fluids[i]);found=new RecipePlan(id).fluid(i,v.getInput().getMatchingInstance(b.fluids[i]).getAmount()).out(result.left()).out(result.right());
                        found.energy=mekanism.api.math.MathUtils.multiplyClamped(energy.getAsLong(),v.getEnergyMultiplier());break;
                    }}
                    case REACTION -> {if(r instanceof PressurizedReactionRecipe v)outer:for(int i=0;i<b.slots();i++)if(v.getInputSolid().test(b.items[i]))for(int f=0;f<Buffers.TANKS;f++)if(v.getInputFluid().test(b.fluids[f]))for(int g=0;g<Buffers.TANKS;g++)if(v.getInputChemical().test(b.chemicals[g])&&v.test(b.items[i],b.fluids[f],b.chemicals[g])){
                        var result=v.getOutput(b.items[i],b.fluids[f],b.chemicals[g]);found=new RecipePlan(id).item(i,v.getInputSolid().getMatchingInstance(b.items[i]).getCount()).fluid(f,v.getInputFluid().getMatchingInstance(b.fluids[f]).getAmount()).chemical(g,v.getInputChemical().getMatchingInstance(b.chemicals[g]).getAmount()).out(result.item()).out(result.chemical());
                        found.ticks=v.getDuration();found.energy=mekanism.api.math.MathUtils.addClamped(energy.getAsLong(),v.getEnergyRequired());break outer;
                    }}
                    case ROTARY -> {if(r instanceof RotaryRecipe v)for(int i=0;i<Buffers.TANKS;i++){
                        if(!c.rotaryReverse&&v.hasFluidToChemical()&&v.getFluidInput().test(b.fluids[i])&&v.test(b.fluids[i])){found=new RecipePlan(id).fluid(i,v.getFluidInput().getMatchingInstance(b.fluids[i]).getAmount()).out(v.getChemicalOutput(b.fluids[i]));break;}
                        if(c.rotaryReverse&&v.hasChemicalToFluid()&&v.getChemicalInput().test(b.chemicals[i])&&v.test(b.chemicals[i])){found=new RecipePlan(id).chemical(i,v.getChemicalInput().getMatchingInstance(b.chemicals[i]).getAmount()).out(v.getFluidOutput(b.chemicals[i]));break;}
                    }}
                }
                if(found!=null){
                    if(kind!=Kind.REACTION)found.ticks=baseTicks;
                    if(found.energy==0)found.energy=energy.getAsLong();
                    found.baseTicks=found.ticks;found.baseEnergy=found.energy;found.fixedEnergy=kind==Kind.SEPARATOR;found.exponential=baseTicks==1;
                    var settings=settings(c,found.baseTicks,found.baseEnergy,found.exponential,found.fixedEnergy);found.ticks=settings.ticks;found.energy=settings.energy;found.operations=settings.operations;
                    if(found.available(b,1)==0)continue;
                    if(c.energy().available()<found.energy){c.status="energy";continue;}
                    if(!new Processing.Job(found,1).store(c.outputs,1,true)){c.status="output";continue;}
                    return found;
                }
            }return null;
        }
    }
    private static final Map<ResourceLocation,Profile> PROFILES=new java.util.concurrent.ConcurrentHashMap<>();private static boolean initialized;
    public static void register(ResourceLocation block,Profile profile){if(PROFILES.putIfAbsent(block,profile)!=null)throw new IllegalArgumentException("Duplicate profile "+block);}
    private static void nativeProfile(Block block,IMekanismRecipeTypeProvider<?,?,?> type,Kind kind,int ticks){register(BuiltInRegistries.BLOCK.getKey(block),new Profile(type,kind,ticks,()->Attribute.get(block,AttributeEnergy.class).getUsage(),c->true));}
    private static synchronized void initialize(){if(initialized)return;initialized=true;
        nativeProfile(MekanismBlocks.ENRICHMENT_CHAMBER.get(),MekanismRecipeType.ENRICHING,Kind.ITEM,200);
        nativeProfile(MekanismBlocks.CRUSHER.get(),MekanismRecipeType.CRUSHING,Kind.ITEM,200);
        nativeProfile(MekanismBlocks.ENERGIZED_SMELTER.get(),MekanismRecipeType.SMELTING,Kind.ITEM,200);
        nativeProfile(MekanismBlocks.CHEMICAL_OXIDIZER.get(),MekanismRecipeType.OXIDIZING,Kind.OXIDIZE,100);
        nativeProfile(MekanismBlocks.CHEMICAL_CRYSTALLIZER.get(),MekanismRecipeType.CRYSTALLIZING,Kind.CRYSTAL,200);
        nativeProfile(MekanismBlocks.CHEMICAL_INFUSER.get(),MekanismRecipeType.CHEMICAL_INFUSING,Kind.INFUSER,1);
        nativeProfile(MekanismBlocks.CHEMICAL_WASHER.get(),MekanismRecipeType.WASHING,Kind.WASHER,1);
        nativeProfile(MekanismBlocks.ELECTROLYTIC_SEPARATOR.get(),MekanismRecipeType.SEPARATING,Kind.SEPARATOR,1);
        nativeProfile(MekanismBlocks.PRESSURIZED_REACTION_CHAMBER.get(),MekanismRecipeType.REACTION,Kind.REACTION,200);
        nativeProfile(MekanismBlocks.ROTARY_CONDENSENTRATOR.get(),MekanismRecipeType.ROTARY,Kind.ROTARY,1);
        for(var tier:mekanism.common.tier.FactoryTier.values())for(var type:List.of(mekanism.common.content.blocktype.FactoryType.ENRICHING,mekanism.common.content.blocktype.FactoryType.CRUSHING,mekanism.common.content.blocktype.FactoryType.SMELTING)){
            var block=MekanismBlocks.getFactory(tier,type).get();var profile=switch(type){case ENRICHING->PROFILES.get(BuiltInRegistries.BLOCK.getKey(MekanismBlocks.ENRICHMENT_CHAMBER.get()));case CRUSHING->PROFILES.get(BuiltInRegistries.BLOCK.getKey(MekanismBlocks.CRUSHER.get()));default->PROFILES.get(BuiltInRegistries.BLOCK.getKey(MekanismBlocks.ENERGIZED_SMELTER.get()));};register(BuiltInRegistries.BLOCK.getKey(block),profile);
        }
    }
    public static Profile get(ItemStack stack){initialize();return stack.getItem() instanceof BlockItem block?PROFILES.get(BuiltInRegistries.BLOCK.getKey(block.getBlock())):null;}
    public static boolean clean(ItemStack s){return ContainerType.ITEM.getAttachmentContainersIfPresent(s).stream().allMatch(v->v.getStack().isEmpty())&&ContainerType.FLUID.getAttachmentContainersIfPresent(s).stream().allMatch(v->v.getFluid().isEmpty())&&ContainerType.CHEMICAL.getAttachmentContainersIfPresent(s).stream().allMatch(v->v.getStack().isEmpty());}
    public static int upgrades(Controller c,Upgrade upgrade){var nativeUpgrades=c.template.getStack().getOrDefault(MekanismDataComponents.UPGRADES,UpgradeAware.EMPTY);return Math.min(upgrade.getMax(),c.getComponent().getUpgrades(upgrade)+nativeUpgrades.getUpgradeCount(upgrade));}
    private Profiles(){}
}
