package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.item.BotaniaItems;

import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ManaMachineGameTests {
    static ManaMachine machine(GameTestHelper h, BlockPos pos, ManaMachineKind kind) {
        if (h.getBlockState(pos.below()).isAir()) h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos, ManaContent.MACHINES.get(kind).get());
        return (ManaMachine) h.getBlockEntity(pos);
    }
    static void power(ManaMachine tile) { tile.energy().setEnergy(EnergyUnit.FORGE_ENERGY.convertFrom(200000L)); }
    static void mana(ManaMachine tile, int amount) { tile.mana().setStack(new ChemicalStack(ManaContent.MANA, amount)); }
    static void stop(ManaMachine tile) { tile.setControlType(RedstoneControl.HIGH); }
    static int output(ManaMachine tile, Item item) { return tile.outputs.stream().filter(slot -> slot.getStack().is(item)).mapToInt(slot -> slot.getCount()).sum(); }
    static void save(GameTestHelper h, ManaMachine tile) {
        var lookup = h.getLevel().registryAccess(); var restored = (ManaMachine) BlockEntity.loadStatic(tile.getBlockPos(), tile.getBlockState(), tile.saveWithFullMetadata(lookup), lookup);
        check(restored != null && restored.mana().getStored() == tile.mana().getStored() && restored.energy().getEnergy() == tile.energy().getEnergy()
              && restored.progressTicks() == tile.progressTicks() && restored.mode() == tile.mode() && restored.poolSide() == tile.poolSide(), "Machine world save lost resources or settings");
    }
    @GameTest(template = "empty", timeoutTicks = 330)
    public static void bridgeRealTubeInfusionAndStackedHopper(GameTestHelper h) {
        var bridge = machine(h, new BlockPos(10, 2, 10), ManaMachineKind.BRIDGE);
        var infusion = machine(h, new BlockPos(12, 2, 10), ManaMachineKind.INFUSER);
        var pool = pool(h, new BlockPos(10, 2, 9), 40000); power(bridge); power(infusion);
        h.setBlock(new BlockPos(11, 2, 10), MekanismBlocks.BASIC_PRESSURIZED_TUBE.get());
        h.setBlock(new BlockPos(12, 3, 10), Blocks.HOPPER);
        ((HopperBlockEntity) h.getBlockEntity(new BlockPos(12, 3, 10))).setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        infusion.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT));
        infusion.getConfig().getConfig(TransmissionType.ITEM).setDataType(DataType.OUTPUT, RelativeSide.FRONT);
        h.setBlock(new BlockPos(12, 2, 9), Blocks.CHEST); var chest = (ChestBlockEntity) h.getBlockEntity(new BlockPos(12, 2, 9));
        check(h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), bridge.getBlockPos(), Direction.NORTH) == null,
              "Bridge exposed chemical capability on its pool face");
        h.startSequence().thenWaitUntil(() -> {
            int made = 0; for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(BotaniaItems.MANASTEEL_INGOT)) made += chest.getItem(i).getCount();
            check(made == 3, "Tube/hopper production: made=" + made + ", status=" + infusion.status() + ", mana=" + infusion.mana().getStored());
        }).thenExecute(() -> {
            stop(bridge); stop(infusion); save(h, infusion);
            check(pool.getCurrentMana() < 40000 && infusion.inputs.getFirst().isEmpty(), "Real pipe did not transfer native mana or consume stacked inputs");
        }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void bridgeSwitchChargerPermissionsAndWirelessMachineFace(GameTestHelper h) {
        var owner = player(h, "mana-routing");
        var bridge = machine(h, new BlockPos(12, 2, 12), ManaMachineKind.BRIDGE); power(bridge); stop(bridge);
        var pool = pool(h, new BlockPos(12, 2, 11), 10000);
        h.startSequence().thenIdle(2).thenExecute(() -> {
            int total = pool.getCurrentMana() + (int) bridge.mana().getStored(); ManaTransfer.tick(bridge);
            check(pool.getCurrentMana() + bridge.mana().getStored() == total && bridge.mana().getStored() == 1000, "Pool-to-chemical transfer changed total mana");
            check(bridge.applySetting(0, "1"), "Bridge mode was rejected"); ManaTransfer.tick(bridge);
            check(pool.getCurrentMana() == total && bridge.mana().isEmpty(), "Chemical-to-pool transfer duplicated or lost mana");
            var charger = machine(h, new BlockPos(13, 2, 11), ManaMachineKind.CHARGER); power(charger); stop(charger);
            charger.applySetting(1, Integer.toString(RelativeSide.fromDirections(charger.getDirection(), Direction.WEST).ordinal()));
            var tablet = new ItemStack(BotaniaItems.MANA_TABLET); charger.inputs.getFirst().setStack(tablet);
            ManaTransfer.tick(charger);
            int charged = ManaItem.LOOKUP.find(charger.inputs.getFirst().getStack()).getMana();
            check(charged == 1000 && pool.getCurrentMana() == total - charged, "Charger failed real-pool mana conservation");
            charger.applySetting(0, "1"); ManaTransfer.tick(charger); ManaTransfer.tick(charger);
            check(pool.getCurrentMana() == total && !charger.outputs.getFirst().isEmpty() && charger.inputs.getFirst().isEmpty(), "Charger did not drain and output the item");
            var target = machine(h, new BlockPos(23, 2, 11), ManaMachineKind.INFUSER); stop(target);
            ApothecaryGameTests.withUsername(owner, () -> target.setOwnerUUID(owner.getUUID()));
            var core = core(h, owner, new BlockPos(18, 2, 16));
            var source = node(h, owner, new BlockPos(11, 2, 11), core, NetworkPlant.SUPPLY, Direction.EAST, 0);
            var receiver = node(h, owner, new BlockPos(23, 2, 12), core, NetworkPlant.RECEIVE, Direction.NORTH, 1000);
            var network = ManaNetworks.get(h.getLevel()).find(core.network);
            ManaNetworks.get(h.getLevel()).tick(core);
            check(target.mana().getStored() > 0 && total - pool.getCurrentMana() == target.mana().getStored() + network.lastFee,
                  "Wireless machine endpoint: mana=" + target.mana().getStored() + ", pool=" + pool.getCurrentMana() + ", total=" + total
                  + ", fee=" + network.lastFee + ", source=" + source.status + ", target=" + receiver.status + ", core=" + core.status
                  + ", sourceEndpoint=" + ManaEndpoint.at(source) + ", targetEndpoint=" + ManaEndpoint.at(receiver));
            target.getConfig().getConfig(TransmissionType.CHEMICAL).setDataType(DataType.NONE, RelativeSide.BACK);
            check(ManaEndpoint.at(receiver) == null || ManaEndpoint.at(receiver).space() == 0, "Wireless endpoint ignored disabled real side");
            BotanicalGameTests.stop(core); ApothecaryGameTests.withUsername(owner, () -> save(h, target));
        }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 460)
    public static void runesRetainCatalystsAndBlockedWorkDoesNotDebit(GameTestHelper h) {
        var tile = machine(h, new BlockPos(16, 2, 16), ManaMachineKind.RUNIC); power(tile); mana(tile, 200000);
        var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.RUNIC_ALTAR_TYPE).stream()
              .filter(holder -> holder.value().getResultItem(h.getLevel().registryAccess()).is(BotaniaItems.RUNE_OF_SPRING)).findFirst().orElseThrow().value();
        var ingredients = new ArrayList<>(recipe.getIngredients()); ingredients.addAll(recipe.getCatalysts());
        for (int i = 0; i < ingredients.size(); i++) tile.inputs.get(i).setStack(ingredients.get(i).getItems()[0].copyWithCount(2));
        tile.extras.getFirst().setStack(recipe.getReagent().getItems()[0].copyWithCount(2));
        tile.outputs.forEach(slot -> slot.setStack(new ItemStack(Items.COBBLESTONE, 64)));
        long energy = tile.energy().getEnergy(); tile.onUpdateServer();
        check(tile.status() == ManaMachine.OUTPUT_FULL && tile.energy().getEnergy() == energy && tile.mana().getStored() == 200000 && tile.progressTicks() == 0,
              "Blocked runic work spent resources or failed to match catalysts");
        tile.outputs.forEach(slot -> slot.setStackUnchecked(ItemStack.EMPTY));
        h.startSequence().thenIdle(40).thenExecute(() -> save(h, tile)).thenWaitUntil(() -> check(output(tile, BotaniaItems.RUNE_OF_SPRING) == 2, "Runic production: status=" + tile.status()))
              .thenExecute(() -> {
                  stop(tile);
                  check(tile.mana().getStored() == 200000 - 2L * recipe.getMana() && tile.extras.getFirst().isEmpty(), "Runic costs incorrect");
                  for (int i = recipe.getIngredients().size(); i < ingredients.size(); i++) check(tile.inputs.get(i).getCount() == 2, "Native rune catalyst was not retained in its source slot");
                  var item = breakAndPick(h, tile.getBlockPos(), tile.getBlockState().getBlock());
                  var owner = player(h, "runic-drop"); ApothecaryGameTests.withUsername(owner, () -> placeItem(owner, tile.getBlockPos(), item));
                  var restored = (ManaMachine) h.getLevel().getBlockEntity(tile.getBlockPos());
                  check(restored != null && restored.mana().getStored() == 200000 - 2L * recipe.getMana() && output(restored, BotaniaItems.RUNE_OF_SPRING) == 2,
                        "Machine item lost chemical or inventory attachments"); stop(restored);
              }).thenSucceed();
    }
}
