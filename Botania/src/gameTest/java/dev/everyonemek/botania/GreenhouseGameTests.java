package dev.everyonemek.botania;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.*;
import mekanism.api.*;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.component.BotaniaDataComponents;
import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class GreenhouseGameTests {
    @GameTest(template = "empty", timeoutTicks = 150)
    public static void unfinishedBurnSurvivesRealDropWithoutExtraFuelOrWork(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var original = greenhouse(h, pos, BotaniaBlocks.ENDOFLAME);
        original.inputs.getFirst().setStack(new ItemStack(Items.STICK));
        long energy = original.energy().getEnergy(), cost = original.energy().getEnergyPerTick();
        var current = new java.util.concurrent.atomic.AtomicReference<>(original);
        h.startSequence().thenIdle(15).thenExecute(() -> {
            stop(original); int progress = original.progressTicks(); check(progress > 0 && progress < 50, "Fuel did not start processing");
            var owner = player(h, "greenhouse-progress");
            ApothecaryGameTests.withUsername(owner, () -> {
                var drop = breakAndPick(h, original.getBlockPos(), ManaContent.MACHINES.get(ManaMachineKind.GREENHOUSE).get());
                placeItem(owner, original.getBlockPos(), drop); var restored = (ManaMachine) h.getBlockEntity(pos);
                check(restored.progressTicks() == progress && restored.inputs.getFirst().getCount() == 1 && restored.mana().isEmpty(), "Unfinished drop lost progress or paid fuel early");
                restored.setControlType(mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl.DISABLED); current.set(restored);
            });
        }).thenWaitUntil(() -> check(current.get().inputs.getFirst().isEmpty(), "Restored burn did not finish"))
              .thenExecute(() -> {
                  var restored = current.get(); stop(restored);
                  check(restored.mana().getStored() == 72 && restored.energy().getEnergy() == energy - 50 * cost, "Restored work charged twice or generated extra mana");
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void greenhouseOrdinarySparkSuppliesMachinesAndRespectsOutputFace(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var source = greenhouse(h, pos, BotaniaBlocks.ENDOFLAME); stop(source); mana(source, 12000);
        var receiver = machine(h, pos.east(5), ManaMachineKind.INFUSER); stop(receiver);
        var owner = player(h, "greenhouse-spark");
        AdjacentPoolGameTests.configure(owner, source, RelativeSide.TOP, DataType.NONE);
        var sparkItem = new ItemStack(vazkii.botania.common.item.BotaniaItems.MANA_SPARK);
        check(vazkii.botania.common.item.ManaSparkItem.attachSpark(h.getLevel(), source.getBlockPos(), sparkItem, ItemStack.EMPTY)
              && vazkii.botania.common.item.ManaSparkItem.attachSpark(h.getLevel(), receiver.getBlockPos(), sparkItem, ItemStack.EMPTY), "Could not place ordinary greenhouse sparks");
        h.startSequence().thenIdle(25).thenExecute(() -> {
            check(source.mana().getStored() == 12000 && receiver.mana().isEmpty(), "Closed top face leaked spark mana");
            AdjacentPoolGameTests.configure(owner, source, RelativeSide.TOP, DataType.OUTPUT);
        }).thenWaitUntil(() -> check(receiver.mana().getStored() == 12000, "Ordinary spark did not supply the receiving machine"))
              .thenExecute(() -> {
                  check(source.mana().isEmpty(), "Spark output duplicated mana");
                  vazkii.botania.api.mana.spark.ManaSparkHelper.getAttachedSpark(h.getLevel(), source.getBlockPos()).entity().discard();
                  vazkii.botania.api.mana.spark.ManaSparkHelper.getAttachedSpark(h.getLevel(), receiver.getBlockPos()).entity().discard();
              }).thenSucceed();
    }
    private static ManaMachine greenhouse(GameTestHelper h, BlockPos pos, Block flower) {
        var tile = machine(h, pos, ManaMachineKind.GREENHOUSE); power(tile); tile.extras.getFirst().setStack(new ItemStack(flower)); return tile;
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void nativeFormulasReadRulesAndFixedDatapackRecipesRoundTrip(GameTestHelper h) {
        check(GreenhouseWork.recipes(h.getLevel()).size() == 9, "Eight native recipes and the test datapack recipe did not load");
        var coal = GreenhouseNative.resolve("endoflame", new ItemStack(BotaniaBlocks.ENDOFLAME), new ItemStack(Items.COAL), FluidStack.EMPTY, h.getLevel());
        check(coal != null && coal.mana() == 1197 && coal.ticks() == 800 && coal.cooldown() == 40, "Native fuel burn conversion changed");
        check(!GreenhouseNative.accepts("endoflame", new ItemStack(Items.LAVA_BUCKET)), "Endoflame accepted a fuel with a container remainder");
        var lava = GreenhouseNative.resolve("thermalily", new ItemStack(BotaniaBlocks.THERMALILY), ItemStack.EMPTY, new FluidStack(Fluids.LAVA, 1000), h.getLevel());
        check(lava != null && lava.mana() == 27000 && lava.ticks() == 600 && lava.cooldown() == 6000, "Thermalily did not read the pinned native burn/cooldown");
        var bread = new ItemStack(Items.BREAD);
        var first = GreenhouseNative.resolve("gourmaryllis", new ItemStack(BotaniaBlocks.GOURMARYLLIS), bread, FluidStack.EMPTY, h.getLevel());
        var repeated = GreenhouseNative.resolve("gourmaryllis", first.flower(), bread, FluidStack.EMPTY, h.getLevel());
        check(first.mana() == 1750 && repeated.mana() == 875 && first.ticks() == 50, "Food repeat penalty or nutrition conversion changed");
        var cake = GreenhouseNative.resolve("kekimurus", new ItemStack(BotaniaBlocks.KEKIMURUS), new ItemStack(Items.CAKE), FluidStack.EMPTY, h.getLevel());
        check(cake.mana() == 12600 && cake.ticks() == 560, "Cake did not account for seven native slices");
        var leaves = GreenhouseNative.resolve("munchdew", new ItemStack(BotaniaBlocks.MUNCHDEW), new ItemStack(Items.OAK_LEAVES), FluidStack.EMPTY, h.getLevel());
        check(leaves.mana() == 160 && leaves.ticks() == 4 && leaves.cooldown() == 0, "Munchdew rested between individual leaves");
        var tnt = GreenhouseNative.resolve("entropinnyum", new ItemStack(BotaniaBlocks.ENTROPINNYUM), new ItemStack(Items.TNT), FluidStack.EMPTY, h.getLevel());
        check(tnt.mana() == 6500 && tnt.ticks() == 80, "TNT recipe yield changed");
        var raff = GreenhouseNative.resolve("rafflowsia", new ItemStack(BotaniaBlocks.RAFFLOWSIA), new ItemStack(BotaniaBlocks.ENDOFLAME), FluidStack.EMPTY, h.getLevel());
        check(raff.mana() == 2000 && raff.ticks() == 40 && raff.flower().has(BotaniaDataComponents.LAST_FLOWERS), "Rafflowsia did not retain native history");
        var recipe = GreenhouseWork.recipes(h.getLevel()).stream().filter(r -> r.id().getPath().equals("greenhouse_test")).findFirst().orElseThrow().value();
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var codec = new GreenhouseRecipe.Serializer().codec().codec();
        var encoded = codec.encodeStart(ops, recipe).getOrThrow();
        var restored = codec.parse(ops, encoded).getOrThrow();
        check(restored.mana() == 1234 && restored.fluid().getAmount() == 250 && restored.materials().size() == 2, "Fixed datapack codec lost requirements");
        var invalid = JsonParser.parseString("{\"flower\":{\"item\":\"botania:endoflame\"},\"mana\":100}");
        boolean rejected;
        try { rejected = codec.parse(ops, invalid).isError(); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Accepted a free-mana fixed recipe without any consumable");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 250)
    public static void realHopperFuelWaitsWhenFullAndConsumesOnlyNeededItems(GameTestHelper h) {
        var pos = new BlockPos(18, 3, 18); var tile = greenhouse(h, pos, BotaniaBlocks.ENDOFLAME);
        tile.inputs.getFirst().setStack(new ItemStack(Items.STICK)); mana(tile, ManaMachine.MANA_CAPACITY);
        h.setBlock(pos.above(), Blocks.HOPPER); ((HopperBlockEntity) h.getBlockEntity(pos.above())).setItem(0, new ItemStack(Items.STICK));
        long energy = tile.energy().getEnergy();
        h.startSequence().thenIdle(10).thenExecute(() -> {
            check(tile.inputs.getFirst().getCount() == 2 && tile.progressTicks() == 0 && tile.energy().getEnergy() == energy, "Full greenhouse consumed fuel/energy or hopper failed to replenish");
            var port = h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), tile.getBlockPos(), Direction.NORTH);
            check(port != null && port.extractChemical(Long.MAX_VALUE, Action.EXECUTE).getAmount() == ManaMachine.MANA_CAPACITY, "Mana output face failed to extract");
        }).thenWaitUntil(() -> check(tile.inputs.getFirst().isEmpty(), "Two fuel batches did not finish"))
              .thenExecute(() -> {
                  stop(tile); check(tile.mana().getStored() == 144 && tile.extras.getFirst().getCount() == 1, "Fuel/flower/mana conservation failed");
                  check(GreenhouseWork.cooldown(tile.extras.getFirst().getStack()) > 0, "Fuel cooldown was not retained"); save(h, tile);
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 750)
    public static void lavaBucketsAndFluidPortStopFeedingDuringCooldownAndSurviveDrops(GameTestHelper h) {
        var pos = new BlockPos(15, 3, 15); var tile = greenhouse(h, pos, BotaniaBlocks.THERMALILY);
        var nativePos = pos.east(4); var remainingBeforePlanting = new int[1];
        tile.greenhouseFluidInput.setStack(new ItemStack(Items.LAVA_BUCKET, 2));
        var fluid = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tile.getBlockPos(), Direction.NORTH);
        check(fluid != null && fluid.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000 && tile.greenhouseFluid().isEmpty(), "Simulated fluid input mutated storage");
        check(fluid.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Native lava recipe accepted water");
        fluid.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
        h.startSequence().thenWaitUntil(() -> check(tile.mana().getStored() == 27000, "Lava did not complete one burn"))
              .thenIdle(25).thenExecute(() -> {
                  stop(tile); check(tile.greenhouseFluid().getFluidAmount() == 2000 && output(tile, Items.BUCKET) == 2, "Cooling wasted lava or bucket filling lost containers");
                  int remaining = GreenhouseWork.cooldown(tile.extras.getFirst().getStack()); check(remaining > 5900, "Cooldown was skipped");
                  remainingBeforePlanting[0] = remaining;
                  var owner = player(h, "greenhouse-save");
                  ApothecaryGameTests.withUsername(owner, () -> {
                      var drop = breakAndPick(h, tile.getBlockPos(), ManaContent.MACHINES.get(ManaMachineKind.GREENHOUSE).get());
                      placeItem(owner, tile.getBlockPos(), drop); var restored = (ManaMachine) h.getBlockEntity(pos); stop(restored);
                      check(restored.mana().getStored() == 27000 && restored.greenhouseFluid().getFluidAmount() == 2000
                            && GreenhouseWork.cooldown(restored.extras.getFirst().getStack()) == remaining && output(restored, Items.BUCKET) == 2, "Dropped greenhouse lost resources/cooldown");
                      var flower = restored.extras.getFirst().extractItem(1, Action.EXECUTE, AutomationType.MANUAL);
                      h.setBlock(nativePos.below(), Blocks.GRASS_BLOCK); placeItem(owner, h.absolutePos(nativePos), flower);
                  });
              }).thenIdle(10).thenExecute(() -> {
                  var nativeFlower = h.getBlockEntity(nativePos);
                  int remaining = nativeFlower.saveWithFullMetadata(h.getLevel().registryAccess()).getInt("cooldown");
                  check(remaining > 0 && remaining < remainingBeforePlanting[0], "Replanted native flower did not continue cooling");
                  var flower = breakAndPick(h, h.absolutePos(nativePos), BotaniaBlocks.THERMALILY);
                  var restored = (ManaMachine) h.getBlockEntity(pos); restored.extras.getFirst().setStack(flower);
                  check(GreenhouseWork.cooldown(restored.extras.getFirst().getStack()) == remaining, "Greenhouse overwrote the native cooldown with a stale second copy");
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void foodVarietyAndWoolOrderRetainFlowerHistory(GameTestHelper h) {
        var food = greenhouse(h, new BlockPos(20, 3, 20), BotaniaBlocks.GOURMARYLLIS);
        food.inputs.getFirst().setStack(new ItemStack(Items.BREAD, 3)); food.inputs.getLast().setStack(new ItemStack(Items.APPLE));
        var wool = greenhouse(h, new BlockPos(40, 3, 20), BotaniaBlocks.SPECTROLUS); GreenhouseWork.prepareFlower(wool);
        var expected = GreenhouseNative.expectedWool(wool.extras.getFirst().getStack(), h.getLevel());
        wool.inputs.getLast().setStack(expected.copyWithCount(8));
        h.startSequence().thenIdle(55).thenExecute(() -> {
            check(wool.mana().getStored() == 1200 && wool.inputs.getLast().getCount() == 7, "Spectrolus ate a stack or ignored its next colour");
            check(food.inputs.getFirst().getCount() == 2 && food.inputs.getLast().getCount() == 1, "First food digestion consumed extra food");
            check(GreenhouseWork.find(food).consume()[15] == 1, "Food selection did not prefer a different meal");
        }).thenWaitUntil(() -> check(food.inputs.getLast().isEmpty(), "Alternating meal did not finish"))
              .thenExecute(() -> {
                  stop(food); stop(wool);
                  check(food.mana().getStored() == 2870 && food.extras.getFirst().getStack().get(BotaniaDataComponents.STREAK_LENGTH) == 1, "Food variety calculation/history changed");
                  save(h, food); save(h, wool);
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 130)
    public static void fixedDatapackRecipeUsesCountsFluidRemaindersAndAdjacentPoolOutput(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var tile = greenhouse(h, pos, BotaniaBlocks.PURE_DAISY);
        var pool = pool(h, pos.north(), 0);
        tile.inputs.getFirst().setStackUnchecked(new ItemStack(Items.MILK_BUCKET)); tile.inputs.getLast().setStackUnchecked(new ItemStack(Items.WHEAT));
        tile.greenhouseFluid().insert(new FluidStack(Fluids.WATER, 500), Action.EXECUTE, AutomationType.INTERNAL);
        tile.outputs.getFirst().setStack(new ItemStack(Items.COBBLESTONE, 64));
        long energy = tile.energy().getEnergy();
        h.startSequence().thenIdle(8).thenExecute(() -> {
            check(tile.status() == ManaMachine.OUTPUT_FULL && tile.energy().getEnergy() == energy && tile.inputs.getFirst().getCount() == 1, "Blocked returned bucket spent ingredients");
            tile.outputs.getFirst().setEmpty();
        }).thenWaitUntil(() -> check(pool.getCurrentMana() == 1234, "Fixed recipe mana did not reach an adjacent pool"))
              .thenExecute(() -> {
                  stop(tile); check(tile.inputs.getFirst().isEmpty() && tile.inputs.getLast().isEmpty() && tile.greenhouseFluid().getFluidAmount() == 250
                        && output(tile, Items.BUCKET) == 1 && tile.mana().isEmpty(), "Fixed recipe resource accounting failed");
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 70)
    public static void munchdewFinishesLeavesBeforeRestingAndRedstonePauses(GameTestHelper h) {
        var tile = greenhouse(h, new BlockPos(20, 3, 20), BotaniaBlocks.MUNCHDEW);
        tile.inputs.getFirst().setStack(new ItemStack(Items.OAK_LEAVES, 3)); stop(tile);
        h.startSequence().thenIdle(5).thenExecute(() -> {
            check(tile.inputs.getFirst().getCount() == 3 && tile.mana().isEmpty(), "Redstone pause consumed leaves");
            tile.setControlType(mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl.DISABLED);
        }).thenWaitUntil(() -> check(GreenhouseWork.cooldown(tile.extras.getFirst().getStack()) > 0, "Leaf starvation did not trigger rest"))
              .thenExecute(() -> {
                  check(tile.mana().getStored() == 480 && tile.inputs.getFirst().isEmpty(), "Munchdew rested before finishing its leaves");
                  tile.inputs.getFirst().setStack(new ItemStack(Items.OAK_LEAVES));
              }).thenIdle(10).thenExecute(() -> {
                  stop(tile); check(tile.inputs.getFirst().getCount() == 1 && tile.mana().getStored() == 480, "Munchdew consumed new leaves during rest");
              }).thenSucceed();
    }
}
