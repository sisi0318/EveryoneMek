package dev.everyonemek.botania;

import appbot.ABBlocks;
import appbot.ABItems;
import appbot.ae2.ManaKey;
import appbot.block.FluixPoolBlockEntity;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.storage.StorageCells;
import appeng.api.util.AEColor;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.*;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class AppliedBotanicsPoolGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void nativeAppbotCapacitiesAndOverfullCellsRemainSafe(GameTestHelper h) {
        var source = IActionSource.empty(); var registries = h.getLevel().registryAccess();
        for (var tier : ABItems.Tier.values()) {
            for (var item : java.util.List.of(ABItems.get(tier).get(), ABItems.getPortableCell(tier).get())) {
                var stack = new ItemStack(item); var type = (appbot.item.cell.IManaCellItem) item;
                int kb = new int[]{1, 4, 16, 64, 256}[tier.ordinal()]; long capacity = kb * 500_000L;
                check(type.getTotalBytes() == kb * 1000L && appbot.ae2.ManaKeyType.TYPE.getAmountPerByte() == 500,
                      "Appbot capacity is still being overridden");
                var cell = StorageCells.getCellInventory(stack, null);
                check(cell.insert(ManaKey.KEY, Long.MAX_VALUE, Actionable.SIMULATE, source) == capacity, "Native capacity wrong");
                long oldAmount = kb * 1024L * 8000; stack.set(appbot.AppliedBotanicsForge.MANA, oldAmount);
                var loaded = ItemStack.parseOptional(registries, (net.minecraft.nbt.CompoundTag) stack.saveOptional(registries));
                cell = StorageCells.getCellInventory(loaded, null);
                check(cell.getAvailableStacks().get(ManaKey.KEY) == oldAmount && cell.getStatus() == appeng.api.storage.cells.CellState.FULL,
                      "Old Appbot cell contents were truncated or hidden");
                check(cell.insert(ManaKey.KEY, 100, Actionable.SIMULATE, source) == 0 && cell.insert(ManaKey.KEY, 100, Actionable.MODULATE, source) == 0,
                      "Overfull Appbot insertion returned negative mana");
                check(cell.extract(ManaKey.KEY, -100, Actionable.MODULATE, source) == 0 && cell.getAvailableStacks().get(ManaKey.KEY) == oldAmount,
                      "Negative Appbot extraction created mana");
                long drain = oldAmount - capacity + 1;
                check(cell.extract(ManaKey.KEY, drain, Actionable.MODULATE, source) == drain && cell.insert(ManaKey.KEY, 10, Actionable.MODULATE, source) == 1,
                      "Drained Appbot cell failed to recover normal capacity");
                cell.persist(); check(loaded.getOrDefault(appbot.AppliedBotanicsForge.MANA, 0L) == capacity, "Appbot persistence lost mana");
            }
        }
        var voidCell = new ItemStack(ABItems.MANA_CELL_1K.get());
        ((appbot.item.cell.IManaCellItem) voidCell.getItem()).getUpgrades(voidCell).setItemDirect(0, new ItemStack(AEItems.VOID_CARD.asItem()));
        voidCell.set(appbot.AppliedBotanicsForge.MANA, 8_192_000L);
        var cell = StorageCells.getCellInventory(voidCell, null);
        check(cell.insert(ManaKey.KEY, 123, Actionable.MODULATE, source) == 123 && cell.getAvailableStacks().get(ManaKey.KEY) == 8_192_000,
              "Void card erased old stored mana instead of only voiding new input");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 250)
    public static void fluixPoolUsesItsMeInventoryAndCannotMountItTwice(GameTestHelper h) {
        var nativeCell = StorageCells.getCellInventory(new ItemStack(ABItems.MANA_CELL_1K.get()), null);
        check(nativeCell.insert(ManaKey.KEY, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty()) == ManaCellTier.K1.capacity,
              "Native and addon mana cells disagree on tier capacity");
        check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("appbot:mana_cell_housing")).isPresent(), "Appbot housing recipe did not load");
        var pos = new BlockPos(20, 3, 20); var owner = player(h, "fluix-pool");
        h.setBlock(pos.west(), AEBlocks.ME_CHEST.block()); var chest = (MEChestBlockEntity) h.getBlockEntity(pos.west());
        var disk = new ItemStack(Content.MANA_CELL.get()); var cell = StorageCells.getCellInventory(disk, null);
        check(cell != null && cell.insert(ManaKey.KEY, 12000, Actionable.MODULATE, IActionSource.empty()) == 12000, "Shared mana cell unavailable"); chest.setCell(disk);
        h.setBlock(pos.west(2), AEBlocks.CREATIVE_ENERGY_CELL.block()); h.setBlock(pos, ABBlocks.FLUIX_MANA_POOL.get());
        var pool = (FluixPoolBlockEntity) h.getBlockEntity(pos);
        var bridge = machine(h, pos.south(), ManaMachineKind.BRIDGE); power(bridge); stop(bridge);
        var access = ManaAccess.at(h.getLevel(), pool.getBlockPos(), Direction.SOUTH);
        check(access != null, "Fluix pool not recognized");
        h.startSequence().thenWaitUntil(() -> check(pool.getMainNode().isActive() && pool.getCurrentMana() == 12000, "Fluix pool did not join powered ME storage"))
              .thenExecute(() -> {
                  check(access.extract(1000, true) == 1000 && chest.getInventory().getAvailableStacks().get(ManaKey.KEY) == 12000, "Pool simulation consumed storage");
                  check(access.extract(1000, false) == 1000 && chest.getInventory().getAvailableStacks().get(ManaKey.KEY) == 11000, "Pool extraction did not reach its cell");
                  check(access.refund(1000) == 1000 && chest.getInventory().getAvailableStacks().get(ManaKey.KEY) == 12000, "Pool refund failed");
                  bridge.setControlType(RedstoneControl.DISABLED);
              }).thenWaitUntil(() -> check(bridge.mana().getStored() > 0, "Bridge did not receive from Fluix pool"))
              .thenExecute(() -> {
                  stop(bridge); check(bridge.mana().getStored() + chest.getInventory().getAvailableStacks().get(ManaKey.KEY) == 12000, "Fluix extraction duplicated or lost mana");
                  bridge.applySetting(0, "1"); bridge.setControlType(RedstoneControl.DISABLED);
              }).thenWaitUntil(() -> check(bridge.mana().isEmpty(), "Bridge did not supply Fluix pool"))
              .thenExecute(() -> {
                  stop(bridge); check(chest.getInventory().getAvailableStacks().get(ManaKey.KEY) == 12000, "Fluix supply did not reach its original cell");
                  h.setBlock(pos.north(), AEBlocks.CABLE_BUS.block()); var host = PartHelper.getPartHost(h.getLevel(), h.absolutePos(pos.north()));
                  host.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, owner); host.addPart(AEParts.STORAGE_BUS.asItem(), Direction.SOUTH, owner);
              }).thenIdle(20).thenExecute(() -> {
                  var inventory = pool.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks();
                  check(inventory.get(ManaKey.KEY) == 12000 && inventory.size() == 1, "Network-backed pool was counted a second time");
                  h.setBlock(pos, Blocks.AIR); check(access.extract(1000, false) == 0, "Removed Fluix pool retained access");
              }).thenSucceed();
    }
}
