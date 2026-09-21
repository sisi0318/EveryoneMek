package dev.everyonemek.factory;

import static dev.everyonemek.factory.FactoryTests.*;
import java.util.*;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.block.attribute.*;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.*;
import mekanism.common.tier.FactoryTier;
import mekanism.common.content.blocktype.FactoryType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekFactory.ID) @PrefixGameTestTemplate(false)
public final class ThroughputTests {
    @GameTest(template="empty",timeoutTicks=80)
    public static void bulkEjectionUsesAllSlotsAndResourceKinds(GameTestHelper h){
        var c=formed(h,Grade.ULTIMATE,3);c.enabled=false;c.autoEject=false;
        var corner=c.structure.at(0,0,0);h.getLevel().setBlockAndUpdate(corner,Content.PORTS.get(Grade.ULTIMATE).get().defaultBlockState().setValue(PartBlock.OUTPUT,true));check(c.structure.valid(),"Corner output did not form");
        var a=(Part)h.getLevel().getBlockEntity(corner);var b=c.structure.ports.stream().filter(p->p!=a&&p.getBlockState().getValue(PartBlock.OUTPUT)).findFirst().orElseThrow();
        var faces=Arrays.stream(Direction.values()).filter(d->c.structure.isOutward(corner,d)).toList();check(faces.size()==3,"Corner fixture is not three-sided");
        var chestA=corner.relative(faces.get(0));var liquid=corner.relative(faces.get(1));var gas=corner.relative(faces.get(2));
        var pipeB=b.getBlockPos().relative(c.structure.outward(b.getBlockPos()));var chestB=pipeB.relative(c.structure.outward(b.getBlockPos()));
        h.getLevel().setBlockAndUpdate(pipeB,MekanismBlocks.ULTIMATE_LOGISTICAL_TRANSPORTER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(chestA,Blocks.CHEST.defaultBlockState());h.getLevel().setBlockAndUpdate(chestB,Blocks.CHEST.defaultBlockState());
        h.getLevel().setBlockAndUpdate(liquid,MekanismBlocks.ULTIMATE_FLUID_TANK.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(gas,MekanismBlocks.ULTIMATE_CHEMICAL_TANK.get().defaultBlockState());
        var tank=(mekanism.common.tile.TileEntityChemicalTank)h.getLevel().getBlockEntity(gas);var tankConfig=tank.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.CHEMICAL);
        for(var side:RelativeSide.values())tankConfig.setDataType(mekanism.common.tile.component.config.DataType.INPUT,side);tankConfig.setEjecting(false);
        for(int i=0;i<8;i++){a.storage().insert(i,new ItemStack(Items.IRON_INGOT,64),false);a.storage().insert(i+8,new ItemStack(Items.GOLD_INGOT,64),false);a.storage().insert(i==7?53:i+16,new ItemStack(Items.DIAMOND,64),false);}
        for(int i=0;i<8;i++)b.storage().insert(i,new ItemStack(Items.COPPER_INGOT,64),false);
        var ca=(ChestBlockEntity)h.getLevel().getBlockEntity(chestA);ca.setItem(0,new ItemStack(Items.IRON_INGOT,7));ca.setChanged();
        a.storage().insertFluid(0,new FluidStack(Fluids.WATER,100000),false);a.storage().insertChem(0,new ChemicalStack(MekanismChemicals.HYDROGEN,500000),false);
        h.startSequence().thenIdle(2).thenExecute(()->check(count(a.storage(),Items.IRON_INGOT)==512&&ca.getItem(0).getCount()==7,"Disabled ejection moved items"))
              .thenWaitUntil(()->check(h.getLevel().getGameTime()%5==1,"Waiting away from old five-tick ejection interval"))
              .thenExecute(()->c.autoEject=true).thenIdle(2).thenExecute(()->{
                  check(!a.storage().hasContents()&&!b.storage().hasContents(),"Bulk remainder: items="+Arrays.stream(a.storage().items).mapToInt(ItemStack::getCount).sum()+", fluid="+a.storage().fluids[0].getAmount()+", chemical="+a.storage().chemicals[0].getAmount()+", second="+count(b.storage(),Items.COPPER_INGOT)+", formed="+c.structure.formed);
                  int iron=0,gold=0,diamonds=0;for(int i=0;i<ca.getContainerSize();i++){var s=ca.getItem(i);if(s.is(Items.IRON_INGOT))iron+=s.getCount();if(s.is(Items.GOLD_INGOT))gold+=s.getCount();if(s.is(Items.DIAMOND))diamonds+=s.getCount();}
                  check(iron==519&&gold==512&&diamonds==512,"Multi-type transfer or topping up an existing stack lost items");

                  var fluid=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,liquid,faces.get(1).getOpposite());check(fluid!=null&&fluid.getFluidInTank(0).getAmount()==100000,"Fluid was still capped at 16000 or duplicated");
                  var chemical=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),gas,faces.get(2).getOpposite());check(chemical!=null&&chemical.getChemicalInTank(0).getAmount()==500000,"Chemical was still capped at 64000 or duplicated");
                  c.autoEject=false;
              }).thenWaitUntil(()->{
                  var cb=(ChestBlockEntity)h.getLevel().getBlockEntity(chestB);int copper=0;for(int i=0;i<cb.getContainerSize();i++)if(cb.getItem(i).is(Items.COPPER_INGOT))copper+=cb.getItem(i).getCount();
                  check(copper==512,"Bulk request did not reach the chest through a real Mek transporter");
              }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void acceleratedWorkPaysEveryStepAndKeepsOneProviderBudget(GameTestHelper h){
        var c=formed(h,Grade.BASIC,3);c.template.setStack(new ItemStack(MekanismBlocks.CRUSHER));var cell=c.structure.cells.getFirst();cell.getEnergyContainer().setEnergy(1000000);
        port(c,false).storage().insert(0,new ItemStack(Items.IRON_INGOT,2),false);long usage=Attribute.get(MekanismBlocks.CRUSHER.get(),AttributeEnergy.class).getUsage();
        int cycles=FactoryConfig.PROCESSING_CYCLES.get();check(cycles==4,"Default processing speed is not quadrupled");
        c.processing.tick(c);check(c.processing.jobs.getFirst().progress==cycles&&c.powerUsed==usage*cycles&&c.running==1,"Two work steps did not pay twice, or UI counted parallel twice");
        check(cell.getEnergyContainer().getEnergy()==1000000-usage*cycles,"Double work energy mismatch");
        c.enabled=false;long before=cell.getEnergyContainer().getEnergy();c.structure.transfer=usage*(cycles+1);c.processing.tick(c);
        check(c.processing.jobs.getFirst().progress==cycles+1&&c.powerUsed==usage&&cell.getEnergyContainer().getEnergy()==before-usage,"Repeated work steps bypassed this tick's provider budget");
        var original=c.processing.save(h.getLevel().registryAccess());c.processing.load(original,h.getLevel().registryAccess());check(c.processing.jobs.getFirst().progress==cycles+1,"Native progress changed on reload");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=170)
    public static void infusingUsesRawAuxiliariesAcrossHatches(GameTestHelper h){
        var c=formed(h,Grade.BASIC,3);c.structure.cells.getFirst().getEnergyContainer().setEnergy(10000000);
        var extra=c.structure.at(1,2,0);h.getLevel().setBlockAndUpdate(extra,Content.PORTS.get(Grade.BASIC).get().defaultBlockState());check(c.structure.valid(),"Second input did not form");
        var inputs=c.structure.ports.stream().filter(p->!p.getBlockState().getValue(PartBlock.OUTPUT)).toList();
        for(var tier:FactoryTier.values())for(var type:List.of(FactoryType.INFUSING,FactoryType.PURIFYING)){var stack=new ItemStack(MekanismBlocks.getFactory(tier,type));check(Profiles.get(stack)!=null&&Profiles.processingLines(stack)==tier.processes,"New native factory variant was not registered");}
        c.template.setStack(new ItemStack(MekanismBlocks.getFactory(FactoryTier.ELITE,FactoryType.INFUSING)));
        var conversion=MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findFirstRecipe(h.getLevel(),new ItemStack(Items.REDSTONE,8));check(conversion!=null,"Native redstone conversion missing");var converted=conversion.getOutput(new ItemStack(Items.REDSTONE));
        var recipe=MekanismRecipeType.METALLURGIC_INFUSING.findFirst(h.getLevel(),r->!r.perTickUsage()&&!r.getItemInput().getRepresentations().isEmpty()&&r.getChemicalInput().test(converted.copyWithAmount(10000)));check(recipe!=null,"Native infused alloy recipe missing");
        var input=recipe.getItemInput().getMatchingInstance(recipe.getItemInput().getRepresentations().getFirst());long needed=recipe.getChemicalInput().getMatchingInstance(converted.copyWithAmount(10000)).getAmount();var result=recipe.getOutput(input,converted.copyWithAmount(needed));
        inputs.get(0).storage().insert(0,input.copyWithCount(input.getCount()*4),false);
        // Basic hatch uses slots 0..8; a distinct second slot/warehouse supplies the auxiliary.
        inputs.get(1).storage().insert(7,new ItemStack(Items.REDSTONE,8),false);
        long totalConverted=converted.getAmount()*(8/conversion.getInput().getMatchingInstance(new ItemStack(Items.REDSTONE,8)).getCount());
        h.startSequence().thenWaitUntil(()->check(count(c.outputBank(),result.getItem())==result.getCount()*4,"Infusing did not combine item and converted auxiliary"))
              .thenExecute(()->{c.enabled=false;check(count(c.inputBank(),input.getItem())==0&&count(c.inputBank(),Items.REDSTONE)==0,"Infusing did not consume exact raw items");long left=inputs.stream().flatMap(p->Arrays.stream(p.storage().chemicals)).mapToLong(ChemicalStack::getAmount).sum();check(left==totalConverted-needed*4,"Infusion reagent amount was multiplied per tick or conversion leftovers were lost");}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=180)
    public static void purificationPausesForOxygenAndRetainsSecondaryWork(GameTestHelper h){
        var c=formed(h,Grade.BASIC,3);var cell=c.structure.cells.getFirst();long start=10000000;cell.getEnergyContainer().setEnergy(start);
        c.template.setStack(new ItemStack(MekanismBlocks.PURIFICATION_CHAMBER));var input=port(c,false).storage();
        var oxygen=new ChemicalStack(MekanismChemicals.OXYGEN,10000);var recipe=MekanismRecipeType.PURIFYING.findFirst(h.getLevel(),r->r.perTickUsage()&&r.test(new ItemStack(Items.RAW_IRON,64),oxygen));check(recipe!=null,"Native raw iron purification missing");
        var material=recipe.getItemInput().getMatchingInstance(new ItemStack(Items.RAW_IRON,64));var result=recipe.getOutput(material,oxygen);input.insert(0,material,false);input.insertChem(0,oxygen.copyWithAmount(2),false);
        long usage=Attribute.get(MekanismBlocks.PURIFICATION_CHAMBER.get(),AttributeEnergy.class).getUsage();int[] progress={0};long[] stoppedEnergy={0};
        h.startSequence().thenIdle(5).thenExecute(()->{
            check(!c.processing.jobs.isEmpty(),"Purifying did not reserve its main ingredient");var job=c.processing.jobs.getFirst();check(job.chemicalWork!=null,"Per-tick oxygen was treated as a one-time input");
            input.chemical(0,ChemicalStack.EMPTY);progress[0]=job.progress;stoppedEnergy[0]=cell.getEnergyContainer().getEnergy();var used=job.chemicalWork.used;var pending=job.chemicalWork.pending;
            var saved=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(saved,h.getLevel().registryAccess());
            check(c.processing.jobs.getFirst().chemicalWork.used==used&&c.processing.jobs.getFirst().chemicalWork.pending==pending,"Reload reset gas use or rerolled pending consumption");
        }).thenIdle(4).thenExecute(()->{
            check(c.processing.jobs.getFirst().progress==progress[0]&&cell.getEnergyContainer().getEnergy()==stoppedEnergy[0],"Oxygen-starved job advanced or spent energy");
            check(c.status.equals("secondary"),"Missing oxygen was not reported");input.insertChem(0,oxygen,false);
        }).thenWaitUntil(()->check(count(c.outputBank(),result.getItem())==result.getCount(),"Purification did not resume after oxygen refill"))
              .thenExecute(()->{c.enabled=false;check(cell.getEnergyContainer().getEnergy()==start-200*usage,"Purification did not pay exactly one native cycle");check(count(c.inputBank(),material.getItem())==0,"Purification duplicated reserved input");
                  var stack=c.template.getStack().copy();stack.set(MekanismDataComponents.UPGRADES,new UpgradeAware(Map.of(Upgrade.SPEED,8,Upgrade.CHEMICAL,8),ItemStack.EMPTY,ItemStack.EMPTY));c.template.setStack(stack);
                  var work=new ChemicalWork(oxygen.copyWithAmount(1),true,false);int ticks=Profiles.settings(c,200,usage,false,false).ticks();long total=0;
                  for(int i=0;i<ticks;i++){long n=work.multiplier(c,200,ticks,i);total+=n;work.used+=n;work.pending=-1;}
                  check(total==200,"Chemical upgrade/speed scaling changed the native fixed total");
                  var infuserWork=new ChemicalWork(oxygen.copyWithAmount(1),false,false);infuserWork.durationBased=true;
                  check(infuserWork.multiplier(c,200,ticks,0)==1,"Custom per-tick single infuser recipe did not follow upgraded duration");
                  check(ChemicalWork.load(infuserWork.save(h.getLevel().registryAccess()),h.getLevel().registryAccess()).durationBased,"Infuser chemical policy was lost on reload");
              }).thenSucceed();
    }
}
