package dev.everyonemek.factory;

import static dev.everyonemek.factory.FactoryTests.*;
import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionCell;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionProvider;
import dev.everyonemek.factory.compat.Compat;
import java.util.*;
import mekanism.api.*;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.attachments.containers.energy.AttachedEnergy;
import mekanism.common.block.attribute.*;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.registries.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(MekFactory.ID) @PrefixGameTestTemplate(false)
public final class ExtrasTests {
    @GameTest(template="empty",timeoutTicks=160)
    public static void extraFactoryLanesStackAndCreativeUpgrades(GameTestHelper h){
        if(!Compat.EXTRAS){h.succeed();return;}
        var c=formed(h,Grade.ULTIMATE,3);c.enabled=false;var cell=c.structure.cells.getFirst();long start=10000000;cell.getEnergyContainer().setEnergy(start);
        for(var tier:ExtraFactoryTier.values())for(var type:List.of(FactoryType.ENRICHING,FactoryType.CRUSHING,FactoryType.SMELTING,FactoryType.INFUSING,FactoryType.PURIFYING)){
            var stack=new ItemStack(ExtraBlocks.getExtraFactory(tier,type));check(Profiles.get(stack)!=null&&Profiles.processingLines(stack)==tier.processes,"Extras type or native line count not recognized: "+tier+"/"+type);
        }
        check(Profiles.get(new ItemStack(ExtraBlocks.getExtraFactory(ExtraFactoryTier.INFINITE,FactoryType.SAWING)))==null,"Unsupported chance-output machine bypassed its recipe contract");
        var infinite=new ItemStack(ExtraBlocks.getExtraFactory(ExtraFactoryTier.INFINITE,FactoryType.CRUSHING),64);c.template.setStack(infinite);
        check(Profiles.availableParallel(c)==1088,"64 infinite factories were still capped at 512 or miscounted");
        var template=new ItemStack(ExtraBlocks.getExtraFactory(ExtraFactoryTier.ABSOLUTE,FactoryType.CRUSHING));
        template.set(MekanismDataComponents.UPGRADES,new UpgradeAware(Map.of(ExtraUpgrade.STACK,1),ItemStack.EMPTY,ItemStack.EMPTY));c.template.setStack(template);c.parallelLimit=2;
        check(c.getComponent().getUpgradeSlot().insertItem(mekanism.common.util.UpgradeUtils.getStack(ExtraUpgrade.STACK,1),Action.EXECUTE,AutomationType.MANUAL).isEmpty(),"Shared upgrade window refused an Extras stack module");
        var block=((BlockItem)template.getItem()).getBlock();long usage=Attribute.get(block,AttributeEnergy.class).getUsage();var dust=BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:dust_iron"));
        h.startSequence().thenWaitUntil(()->check(c.getComponent().getUpgrades(ExtraUpgrade.STACK)==1,"Stack upgrade did not install"))
              .thenExecute(()->{check(Compat.stackOperations(c)==4,"Template/shared stack upgrades did not combine");port(c,false).storage().insert(0,new ItemStack(Items.IRON_INGOT,8),false);c.enabled=true;})
              .thenIdle(2).thenExecute(()->{
                  check(c.processing.reserved()==8&&c.running==2&&c.powerUsed==usage*8*FactoryConfig.PROCESSING_CYCLES.get(),"Stack upgrade lost batch size, lane accounting, or energy");
                  var tag=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(tag,h.getLevel().registryAccess());check(Compat.stackOperations(c)==4,"Reload lost stack upgrades");c.enabled=false;
              }).thenWaitUntil(()->check(count(c.outputBank(),dust)==8,"Extras batch did not complete"))
              .thenExecute(()->{
                  check(cell.getEnergyContainer().getEnergy()==start-8*200*usage,"Extras stacking discounted or multiplied the per-item energy bill");
                  var creative=new ItemStack(ExtraBlocks.getExtraFactory(ExtraFactoryTier.INFINITE,FactoryType.CRUSHING));creative.set(MekanismDataComponents.UPGRADES,new UpgradeAware(Map.of(ExtraUpgrade.CREATIVE,1),ItemStack.EMPTY,ItemStack.EMPTY));c.template.setStack(creative);
                  cell.getEnergyContainer().setEnergy(0);port(c,false).storage().insert(0,new ItemStack(Items.IRON_INGOT),false);c.enabled=true;
              }).thenWaitUntil(()->check(count(c.outputBank(),dust)==9,"Creative upgrade required power or stalled"))
              .thenExecute(()->{c.enabled=false;check(cell.getEnergyContainer().isEmpty()&&c.powerUsed==0,"Creative work copied energy into the original cell");}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void extraInductionUsesOriginalEnergyAndRestoresItsLease(GameTestHelper h){
        if(!Compat.EXTRAS){h.succeed();return;}
        var c=formed(h,Grade.ULTIMATE,3);c.enabled=false;var cellPos=c.structure.at(1,1,1);var roof=c.structure.at(1,2,1);
        h.getLevel().setBlockAndUpdate(cellPos,ExtraBlocks.ABSOLUTE_INDUCTION_CELL.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(roof,ExtraBlocks.SUPREME_INDUCTION_PROVIDER.get().defaultBlockState());check(c.structure.valid(),"Extras induction parts did not form");
        var cell=(TileEntityExtraInductionCell)h.getLevel().getBlockEntity(cellPos);var provider=(TileEntityExtraInductionProvider)h.getLevel().getBlockEntity(roof);long start=1000000000000L;cell.getEnergyContainer().setEnergy(start);
        check(c.energy().getEnergy()==start&&c.energy().getMaxEnergy()==cell.getEnergyContainer().getMaxEnergy(),"Factory did not read the actual Extras cell");
        check(c.structure.transfer==provider.tier.getOutput()&&c.structure.transfer>Integer.MAX_VALUE,"Extras provider throughput was truncated or hard-coded");
        check(c.energy().extract(10000,Action.SIMULATE,AutomationType.INTERNAL)==10000&&cell.getEnergyContainer().getEnergy()==start,"Extras energy simulation mutated its cell");
        check(c.energy().extract(10000,Action.EXECUTE,AutomationType.INTERNAL)==10000&&cell.getEnergyContainer().getEnergy()==start-10000,"Work did not debit the Extras cell");
        var controllerItem=new ItemStack(Content.CONTROLLERS.get(Grade.ULTIMATE));c.saveToItem(controllerItem,h.getLevel().registryAccess());
        check(controllerItem.getOrDefault(MekanismDataComponents.ATTACHED_ENERGY,AttachedEnergy.EMPTY).containers().stream().allMatch(n->n==0),"Controller item duplicated an Extras battery");
        var p=player(h,roof.above());try{
            var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,roof,new BlockHitResult(roof.getCenter(),Direction.UP,roof,false));NeoForge.EVENT_BUS.post(event);
            check(event.isCanceled()&&p.containerMenu instanceof FactoryMenu&&p.containerMenu.stillValid(p),"Extras roof provider did not open the factory menu");
        }finally{close(p);}
        var tag=cell.saveWithFullMetadata(h.getLevel().registryAccess());var input=port(c,false);var cached=new Ports.PowerPort(input,c.structure.outward(input.getBlockPos()));
        h.getLevel().setBlockAndUpdate(cellPos,Blocks.AIR.defaultBlockState());check(cached.getEnergyContainerCount()==0&&c.energy().available()==0,"Removed Extras cell kept a usable lease");
        h.getLevel().setBlockAndUpdate(cellPos,ExtraBlocks.ABSOLUTE_INDUCTION_CELL.get().defaultBlockState());h.getLevel().getBlockEntity(cellPos).loadWithComponents(tag,h.getLevel().registryAccess());
        check(c.structure.valid()&&cached.getEnergy(0)==start-10000&&c.structure.cells.getFirst().tile()!=cell,"Restoring Extras storage lost energy or reused the removed entity");
        FactoryAppearance.sync(c);check(c.publishedAppearance.formed()&&c.publishedAppearance.contains(cellPos)&&c.publishedAppearance.contains(roof),"Extras induction was left outside the assembled appearance");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void legacyMaximumMigratesWithoutLosingLowerLimitsOrStock(GameTestHelper h){
        var c=formed(h,Grade.BASIC,3);c.enabled=false;c.template.setStack(new ItemStack(MekanismBlocks.CRUSHER,8));c.inputs.insert(0,new ItemStack(Items.DIAMOND,13),false);
        var tag=c.saveWithFullMetadata(h.getLevel().registryAccess());var data=tag.getCompound("factory");data.remove("parallel_revision");data.putInt("parallel",512);
        c.loadWithComponents(tag,h.getLevel().registryAccess());check(c.parallelLimit==FactoryConfig.MAX_PARALLEL&&c.template.getCount()==8&&count(c.inputs,Items.DIAMOND)==13,"Legacy maximum migration lost settings or inventory");
        data.putInt("parallel",37);c.loadWithComponents(tag,h.getLevel().registryAccess());check(c.parallelLimit==37,"A manually lowered legacy limit was overwritten");
        c.parallelLimit=512;var saved=c.saveWithFullMetadata(h.getLevel().registryAccess());c.loadWithComponents(saved,h.getLevel().registryAccess());check(c.parallelLimit==512,"A new explicit 512 limit was migrated again");h.succeed();
    }
}
