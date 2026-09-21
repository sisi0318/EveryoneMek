package dev.everyonemek.factory;
import java.util.*;
import dev.everyonemek.factory.compat.Compat;
import mekanism.common.content.blocktype.FactoryType;
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
    public enum Kind {ITEM,OXIDIZE,CRYSTAL,INFUSER,WASHER,SEPARATOR,REACTION,ROTARY,METALLURGIC,PURIFYING}
    public record Settings(int ticks,long energy,int operations){}
    public static Settings settings(Controller c,int baseTicks,long baseEnergy,boolean exponential,boolean fixed){
        int speed=upgrades(c,Upgrade.SPEED),efficiency=upgrades(c,Upgrade.ENERGY);
        double factor=Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(),speed/(double)Upgrade.SPEED.getMax());
        boolean creative=Compat.creative(c);
        int operations=exponential?1<<speed:(int)Math.clamp(Math.max(1,factor/baseTicks)*Compat.stackOperations(c),1,65536);
        return new Settings(creative?1:Math.max(1,(int)(baseTicks/factor)),creative?0:Math.max(1,fixed?baseEnergy:mekanism.api.math.MathUtils.ceilToLong(baseEnergy*Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(),2*speed/(double)Upgrade.SPEED.getMax()-efficiency/(double)Upgrade.ENERGY.getMax()))),operations);
    }
    public static int chemicalTicks(Controller c,int baseTicks){return Math.max(1,(int)(baseTicks/Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(),upgrades(c,Upgrade.SPEED)/(double)Upgrade.SPEED.getMax())));}
    public record Profile(IMekanismRecipeTypeProvider<?,?,?> recipes,Kind kind,int baseTicks,LongSupplier energy,Predicate<Controller> condition) {
        public RecipePlan find(Controller c){return find(c,c.inputBank());}
        public RecipePlan find(Controller c,ResourceBank b){
            if(!condition.test(c))return null;
            for(var holder:recipes.getRecipes(c.getLevel())){
                var r=holder.value();if(r.isIncomplete())continue;RecipePlan found=null;String id=holder.id().toString();
                switch(kind){
                    case ITEM -> {if(r instanceof ItemStackToItemStackRecipe v)for(int i=0;i<b.itemSlots();i++)if(v.test(b.item(i))){found=new RecipePlan(id).item(i,v.getInput().getMatchingInstance(b.item(i)).getCount()).out(v.getOutput(b.item(i)));break;}}
                    case METALLURGIC,PURIFYING -> {if(r instanceof ItemStackChemicalToItemStackRecipe v)outer:for(int i=0;i<b.itemSlots();i++)if(v.getItemInput().test(b.item(i)))for(int g=0;g<b.chemicalTanks();g++)if(!b.chemical(g).isRadioactive()&&v.test(b.item(i),b.chemical(g))){
                        var chemical=v.getChemicalInput().getMatchingInstance(b.chemical(g));
                        found=new RecipePlan(id).item(i,v.getItemInput().getMatchingInstance(b.item(i)).getCount()).out(v.getOutput(b.item(i),b.chemical(g)));
                        if(v.perTickUsage()){
                            found.chemicalWork=new ChemicalWork(chemical,kind==Kind.PURIFYING,kind==Kind.PURIFYING&&MekanismConfig.usage.randomizedConsumption.get());
                            // Native single infusers use their upgraded duration; infusing factories use the base duration.
                            found.chemicalWork.durationBased=kind==Kind.METALLURGIC&&processingLines(c.template.getStack())==1;
                        }
                        else found.chemical(g,chemical.getAmount());
                        break outer;
                    }}
                    case OXIDIZE -> {if(r instanceof ItemStackToChemicalRecipe v)for(int i=0;i<b.itemSlots();i++)if(v.test(b.item(i))){found=new RecipePlan(id).item(i,v.getInput().getMatchingInstance(b.item(i)).getCount()).out(v.getOutput(b.item(i)));break;}}
                    case CRYSTAL -> {if(r instanceof ChemicalCrystallizerRecipe v)for(int i=0;i<b.chemicalTanks();i++)if(v.test(b.chemical(i))){found=new RecipePlan(id).chemical(i,v.getInput().getMatchingInstance(b.chemical(i)).getAmount()).out(v.getOutput(b.chemical(i)));break;}}
                    case INFUSER -> {if(r instanceof ChemicalChemicalToChemicalRecipe v)outer:for(int i=0;i<b.chemicalTanks();i++)for(int j=0;j<b.chemicalTanks();j++)if(v.getLeftInput().test(b.chemical(i))&&v.getRightInput().test(b.chemical(j))&&v.test(b.chemical(i),b.chemical(j))){
                        var p=new RecipePlan(id).chemical(i,v.getLeftInput().getMatchingInstance(b.chemical(i)).getAmount()).chemical(j,v.getRightInput().getMatchingInstance(b.chemical(j)).getAmount()).out(v.getOutput(b.chemical(i),b.chemical(j)));
                        if(p.available(b,1)>0){found=p;break outer;}
                    }}
                    case WASHER -> {if(r instanceof FluidChemicalToChemicalRecipe v)outer:for(int i=0;i<b.fluidTanks();i++)for(int j=0;j<b.chemicalTanks();j++)if(v.test(b.fluid(i),b.chemical(j))){
                        found=new RecipePlan(id).fluid(i,v.getFluidInput().getMatchingInstance(b.fluid(i)).getAmount()).chemical(j,v.getChemicalInput().getMatchingInstance(b.chemical(j)).getAmount()).out(v.getOutput(b.fluid(i),b.chemical(j)));break outer;
                    }}
                    case SEPARATOR -> {if(r instanceof ElectrolysisRecipe v)for(int i=0;i<b.fluidTanks();i++)if(v.test(b.fluid(i))){
                        var result=v.getOutput(b.fluid(i));found=new RecipePlan(id).fluid(i,v.getInput().getMatchingInstance(b.fluid(i)).getAmount()).out(result.left()).out(result.right());
                        found.energy=mekanism.api.math.MathUtils.multiplyClamped(energy.getAsLong(),v.getEnergyMultiplier());break;
                    }}
                    case REACTION -> {if(r instanceof PressurizedReactionRecipe v)outer:for(int i=0;i<b.itemSlots();i++)if(v.getInputSolid().test(b.item(i)))for(int f=0;f<b.fluidTanks();f++)if(v.getInputFluid().test(b.fluid(f)))for(int g=0;g<b.chemicalTanks();g++)if(v.getInputChemical().test(b.chemical(g))&&v.test(b.item(i),b.fluid(f),b.chemical(g))){
                        var result=v.getOutput(b.item(i),b.fluid(f),b.chemical(g));found=new RecipePlan(id).item(i,v.getInputSolid().getMatchingInstance(b.item(i)).getCount()).fluid(f,v.getInputFluid().getMatchingInstance(b.fluid(f)).getAmount()).chemical(g,v.getInputChemical().getMatchingInstance(b.chemical(g)).getAmount()).out(result.item()).out(result.chemical());
                        found.ticks=v.getDuration();found.energy=mekanism.api.math.MathUtils.addClamped(energy.getAsLong(),v.getEnergyRequired());break outer;
                    }}
                    case ROTARY -> {if(r instanceof RotaryRecipe v)for(int i=0;i<(c.rotaryReverse?b.chemicalTanks():b.fluidTanks());i++){
                        if(!c.rotaryReverse&&v.hasFluidToChemical()&&v.getFluidInput().test(b.fluid(i))&&v.test(b.fluid(i))){found=new RecipePlan(id).fluid(i,v.getFluidInput().getMatchingInstance(b.fluid(i)).getAmount()).out(v.getChemicalOutput(b.fluid(i)));break;}
                        if(c.rotaryReverse&&v.hasChemicalToFluid()&&v.getChemicalInput().test(b.chemical(i))&&v.test(b.chemical(i))){found=new RecipePlan(id).chemical(i,v.getChemicalInput().getMatchingInstance(b.chemical(i)).getAmount()).out(v.getFluidOutput(b.chemical(i)));break;}
                    }}
                }
                if(found!=null){
                    if(kind!=Kind.REACTION)found.ticks=baseTicks;
                    if(found.energy==0)found.energy=energy.getAsLong();
                    found.baseTicks=found.ticks;found.baseEnergy=found.energy;found.fixedEnergy=kind==Kind.SEPARATOR;found.exponential=baseTicks==1;
                    var settings=settings(c,found.baseTicks,found.baseEnergy,found.exponential,found.fixedEnergy);found.ticks=settings.ticks;found.energy=settings.energy;found.operations=settings.operations;
                    if(found.available(b,1)==0)continue;
                    if(c.energy().available()<found.energy){c.status="energy";continue;}
                    if(!new Processing.Job(found,1).store(c.outputBank(),1,true)){c.status="output";continue;}
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
        nativeProfile(MekanismBlocks.METALLURGIC_INFUSER.get(),MekanismRecipeType.METALLURGIC_INFUSING,Kind.METALLURGIC,200);
        nativeProfile(MekanismBlocks.PURIFICATION_CHAMBER.get(),MekanismRecipeType.PURIFYING,Kind.PURIFYING,200);
        nativeProfile(MekanismBlocks.CHEMICAL_OXIDIZER.get(),MekanismRecipeType.OXIDIZING,Kind.OXIDIZE,100);
        nativeProfile(MekanismBlocks.CHEMICAL_CRYSTALLIZER.get(),MekanismRecipeType.CRYSTALLIZING,Kind.CRYSTAL,200);
        nativeProfile(MekanismBlocks.CHEMICAL_INFUSER.get(),MekanismRecipeType.CHEMICAL_INFUSING,Kind.INFUSER,1);
        nativeProfile(MekanismBlocks.CHEMICAL_WASHER.get(),MekanismRecipeType.WASHING,Kind.WASHER,1);
        nativeProfile(MekanismBlocks.ELECTROLYTIC_SEPARATOR.get(),MekanismRecipeType.SEPARATING,Kind.SEPARATOR,1);
        nativeProfile(MekanismBlocks.PRESSURIZED_REACTION_CHAMBER.get(),MekanismRecipeType.REACTION,Kind.REACTION,200);
        nativeProfile(MekanismBlocks.ROTARY_CONDENSENTRATOR.get(),MekanismRecipeType.ROTARY,Kind.ROTARY,1);
        for(var tier:mekanism.common.tier.FactoryTier.values())for(var type:List.of(FactoryType.ENRICHING,FactoryType.CRUSHING,FactoryType.SMELTING,FactoryType.INFUSING,FactoryType.PURIFYING))
            registerFactoryVariant(MekanismBlocks.getFactory(tier,type).get(),type);
        Compat.registerFactories();
    }
    public static void registerFactoryVariant(Block block,FactoryType type){
        var base=switch(type){case ENRICHING->MekanismBlocks.ENRICHMENT_CHAMBER;case CRUSHING->MekanismBlocks.CRUSHER;case SMELTING->MekanismBlocks.ENERGIZED_SMELTER;case INFUSING->MekanismBlocks.METALLURGIC_INFUSER;case PURIFYING->MekanismBlocks.PURIFICATION_CHAMBER;default->null;};
        if(base==null)return;
        var p=PROFILES.get(BuiltInRegistries.BLOCK.getKey(base.get()));var energy=Attribute.get(block,AttributeEnergy.class);
        register(BuiltInRegistries.BLOCK.getKey(block),new Profile(p.recipes(),p.kind(),p.baseTicks(),energy==null?p.energy():energy::getUsage,p.condition()));
    }
    public static Profile get(ItemStack stack){initialize();return stack.getItem() instanceof BlockItem block?PROFILES.get(BuiltInRegistries.BLOCK.getKey(block.getBlock())):null;}
    /** A factory contributes its native lanes, not a second speed or energy multiplier. */
    public static int processingLines(ItemStack stack){
        if(stack.isEmpty()||get(stack)==null)return 0;
        int extra=Compat.factoryLines(stack);if(extra>0)return extra;
        var tier=Attribute.get(((BlockItem)stack.getItem()).getBlock(),AttributeTier.class);
        return tier!=null&&tier.tier() instanceof mekanism.common.tier.FactoryTier factory?factory.processes:1;
    }
    public static int machineParallel(ItemStack stack){return (int)Math.min(Integer.MAX_VALUE,(long)stack.getCount()*processingLines(stack));}
    public static int availableParallel(Controller c){return c.structure.formed?Math.max(0,Math.min(machineParallel(c.template.getStack()),Math.min(c.parallelLimit,c.structure.parallel))):0;}
    public static boolean rotary(ItemStack stack){var profile=get(stack);return profile!=null&&profile.kind()==Kind.ROTARY;}
    public static boolean clean(ItemStack s){return ContainerType.ITEM.getAttachmentContainersIfPresent(s).stream().allMatch(v->v.getStack().isEmpty())&&ContainerType.FLUID.getAttachmentContainersIfPresent(s).stream().allMatch(v->v.getFluid().isEmpty())&&ContainerType.CHEMICAL.getAttachmentContainersIfPresent(s).stream().allMatch(v->v.getStack().isEmpty());}
    public static int upgrades(Controller c,Upgrade upgrade){var nativeUpgrades=c.template.getStack().getOrDefault(MekanismDataComponents.UPGRADES,UpgradeAware.EMPTY);return Math.min(upgrade.getMax(),c.getComponent().getUpgrades(upgrade)+nativeUpgrades.getUpgradeCount(upgrade));}
    private Profiles(){}
}
