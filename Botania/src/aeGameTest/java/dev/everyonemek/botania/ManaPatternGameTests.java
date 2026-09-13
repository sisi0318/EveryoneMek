package dev.everyonemek.botania;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.StorageCells;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.*;
import dev.everyonemek.botania.compat.ae2.*;
import dev.everyonemek.botania.compat.jei.ManaIngredient;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ManaPatternGameTests {
    private static GenericStack convertedMana(int amount) {
        // Verify the actual companion API, then restore its process-wide registry.
        try {
            var type = tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters.class;
            var list = type.getDeclaredField("converters"); var map = type.getDeclaredField("convertersByType");
            list.setAccessible(true); map.setAccessible(true); var oldList = list.get(null); var oldMap = map.get(null);
            try {
                ManaIngredientConverter.register();
                var converter = tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters.getConverter(ManaIngredient.TYPE);
                check(converter != null && converter.getIngredientFromStack(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)) == null, "Converter claimed an ordinary item");
                var mana = converter.getStackFromIngredient(new ManaIngredient(amount));
                check(mana.what() == ManaKeys.current() && mana.amount() == amount && converter.getIngredientFromStack(mana).amount() == amount, "JEI converter changed the mana amount");
                check(converter.getIngredientFromStack(new GenericStack(ManaKeys.current(), 0)).amount() == 1, "Zero-count search lost the mana type");
                return mana;
            } finally { list.set(null, oldList); map.set(null, oldMap); }
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    @GameTest(template = "empty", timeoutTicks = 950)
    public static void realPatternProviderPaysManaAndRoutesReagentThenReturnsRunes(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var owner = player(h, "mana-pattern"); var registry = h.getLevel().registryAccess();
        var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.RUNIC_ALTAR_TYPE).stream()
              .filter(r -> r.value().getResultItem(registry).is(vazkii.botania.common.item.BotaniaItems.RUNE_OF_AIR)).findFirst().orElseThrow().value();
        int batches = 3; var output = recipe.getResultItem(registry);
        var inputs = new ArrayList<GenericStack>();
        for (var ingredient : recipe.getIngredients()) inputs.add(GenericStack.fromItemStack(ingredient.getItems()[0].copyWithCount(1)));
        inputs.add(GenericStack.fromItemStack(recipe.getReagent().getItems()[0].copyWithCount(1)));
        var mana = convertedMana(recipe.getMana()); inputs.add(mana);
        var itemCell = AEItems.ITEM_CELL_4K.stack(); var itemStorage = StorageCells.getCellInventory(itemCell, null);
        for (var input : inputs) if (input.what() instanceof AEItemKey) itemStorage.insert(input.what(), input.amount() * batches, Actionable.MODULATE, IActionSource.empty());
        itemStorage.persist();
        var manaCell = new ItemStack(Content.MANA_CELL.get()); ManaStorageItem.store(manaCell, mana.amount() * batches);
        h.setBlock(pos, AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.setBlock(pos.east(), AEBlocks.ME_CHEST.block()); ((MEChestBlockEntity) h.getBlockEntity(pos.east())).setCell(itemCell);
        h.setBlock(pos.west(), AEBlocks.ME_CHEST.block()); var manaChest = (MEChestBlockEntity) h.getBlockEntity(pos.west()); manaChest.setCell(manaCell);
        h.setBlock(pos.north(), AEBlocks.CRAFTING_STORAGE_4K.block());
        var master = SparkMeGameTests.spark(h, pos.west(), true, DyeColor.PINK);
        master.modules.setItem(2, new ItemStack(MechanicalSparks.CHANNEL.get()));
        h.setBlock(pos.south(11), AEBlocks.PATTERN_PROVIDER.block()); var provider = (PatternProviderBlockEntity) h.getBlockEntity(pos.south(11));
        var receiver = SparkMeGameTests.spark(h, pos.south(11), false, DyeColor.PINK);
        provider.getLogic().getPatternInv().setItemDirect(0, PatternDetailsHelper.encodeProcessingPattern(inputs, List.of(GenericStack.fromItemStack(output))));
        var machine = ManaMachineGameTests.machine(h, pos.south(12), ManaMachineKind.RUNIC); ManaMachineGameTests.power(machine);
        for (int i = 0; i < recipe.getCatalysts().size(); i++)
            machine.inputs.get(15 - i).setStack(recipe.getCatalysts().get(i).getItems()[0].copyWithCount(1));
        AdjacentPoolGameTests.configure(owner, machine, TransmissionType.ITEM, RelativeSide.FRONT, DataType.INPUT_OUTPUT);
        AdjacentPoolGameTests.configure(owner, machine, RelativeSide.FRONT, DataType.NONE);
        var future = new AtomicReference<Future<ICraftingPlan>>(); var resultKey = AEItemKey.of(output);
        h.startSequence().thenWaitUntil(() -> {
            var grid = provider.getMainNode().getGrid();
            check(provider.getMainNode().isActive() && grid != null && !grid.getCraftingService().getCpus().isEmpty()
                  && grid.getCraftingService().getCraftables(k -> k.equals(resultKey)).contains(resultKey), "Pattern network is not ready");
        }).thenExecute(() -> {
            var grid = provider.getMainNode().getGrid(); grid.getStorageService().invalidateCache();
            future.set(grid.getCraftingService().beginCraftingCalculation(h.getLevel(), new ICraftingSimulationRequester() {
                @Override public IActionSource getActionSource() { return IActionSource.empty(); }
                @Override public appeng.api.networking.IGridNode getGridNode() { return provider.getMainNode().getNode(); }
            }, resultKey, (long) output.getCount() * batches, CalculationStrategy.REPORT_MISSING_ITEMS));
        }).thenWaitUntil(() -> check(future.get().isDone(), "Pattern calculation did not finish"))
              .thenExecute(() -> {
                  try {
                      var plan = future.get().get(); check(!plan.simulation(), "The encoded recipe is missing ingredients");
                      var result = provider.getMainNode().getGrid().getCraftingService().submitJob(plan, null, null, false, IActionSource.empty());
                      check(result.successful(), "CPU refused the pattern: " + result.errorCode());
                  } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                  catch (ExecutionException e) { throw new RuntimeException(e); }
              }).thenIdle(10).thenExecute(() -> {
                  check(machine.mana().isEmpty() && machine.inputs.stream().mapToInt(s -> s.getCount()).sum() == recipe.getCatalysts().size() && machine.extras.getFirst().isEmpty(), "Provider partially sent ingredients through a closed mana face");
                  AdjacentPoolGameTests.configure(owner, machine, RelativeSide.FRONT, DataType.INPUT);
              }).thenWaitUntil(() -> check(provider.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(resultKey)
                    == (long) output.getCount() * batches, "Provider did not deliver mana, reagent and ingredients or receive runes: status=" + machine.status() + ", mana=" + machine.mana().getStored() + ", recipe=" + output + ", input=" + machine.inputs.stream().map(s -> s.getStack().toString()).toList() + ", extra=" + machine.extras.getFirst().getStack() + ", output=" + machine.outputs.stream().map(s -> s.getStack().toString()).toList()))
              .thenExecute(() -> {
                  check(machine.mana().isEmpty() && manaChest.getInventory().getAvailableStacks().get(ManaKeys.current()) == 0, "Pattern mana was not paid exactly once");
                  check(machine.inputs.stream().mapToInt(s -> s.getCount()).sum() == recipe.getCatalysts().size() && machine.extras.getFirst().isEmpty(), "Pattern left unpaid or duplicate ingredients");
                  ManaMachineGameTests.stop(machine);
                  master.discard(); receiver.discard();
              }).thenSucceed();
    }
}
