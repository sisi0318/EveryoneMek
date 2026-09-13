package dev.everyonemek.botania;

import mekanism.api.RelativeSide;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.mana.ManaSpreaderBlockEntity;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.WandOfTheForestItem;
import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class NativeManaInputGameTests {
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void wandBindsRealSpreaderWithoutDismantlingAndBurstsFollowInputFace(GameTestHelper h) {
        var owner = player(h, "spreader-machine"); var pos = new BlockPos(20, 3, 20);
        var tile = machine(h, pos, ManaMachineKind.INFUSER); stop(tile);
        h.setBlock(pos.north(5), BotaniaBlocks.MANA_SPREADER); var spreader = (ManaSpreaderBlockEntity) h.getBlockEntity(pos.north(5));
        var side = RelativeSide.fromDirections(tile.getDirection(), Direction.NORTH);
        AdjacentPoolGameTests.configure(owner, tile, side, DataType.NONE);
        var wand = new ItemStack(BotaniaItems.WAND_OF_THE_FOREST); WandOfTheForestItem.setBindMode(wand, true);
        var held = owner.getMainHandItem().copy(); var location = owner.position();
        try {
            owner.setItemInHand(InteractionHand.MAIN_HAND, wand); owner.setShiftKeyDown(true);
            for (var target : new BlockPos[]{spreader.getBlockPos(), tile.getBlockPos(), tile.getBlockPos()}) {
                owner.setPos(target.north().getCenter());
                var hit = new BlockHitResult(target.getCenter(), Direction.NORTH, target, false);
                check(owner.gameMode.useItemOn(owner, h.getLevel(), wand, InteractionHand.MAIN_HAND, hit).consumesAction(), "Native wand interaction was not handled");
                check(h.getLevel().getBlockEntity(tile.getBlockPos()) == tile, "Forest wand dismantled the machine");
            }
            check(WandOfTheForestItem.getBindingAttempt(wand).isEmpty(), "Spreader binding did not complete");
        } finally { owner.setShiftKeyDown(false); owner.setItemInHand(InteractionHand.MAIN_HAND, held); owner.setPos(location); }
        int payload = spreader.getSpreaderBlock().getDefaultBurstProperties().maxMana;
        spreader.receiveMana(payload);
        var receiver = ManaReceiver.LOOKUP.find(h.getLevel(), tile.getBlockPos(), Direction.NORTH);
        check(receiver != null && receiver.isFull(), "Closed input was not reflected in native receiver");
        h.startSequence().thenIdle(12).thenExecute(() -> {
            check(tile.mana().isEmpty() && spreader.getCurrentMana() == payload, "Spreader paid for a burst into a disabled face");
            AdjacentPoolGameTests.configure(owner, tile, side, DataType.INPUT);
        }).thenWaitUntil(() -> check(tile.mana().getStored() == payload, "Bound spreader burst did not reach machine"))
              .thenExecute(() -> {
                  check(spreader.getCurrentMana() == 0, "Burst duplicated source mana");
                  receiver.receiveMana(Integer.MAX_VALUE); check(tile.mana().getStored() == ManaMachine.MANA_CAPACITY && receiver.isFull(), "Burst receiver exceeded capacity");
                  h.setBlock(pos, Blocks.AIR); check(!receiver.canReceiveManaFromBursts(), "Removed receiver stayed live");
                  h.setBlock(pos.north(5), Blocks.AIR);
              }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void floorCatalystAndInternalCatalystShareWorkAndOverlayState(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); var tile = machine(h, pos, ManaMachineKind.INFUSER); power(tile); mana(tile, 20000);
        h.setBlock(pos.below(), BotaniaBlocks.CONJURATION_CATALYST); tile.inputs.getLast().setStack(new ItemStack(Items.REDSTONE, 2));
        check(tile.applySetting(3, "botania:mana_infusion/conjuration/redstone"), "Conjuration recipe lock unavailable");
        h.startSequence().thenIdle(5).thenExecute(() -> {
            check(tile.progressTicks() > 0 && tile.infusionCatalyst().is(BotaniaBlocks.CONJURATION_CATALYST), "Floor catalyst did not enable conjuration");
            h.setBlock(pos.below(), Blocks.STONE);
        }).thenIdle(3).thenExecute(() -> {
            check(tile.progressTicks() == 0 && tile.inputs.getLast().getCount() == 2 && tile.mana().getStored() == 20000, "Removed catalyst still completed or spent recipe mana");
            h.setBlock(pos.below(), BotaniaBlocks.CONJURATION_CATALYST);
        }).thenWaitUntil(() -> check(output(tile, Items.REDSTONE) == 4, "Floor catalyst did not resume conjuration"))
              .thenExecute(() -> {
                  check(tile.mana().getStored() == 10000, "Conjuration did not pay for two inputs");
                  tile.applySetting(3, "");
                  tile.extras.getFirst().setStack(new ItemStack(BotaniaBlocks.ALCHEMY_CATALYST));
                  tile.inputs.getLast().setStack(new ItemStack(Items.COBBLESTONE, 3));
              }).thenWaitUntil(() -> check(output(tile, Items.SAND) == 3, "Internal catalyst did not override the floor catalyst"))
              .thenExecute(() -> {
                  check(tile.mana().getStored() == 9850 && tile.extras.getFirst().getCount() == 1, "Internal catalyst was consumed or charged incorrectly");
                  var tag = tile.getReducedUpdateTag(h.getLevel().registryAccess());
                  check(Block.stateById(tag.getInt("infusion_catalyst")).is(BotaniaBlocks.ALCHEMY_CATALYST), "World update did not carry the active overlay state");
                  tile.extras.getFirst().setStackUnchecked(ItemStack.EMPTY);
                  check(tile.infusionCatalyst().is(BotaniaBlocks.CONJURATION_CATALYST), "Removing the internal catalyst did not restore the floor catalyst");
                  stop(tile);
              }).thenSucceed();
    }
}
