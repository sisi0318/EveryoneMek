package dev.everyonemek.botania;

import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.api.mana.spark.ManaSparkHelper;
import vazkii.botania.common.entity.ManaSparkEntity;
import vazkii.botania.common.item.*;

import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class SparkGameTests {
    private static ManaSparkEntity spark(GameTestHelper h, BlockPos relative) {
        check(ManaSparkItem.attachSpark(h.getLevel(), h.absolutePos(relative), new ItemStack(BotaniaItems.MANA_SPARK), ItemStack.EMPTY), "Native spark could not attach");
        return (ManaSparkEntity) ManaSparkHelper.getAttachedSpark(h.getLevel(), h.absolutePos(relative));
    }
    private static void interact(GameTestHelper h, ManaSparkEntity spark, ItemStack item) {
        var owner = player(h, "spark-player"); var old = owner.getMainHandItem().copy();
        try { owner.setItemInHand(InteractionHand.MAIN_HAND, item); check(spark.interact(owner, InteractionHand.MAIN_HAND).consumesAction(), "Native spark interaction failed"); }
        finally { owner.setItemInHand(InteractionHand.MAIN_HAND, old); }
    }
    @GameTest(template = "empty", timeoutTicks = 150)
    public static void ordinarySparksSupplyMachineAndRespectColorAndTopFace(GameTestHelper h) {
        var pos = new BlockPos(20, 2, 20); var source = pool(h, pos, 12000);
        var machine = machine(h, pos.east(8), ManaMachineKind.INFUSER); stop(machine);
        var owner = player(h, "spark-player"); var stack = new ItemStack(BotaniaItems.MANA_SPARK);
        check(((ManaMachineBlock) machine.getBlockState().getBlock()).useItemOn(stack, machine.getBlockState(), h.getLevel(), machine.getBlockPos(), owner,
              InteractionHand.MAIN_HAND, new BlockHitResult(machine.getBlockPos().getCenter(), Direction.UP, machine.getBlockPos(), false))
              == net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION, "Machine GUI intercepted native spark placement");
        var left = spark(h, pos); var right = spark(h, pos.east(8)); long[] stopped = {0};
        h.startSequence().thenWaitUntil(() -> check(machine.mana().getStored() > 0, "Ordinary native sparks did not supply machine"))
              .thenExecute(() -> {
                  machine.getConfig().getConfig(TransmissionType.CHEMICAL).setDataType(DataType.NONE, RelativeSide.TOP); stopped[0] = machine.mana().getStored();
              }).thenIdle(8).thenExecute(() -> {
                  check(machine.mana().getStored() == stopped[0] && machine.mana().getStored() + source.getCurrentMana() == 12000, "Closed top face transferred or lost mana");
                  interact(h, right, new ItemStack(Items.RED_DYE));
                  machine.getConfig().getConfig(TransmissionType.CHEMICAL).setDataType(DataType.INPUT, RelativeSide.TOP);
              }).thenIdle(24).thenExecute(() -> {
                  check(machine.mana().getStored() == stopped[0], "Different spark colors still transferred");
                  interact(h, left, new ItemStack(Items.RED_DYE));
              }).thenWaitUntil(() -> check(machine.mana().getStored() == 12000, "Matching native dyes did not restore supply"))
              .thenExecute(() -> { check(source.getCurrentMana() == 0, "Native spark conservation failed"); left.discard(); right.discard(); }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void rangeRequiresBothEndsAndRemovalStopsTheOldLongLink(GameTestHelper h) {
        var pos = new BlockPos(16, 2, 30); var pool = pool(h, pos, 24000);
        var machine = machine(h, pos.east(24), ManaMachineKind.RUNIC); stop(machine);
        var source = spark(h, pos); var target = spark(h, pos.east(24)); long[] afterRemoval = {0};
        h.startSequence().thenIdle(25).thenExecute(() -> {
            check(machine.mana().isEmpty(), "Ordinary spark gained extended range"); interact(h, target, new ItemStack(Content.SPARK_AUGMENT.get()));
        }).thenIdle(25).thenExecute(() -> {
            check(machine.mana().isEmpty(), "One upgraded end bypassed the two-end range requirement"); interact(h, source, new ItemStack(Content.SPARK_AUGMENT.get()));
        }).thenWaitUntil(() -> check(machine.mana().getStored() > 0, "Two range augments did not connect 24 blocks"))
              .thenExecute(() -> {
                  var tag = new CompoundTag(); target.saveWithoutId(tag); var restored = new ManaSparkEntity(h.getLevel()); restored.load(tag);
                  check(SparkExpansion.extended(restored) && restored.getNetwork() == target.getNetwork(), "Range or dye lost in native entity save");
                  restored.discard();
                  var original = target.position();
                  try {
                      target.setPos(source.getX(), source.getY() + 32, source.getZ());
                      check(SparkExpansion.nearby(source, h.getLevel(), source.getX(), source.getY() - .75, source.getZ(), source.getNetwork()).contains(target), "Block-centered request lost the vertical 32-block boundary");
                      target.setPos(source.getX() + 33, source.getY(), source.getZ());
                      check(!SparkExpansion.nearby(source, h.getLevel(), source.getX(), source.getY(), source.getZ(), source.getNetwork()).contains(target), "Spark exceeded the 32-block range");
                  } finally { target.setPos(original); }
                  var owner = player(h, "spark-player"); owner.setShiftKeyDown(true);
                  try { interact(h, source, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST)); } finally { owner.setShiftKeyDown(false); }
                  check(source.getUpgrade().isEmpty(), "Native wand did not remove range augment"); afterRemoval[0] = machine.mana().getStored();
              }).thenIdle(25).thenExecute(() -> {
                  check(machine.mana().getStored() == afterRemoval[0] && machine.mana().getStored() + pool.getCurrentMana() == 24000, "Removed augment left an active long-distance link");
                  source.discard(); target.discard();
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 90)
    public static void rangeCombinesWithNativeDominantAugmentAndKeepsItOnRemoval(GameTestHelper h) {
        var pos = new BlockPos(16, 2, 48); var source = pool(h, pos, 12000); var receiver = pool(h, pos.east(24), 0);
        var left = spark(h, pos); var right = spark(h, pos.east(24));
        interact(h, left, new ItemStack(Content.SPARK_AUGMENT.get()));
        interact(h, right, new ItemStack(BotaniaItems.SPARK_AUGMENT_DOMINANT)); interact(h, right, new ItemStack(Content.SPARK_AUGMENT.get()));
        check(right.getUpgrade().is(BotaniaItems.SPARK_AUGMENT_DOMINANT) && SparkExpansion.extended(right), "Range replaced the native augment role");
        h.startSequence().thenWaitUntil(() -> check(receiver.getCurrentMana() == 12000, "Native dominant logic did not transfer at extended range"))
              .thenExecute(() -> {
                  check(source.getCurrentMana() == 0, "Extended dominant transfer changed mana total");
                  var owner = player(h, "spark-player"); owner.setShiftKeyDown(true);
                  try { interact(h, right, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST)); } finally { owner.setShiftKeyDown(false); }
                  var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, right.getBoundingBox().inflate(3));
                  check(drops.stream().anyMatch(item -> item.getItem().is(BotaniaItems.SPARK_AUGMENT_DOMINANT) && SparkExpansion.extended(item.getItem())), "Removing combined augment lost native role or range");
                  drops.forEach(net.minecraft.world.entity.Entity::discard); left.discard(); right.discard();
              }).thenSucceed();
    }
}
