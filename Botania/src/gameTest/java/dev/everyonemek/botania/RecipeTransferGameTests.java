package dev.everyonemek.botania;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.common.item.BotaniaItems;
import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class RecipeTransferGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void recipeTransferMovesRealStacksAndRollsBackFullInventory(GameTestHelper h) {
        var pos = new BlockPos(5, 2, 5); h.setBlock(pos.below(), Blocks.STONE); h.setBlock(pos, ApothecaryContent.BLOCK.get());
        var tile = (MechanicalApothecary) h.getBlockEntity(pos); var player = player(h, "recipe-fill");
        player.setPos(tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ());
        ApothecaryGameTests.withUsername(player, () -> tile.setOwnerUUID(player.getUUID()));
        var menu = new ApothecaryMenu(61, player.getInventory(), tile); player.containerMenu = menu;
        player.getInventory().clearContent();
        var id = ResourceLocation.parse("botania:petal_apothecary/pure_daisy");
        // Find by output so this remains aligned with the pinned recipe data.
        id = ApothecaryWork.options(h.getLevel()).stream().filter(r -> r.recipe().getResultItem(h.getLevel().registryAccess())
              .is(vazkii.botania.common.block.BotaniaBlocks.PURE_DAISY.asItem())).findFirst().orElseThrow().id();
        player.getInventory().setItem(0, new ItemStack(BotaniaItems.WHITE_MYSTICAL_PETAL, 12));
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT_SEEDS, 3));
        check(MachineRecipeTransfer.transfer(player, menu, id, true, false).isEmpty() && tile.inputs.stream().allMatch(s -> s.isEmpty())
              && player.getInventory().getItem(0).getCount() == 12, "Transfer simulation mutated inventory");
        check(MachineRecipeTransfer.transfer(player, menu, id, true, true).isEmpty(), "Native petal transfer failed");
        check(tile.inputs.stream().mapToInt(s -> s.getCount()).sum() == 12 && tile.reagent.getCount() == 3
              && ApothecaryWork.find(tile) != null, "Max fill missed reagent or actual recipe");
        // A conflicting input must fit back into the player's inventory before anything is written.
        tile.inputs.get(15).setStackUnchecked(new ItemStack(Items.DIAMOND, 7));
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        var before = tile.saveWithoutMetadata(h.getLevel().registryAccess());
        check(MachineRecipeTransfer.transfer(player, menu, id, false, true).equals("full"), "Full backpack did not refuse transfer");
        check(before.equals(tile.saveWithoutMetadata(h.getLevel().registryAccess())) && player.getInventory().getItem(0).getCount() == 64,
              "Rejected transfer changed machine or player inventory");
        player.containerMenu = player.inventoryMenu;
        check(MachineRecipeTransfer.transfer(player, menu, id, false, true).equals("unavailable"), "Stale menu accepted transfer");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bionicAndRunicTransfersUseDistinctExtraSlots(GameTestHelper h) {
        var pos = new BlockPos(5, 2, 5); h.setBlock(pos.below(), Blocks.STONE); h.setBlock(pos, ApothecaryContent.BLOCK.get());
        var tile = (MechanicalApothecary) h.getBlockEntity(pos); var player = player(h, "bionic-fill");
        player.setPos(tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ());
        ApothecaryGameTests.withUsername(player, () -> tile.setOwnerUUID(player.getUUID()));
        var holder = h.getLevel().getRecipeManager().getAllRecipesFor(ApothecaryContent.RECIPE_TYPE.get()).stream()
              .filter(r -> r.value().output().is(Content.LOTUS.get().asItem())).findFirst().orElseThrow();
        player.getInventory().clearContent(); int slot = 0;
        for (var ingredient : holder.value().materials()) player.getInventory().setItem(slot++, ingredient.getItems()[0].copyWithCount(1));
        player.getInventory().setItem(slot, holder.value().reagent().getItems()[0].copyWithCount(1));
        var menu = new ApothecaryMenu(62, player.getInventory(), tile); player.containerMenu = menu;
        check(MachineRecipeTransfer.transfer(player, menu, holder.id(), false, true).isEmpty() && ApothecaryWork.find(tile) != null, "Bionic recipe did not fill real input/reagent slots");
        var runic = ManaMachineGameTests.machine(h, pos.east(2), ManaMachineKind.RUNIC);
        var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(vazkii.botania.common.crafting.BotaniaRecipeTypes.RUNIC_ALTAR_TYPE).getFirst();
        player.getInventory().clearContent(); slot = 0;
        for (var ingredient : recipe.value().getIngredients()) player.getInventory().setItem(slot++, ingredient.getItems()[0].copyWithCount(1));
        for (var ingredient : recipe.value().getCatalysts()) player.getInventory().setItem(slot++, ingredient.getItems()[0].copyWithCount(1));
        player.getInventory().setItem(slot, recipe.value().getReagent().getItems()[0].copyWithCount(1));
        var runicMenu = new ManaMachineMenu(63, player.getInventory(), runic); player.containerMenu = runicMenu;
        check(MachineRecipeTransfer.transfer(player, runicMenu, recipe.id(), false, true).isEmpty(), "Runic transfer failed");
        check(!runic.extras.getFirst().isEmpty() && ManaWork.find(runic) != null && runic.recipeLock().equals(recipe.id().toString()), "Runic catalysts/reagent or recipe lock mismatch");
        check(MachineRecipeTransfer.transfer(player, runicMenu, holder.id(), false, true).equals("unsupported"), "Wrong machine accepted bionic recipe");
        player.containerMenu = player.inventoryMenu; h.succeed();
    }
}
