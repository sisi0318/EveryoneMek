package dev.everyonemek.botania;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class PureConversionGameTests {
    @GameTest(template = "empty", timeoutTicks = 1400)
    public static void mixedInputsUseAllSlotsAndInfusionProcessesEightInOneSecond(GameTestHelper h) {
        var purePos = new BlockPos(14, 3, 20); var pure = machine(h, purePos, ManaMachineKind.PURE); power(pure);
        pure.inputs.getFirst().setStack(new ItemStack(Items.STONE)); pure.inputs.getLast().setStack(new ItemStack(Items.OAK_LOG));
        h.setBlock(purePos.above(), Blocks.HOPPER); ((HopperBlockEntity) h.getBlockEntity(purePos.above())).setItem(0, new ItemStack(Items.STONE, 2));
        var infuser = machine(h, new BlockPos(28, 3, 20), ManaMachineKind.INFUSER); power(infuser); mana(infuser, 100000);
        infuser.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT, 64)); infuser.inputs.getLast().setStack(new ItemStack(Items.ENDER_PEARL));
        var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.MANA_INFUSION_TYPE);
        int ironCost = recipes.stream().filter(r -> r.id().getPath().equals("mana_infusion/manasteel_ingot")).findFirst().orElseThrow().value().getManaToConsume();
        int pearlCost = recipes.stream().filter(r -> r.id().getPath().equals("mana_infusion/mana_pearl")).findFirst().orElseThrow().value().getManaToConsume();
        var plan = ManaWork.find(infuser);
        check(plan != null && plan.ticks() == 20 && plan.used()[0] == 7 && plan.used()[7] == 1, "Infusion did not distribute its eight-item batch across slots");
        h.startSequence().thenIdle(21).thenExecute(() -> {
            stop(infuser);
            check(output(infuser, vazkii.botania.common.item.BotaniaItems.MANASTEEL_INGOT) == 7
                  && output(infuser, vazkii.botania.common.item.BotaniaItems.MANA_PEARL) == 1 && infuser.inputs.getLast().isEmpty(), "Infusion did not finish a mixed batch in twenty ticks");
            check(infuser.mana().getStored() == 100000 - 7L * ironCost - pearlCost, "Infusion did not pay per-item recipe mana");
            check(pure.inputs.getFirst().getCount() == 3 && pure.inputs.getLast().getCount() == 1, "Real hopper did not replenish the expanded input inventory");
        }).thenWaitUntil(() -> check(pure.inputs.stream().allMatch(slot -> slot.isEmpty()), "Mixed partial pure batch did not complete"))
              .thenExecute(() -> {
                  check(output(pure, BotaniaBlocks.LIVINGROCK.asItem()) == 3 && output(pure, BotaniaBlocks.LIVINGWOOD_LOG.asItem()) == 1,
                        "Mixed pure conversion merged the wrong recipes"); stop(pure);
              }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void expandedMachineSlotsKeepOldWorldAndDroppedItemIndices(GameTestHelper h) {
        var owner = player(h, "expanded-machine-save");
        var kinds = new ManaMachineKind[]{ManaMachineKind.PURE, ManaMachineKind.INFUSER, ManaMachineKind.ORE, ManaMachineKind.METAMORPHIC};
        ApothecaryGameTests.withUsername(owner, () -> {
            for (int index = 0; index < kinds.length; index++) {
                var kind = kinds[index]; var pos = new BlockPos(8 + index * 10, 3, 10); var tile = machine(h, pos, kind); stop(tile);
                var oldInput = new ItemStack(Items.STONE, 5); tile.inputs.getFirst().setStack(oldInput.copy());
                for (int slot = 0; slot < 6; slot++) tile.outputs.get(slot).setStack(new ItemStack(Items.OBSIDIAN, slot + 1));
                var battery = new ItemStack(mekanism.common.registries.MekanismItems.ENERGY_TABLET.get());
                battery.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Saved battery"));
                int oldSize = 1 + kind.extras + 6 + 1;
                tile.getInventorySlots(null).get(oldSize - 1).setStack(battery.copy());
                if (kind.extras == 1) tile.extras.getFirst().setStack(new ItemStack(kind == ManaMachineKind.ORE ? BotaniaBlocks.ORECHID : BotaniaBlocks.ALCHEMY_CATALYST));
                var registry = h.getLevel().registryAccess();
                var restored = (ManaMachine) BlockEntity.loadStatic(tile.getBlockPos(), tile.getBlockState(), tile.saveWithFullMetadata(registry), registry);
                check(restored != null && restored.inputs.size() == 8 && restored.outputs.get(5).getCount() == 6
                      && ItemStack.isSameItemSameComponents(restored.getInventorySlots(null).get(oldSize - 1).getStack(), battery), "World slot indices shifted");
                var drop = breakAndPick(h, tile.getBlockPos(), ManaContent.MACHINES.get(kind).get());
                var attached = drop.get(mekanism.common.registries.MekanismDataComponents.ATTACHED_ITEMS);
                check(attached != null && attached.size() == kind.inputs + kind.extras + kind.outputs + 1, "New drop layout is incomplete");
                drop.set(mekanism.common.registries.MekanismDataComponents.ATTACHED_ITEMS,
                      new mekanism.common.attachments.containers.item.AttachedItems(new java.util.ArrayList<>(attached.containers().subList(0, oldSize))));
                placeItem(owner, tile.getBlockPos(), drop); var placed = (ManaMachine) h.getBlockEntity(pos); stop(placed);
                check(placed.inputs.getFirst().getCount() == 5 && placed.inputs.getLast().isEmpty() && placed.outputs.get(5).getCount() == 6
                      && ItemStack.isSameItemSameComponents(placed.getInventorySlots(null).get(oldSize - 1).getStack(), battery), "Old item layout lost inventory during expansion");
                placed.inputs.getLast().setStack(new ItemStack(Items.STONE, 7));
                if (kind.outputs == 8) placed.outputs.getLast().setStack(new ItemStack(Items.CALCITE, 3));
                var fullDrop = breakAndPick(h, placed.getBlockPos(), ManaContent.MACHINES.get(kind).get());
                placeItem(owner, placed.getBlockPos(), fullDrop); var again = (ManaMachine) h.getBlockEntity(pos); stop(again);
                check(again.inputs.getLast().getCount() == 7 && (kind.outputs != 8 || again.outputs.getLast().getCount() == 3), "Appended slots did not survive a real drop and placement");
                h.setBlock(pos, Blocks.AIR);
            }
        });
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 2600)
    public static void pureConvertsEightTogetherThenTheRemainderWithoutLosingBlockedOutputs(GameTestHelper h) {
        var tile = machine(h, new BlockPos(20, 3, 20), ManaMachineKind.PURE); power(tile);
        var livingrock = BotaniaBlocks.LIVINGROCK.asItem();
        tile.inputs.getFirst().setStack(new ItemStack(Items.STONE, 11));
        tile.outputs.forEach(slot -> slot.setStack(new ItemStack(Items.COBBLESTONE, 64)));
        tile.outputs.getFirst().setStack(new ItemStack(livingrock, 60)); // Four spaces cannot hold a batch of eight.
        var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.PURE_DAISY_TYPE).stream()
              .filter(r -> r.id().getPath().equals("pure_daisy/livingrock")).findFirst().orElseThrow().value();
        int cycle = recipe.getTime() * 8;
        long energy = tile.energy().getEnergy(), cycleCost = tile.energy().getEnergyPerTick() * cycle;
        h.startSequence().thenIdle(5).thenExecute(() -> {
            check(tile.status() == ManaMachine.OUTPUT_FULL && tile.progressTicks() == 0 && tile.energy().getEnergy() == energy
                  && tile.inputs.getFirst().getCount() == 11, "Blocked batch consumed resources or advanced");
            tile.outputs.getFirst().setStack(new ItemStack(livingrock, 56));
        }).thenIdle(recipe.getTime() + 1).thenExecute(() -> {
            check(output(tile, livingrock) == 56 && tile.inputs.getFirst().getCount() == 11 && tile.progressTicks() > 0,
                  "Pure conversion still emitted one item per check interval");
            save(h, tile);
        }).thenWaitUntil(() -> check(tile.inputs.getFirst().getCount() == 3, "First batch did not consume exactly eight"))
              .thenExecute(() -> check(output(tile, livingrock) == 64 && tile.energy().getEnergy() == energy - cycleCost,
                    "Eight outputs were not committed together at the native cycle cost"))
              .thenIdle(5).thenExecute(() -> {
                  check(tile.inputs.getFirst().getCount() == 3 && tile.energy().getEnergy() == energy - cycleCost,
                        "Full output kept consuming the remainder");
                  tile.outputs.getFirst().setStack(new ItemStack(livingrock, 61));
              }).thenWaitUntil(() -> check(tile.inputs.getFirst().isEmpty(), "Fewer than eight inputs could not complete"))
              .thenExecute(() -> {
                  check(output(tile, livingrock) == 64 && tile.energy().getEnergy() == energy - 2 * cycleCost,
                        "Partial batch lost items or used a shorter cycle");
                  stop(tile); save(h, tile);
              }).thenSucceed();
    }
}
