package dev.everyonemek.botania;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import appeng.api.util.AEColor;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.*;
import appeng.parts.automation.ExportBusPart;
import dev.everyonemek.botania.compat.ae2.*;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ManaAeGameTests {
    private static void sidePacket(net.minecraft.server.level.ServerPlayer owner, ManaMachine machine, mekanism.common.network.MekClickType click) {
        var oldMenu = owner.containerMenu; var oldPos = owner.position();
        try {
            owner.setPos(machine.getBlockPos().getCenter()); owner.containerMenu = new ManaMachineMenu(93, owner.getInventory(), machine);
            new mekanism.common.network.to_server.configuration_update.PacketSideData(machine.getBlockPos(), click, RelativeSide.FRONT, TransmissionType.CHEMICAL).handle(context(owner));
        } finally { owner.containerMenu = oldMenu; owner.setPos(oldPos); }
    }
    @GameTest(template = "empty", timeoutTicks = 50)
    public static void manaCellKeepsOneStoreAndRoundTripsItsKey(GameTestHelper h) {
        check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("botanicalmekanism:mana_storage_cell")).isPresent(), "Mana cell recipe did not load");
        var stack = new ItemStack(Content.MANA_CELL.get()); var cell = StorageCells.getCellInventory(stack, null);
        check(cell != null, "Mana cell handler missing"); var source = IActionSource.empty(); var key = ManaKeys.current();
        check(cell.insert(AEItemKey.of(Items.DIAMOND), 1, Actionable.MODULATE, source) == 0, "Mana cell accepted an item");
        check(cell.insert(key, Long.MAX_VALUE, Actionable.SIMULATE, source) == 8_192_000 && ManaStorageItem.stored(stack) == 0, "Cell simulation mutated storage");
        check(cell.insert(key, Long.MAX_VALUE, Actionable.MODULATE, source) == 8_192_000, "1k capacity is wrong");
        check(!cell.canFitInsideCell() && cell.insert(key, 1, Actionable.MODULATE, source) == 0, "Filled cell allowed nesting or overflow");
        var second = StorageCells.getCellInventory(stack, null);
        check(second.extract(key, 100, Actionable.MODULATE, source) == 100 && cell.getAvailableStacks().get(key) == 8_191_900, "Cached handles duplicated cell contents");
        var registries = h.getLevel().registryAccess(); var saved = ItemStack.parseOptional(registries, (net.minecraft.nbt.CompoundTag) stack.saveOptional(registries));
        check(ManaStorageItem.stored(saved) == 8_191_900 && StorageCells.getCellInventory(saved, null).getAvailableStacks().get(key) == 8_191_900, "Cell item save lost mana");
        long[] capacities = {8_192_000L, 32_768_000L, 131_072_000L, 524_288_000L, 2_097_152_000L};
        for (var tier : ManaCellTier.values()) {
            var disk = new ItemStack(Content.MANA_CELLS.get(tier).get()); var storage = StorageCells.getCellInventory(disk, null);
            long capacity = capacities[tier.ordinal()];
            check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("botanicalmekanism:" + tier.id())).isPresent(), "Missing tier recipe");
            check(storage != null && storage.insert(key, Long.MAX_VALUE, Actionable.SIMULATE, source) == capacity && ManaStorageItem.stored(disk) == 0, "Tier simulation/capacity wrong");
            check(storage.insert(key, Long.MAX_VALUE, Actionable.MODULATE, source) == capacity && storage.getStatus() == appeng.api.storage.cells.CellState.FULL, "Tier overflow/status wrong");
            var restored = ItemStack.parseOptional(registries, (net.minecraft.nbt.CompoundTag) disk.saveOptional(registries));
            check(ManaStorageItem.stored(restored) == capacity && ManaStorageItem.capacity(restored) == capacity, "Large tier item save truncated mana");
            check(StorageCells.getCellInventory(restored, null).extract(key, Long.MAX_VALUE, Actionable.MODULATE, source) == capacity, "Large tier extraction lost mana");
            var emptySample = appeng.api.behaviors.ContainerItemStrategies.getContainedStack(restored);
            check(emptySample != null && emptySample.what() == key && emptySample.amount() == 0, "Tier could not select mana");
        }
        check(AEKey.fromTagGeneric(registries, key.toTagGeneric(registries)) == key, "Mana key codec failed");
        for (String id : new String[]{"botanicalmekanism:mana", "appbot:mana"}) {
            var oldKey = new net.minecraft.nbt.CompoundTag(); oldKey.putString(AEKey.TYPE_FIELD, id);
            check(AEKey.fromTagGeneric(registries, oldKey) == key, "Legacy mana pattern did not migrate: " + id);
        }
        check(registries.registryOrThrow(AEKeyType.REGISTRY_KEY).stream().filter(type -> type.getId().getPath().equals("mana")).count() == 1,
              "Two mana filters were registered");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries);
        try { AEKey.writeKey(buffer, key); check(AEKey.readKey(buffer) == key, "Mana key network codec failed"); } finally { buffer.release(); }
        var sample = appeng.api.behaviors.ContainerItemStrategies.getContainedStack(new ItemStack(Content.MANA_CELL.get()));
        check(sample != null && sample.what() == key && sample.amount() == 0, "Empty cell cannot select mana without creating it");
        var drops = new java.util.ArrayList<ItemStack>(); key.addDrops(12345, drops, h.getLevel(), h.absolutePos(BlockPos.ZERO));
        check(drops.size() == 1 && ManaStorageItem.stored(drops.getFirst()) == 12345 && drops.getFirst().is(Content.MANA_PACKET.get()), "Interface drops lost mana or granted free cells");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void nativeStorageBusShowsAndExtractsPoolMana(GameTestHelper h) {
        var poolKey = AppliedBotanics.loaded() ? AppliedBotanicsCompat.poolKey() : ManaKeys.current();
        var pos = new BlockPos(20, 3, 20); var pool = pool(h, pos.north(), 12000); var owner = player(h, "mana-storage-bus");
        h.setBlock(pos, AEBlocks.CABLE_BUS.block()); h.setBlock(pos.west(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var host = PartHelper.getPartHost(h.getLevel(), h.absolutePos(pos));
        host.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, owner);
        var bus = host.addPart(AEParts.STORAGE_BUS.asItem(), Direction.NORTH, owner);
        h.startSequence().thenWaitUntil(() -> {
            check(bus.getGridNode() != null && bus.getGridNode().isActive(), "Storage bus not connected");
            check(bus.getGridNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(poolKey) == 12000, "Storage bus did not mount mana");
        }).thenExecute(() -> {
            var storage = bus.getGridNode().getGrid().getStorageService().getInventory();
            check(storage.extract(poolKey, 3000, Actionable.SIMULATE, IActionSource.empty()) == 3000 && pool.getCurrentMana() == 12000, "Storage bus simulation changed pool");
            check(storage.extract(poolKey, 3000, Actionable.MODULATE, IActionSource.empty()) == 3000 && pool.getCurrentMana() == 9000, "Storage bus used a separate mana inventory");
            check(storage.insert(poolKey, 2000, Actionable.MODULATE, IActionSource.empty()) == 2000 && pool.getCurrentMana() == 11000, "Storage bus could not return mana to pool");
            h.setBlock(pos.north(), Blocks.AIR);
            check(storage.extract(poolKey, 1, Actionable.MODULATE, IActionSource.empty()) == 0, "Storage bus retained a removed pool");
            h.setBlock(pos.north(), vazkii.botania.common.block.BotaniaBlocks.CREATIVE_MANA_POOL);
        }).thenWaitUntil(() -> {
            var storage = bus.getGridNode().getGrid().getStorageService().getInventory();
            check(storage.getAvailableStacks().get(poolKey) == 1_000_000, "Storage bus did not recognize replacement everlasting pool");
        }).thenExecute(() -> {
            var storage = bus.getGridNode().getGrid().getStorageService().getInventory();
            check(storage.extract(poolKey, 3000, Actionable.SIMULATE, IActionSource.empty()) == 3000, "Everlasting ME simulation failed");
            check(storage.extract(poolKey, 3000, Actionable.MODULATE, IActionSource.empty()) == 3000, "Everlasting ME extraction failed");
            check(((vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity) h.getBlockEntity(pos.north())).getCurrentMana() == 1_000_000, "AE drained the everlasting pool");
        }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void realMeBusesMovePoolManaThroughCellIntoCharger(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var owner = player(h, "mana-me-buses");
        h.setBlock(pos, AEBlocks.ME_CHEST.block()); var chest = (MEChestBlockEntity) h.getBlockEntity(pos);
        var stack = new ItemStack(Content.MANA_CELL.get()); chest.setCell(stack);
        h.setBlock(pos.west(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        for (var side : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            h.setBlock(pos.relative(side), AEBlocks.CABLE_BUS.block());
            var host = PartHelper.getPartHost(h.getLevel(), h.absolutePos(pos.relative(side)));
            host.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, owner);
            appeng.api.parts.IPart part = side == Direction.NORTH ? host.addPart(AEParts.IMPORT_BUS.asItem(), side, owner)
                  : host.addPart(AEParts.EXPORT_BUS.asItem(), side, owner);
            if (part instanceof ExportBusPart exporter) {
                var menu = new appeng.menu.implementations.IOBusMenu(appeng.menu.implementations.IOBusMenu.EXPORT_TYPE, 94, owner.getInventory(), exporter);
                var carried = new ItemStack(Content.MANA_CELLS.get(ManaCellTier.K256).get()); menu.setCarried(carried);
                var filterSlot = menu.slots.stream().filter(slot -> slot instanceof appeng.menu.slot.FakeSlot).findFirst().orElseThrow();
                menu.doAction(owner, appeng.helpers.InventoryAction.EMPTY_ITEM, filterSlot.index, 0);
                check(exporter.getConfig().getKey(0) == ManaKeys.current() && carried.getCount() == 1 && ManaStorageItem.stored(carried) == 0,
                      "AE right-click filter did not select mana from an empty cell");
            }
        }
        var pool = new vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity[1];
        var charger = new ManaMachine[1];
        h.startSequence().thenIdle(15).thenExecute(() -> {
            // Place targets after their buses to verify capability changes are followed.
            pool[0] = pool(h, pos.north(2), 20000);
            charger[0] = ManaMachineGameTests.machine(h, pos.south(2), ManaMachineKind.CHARGER); ManaMachineGameTests.stop(charger[0]);
            sidePacket(owner, charger[0], mekanism.common.network.MekClickType.SHIFT_LEFT);
        }).thenWaitUntil(() -> check(chest.getInventory().getAvailableStacks().get(ManaKeys.current()) == 20000, "Import bus did not fill the real mana cell"))
              .thenExecute(() -> {
                  check(charger[0].mana().isEmpty() && pool[0].getCurrentMana() == 0, "Disabled machine face accepted exported mana");
                  sidePacket(owner, charger[0], mekanism.common.network.MekClickType.LEFT);
              }).thenWaitUntil(() -> check(charger[0].mana().getStored() == 20000, "Export bus failed: machine=" + charger[0].mana().getStored() + ", cell=" + chest.getInventory().getAvailableStacks().get(ManaKeys.current()) + ", acceptance=" + ManaAccess.at(h.getLevel(), charger[0].getBlockPos(), Direction.NORTH).insert(1000, true)))
              .thenExecute(() -> {
                  check(chest.getInventory().getAvailableStacks().get(ManaKeys.current()) == 0, "ME export duplicated mana");
                  var adapter = new ManaBusStorage(ManaAccess.at(h.getLevel(), charger[0].getBlockPos(), Direction.NORTH), false, () -> {});
                  h.setBlock(pos.south(2), Blocks.AIR);
                  check(adapter.getAvailableStacks().isEmpty() && adapter.insert(ManaKeys.current(), 100, Actionable.MODULATE, IActionSource.empty()) == 0, "Cached mana storage survived removal");
              }).thenSucceed();
    }
}
