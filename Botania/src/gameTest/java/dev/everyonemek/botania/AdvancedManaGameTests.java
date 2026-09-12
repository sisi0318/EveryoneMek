package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.brew.BrewContainer;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.AlfheimPortalBlockEntity;
import vazkii.botania.common.block.block_entity.ManaEnchanterBlockEntity;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.item.BotaniaItems;

import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class AdvancedManaGameTests {
    @GameTest(template = "empty", timeoutTicks = 460)
    public static void pureBrewAndTerraRespectNativeCostsAndPlatform(GameTestHelper h) {
        var pure = machine(h, new BlockPos(12, 2, 12), ManaMachineKind.PURE); power(pure); pure.inputs.getFirst().setStack(new ItemStack(Items.STONE));
        check(ManaWork.choices(pure).stream().noneMatch(choice -> choice.id().getPath().contains("deepslate")), "Pure converter accepted a world-function recipe");
        var brewery = machine(h, new BlockPos(22, 2, 12), ManaMachineKind.BREWERY); power(brewery); mana(brewery, 200000);
        var vessel = new ItemStack(BotaniaItems.MANAGLASS_VIAL);
        var brew = h.getLevel().getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.BREW_TYPE).getFirst().value();
        for (int i = 0; i < brew.getIngredients().size(); i++) brewery.inputs.get(i).setStack(brew.getIngredients().get(i).getItems()[0].copyWithCount(1));
        brewery.extras.getFirst().setStack(vessel); var brewed = brew.getOutput(vessel.copy());
        int brewCost = ((BrewContainer) vessel.getItem()).getManaCost(brew.getBrew(), vessel.copy());
        var terra = machine(h, new BlockPos(32, 2, 12), ManaMachineKind.TERRA); power(terra); mana(terra, 600000);
        var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.TERRA_PLATE_TYPE).getFirst().value();
        for (int i = 0; i < recipe.getIngredients().size(); i++) terra.inputs.get(i).setStack(recipe.getIngredients().get(i).getItems()[0].copyWithCount(1));
        long energy = terra.energy().getEnergy(); terra.onUpdateServer();
        check(terra.status() == ManaMachine.STRUCTURE && terra.energy().getEnergy() == energy && terra.mana().getStored() == 600000, "Terra processed without a real platform");
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) h.setBlock(new BlockPos(32 + x, 1, 12 + z),
              Math.abs(x) + Math.abs(z) == 1 ? Blocks.LAPIS_BLOCK : BotaniaBlocks.LIVINGROCK);
        h.startSequence().thenIdle(40).thenExecute(() -> {
            check(terra.progressTicks() > 0, "Terra did not start on tagged platform"); save(h, terra);
            h.setBlock(new BlockPos(32, 1, 11), Blocks.AIR);
        }).thenIdle(8).thenExecute(() -> {
            check(terra.status() == ManaMachine.STRUCTURE && terra.mana().getStored() == 600000, "Broken platform spent recipe mana");
            h.setBlock(new BlockPos(32, 1, 11), Blocks.LAPIS_BLOCK);
        }).thenWaitUntil(() -> {
            check(!pure.outputs.getFirst().isEmpty() && !brewery.outputs.getFirst().isEmpty() && !terra.outputs.getFirst().isEmpty(),
                  "Advanced recipes pending: pure=" + pure.status() + ", brew=" + brewery.status() + ", terra=" + terra.status());
        }).thenExecute(() -> {
            stop(pure); stop(brewery); stop(terra);
            check(ItemStack.isSameItemSameComponents(brewed, brewery.outputs.getFirst().getStack()) && brewery.mana().getStored() == 200000 - brewCost,
                  "Brew ignored native container output/components or mana cost");
            check(terra.mana().getStored() == 600000 - recipe.getMana() && pure.outputs.getFirst().getStack().is(BotaniaBlocks.LIVINGROCK.asItem()), "Terra or pure conversion cost/output mismatch");
        }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void randomOresReserveSpaceAndPersistCommittedResult(GameTestHelper h) {
        var ore = machine(h, new BlockPos(18, 2, 18), ManaMachineKind.ORE); power(ore); mana(ore, 200000);
        ore.inputs.getFirst().setStack(new ItemStack(Items.STONE)); ore.extras.getFirst().setStack(new ItemStack(BotaniaBlocks.ORECHID));
        var plan = ManaWork.find(ore); check(plan != null && plan.results().size() > 1, "Ore table did not match stone");
        ore.outputs.forEach(slot -> slot.setStack(new ItemStack(Items.COBBLESTONE, 64)));
        long energy = ore.energy().getEnergy(); ore.onUpdateServer();
        check(ore.status() == ManaMachine.OUTPUT_FULL && ore.energy().getEnergy() == energy && ore.mana().getStored() == 200000, "Blocked random work spent resources");
        ore.outputs.forEach(slot -> slot.setStackUnchecked(ItemStack.EMPTY));
        for (int tick = 0; tick < plan.ticks(); tick++) ore.onUpdateServer();
        check(ore.inputs.getFirst().isEmpty() && !ore.outputs.getFirst().isEmpty(), "Ore batch did not commit");
        var result = ore.outputs.getFirst().getStack();
        check(plan.results().stream().anyMatch(option -> option.mana() == 200000 - ore.mana().getStored()
              && option.randomOutput().getDisplayed().stream().anyMatch(state -> result.is(state.getBlock().asItem()))), "Ore selection did not pay its actual recipe cost");
        save(h, ore); stop(ore);
        var morph = machine(h, new BlockPos(28, 2, 18), ManaMachineKind.METAMORPHIC); power(morph); mana(morph, 10000); morph.inputs.getFirst().setStack(new ItemStack(Items.STONE));
        var morphPlan = ManaWork.find(morph); check(morphPlan != null, "Metamorphic table missing");
        for (int i = 0; i < morphPlan.ticks(); i++) morph.onUpdateServer();
        check(!morph.outputs.getFirst().isEmpty(), "Metamorphic conversion failed"); stop(morph);
        ore.inputs.getFirst().setStack(new ItemStack(Items.NETHERRACK)); ore.extras.getFirst().setStack(new ItemStack(BotaniaBlocks.ORECHID_IGNEM));
        check(h.getLevel().dimensionType().hasCeiling() || ManaWork.find(ore) == null, "Ignem ignored the native ceiling condition"); h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 130)
    public static void realElvenPortalPaysOpeningAndTradeExactlyOnce(GameTestHelper h) {
        BlockPos center = new BlockPos(20, 2, 20); h.setBlock(center, BotaniaBlocks.ELVEN_GATEWAY_CORE);
        for (int x = -1; x <= 1; x++) {
            if (x != 0) h.setBlock(center.offset(x, 0, 0), BotaniaBlocks.LIVINGWOOD_LOG);
            h.setBlock(center.offset(x, 4, 0), x == 0 ? BotaniaBlocks.GLIMMERING_LIVINGWOOD_LOG : BotaniaBlocks.LIVINGWOOD_LOG);
        }
        for (int x : new int[]{-2, 2}) for (int y = 1; y <= 3; y++) h.setBlock(center.offset(x, y, 0), y == 2 ? BotaniaBlocks.GLIMMERING_LIVINGWOOD_LOG : BotaniaBlocks.LIVINGWOOD_LOG);
        var left = pool(h, center.offset(-3, 0, -3), 150000); var right = pool(h, center.offset(3, 0, -3), 150000);
        h.setBlock(center.offset(-3, 1, -3), BotaniaBlocks.NATURA_PYLON); h.setBlock(center.offset(3, 1, -3), BotaniaBlocks.NATURA_PYLON);
        var portal = (AlfheimPortalBlockEntity) h.getBlockEntity(center);
        var controller = machine(h, center.north(), ManaMachineKind.ELVEN); power(controller); controller.applySetting(1, Integer.toString(RelativeSide.BACK.ordinal()));
        controller.inputs.getFirst().setStack(new ItemStack(BotaniaItems.MANASTEEL_INGOT, 2));
        h.startSequence().thenIdle(2).thenExecute(() -> check(portal.onUsedByWand(null, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST), Direction.UP), "Real elven frame failed to open"))
              .thenWaitUntil(() -> check(output(controller, BotaniaItems.ELEMENTIUM_INGOT) == 1, "Portal trade pending: stage=" + portal.ticksOpen + ", status=" + controller.status()))
              .thenExecute(() -> {
                  stop(controller); check(left.getCurrentMana() + right.getCurrentMana() == 300000 - AlfheimPortalBlockEntity.MANA_COST_OPENING - AlfheimPortalBlockEntity.MANA_COST,
                        "Controller bypassed or double-charged native opening/trade mana");
                  check(controller.inputs.getFirst().isEmpty(), "Trade did not consume exact matched count");
                  h.setBlock(center.offset(-2, 2, 0), Blocks.AIR); controller.inputs.getFirst().setStack(new ItemStack(BotaniaItems.MANASTEEL_INGOT, 2));
                  NativeControllers.tick(controller); check(controller.status() == ManaMachine.STRUCTURE && controller.inputs.getFirst().getCount() == 2, "Broken native portal still traded");
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 500)
    public static void realEnchanterKeepsBooksAndResumesAfterStructureRepair(GameTestHelper h) {
        var owner = player(h, "real-enchanter"); BlockPos center = new BlockPos(24, 3, 24);
        String[] base = {"___________", "____BBB____", "___B_B_B___", "___BB0BB___", "___B_B_B___", "____BBB____", "___________"};
        String[] flowers = {"_F_______F_", "___________", "____F_F____", "F____L____F", "____F_F____", "___________", "_F_______F_"};
        for (int z = 0; z < 7; z++) for (int x = 0; x < 11; x++) {
            if (base[z].charAt(x) != '_') h.setBlock(center.offset(x - 5, -1, z - 3), Blocks.OBSIDIAN);
            if (flowers[z].charAt(x) == 'F') {
                h.setBlock(center.offset(x - 5, -1, z - 3), Blocks.DIRT); h.setBlock(center.offset(x - 5, 0, z - 3), BotaniaBlocks.WHITE_MYSTICAL_FLOWER);
                if (Math.abs(x - 5) >= 4) h.setBlock(center.offset(x - 5, 1, z - 3), BotaniaBlocks.MANA_PYLON);
            }
        }
        h.setBlock(center, Blocks.LAPIS_BLOCK);
        var wandable = ManaEnchanterBlockEntity.createLapisBlockWandable(h.getLevel(), h.absolutePos(center), Blocks.LAPIS_BLOCK.defaultBlockState(), null, Direction.UP);
        check(wandable.onUsedByWand(owner, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST), Direction.UP), "Native enchanter structure did not form");
        var enchanter = (ManaEnchanterBlockEntity) h.getBlockEntity(center);
        var machine = machine(h, center.west(), ManaMachineKind.ENCHANTER); power(machine); mana(machine, 300000);
        machine.applySetting(1, Integer.toString(RelativeSide.fromDirections(machine.getDirection(), Direction.EAST).ordinal()));
        var book = new ItemStack(Items.ENCHANTED_BOOK); var sharpness = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);
        EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(sharpness, 3)); machine.extras.getFirst().setStack(book.copy()); machine.inputs.getFirst().setStack(new ItemStack(Items.DIAMOND_SWORD));
        int[] pausedStageTicks = {0};
        h.startSequence().thenWaitUntil(() -> check(enchanter.stage == ManaEnchanterBlockEntity.State.GATHER_MANA && enchanter.getCurrentMana() > 0, "Enchanter did not gather mana from controller"))
              .thenExecute(() -> { save(h, machine); h.setBlock(center.offset(-1, 0, -1), Blocks.AIR); pausedStageTicks[0] = enchanter.stageTicks; })
              .thenIdle(8).thenExecute(() -> {
                  check(enchanter.stageTicks == pausedStageTicks[0] && !enchanter.itemToEnchant.isEmpty(), "Broken structure destroyed or advanced controlled work");
                  h.setBlock(center.offset(-1, 0, -1), BotaniaBlocks.WHITE_MYSTICAL_FLOWER);
              }).thenWaitUntil(() -> check(!machine.outputs.getFirst().isEmpty(), "Enchanter did not finish after repair: status=" + machine.status() + ", stage=" + enchanter.stage))
              .thenExecute(() -> {
                  stop(machine); var output = machine.outputs.getFirst().getStack();
                  check(EnchantmentHelper.getItemEnchantmentLevel(sharpness, output) == 3 && ItemStack.isSameItemSameComponents(book, machine.extras.getFirst().getStack()), "Native enchantment or retained book mismatch");
                  check(!output.has(DataComponents.CUSTOM_DATA) && enchanter.itemToEnchant.isEmpty() && machine.mana().getStored() < 300000, "Enchanter output retained job metadata or duplicated equipment");
              }).thenSucceed();
    }
}
