package dev.everyonemek.botania;

import java.util.List;
import dev.everyonemek.botania.mixin.SparkTransfersAccess;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.*;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;
import vazkii.botania.common.entity.ManaSparkEntity;
import vazkii.botania.common.lib.BotaniaTags;

public final class SparkExpansion {
    public static final int RANGE = 32;
    public static boolean extended(ItemStack stack) { return stack.is(Content.SPARK_AUGMENT.get()) || Boolean.TRUE.equals(stack.get(Content.SPARK_RANGE.get())); }
    public static boolean extended(ManaSpark spark) { return extended(spark.getUpgrade()); }
    public static void register(IEventBus bus) {
        bus.addListener(SparkExpansion::capabilities);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SparkExpansion::tooltip);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (var entry : ManaContent.MACHINE_TILES.entrySet()) if (entry.getKey().chemical) {
            event.registerBlockEntity(BotaniaNeoForgeCapabilities.getBlockApiLookupById(ManaReceiver.LOOKUP), entry.getValue().get(), (tile, side) -> new MachineSparkPort(tile));
            event.registerBlockEntity(BotaniaNeoForgeCapabilities.getBlockApiLookupById(ManaSparkAttachable.LOOKUP), entry.getValue().get(), (tile, side) -> new MachineSparkPort(tile));
        }
    }
    private static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (extended(event.getItemStack())) event.getToolTip().add(Component.translatable("tooltip.botanicalmekanism.spark_range", RANGE));
    }
    public static void supplyMachine(ManaMachine machine) {
        if (!machine.kind().chemical || machine.ticker % 20 != 0 || new MachineSparkPort(machine).isFull()) return;
        ManaSparkHelper.registerTransferFromSparksAround(ManaSparkHelper.getAttachedSpark(machine.getLevel(), machine.getBlockPos()), machine.getLevel(), machine.getBlockPos());
    }
    public static List<ManaSpark> nearby(ManaSpark origin, Level level, double x, double y, double z, DyeColor color) {
        if (origin == null || !extended(origin)) return ManaSparkHelper.getSparksAround(level, x, y, z, color);
        // Callers use either the block center or spark center; range is measured between sparks.
        return level.getEntitiesOfClass(ManaSparkEntity.class, origin.entity().getBoundingBox().inflate(RANGE),
                    spark -> spark.getNetwork() == color && inRange(origin, spark)).stream().map(spark -> (ManaSpark) spark).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }
    private static boolean inRange(ManaSpark a, ManaSpark b) {
        if (!extended(a) && !extended(b)) return true;
        int range = extended(a) && extended(b) ? RANGE : ManaSparkHelper.SPARK_SCAN_RANGE;
        var first = a.entity(); var second = b.entity();
        return first.level() == second.level() && Math.abs(first.getX()-second.getX()) <= range
              && Math.abs(first.getY()-second.getY()) <= range && Math.abs(first.getZ()-second.getZ()) <= range;
    }
    public static void prune(ManaSparkEntity spark) {
        spark.getOutgoingTransfers().removeIf(other -> !inRange(spark, other));
        ((SparkTransfersAccess) spark).botanicalmekanism$inbound().removeIf(other -> !inRange(spark, other));
    }
    public static void refresh(ManaSparkEntity spark) {
        if (spark.level().isClientSide) return;
        // Also notify old long-distance peers when an augment/color is removed or changed.
        for (var other : spark.level().getEntitiesOfClass(ManaSparkEntity.class, spark.getBoundingBox().inflate(RANGE))) other.updateTransfers();
    }
    /** Return null to retain the native interaction unchanged. */
    public static InteractionResult interact(ManaSparkEntity spark, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand); var upgrade = spark.getUpgrade();
        var pos = spark.getAttachPos();
        var target = spark.level().hasChunkAt(pos) ? spark.level().getBlockEntity(pos) : null;
        if (target instanceof ManaMachine && !IBlockSecurityUtils.INSTANCE.canAccess(player, spark.level(), pos, target)) return InteractionResult.FAIL;
        boolean installingRange = stack.is(Content.SPARK_AUGMENT.get()) && !extended(upgrade);
        boolean addingNative = upgrade.is(Content.SPARK_AUGMENT.get()) && stack.is(BotaniaTags.Items.MANA_SPARK_AUGMENTS) && !stack.is(Content.SPARK_AUGMENT.get());
        // Native pool control augments remain pool controls. Machines accept range extension only.
        if (target instanceof ManaMachine && stack.is(BotaniaTags.Items.MANA_SPARK_AUGMENTS) && !stack.is(Content.SPARK_AUGMENT.get())) return InteractionResult.FAIL;
        if (!installingRange && !addingNative) return null;
        if (!spark.level().isClientSide) {
            ItemStack updated = installingRange ? upgrade.isEmpty() ? new ItemStack(Content.SPARK_AUGMENT.get()) : upgrade.copy() : stack.copyWithCount(1);
            updated.set(Content.SPARK_RANGE.get(), true); spark.setUpgrade(updated);
            if (!player.isCreative()) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(spark.level().isClientSide);
    }
    private SparkExpansion() { }
}
