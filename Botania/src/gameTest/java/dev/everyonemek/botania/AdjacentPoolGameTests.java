package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.RelativeSide;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.network.MekClickType;
import mekanism.common.network.to_server.configuration_update.PacketSideData;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;
import vazkii.botania.common.item.BotaniaItems;
import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class AdjacentPoolGameTests {
    static void configure(ServerPlayer player, ManaMachine machine, RelativeSide side, DataType type) {
        configure(player, machine, TransmissionType.CHEMICAL, side, type);
    }
    static void configure(ServerPlayer player, ManaMachine machine, TransmissionType transmission, RelativeSide side, DataType type) {
        var previous = player.containerMenu; var position = player.position();
        try {
            player.setPos(machine.getBlockPos().getCenter()); player.containerMenu = new ManaMachineMenu(95, player.getInventory(), machine);
            new PacketSideData(machine.getBlockPos(), MekClickType.SHIFT_LEFT, side, transmission).handle(context(player));
            for (int i = 0; i < 5 && machine.getConfig().getDataType(transmission, side) != type; i++)
                new PacketSideData(machine.getBlockPos(), MekClickType.LEFT, side, transmission).handle(context(player));
            check(machine.getConfig().getDataType(transmission, side) == type, "Mek side packet did not apply " + type);
        } finally { player.containerMenu = previous; player.setPos(position); }
    }
    private static ManaPoolBlockEntity barePool(GameTestHelper h, BlockPos position, int mana) {
        // No support placement here: a pool above the machine must not overwrite it with stone.
        h.setBlock(position, BotaniaBlocks.MANA_POOL); var pool = (ManaPoolBlockEntity) h.getBlockEntity(position); pool.receiveMana(mana); return pool;
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void sixFacesFollowMekPacketsAndShareOnePullBudget(GameTestHelper h) {
        var pos = new BlockPos(20, 4, 20); var player = player(h, "direct-pool-sides");
        h.setBlock(pos, Attribute.setFacing(ManaContent.MACHINES.get(ManaMachineKind.CHARGER).get().defaultBlockState(), Direction.EAST));
        var machine = (ManaMachine) h.getBlockEntity(pos); stop(machine);
        var pools = new EnumMap<RelativeSide, ManaPoolBlockEntity>(RelativeSide.class);
        for (var side : RelativeSide.values()) {
            pools.put(side, barePool(h, pos.relative(side.getDirection(machine.getDirection())), 4000));
            configure(player, machine, side, DataType.OUTPUT);
        }
        long[] before = {0}; var snapshot = new EnumMap<RelativeSide, Integer>(RelativeSide.class);
        var sequence = h.startSequence().thenIdle(3).thenExecute(() -> check(machine.mana().isEmpty(), "Output faces pulled mana from pools"));
        for (var side : RelativeSide.values()) {
            sequence.thenExecute(() -> {
                before[0] = machine.mana().getStored(); pools.forEach((s, pool) -> snapshot.put(s, pool.getCurrentMana()));
                configure(player, machine, side, side == RelativeSide.TOP ? DataType.INPUT_OUTPUT : DataType.INPUT);
            }).thenWaitUntil(() -> check(machine.mana().getStored() > before[0], "Input face did not pull: " + side))
                  .thenExecute(() -> {
                      check(machine.mana().getStored() - before[0] <= ManaTransfer.RATE, "A face exceeded the tick budget");
                      for (var other : RelativeSide.values()) check(pools.get(other).getCurrentMana() == snapshot.get(other)
                            - (other == side ? machine.mana().getStored() - before[0] : 0), "Rotated face used the wrong pool");
                      long stored = machine.mana().getStored(); int remaining = machine.poolPullRemaining();
                      ManaTransfer.fillFromAdjacentPools(machine); int again = ManaTransfer.fillFromAdjacentPools(machine);
                      check(machine.mana().getStored() - stored <= remaining && again == 0, "Repeated calls bypassed the tick budget");
                      configure(player, machine, side, DataType.OUTPUT);
                  });
        }
        sequence.thenExecute(() -> {
            before[0] = machine.mana().getStored();
            configure(player, machine, RelativeSide.TOP, DataType.INPUT);
            configure(player, machine, RelativeSide.BOTTOM, DataType.INPUT);
        }).thenWaitUntil(() -> check(machine.mana().getStored() > before[0], "Multiple input pools did not supply mana")).thenExecute(() -> {
            check(machine.mana().getStored() > before[0] && machine.mana().getStored() - before[0] <= ManaTransfer.RATE, "Two pools multiplied the pull rate");
            configure(player, machine, RelativeSide.TOP, DataType.NONE); configure(player, machine, RelativeSide.BOTTOM, DataType.NONE);
            before[0] = machine.mana().getStored();
        }).thenIdle(2).thenExecute(() -> {
            check(machine.mana().getStored() == before[0], "Closed inputs continued pulling");
            check(machine.mana().getStored() + pools.values().stream().mapToInt(ManaPoolBlockEntity::getCurrentMana).sum() == 24000, "Pool transfer lost or created mana");
            save(h, machine);
        }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void poolPermissionsCapacityAndDirectInfusionUseRealMana(GameTestHelper h) {
        var pos = new BlockPos(20, 4, 20); var machine = machine(h, pos, ManaMachineKind.INFUSER); power(machine);
        var pool = barePool(h, pos.north(), 20000);
        h.setBlock(pos.south(), BotaniaBlocks.CREATIVE_MANA_POOL);
        var registry = h.getLevel().registryAccess(); var locked = pool.saveWithoutMetadata(registry); locked.putBoolean("canSpare", false); pool.loadWithComponents(locked, registry);
        h.startSequence().thenIdle(3).thenExecute(() -> {
            check(machine.mana().isEmpty() && pool.getCurrentMana() == 20000, "Machine took locked or creative mana");
            h.setBlock(pos.south(), Blocks.AIR);
            var unlocked = pool.saveWithoutMetadata(registry); unlocked.putBoolean("canSpare", true); pool.loadWithComponents(unlocked, registry);
            mana(machine, ManaMachine.MANA_CAPACITY - 75);
        }).thenWaitUntil(() -> check(machine.mana().getStored() == ManaMachine.MANA_CAPACITY, "Partial capacity did not fill"))
              .thenIdle(2).thenExecute(() -> {
                  check(pool.getCurrentMana() == 19925, "Full tank kept draining or lost its remainder");
                  mana(machine, 0); pool.receiveMana(75);
                  machine.inputs.getFirst().setStack(new ItemStack(Items.IRON_INGOT, 3));
              }).thenWaitUntil(() -> check(output(machine, BotaniaItems.MANASTEEL_INGOT) == 3, "Adjacent pool did not power actual infusion"))
              .thenExecute(() -> {
                  var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(vazkii.botania.common.crafting.BotaniaRecipeTypes.MANA_INFUSION_TYPE).stream()
                        .filter(r -> r.value().getResultItem(registry).is(BotaniaItems.MANASTEEL_INGOT)).findFirst().orElseThrow().value();
                  check(pool.getCurrentMana() + machine.mana().getStored() == 20000 - 3L * recipe.getManaToConsume(), "Infusion mana total is wrong");
                  h.setBlock(pos.north(), Blocks.AIR); long stored = machine.mana().getStored();
                  check(ManaTransfer.fillFromAdjacentPools(machine) == 0 && machine.mana().getStored() == stored, "Removed pool still supplied mana");
                  stop(machine);
              }).thenSucceed();
    }
}
