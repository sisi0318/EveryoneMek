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
    @GameTest(template = "empty", timeoutTicks = 250)
    public static void fluixPoolUsesItsMeInventoryAndCannotMountItTwice(GameTestHelper h) {
        check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("appbot:mana_cell_housing")).isPresent(), "Appbot housing recipe did not load");
        var pos = new BlockPos(20, 3, 20); var owner = player(h, "fluix-pool");
        h.setBlock(pos.west(), AEBlocks.ME_CHEST.block()); var chest = (MEChestBlockEntity) h.getBlockEntity(pos.west());
        var disk = new ItemStack(ABItems.MANA_CELL_1K.get()); var cell = StorageCells.getCellInventory(disk, null);
        check(cell != null && cell.insert(ManaKey.KEY, 12000, Actionable.MODULATE, IActionSource.empty()) == 12000, "Appbot cell unavailable"); chest.setCell(disk);
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
                  check(inventory.get(ManaKey.KEY) == 12000 && inventory.get(dev.everyonemek.botania.compat.ae2.ManaKey.INSTANCE) == 0, "Network-backed pool was counted a second time");
                  h.setBlock(pos, Blocks.AIR); check(access.extract(1000, false) == 0, "Removed Fluix pool retained access");
              }).thenSucceed();
    }
}
