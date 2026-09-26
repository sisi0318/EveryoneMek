package dev.everyonemek.gravity;
import static dev.everyonemek.gravity.ReactorTests.check;
import dev.everyonemek.gravity.corona.*;
import dev.everyonemek.gravity.solar.*;
import java.util.*;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(MekGravity.ID) @PrefixGameTestTemplate(false)
public final class CoronalTests{
    @GameTest(template="empty",timeoutTicks=80)
    public static void coronalBulkSlotsSaveClickAndReplace(GameTestHelper h){var c=SolarTests.formed(h);var m=attach(h,c);m.autoEject=false;var level=h.getLevel();var registry=level.registryAccess();
        var input=level.getCapability(Capabilities.ItemHandler.BLOCK,m.getBlockPos(),Direction.UP);
        check(input.getSlotLimit(0)==4096&&input.insertItem(0,new ItemStack(Items.RAW_IRON,4096),true).isEmpty()&&m.inputs.getFirst().isEmpty(),"Bulk input capacity/simulation incorrect");
        input.insertItem(0,new ItemStack(Items.RAW_IRON,4096),false);check(input.insertItem(0,new ItemStack(Items.RAW_IRON),false).getCount()==1,"Bulk slot exceeded capacity");m.outputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT,4096));
        var p=ReactorTests.player(h,m.getBlockPos().relative(m.getDirection()));try{
            p.gameMode.useItemOn(p,level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(m.getBlockPos().getCenter(),m.getDirection(),m.getBlockPos(),false));var menu=p.containerMenu;check(menu instanceof CoronalMenu,"Bulk inventory GUI did not open");
            menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,p);check(menu.getCarried().getCount()==64&&m.inputs.getFirst().getCount()==4032,"Manual extraction created an oversized hand stack");
            menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,p);check(menu.getCarried().isEmpty()&&m.inputs.getFirst().getCount()==4096,"Manual merge into a bulk stack lost material");
            menu.clicked(0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,p);int carried=p.getInventory().countItem(Items.RAW_IRON);check(carried>0&&carried+m.inputs.getFirst().getCount()==4096,"Shift extraction broke bulk conservation");
            p.getInventory().clearContent();m.inputs.getFirst().setStack(new ItemStack(Items.RAW_IRON,4096));
            var tag=m.saveWithFullMetadata(registry);var reload=(CoronalMachine)BlockEntity.loadStatic(m.getBlockPos(),m.getBlockState(),tag,registry);check(reload.inputs.getFirst().getCount()==4096&&reload.outputs.getFirst().getCount()==4096,"Bulk world serialization truncated inventory");
            var dropped=Block.getDrops(m.getBlockState(),level,m.getBlockPos(),m).getFirst();var item=ItemStack.parseOptional(registry,(net.minecraft.nbt.CompoundTag)dropped.save(registry));check(!item.isEmpty(),"Bulk dropped-item serialization failed");
            var pos=m.getBlockPos();var outward=m.getDirection();p.closeContainer();level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());p.setYRot(outward.getOpposite().toYRot());p.setItemInHand(InteractionHand.MAIN_HAND,item);p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
            var replacement=(CoronalMachine)level.getBlockEntity(pos);check(replacement.inputs.getFirst().getCount()==4096&&replacement.outputs.getFirst().getCount()==4096,"Bulk inventory changed on real replacement");
        }finally{p.containerMenu.setCarried(ItemStack.EMPTY);ReactorTests.close(p);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=360)
    public static void coronalFourWingsSustainRatedThroughput(GameTestHelper h){
        var c=SolarTests.formed(h);hot(c);var chambers=new ArrayList<CoronalMachine>();var bins=new ArrayList<mekanism.common.tile.TileEntityBin>();
        int[][] points={{-1,4,4},{9,4,4},{4,4,-1},{4,4,9}};Direction[] faces={Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH};
        for(int i=0;i<4;i++){var pos=c.structure.at(points[i][0],points[i][1],points[i][2]);var face=c.structure.direction(faces[i]);h.getLevel().setBlockAndUpdate(pos,Attribute.setFacing(CoronalContent.BLOCK.get().defaultBlockState(),face));
            var m=(CoronalMachine)h.getLevel().getBlockEntity(pos);chambers.add(m);check(m.solar()==c,"Wing attachment failed: "+i);
            var target=pos.relative(face);h.getLevel().setBlockAndUpdate(target,mekanism.common.registries.MekanismBlocks.ULTIMATE_BIN.get().defaultBlockState());bins.add((mekanism.common.tile.TileEntityBin)h.getLevel().getBlockEntity(target));
        }
        long[] supplied={0},baseline={0},tick={0};boolean[] feeding={true};
        h.onEachTick(()->{if(feeding[0])for(var m:chambers){var input=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,m.getBlockPos(),Direction.UP);for(int slot=0;slot<9;slot++)supplied[0]+=64-input.insertItem(slot,new ItemStack(Items.RAW_IRON,64),false).getCount();}});
        h.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo t){}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){}private void stop(){feeding[0]=false;c.enabled=false;}public void testPassed(GameTestInfo t,GameTestRunner r){stop();}public void testFailed(GameTestInfo t,GameTestRunner r){stop();}});
        var sequence=h.startSequence();
        for(int tier=0;tier<4;tier++){int grade=tier;sequence.thenExecute(()->SolarTests.tier(h,c,grade)).thenIdle(20)
            .thenExecute(()->{baseline[0]=bins.stream().mapToLong(b->b.getBinSlot().getCount()).sum();tick[0]=h.getLevel().getGameTime();})
            .thenIdle(40).thenExecute(()->{
                long elapsed=h.getLevel().getGameTime()-tick[0],actual=bins.stream().mapToLong(b->b.getBinSlot().getCount()).sum()-baseline[0];int ticks=8>>grade;
                check(elapsed==40&&actual==4L*512*elapsed/ticks,"Tier "+grade+" sustained throughput: "+actual+" items / "+elapsed+" ticks; expected "+(4L*512*40/ticks));
                long accounted=bins.stream().mapToLong(b->b.getBinSlot().getCount()).sum();for(var m:chambers){accounted+=m.remaining;accounted+=m.inputs.stream().mapToLong(s->s.getCount()).sum()+m.outputs.stream().mapToLong(s->s.getCount()).sum();check(m.batch==512&&m.paidEnergy==512*SolarConfig.CORONAL_ENERGY.get(),"High throughput lost displayed batch or payment");}
                check(accounted==supplied[0],"High-throughput supply/output conservation failed");check(c.stored>=c.capacity()-4*512*SolarConfig.CORONAL_ENERGY.get(),"Four chambers cannot sustain themselves from actual solar generation");
            });}
        sequence.thenExecute(()->{feeding[0]=false;c.enabled=false;}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void coronalRetainsLegacyWorkAndBoundsPartialBatches(GameTestHelper h){
        check(SolarConfig.upgradedBatch(0,32)==512&&SolarConfig.upgradedBatch(0,48)==48&&SolarConfig.upgradedBatch(1,32)==32,"Batch config migration overwrote a custom value");
        check(SolarConfig.upgradedEnergy(0,5_000_000_000L)==1_000_000_000L&&SolarConfig.upgradedEnergy(0,7)==7&&SolarConfig.upgradedEnergy(1,5_000_000_000L)==5_000_000_000L,"Energy migration was not one-time/default-only");
        var c=SolarTests.formed(h);var m=attach(h,c);hot(c);m.autoEject=false;m.inputs.getFirst().setStack(new ItemStack(Items.RAW_IRON,64));for(var slot:m.outputs)slot.setStack(new ItemStack(Items.IRON_INGOT,CoronalInventorySlot.CAPACITY-1));
        c.stored=c.reserve()+3*SolarConfig.CORONAL_ENERGY.get();CoronalMachine.processAttached(c);c.enabled=false;
        check(m.batch==3&&m.remaining==3&&m.inputs.getFirst().getCount()==61&&c.stored==c.reserve(),"Partial funded batch failed reserve/space bounds");
        var saved=m.saveWithFullMetadata(h.getLevel().registryAccess());var work=saved.getCompound("coronal_work");work.putInt("batch",32);work.putInt("remaining",32);work.putInt("duration",40);work.putInt("progress",17);work.putLong("paid",160_000_000_000L);
        var legacy=(CoronalMachine)BlockEntity.loadStatic(m.getBlockPos(),m.getBlockState(),saved,h.getLevel().registryAccess());
        check(legacy.batch==32&&legacy.remaining==32&&legacy.duration==40&&legacy.progress==17&&legacy.paidEnergy==160_000_000_000L,"Upgrade recalculated old prepaid work");h.succeed();
    }
    private static CoronalMachine attach(GameTestHelper h,SolarController c){var pos=c.structure.at(4,4,-1);var out=c.structure.direction(Direction.NORTH);h.getLevel().setBlockAndUpdate(pos,Attribute.setFacing(CoronalContent.BLOCK.get().defaultBlockState(),out));var m=(CoronalMachine)h.getLevel().getBlockEntity(pos);check(m.solar()==c,"Chamber did not recognize the collector center");return m;}
    private static void hot(SolarController c){c.enabled=c.ignited=true;c.stored=c.capacity();c.fuelRemaining=c.fuelTotal=100_000_000_000_000_000L;}
    @GameTest(template="empty",timeoutTicks=180)
    public static void coronalRunsRealHopperAndChest(GameTestHelper h){var c=SolarTests.formed(h);var m=attach(h,c);hot(c);var level=h.getLevel();
        var top=m.getBlockPos().above();level.setBlockAndUpdate(top,Blocks.HOPPER.defaultBlockState());var hopper=(HopperBlockEntity)level.getBlockEntity(top);hopper.setItem(0,new ItemStack(Items.RAW_IRON,2));
        var front=m.getBlockPos().relative(m.getDirection());level.setBlockAndUpdate(front,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)level.getBlockEntity(front);
        var input=level.getCapability(Capabilities.ItemHandler.BLOCK,m.getBlockPos(),Direction.UP);var output=level.getCapability(Capabilities.ItemHandler.BLOCK,m.getBlockPos(),m.getDirection());
        check(input!=null&&output!=null&&input.getSlots()==9&&output.getSlots()==9,"Native sided inventory missing");
        check(input.insertItem(0,new ItemStack(Items.RAW_IRON,64),true).isEmpty()&&m.inputs.getFirst().isEmpty(),"Input simulation mutated inventory");input.insertItem(0,new ItemStack(Items.RAW_IRON,64),false);
        check(output.insertItem(0,new ItemStack(Items.DIRT),false).getCount()==1,"Output accepted external insertion");
        h.startSequence().thenWaitUntil(()->{int total=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.IRON_INGOT))total+=chest.getItem(i).getCount();check(total==66,"Real hopper/auto-output pending: "+total+" status="+m.status);})
        .thenExecute(()->{check(hopper.isEmpty()&&m.inputs.stream().allMatch(s->s.isEmpty())&&m.remaining==0,"Input/product conservation failed");c.enabled=false;}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void coronalPaidBatchSurvivesPauseAndDrop(GameTestHelper h){var c=SolarTests.formed(h);var m=attach(h,c);hot(c);m.autoEject=false;for(int i=0;i<8;i++)m.inputs.get(i).setStack(new ItemStack(Items.RAW_IRON,64));m.inputs.get(8).setStack(new ItemStack(Items.RAW_IRON,32));long before=c.stored;
        CoronalMachine.processAttached(c);check(m.remaining==512&&m.batch==512&&m.progress==1&&before-c.stored==512*SolarConfig.CORONAL_ENERGY.get(),"Batch was not paid atomically");c.enabled=false;
        var saved=m.saveWithFullMetadata(h.getLevel().registryAccess());var loaded=(CoronalMachine)BlockEntity.loadStatic(m.getBlockPos(),m.getBlockState(),saved,h.getLevel().registryAccess());
        check(loaded.remaining==512&&loaded.progress==1&&loaded.inputs.get(8).getCount()==32,"Saved paid work or real inventory missing");
        h.startSequence().thenIdle(3).thenExecute(()->check(m.progress==1,"Cold core advanced paid work"))
        .thenExecute(()->{
            var drops=Block.getDrops(m.getBlockState(),h.getLevel(),m.getBlockPos(),m);check(drops.size()==1&&drops.getFirst().get(CoronalContent.WORK.get()).getInt("remaining")==512,"Drop lost paid work");
            var pos=m.getBlockPos();var outward=m.getDirection();h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());var p=ReactorTests.player(h,pos.relative(outward));
            try{p.setYRot(outward.getOpposite().toYRot());p.setItemInHand(InteractionHand.MAIN_HAND,drops.getFirst());p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));}finally{ReactorTests.close(p);}
            var replacement=(CoronalMachine)h.getLevel().getBlockEntity(pos);check(replacement!=null&&replacement.remaining==512&&replacement.progress==1&&replacement.inputs.get(8).getCount()==32,"Real placement failed to restore work and inventory");
            replacement.inputs.forEach(slot->slot.setStack(ItemStack.EMPTY));for(var slot:replacement.outputs)slot.setStack(new ItemStack(Items.COBBLESTONE,CoronalInventorySlot.CAPACITY));hot(c);
        }).thenIdle(43).thenExecute(()->{
            var replacement=(CoronalMachine)h.getLevel().getBlockEntity(m.getBlockPos());check(replacement.remaining==512&&replacement.progress==replacement.duration&&replacement.status.equals("output_full"),"Blocked paid products were lost");
            check(c.stored==c.capacity(),"Resumed prepaid work charged a second time");replacement.outputs.forEach(slot->slot.setStack(ItemStack.EMPTY));c.enabled=false;
        }).thenIdle(2).thenExecute(()->{var replacement=(CoronalMachine)h.getLevel().getBlockEntity(m.getBlockPos());check(replacement.remaining==0&&replacement.outputs.getFirst().getStack().is(Items.IRON_INGOT)&&replacement.outputs.stream().mapToInt(slot->slot.getCount()).sum()==512,"Completed products could not leave a cold chamber");}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=120)
    public static void coronalAlloysAndOverlappingInputs(GameTestHelper h){
        var overlap=new CoronalRecipe.Inventory(List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.COPPER_INGOT)));
        var used=CoronalRecipe.allocate(overlap,List.of(new CoronalRecipe.Input(Ingredient.of(Items.IRON_INGOT,Items.COPPER_INGOT),1),new CoronalRecipe.Input(Ingredient.of(Items.IRON_INGOT),1)),1);
        check(Arrays.equals(used,new int[]{1,1}),"Overlapping recipe allocated the exact ingredient to the wrong requirement");
        var c=SolarTests.formed(h);var m=attach(h,c);hot(c);m.autoEject=false;m.inputs.get(0).setStack(new ItemStack(Items.COPPER_INGOT,8));m.inputs.get(1).setStack(new ItemStack(Items.COPPER_INGOT,8));m.inputs.get(2).setStack(new ItemStack(Items.REDSTONE,16));
        CoronalMachine.processAttached(c);check(m.batch==16&&m.remaining==16&&m.inputs.stream().allMatch(s->s.isEmpty()),"Custom alloy recipe failed split input allocation");
        h.startSequence().thenWaitUntil(()->check(m.outputs.getFirst().getCount()==16,"Alloy batch pending"))
        .thenExecute(()->{check(m.outputs.getFirst().getStack().is(mekanism.common.registries.MekanismItems.INFUSED_ALLOY.get()),"Wrong alloy output");m.inputs.get(0).setStack(new ItemStack(mekanism.common.registries.MekanismItems.INFUSED_ALLOY.get(),4));m.inputs.get(1).setStack(new ItemStack(mekanism.common.registries.MekanismItems.ENRICHED_DIAMOND.get()));})
        .thenIdle(6).thenExecute(()->{check(m.status.equals("tier_low")&&m.inputs.getFirst().getCount()==4,"Tier gate consumed inputs");
            var p=ReactorTests.player(h,m.getBlockPos().relative(m.getDirection()));try{p.gameMode.useItemOn(p,h.getLevel(),ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(m.getBlockPos().getCenter(),m.getDirection(),m.getBlockPos(),false));check(p.containerMenu instanceof CoronalMenu,"Native machine menu missing");check(p.containerMenu.clickMenuButton(p,0)&&!m.enabled,"Menu pause ignored");}finally{ReactorTests.close(p);}c.enabled=false;
        }).thenSucceed();
    }
}
