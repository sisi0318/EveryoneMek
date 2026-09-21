package dev.everyonemek.factory;

import static dev.everyonemek.factory.FactoryTests.*;
import java.util.Arrays;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekFactory.ID) @PrefixGameTestTemplate(false)
public final class ConversionTests {
    private static Part add(Controller c,GameTestHelper h){var pos=c.structure.at(0,0,0);h.getLevel().setBlockAndUpdate(pos,Content.CONVERTERS.get(Grade.BASIC).get().defaultBlockState());check(c.structure.valid(),"Conversion module broke the structure");return (Part)h.getLevel().getBlockEntity(pos);}
    private static long amount(Part p){return Arrays.stream(p.storage().chemicals).mapToLong(ChemicalStack::getAmount).sum();}

    @GameTest(template="empty",timeoutTicks=150)
    public static void enrichedDiamondFeedsFactoryWithoutExposingRawAuxiliaries(GameTestHelper h){
        var c=formed(h,Grade.BASIC,3);c.autoEject=false;var module=add(c,h);var side=Arrays.stream(Direction.values()).filter(d->d.getAxis()!=Direction.Axis.Y&&c.structure.isOutward(module.getBlockPos(),d)).findFirst().orElseThrow();var items=new Ports.ItemPort(module,side);
        var enriched=new ItemStack(MekanismItems.ENRICHED_DIAMOND.get());var conversion=MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findFirstRecipe(h.getLevel(),enriched);check(conversion!=null,"Missing enriched diamond conversion");var diamond=conversion.getOutput(enriched);
        check(diamond.getAmount()==80,"Published Mek enriched diamond contract changed");
        check(!items.isItemValid(0,new ItemStack(Items.COBBLESTONE))&&!items.insertItem(0,new ItemStack(Items.COBBLESTONE),false).isEmpty(),"Converter accepted unrelated items");
        check(items.insertItem(0,enriched,true).isEmpty()&&module.storage().item(0).isEmpty(),"Conversion insertion simulation changed storage");check(items.insertItem(0,enriched,false).isEmpty(),"Enriched diamond insertion failed");
        check(count(c.inputBank(),MekanismItems.ENRICHED_DIAMOND.get())==0,"Factory saw the converter's raw auxiliary as a main ingredient");
        var recipe=MekanismRecipeType.METALLURGIC_INFUSING.findFirst(h.getLevel(),r->!r.perTickUsage()&&!r.getItemInput().getRepresentations().isEmpty()&&r.getChemicalInput().test(diamond));check(recipe!=null,"No diamond infusion recipe");
        var input=recipe.getItemInput().getMatchingInstance(recipe.getItemInput().getRepresentations().getFirst());var needed=recipe.getChemicalInput().getMatchingInstance(diamond);var result=recipe.getOutput(input,needed);
        c.template.setStack(new ItemStack(MekanismBlocks.METALLURGIC_INFUSER,4));port(c,false).storage().insert(0,input.copyWithCount(input.getCount()*4),false);c.structure.cells.getFirst().getEnergyContainer().setEnergy(1000000);
        h.startSequence().thenWaitUntil(()->check(count(c.outputBank(),result.getItem())==result.getCount()*4,"Infuser could not consume the module's chemical"))
              .thenExecute(()->{c.enabled=false;check(module.storage().item(0).isEmpty()&&amount(module)==diamond.getAmount()-needed.getAmount()*4,"Converter lost or duplicated reagent");check(Arrays.stream(port(c,false).storage().chemicals).allMatch(ChemicalStack::isEmpty),"Module chemicals were copied into an input hatch");}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=150)
    public static void externalExportRunsAfterAllInternalWorkSteps(GameTestHelper h){
        var c=formed(h,Grade.BASIC,3);c.enabled=false;var module=add(c,h);var side=Arrays.stream(Direction.values()).filter(d->d.getAxis()!=Direction.Axis.Y&&c.structure.isOutward(module.getBlockPos(),d)).findFirst().orElseThrow();var tube=module.getBlockPos().relative(side);var tankPos=tube.relative(side);
        h.getLevel().setBlockAndUpdate(tube,MekanismBlocks.ULTIMATE_PRESSURIZED_TUBE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(tankPos,MekanismBlocks.BASIC_CHEMICAL_TANK.get().defaultBlockState());
        var tank=(mekanism.common.tile.TileEntityChemicalTank)h.getLevel().getBlockEntity(tankPos);var config=tank.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.CHEMICAL);
        for(var direction:RelativeSide.values())config.setDataType(mekanism.common.tile.component.config.DataType.INPUT,direction);config.setEjecting(false);
        // Match the native side-config packet: invalidate handlers already cached by the tube.
        h.getLevel().invalidateCapabilities(tankPos);h.getLevel().updateNeighborsAt(tankPos,tank.getBlockState().getBlock());
        module.storage().insert(0,new ItemStack(Items.FLINT,2),false);var source=new Ports.ChemPort(module,side);
        check(!source.isValid(0,new ChemicalStack(MekanismChemicals.OXYGEN,1))&&source.insertChemical(0,new ChemicalStack(MekanismChemicals.OXYGEN,1),Action.EXECUTE).getAmount()==1,"Converter allowed external chemical input");
        check(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,module.getBlockPos(),side)==null&&h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,module.getBlockPos(),side)==null,"Converter advertised unrelated fluid or energy input");
        var oxygen=new ChemicalStack(MekanismChemicals.OXYGEN,10000);
        var recipe=MekanismRecipeType.CHEMICAL_INFUSING.findFirst(h.getLevel(),r->!r.isIncomplete()&&!r.getLeftInput().getRepresentations().isEmpty()&&!r.getRightInput().getRepresentations().isEmpty()&&(r.getLeftInput().testType(oxygen)!=r.getRightInput().testType(oxygen)));check(recipe!=null,"No oxygen chemical-infuser fixture");
        boolean oxygenLeft=recipe.getLeftInput().testType(oxygen);var left=recipe.getLeftInput().getMatchingInstance(oxygenLeft?oxygen:recipe.getLeftInput().getRepresentations().getFirst());var right=recipe.getRightInput().getMatchingInstance(oxygenLeft?recipe.getRightInput().getRepresentations().getFirst():oxygen);var other=oxygenLeft?right:left;int cycles=FactoryConfig.PROCESSING_CYCLES.get();long perOxygen=(oxygenLeft?left:right).getAmount();var result=recipe.getOutput(left,right);
        h.startSequence().thenIdle(3).thenExecute(()->{check(module.storage().item(0).getCount()==2&&tank.getChemicalTank().isEmpty(),"Stopped converter consumed auxiliaries");c.enabled=true;})
              .thenWaitUntil(()->check(tank.getChemicalTank().getStored()==20,"Tube export: target="+tank.getChemicalTank().getStored()+", source="+amount(module)+", raw="+module.storage().item(0).getCount()+", state="+ChemicalConversions.status(module)+", formed="+c.structure.formed))
              .thenExecute(()->{
                  check(c.template.isEmpty()&&c.structure.cells.getFirst().getEnergyContainer().isEmpty(),"Auxiliary conversion needed a main machine or charged energy");
                  c.template.setStack(new ItemStack(MekanismBlocks.CHEMICAL_INFUSER));c.parallelLimit=1;c.structure.cells.getFirst().getEnergyContainer().setEnergy(1000000);
                  port(c,false).storage().insertChem(0,other.copyWithAmount(other.getAmount()*cycles),false);module.storage().insert(0,new ItemStack(Items.FLINT,64),false);
              }).thenWaitUntil(()->check(tank.getChemicalTank().getStored()==20+640-perOxygen*cycles,"External export did not preserve internal consumption"))
              .thenExecute(()->{
                  long made=0;var output=c.outputBank();for(int i=0;i<output.chemicalTanks();i++)if(ChemicalStack.isSameChemical(output.chemical(i),result))made+=output.chemical(i).getAmount();
                  check(made==result.getAmount()*cycles,"Export raced the factory's full input snapshot");long usage=Attribute.get(MekanismBlocks.CHEMICAL_INFUSER.get(),AttributeEnergy.class).getUsage();check(c.structure.cells.getFirst().getEnergyContainer().getEnergy()==1000000-usage*cycles,"Chemical sharing charged the wrong work");c.enabled=false;
              }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void fullConverterKeepsItemsModeAndDroppedContents(GameTestHelper h){
        var c=formed(h,Grade.BASIC,3);c.autoEject=false;var module=add(c,h);var side=Arrays.stream(Direction.values()).filter(d->d.getAxis()!=Direction.Axis.Y&&c.structure.isOutward(module.getBlockPos(),d)).findFirst().orElseThrow();var chem=new Ports.ChemPort(module,side);var items=new Ports.ItemPort(module,side);
        for(int i=0;i<Buffers.TANKS;i++)module.storage().chemicals[i]=new ChemicalStack(MekanismChemicals.DIAMOND,Grade.BASIC.capacity-(i==3?40:0));module.storage().insert(0,new ItemStack(MekanismItems.ENRICHED_DIAMOND.get(),2),false);
        h.startSequence().thenIdle(3).thenExecute(()->{
            check(module.storage().item(0).getCount()==2&&ChemicalConversions.status(module)==ChemicalConversions.FULL,"Part of an enriched item was consumed into insufficient room");
            check(chem.extractChemical(0,80,Action.SIMULATE).getAmount()==80&&amount(module)==4L*Grade.BASIC.capacity-40,"Simulated export mutated the converter");chem.extractChemical(0,80,Action.EXECUTE);
        }).thenIdle(3).thenExecute(()->{
            check(module.storage().item(0).getCount()==1&&amount(module)==4L*Grade.BASIC.capacity-40,"Freeing space did not consume exactly one enriched diamond");
            var p=player(h,module.getBlockPos().relative(side));try{
                module.open(p);var menu=(WarehouseMenu)p.containerMenu;check(menu.conversion,"Converter did not open its own menu");
                var configurable=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CONFIGURABLE,module.getBlockPos(),side);check(configurable!=null&&configurable.onSneakRightClick(p).consumesAction()&&!module.isOutput(),"Configurator changed a fixed-I/O module");
                PortConfiguration.refresh(c);check(Arrays.stream(c.portConverters).sum()==3,"Corner module was missing from its three faces");
                var drops=Block.getDrops(module.getBlockState(),h.getLevel(),module.getBlockPos(),module);check(drops.size()==1&&drops.getFirst().has(Content.PORT_DATA.get()),"Converter drop did not contain storage data");
                var pos=module.getBlockPos();var held=p.getMainHandItem().copy();h.getLevel().destroyBlock(pos,false);check(chem.getChemicalTanks()==0&&items.getSlots()==0&&!menu.stillValid(p),"Removed converter retained transfer or menu access");
                p.setItemInHand(InteractionHand.MAIN_HAND,drops.getFirst());try{p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));}finally{p.setItemInHand(InteractionHand.MAIN_HAND,held);}
                var restored=(Part)h.getLevel().getBlockEntity(pos);check(c.structure.valid()&&restored.isConverter()&&restored.storage().item(0).getCount()==1&&amount(restored)==4L*Grade.BASIC.capacity-40&&!restored.isOutput(),"Converter restore: block="+h.getLevel().getBlockState(pos)+", raw="+(restored==null?-1:restored.storage().item(0).getCount())+", chemical="+(restored==null?-1:amount(restored))+", error="+c.structure.error);
            }finally{close(p);}c.enabled=false;
        }).thenSucceed();
    }
}
