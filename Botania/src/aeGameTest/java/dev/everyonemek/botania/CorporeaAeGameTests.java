package dev.everyonemek.botania;

import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.*;
import appeng.api.util.AEColor;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.*;
import dev.everyonemek.botania.compat.ae2.AeBridge;
import dev.everyonemek.botania.corporea.CorporeaFlower;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.corporea.*;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.item.*;

import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class CorporeaAeGameTests {
    private record Rig(BlockPos pos, CorporeaFlower flower, AeBridge bridge, ChestBlockEntity chest, MEChestBlockEntity meChest, CorporeaSpark spark) {
        MEStorage storage() { return bridge.managedNode().getGrid().getStorageService().getInventory(); }
    }
    private static CorporeaSpark spark(GameTestHelper h, BlockPos pos, boolean master) {
        check(CorporeaSparkItem.attachSpark(h.getLevel(), h.absolutePos(pos), new ItemStack(master ? BotaniaItems.MASTER_CORPOREA_SPARK : BotaniaItems.CORPOREA_SPARK), ItemStack.EMPTY), "Corporea spark placement failed");
        return CorporeaHelper.instance().getSparkForBlock(h.getLevel(), h.absolutePos(pos));
    }
    private static Rig rig(GameTestHelper h, BlockPos pos) {
        var owner = player(h, "corporea-me");
        h.setBlock(pos.below(), AEBlocks.CABLE_BUS.block());
        var cable = PartHelper.getPartHost(h.getLevel(), h.absolutePos(pos.below()));
        check(cable != null && cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, owner) != null, "Real ME cable failed to install");
        h.setBlock(pos.below().east(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.setBlock(pos.below().east(2), AEBlocks.ME_CHEST.block());
        var me = (MEChestBlockEntity) h.getBlockEntity(pos.below().east(2));
        var cell = AEItems.ITEM_CELL_4K.stack(); var inventory = StorageCells.getCellInventory(cell, null);
        check(inventory != null, "Native ME cell handler missing");
        inventory.insert(AEItemKey.of(Items.DIAMOND), 96, Actionable.MODULATE, IActionSource.empty());
        inventory.insert(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.MODULATE, IActionSource.empty()); inventory.persist(); me.setCell(cell);
        h.setBlock(pos, Content.CORPOREA.get()); var flower = (CorporeaFlower) h.getBlockEntity(pos); Flowers.claim(flower, owner);
        check(flower.getBlockState().canSurvive(h.getLevel(), flower.getBlockPos()), "Bridge flower cannot stand on ME cable");
        h.setBlock(pos.west(5), Blocks.CHEST); var chest = (ChestBlockEntity) h.getBlockEntity(pos.west(5));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 32)); chest.setItem(1, new ItemStack(Items.EMERALD, 8));
        var named = new ItemStack(Items.EMERALD, 2); named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Named sample")); chest.setItem(2, named);
        h.setBlock(pos.west(5).north(3), Blocks.CHEST);
        spark(h, pos.west(5).north(3), true); spark(h, pos.west(5), false); var spark = spark(h, pos, false);
        return new Rig(pos, flower, (AeBridge) flower.backend(), chest, me, spark);
    }
    private static void ready(Rig rig) {
        var state = new CompoundTag(); rig.bridge.describe(state);
        check(rig.bridge.connected() && rig.spark.getMaster() != null && state.getString("status").equals("ready"), "Bridge not ready: " + state);
    }
    private static int count(Rig rig, Item item, boolean execute, int amount) {
        return CorporeaHelper.instance().requestItem(CorporeaHelper.instance().createMatcher(new ItemStack(item), true), amount, rig.spark, null, execute)
              .stacks().stream().mapToInt(ItemStack::getCount).sum();
    }
    @GameTest(template = "empty", timeoutTicks = 260)
    public static void nativeMeCellAndCorporeaAccessBothWaysWithoutDoubleCounting(GameTestHelper h) {
        var rig = rig(h, new BlockPos(20, 4, 20));
        h.startSequence().thenWaitUntil(() -> ready(rig)).thenExecute(() -> {
            var me = rig.storage(); var diamond = AEItemKey.of(Items.DIAMOND); var emerald = AEItemKey.of(Items.EMERALD);
            var snapshot = new CompoundTag(); rig.bridge.describe(snapshot);
            check(me.getAvailableStacks().get(diamond) == 128, "ME count=" + me.getAvailableStacks().get(diamond)
                  + ", local chest=" + rig.chest.getItem(0).getCount() + ", native ME=" + rig.meChest.getInventory().getAvailableStacks().get(diamond)
                  + ", sameGrid=" + (rig.meChest.getMainNode().getGrid() == rig.bridge.managedNode().getGrid()) + ", status=" + snapshot);
            check(count(rig, Items.DIAMOND, false, -1) == 128, "Corporea counted its own ME-mounted storage twice");
            check(me.extract(emerald, 3, Actionable.SIMULATE, IActionSource.empty()) == 3 && rig.chest.getItem(1).getCount() == 8, "AE simulation mutated physical Corporea inventory");
            check(count(rig, Items.IRON_INGOT, false, 8) == 8 && me.getAvailableStacks().get(AEItemKey.of(Items.IRON_INGOT)) == 64, "Corporea simulation mutated ME storage");
            check(me.extract(emerald, 3, Actionable.MODULATE, IActionSource.empty()) == 3 && rig.chest.getItem(1).getCount() == 5, "AE could not withdraw physical items");
            check(me.insert(emerald, 2, Actionable.MODULATE, IActionSource.empty()) == 2 && rig.chest.getItem(1).getCount() == 7, "AE did not refill existing Corporea stack");
            var named = AEItemKey.of(rig.chest.getItem(2));
            check(me.extract(named, 1, Actionable.MODULATE, IActionSource.empty()) == 1 && rig.chest.getItem(2).getCount() == 1
                  && rig.chest.getItem(1).getCount() == 7, "ME merged different item components");
            check(me.insert(named, 1, Actionable.MODULATE, IActionSource.empty()) == 1 && rig.chest.getItem(2).getCount() == 2, "ME did not retain item components on insertion");
            check(count(rig, Items.IRON_INGOT, true, 8) == 8 && me.getAvailableStacks().get(AEItemKey.of(Items.IRON_INGOT)) == 56, "Corporea did not extract real ME items");
            rig.flower.mode = 1; rig.bridge.settingsChanged();
            check(count(rig, Items.DIAMOND, false, -1) == 32 && me.getAvailableStacks().get(diamond) == 128, "ME-only mode did not affect actual routing");
            rig.flower.mode = 2; rig.bridge.settingsChanged();
            check(me.getAvailableStacks().get(diamond) == 96 && count(rig, Items.DIAMOND, false, -1) == 128, "Corporea-only mode did not affect actual routing");
            rig.flower.mode = 0; rig.bridge.settingsChanged();
        }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void realCorporeaFunnelRequestsMeItemsAfterRedstonePulse(GameTestHelper h) {
        var rig = rig(h, new BlockPos(20, 4, 20)); var funnel = rig.pos.north(4);
        h.setBlock(funnel, BotaniaBlocks.CORPOREA_FUNNEL); h.setBlock(funnel.below(), Blocks.CHEST);
        var target = (ChestBlockEntity) h.getBlockEntity(funnel.below()); spark(h, funnel, false);
        var frame = new ItemFrame(h.getLevel(), h.absolutePos(funnel.north()), Direction.NORTH);
        frame.setItem(new ItemStack(Items.IRON_INGOT)); frame.setRotation(3); h.getLevel().addFreshEntity(frame);
        h.startSequence().thenWaitUntil(() -> ready(rig)).thenIdle(5).thenExecute(() -> h.setBlock(funnel.east(), Blocks.REDSTONE_BLOCK))
              .thenWaitUntil(() -> check(target.getItem(0).is(Items.IRON_INGOT) && target.getItem(0).getCount() == 8, "Real funnel did not deliver eight ME iron ingots"))
              .thenExecute(() -> {
                  check(rig.storage().getAvailableStacks().get(AEItemKey.of(Items.IRON_INGOT)) == 56, "Funnel delivery did not debit ME exactly once");
                  frame.discard(); h.setBlock(funnel.east(), Blocks.AIR);
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void duplicateBridgesAndStorageBusRoutesAreRejectedAndStaleViewsStop(GameTestHelper h) {
        var rig = rig(h, new BlockPos(20, 4, 20)); var diamond = AEItemKey.of(Items.DIAMOND);
        MEStorage[] held = {null}; rig.bridge.mountInventories((storage, priority) -> held[0] = storage);
        var secondPos = rig.pos.east().north();
        h.startSequence().thenWaitUntil(() -> ready(rig)).thenExecute(() -> {
            h.setBlock(secondPos.below(), AEBlocks.CABLE_BUS.block());
            var cable = PartHelper.getPartHost(h.getLevel(), h.absolutePos(secondPos.below()));
            cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, player(h, "corporea-me"));
            h.setBlock(secondPos, Content.CORPOREA.get()); Flowers.claim(h.getBlockEntity(secondPos), player(h, "corporea-me")); spark(h, secondPos, false);
        }).thenWaitUntil(() -> check(((CorporeaFlower) h.getBlockEntity(secondPos)).backend().connected(), "Second bridge did not join real ME grid"))
              .thenIdle(10).thenExecute(() -> {
                  check(rig.storage().getAvailableStacks().get(diamond) == 128, "Duplicate bridge count=" + rig.storage().getAvailableStacks().get(diamond));
                  h.setBlock(secondPos, Blocks.AIR);
                  var busPos = rig.pos.west(4); h.setBlock(busPos, AEBlocks.CABLE_BUS.block());
                  var host = PartHelper.getPartHost(h.getLevel(), h.absolutePos(busPos));
                  host.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, player(h, "corporea-me"));
                  host.addPart(AEParts.STORAGE_BUS.asItem(), Direction.WEST, player(h, "corporea-me"));
              }).thenIdle(5).thenExecute(() -> {
                  var host = PartHelper.getPartHost(h.getLevel(), h.absolutePos(rig.pos.west(4)));
                  GridHelper.createConnection(host.getPart(null).getGridNode(), rig.bridge.managedNode().getNode());
              }).thenIdle(10).thenExecute(() -> {
                  var state = new CompoundTag(); rig.bridge.describe(state);
                  check(state.getString("status").equals("duplicate_storage"), "Parallel storage-bus route was not rejected: " + state);
                  h.setBlock(rig.pos.west(4), Blocks.AIR);
              }).thenIdle(10).thenExecute(() -> {
                  ready(rig); check(held[0].getAvailableStacks().get(diamond) == 32, "Bridge did not recover after duplicate route removal");
                  h.setBlock(rig.pos, Blocks.AIR);
                  check(held[0].getAvailableStacks().isEmpty() && held[0].extract(diamond, 32, Actionable.MODULATE, IActionSource.empty()) == 0,
                        "Cached bridge storage survived block removal");
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void separateCorporeaNetworksShareMeWithoutHidingEachOtherOrLooping(GameTestHelper h) {
        var rig = rig(h, new BlockPos(20, 4, 20)); var otherPos = rig.pos.south();
        h.setBlock(otherPos.below(), AEBlocks.CABLE_BUS.block());
        PartHelper.getPartHost(h.getLevel(), h.absolutePos(otherPos.below())).addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, player(h, "corporea-me"));
        h.setBlock(otherPos, Content.CORPOREA.get()); var other = (CorporeaFlower) h.getBlockEntity(otherPos); Flowers.claim(other, player(h, "corporea-me"));
        BlockPos stockPos = rig.pos.south(5), masterPos = stockPos.east(3);
        h.setBlock(stockPos, Blocks.CHEST); var chest = (ChestBlockEntity) h.getBlockEntity(stockPos); chest.setItem(0, new ItemStack(Items.GOLD_INGOT, 32));
        h.setBlock(masterPos, Blocks.CHEST);
        var master = spark(h, masterPos, true); master.setNetwork(DyeColor.RED);
        var store = spark(h, stockPos, false); store.setNetwork(DyeColor.RED);
        var link = spark(h, otherPos, false); link.setNetwork(DyeColor.RED);
        h.startSequence().thenWaitUntil(() -> { ready(rig); check(other.backend().connected() && link.getMaster() != null, "Second Corporea network not ready"); })
              .thenExecute(() -> {
                  check(rig.storage().getAvailableStacks().get(AEItemKey.of(Items.GOLD_INGOT)) == 32, "Second Corporea network was not mounted");
                  check(count(rig, Items.GOLD_INGOT, true, 16) == 16 && chest.getItem(0).getCount() == 16, "Corporea could not reach the other network through ME");
                  int diamonds = CorporeaHelper.instance().requestItem(CorporeaHelper.instance().createMatcher(new ItemStack(Items.DIAMOND), true), -1, link, null, false)
                        .stacks().stream().mapToInt(ItemStack::getCount).sum();
                  check(diamonds == 128, "Cross-network request looped or hid valid ME/Corporea stock: " + diamonds);
              }).thenSucceed();
    }
}
