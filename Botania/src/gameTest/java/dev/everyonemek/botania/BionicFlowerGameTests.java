package dev.everyonemek.botania;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.block.Wandable;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;
import vazkii.botania.api.state.BotaniaStateProperties;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.item.BotaniaItems;

import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class BionicFlowerGameTests {
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void allBionicTypesKeepNativeModelsModesAndPaidBudgets(GameTestHelper h) {
        var owner = player(h, "all-bionics"); var stranger = player(h, "bionic-stranger");
        int x = 16;
        for (var block : Content.bionics()) {
            var tile = (FunctionalFlowerBlockEntity) plant(h, owner, new BlockPos(x, 2, 20), block.get()); x += 10;
            var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, tile.getBlockPos(), Direction.DOWN);
            check(energy != null && energy.receiveEnergy(20000, true) == 20000 && Flowers.storedFE(tile) == 0, "Bionic FE simulation or registration failed");
            energy.receiveEnergy(20000, false); int before = Flowers.storedFE(tile); Flowers.supplyBionic(tile);
            check(tile.getMana() >= Flowers.workReserve(tile) && Flowers.storedFE(tile) + tile.getMana() * Balance.FE_PER_MANA.get() == before,
                  "Bionic paid reserve does not conserve FE");
            if (tile instanceof Wandable wand) {
                owner.setShiftKeyDown(true); stranger.setShiftKeyDown(true);
                try {
                    var oldState = tile.getBlockState();
                    check(!wand.onUsedByWand(stranger, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST), Direction.UP) && tile.getBlockState() == oldState, "Stranger changed bionic filter");
                    check(wand.onUsedByWand(owner, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST), Direction.UP) && tile.getBlockState() != oldState, "Native wand did not change bionic mode");
                } finally { owner.setShiftKeyDown(false); stranger.setShiftKeyDown(false); }
            }
            stop(tile); checkWorldSave(h, tile);
            var state = tile.getBlockState(); int mana = tile.getMana(), fe = Flowers.storedFE(tile);
            var dropped = breakAndPick(h, tile.getBlockPos(), block.get()); placeItem(owner, tile.getBlockPos(), dropped);
            var restored = (FunctionalFlowerBlockEntity) h.getLevel().getBlockEntity(tile.getBlockPos());
            check(restored != null && restored.getMana() == mana && Flowers.storedFE(restored) == fe && restored.getBlockState() == state, "Bionic drop lost native mode or paid reserve");
            stop(restored);
        }
        h.setBlock(new BlockPos(16, 2, 8), BotaniaBlocks.HOPPERHOCK);
        check(h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(new BlockPos(16, 2, 8)), Direction.DOWN) == null,
              "Ordinary native flower gained bionic FE capability"); h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 130)
    public static void hopperhockStopsWithoutBudgetAndResumesNativeCollection(GameTestHelper h) {
        var owner = player(h, "bionic-hopper"); var pos = new BlockPos(20, 2, 20);
        var flower = (FunctionalFlowerBlockEntity) plant(h, owner, pos, Content.HOPPERHOCK.get());
        h.setBlock(pos.east(), Blocks.CHEST); var chest = (ChestBlockEntity) h.getBlockEntity(pos.east());
        var worldPos = h.absolutePos(pos.west(2)); var item = new ItemEntity(h.getLevel(), worldPos.getX() + .5, worldPos.getY() + .5, worldPos.getZ() + .5, new ItemStack(Items.CLAY_BALL, 3));
        item.setNoGravity(true); item.setDeltaMovement(0, 0, 0); h.getLevel().addFreshEntity(item);
        h.startSequence().thenIdle(60).thenExecute(() -> {
            check(chest.isEmpty() && item.isAlive() && item.getItem().getCount() == 3, "Bionic hopper fell back to free native operation");
            Flowers.setFE(flower, Balance.FE_PER_MANA.get());
        }).thenWaitUntil(() -> check(!chest.isEmpty(), "Paid bionic hopper did not collect native item entity"))
              .thenExecute(() -> {
                  check(chest.getItem(0).is(Items.CLAY_BALL) && chest.getItem(0).getCount() == 3 && Flowers.storedFE(flower) == 0 && flower.getMana() == 0,
                        "Native collection cost or stack conservation incorrect"); stop(flower); item.discard();
              }).thenSucceed();
    }
}
