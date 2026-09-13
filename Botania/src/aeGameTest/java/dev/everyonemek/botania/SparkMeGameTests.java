package dev.everyonemek.botania;

import java.util.*;
import appeng.api.networking.*;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.core.definitions.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.mana.spark.ManaSparkHelper;
import vazkii.botania.common.item.BotaniaItems;
import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class SparkMeGameTests {
    static void cable(GameTestHelper h, BlockPos pos, boolean dense) {
        h.setBlock(pos, AEBlocks.CABLE_BUS.block());
        var host = PartHelper.getPartHost(h.getLevel(), h.absolutePos(pos));
        if (dense) host.addPart(AEParts.SMART_DENSE_CABLE.item(AEColor.TRANSPARENT), null, player(h, "spark-me"));
        else host.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, player(h, "spark-me"));
    }
    static MechanicalSparkEntity spark(GameTestHelper h, BlockPos pos, boolean master, DyeColor color) {
        var owner = player(h, "spark-me"); var abs = h.absolutePos(pos); owner.setPos(abs.getCenter());
        var item = new ItemStack(master ? MechanicalSparks.MASTER.get() : MechanicalSparks.SPARK.get()); owner.setItemInHand(InteractionHand.MAIN_HAND, item);
        check(owner.gameMode.useItemOn(owner, h.getLevel(), item, InteractionHand.MAIN_HAND, new BlockHitResult(abs.getCenter(), Direction.UP, abs, false)).consumesAction(),
              "AE block intercepted mechanical spark placement");
        var attached = ManaSparkHelper.getAttachedSpark(h.getLevel(), abs);
        check(attached instanceof MechanicalSparkEntity && item.isEmpty(), "Mechanical spark did not attach to AE block exactly once: pos=" + pos + ", spark=" + attached + ", hand=" + item);
        var spark = (MechanicalSparkEntity) attached; spark.setNetwork(color); return spark;
    }
    private static long active(List<InterfaceBlockEntity> devices) { return devices.stream().filter(be -> be.getMainNode().isActive()).count(); }
    @GameTest(template = "empty", timeoutTicks = 1400)
    public static void wirelessChannelsScaleTo256AndRecoverAfterRangePowerAndEntityReload(GameTestHelper h) {
        check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("botanicalmekanism:spark_channel_upgrade")).isPresent(), "Channel module recipe missing");
        var pos = new BlockPos(10, 3, 40); h.setBlock(pos, AEBlocks.CONTROLLER.block()); h.setBlock(pos.below(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var controller = (ControllerBlockEntity) h.getBlockEntity(pos);
        var master = new MechanicalSparkEntity[]{spark(h, pos, true, DyeColor.BLACK)};
        master[0].modules.setItem(0, new ItemStack(MechanicalSparks.RANGE.get(), 8));
        List<MechanicalSparkEntity> sparks = new ArrayList<>(); List<InterfaceBlockEntity> devices = new ArrayList<>(); List<BlockPos> cableAnchors = new ArrayList<>();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            if (row == 2 && col == 2) continue;
            var start = new BlockPos(30 + col * 20, 3, 10 + row * 30); cableAnchors.add(start);
            for (int i = 0; i < 16; i++) {
                var at = start.south(i); cable(h, at, true);
                for (var face : new Direction[]{Direction.EAST, Direction.WEST}) {
                    h.setBlock(at.relative(face), AEBlocks.INTERFACE.block()); devices.add((InterfaceBlockEntity) h.getBlockEntity(at.relative(face)));
                }
            }
            sparks.add(spark(h, start, false, DyeColor.BLACK));
        }
        var saved = new CompoundTag[1]; var sequence = h.startSequence();
        sequence.thenIdle(30).thenExecute(() -> check(active(devices) == 0, "Wireless AE connected without a channel module"));
        for (int tier = 1; tier <= 4; tier++) {
            int count = tier, expected = 32 << (tier - 1);
            sequence.thenExecute(() -> master[0].modules.setItem(2, new ItemStack(MechanicalSparks.CHANNEL.get(), count)))
                  .thenWaitUntil(() -> check(active(devices) == expected, "Channel tier " + expected + " active=" + active(devices) + ", stats=" + master[0].meLink().stats()))
                  .thenExecute(() -> {
                      check(master[0].meLink().stats().capacity() == expected, "Controller reported wrong channel capacity");
                      var nativeCable = GridHelper.getExposedNode(h.getLevel(), h.absolutePos(cableAnchors.getFirst()), Direction.UP);
                      check(nativeCable != null && nativeCable.getMaxChannels() == 32, "Spark module changed vanilla dense cable capacity");
                  });
        }
        sequence.thenExecute(() -> {
            check(master[0].meLink().stats().used() == 256 && master[0].meLink().stats().links() == 8 && master[0].meLink().stats().power() > 0, "Channels or maintenance power were not accounted for");
            master[0].modules.setItem(0, new ItemStack(MechanicalSparks.RANGE.get(), 1));
        }).thenWaitUntil(() -> check(active(devices) == 96, "Short range did not use three in-range relay endpoints"))
              .thenExecute(() -> master[0].modules.setItem(0, ItemStack.EMPTY))
              .thenWaitUntil(() -> check(active(devices) == 0, "Range removal retained a long wireless path"))
              .thenExecute(() -> master[0].modules.setItem(0, new ItemStack(MechanicalSparks.RANGE.get(), 8)))
              .thenWaitUntil(() -> check(active(devices) == 256, "Restoring range failed to reconnect"))
              .thenExecute(() -> master[0].setNetwork(DyeColor.PURPLE))
              .thenWaitUntil(() -> check(active(devices) == 0, "Dye did not disconnect the data network"))
              .thenExecute(() -> master[0].setNetwork(DyeColor.BLACK))
              .thenWaitUntil(() -> check(active(devices) == 256, "Matching dye did not restore the data network"))
              .thenExecute(() -> h.setBlock(pos.below(), Blocks.AIR))
              .thenWaitUntil(() -> check(active(devices) == 0 && master[0].meLink().stats().state() == SparkMeLink.NO_POWER, "Unpowered wireless network remained active"))
              .thenExecute(() -> h.setBlock(pos.below(), AEBlocks.CREATIVE_ENERGY_CELL.block()))
              .thenWaitUntil(() -> check(active(devices) == 256, "Power recovery failed"))
              .thenExecute(() -> { saved[0] = new CompoundTag(); master[0].saveWithoutId(saved[0]); master[0].discard(); })
              .thenWaitUntil(() -> check(active(devices) == 0, "Removed master kept channels alive"))
              .thenExecute(() -> {
                  master[0] = new MechanicalSparkEntity(MechanicalSparks.ENTITY.get(), h.getLevel()); master[0].load(saved[0]);
                  check(h.getLevel().addFreshEntity(master[0]), "Saved master could not reload");
              }).thenWaitUntil(() -> check(active(devices) == 256, "Reloaded master lost channel modules or node lifecycle"))
              .thenExecute(() -> { master[0].discard(); sparks.forEach(net.minecraft.world.entity.Entity::discard); })
              .thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 250)
    public static void wirelessRefusesIndependentControllersAndRespectsThinCableBottlenecks(GameTestHelper h) {
        var pos = new BlockPos(20, 3, 20); h.setBlock(pos, AEBlocks.CONTROLLER.block()); h.setBlock(pos.below(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var creative = new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(), player(h, "spark-me-creative").getGameProfile()) {
            @Override public boolean isCreative() { return true; }
        };
        var creativePos = h.absolutePos(pos.west(8)); h.setBlock(pos.west(8), AEBlocks.CONTROLLER.block()); creative.setPos(creativePos.getCenter());
        var held = new ItemStack(MechanicalSparks.SPARK.get(), 8); creative.setItemInHand(InteractionHand.MAIN_HAND, held);
        creative.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BLUE_DYE, 2));
        creative.gameMode.useItemOn(creative, h.getLevel(), held, InteractionHand.MAIN_HAND, new BlockHitResult(creativePos.getCenter(), Direction.UP, creativePos, false));
        var creativeSpark = ManaSparkHelper.getAttachedSpark(h.getLevel(), creativePos);
        check(creativeSpark instanceof MechanicalSparkEntity && held.getCount() == 8 && creative.getOffhandItem().getCount() == 2 && creativeSpark.getNetwork() == DyeColor.BLUE,
              "Early AE placement consumed creative items or lost offhand dye");
        creativeSpark.entity().discard(); h.setBlock(pos.west(8), Blocks.AIR);
        var source = (ControllerBlockEntity) h.getBlockEntity(pos); var master = spark(h, pos, true, DyeColor.GREEN);
        master.modules.setItem(2, new ItemStack(MechanicalSparks.CHANNEL.get(), 4));
        h.setBlock(pos.east(8), AEBlocks.CONTROLLER.block()); h.setBlock(pos.east(8).below(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var foreign = (ControllerBlockEntity) h.getBlockEntity(pos.east(8)); var foreignSpark = spark(h, pos.east(8), false, DyeColor.GREEN);
        List<InterfaceBlockEntity> devices = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            var at = pos.south(8 + i); cable(h, at, false); h.setBlock(at.east(), AEBlocks.INTERFACE.block()); devices.add((InterfaceBlockEntity) h.getBlockEntity(at.east()));
        }
        var child = spark(h, pos.south(8), false, DyeColor.GREEN);
        h.startSequence().thenWaitUntil(() -> check(master.meLink().stats().state() == SparkMeLink.CONTROLLERS && active(devices) == 8,
              "Thin cable or controller conflict rules failed: " + master.meLink().stats()))
              .thenExecute(() -> {
                  check(source.getMainNode().getGrid() != foreign.getMainNode().getGrid(), "Two controller networks were merged");
                  check(GridHelper.getExposedNode(h.getLevel(), h.absolutePos(pos.south(8)), Direction.UP).getMaxChannels() == 8, "Thin cable was silently upgraded");
                  master.discard(); foreignSpark.discard(); child.discard();
              }).thenSucceed();
    }
}
