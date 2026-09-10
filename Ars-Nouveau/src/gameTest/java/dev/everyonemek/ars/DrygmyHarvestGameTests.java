package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.common.items.MobJarItem;
import com.hollingsworth.arsnouveau.common.items.data.MobJarData;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import java.util.List;
import mekanism.api.Upgrade;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.item.AttachedItems;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsMekanism.ID)
@PrefixGameTestTemplate(false)
public final class DrygmyHarvestGameTests {
    private static final BlockPos POS = new BlockPos(5, 1, 5);
    private static void check(boolean value, String message) { if (!value) throw new GameTestAssertException(message); }
    private static SourceMachine machine(GameTestHelper h) {
        h.setBlock(POS, Content.MACHINES.get(MachineKind.DRYGMY_STATION).defaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var m = (SourceMachine) h.getBlockEntity(POS); m.energy().setEnergy(m.energy().getMaxEnergy()); return m;
    }
    private static void source(SourceMachine m, int amount) { m.sourceTank().setStack(new ChemicalStack(Content.SOURCE, amount)); }
    private static void ticks(SourceMachine m, int count) { for (int i = 0; i < count; i++) m.onUpdateServer(); }
    private static int count(SourceMachine m, net.minecraft.world.level.ItemLike item) {
        return m.outputs().stream().filter(s -> s.getStack().is(item.asItem())).mapToInt(s -> s.getCount()).sum();
    }
    private static ItemStack jar(GameTestHelper h, EntityType<?> type, boolean baby) {
        var entity = type.create(h.getLevel()); var tag = new CompoundTag();
        entity.setCustomName(Component.literal("retained captive")); check(entity.save(tag), "Could not save test captive");
        tag.putString("DeathLootTable", "arsmekanism:drygmy_contract");
        if (baby) tag.putBoolean("IsBaby", true);
        var stack = new ItemStack(BlockRegistry.MOB_JAR);
        stack.set(DataComponentRegistry.MOB_JAR, new MobJarData(tag, new CompoundTag())); return stack;
    }
    private static ItemStack drop(GameTestHelper h, SourceMachine m) {
        var item = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
        return ItemStack.parse(h.getLevel().registryAccess(), item.save(h.getLevel().registryAccess())).orElseThrow();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void internalHarvestUsesNativeBonusAndXPAndSpeedUpgradesShortenProduction(GameTestHelper h) {
        var m = machine(h);
        try {
            ItemStack zombie = jar(h, EntityType.ZOMBIE, true), chicken = jar(h, EntityType.CHICKEN, false);
            List<ItemStack> jars = List.of(zombie, zombie.copy(), chicken);
            var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, m.getBlockPos(), Direction.UP);
            check(handler != null && handler.getSlots() == 8, "Eight jar slots were not exposed on input 2");
            for (int i = 0; i < jars.size(); i++) {
                check(handler.insertItem(i, jars.get(i).copy(), true).isEmpty() && m.jarSlots.get(i).isEmpty(), "Jar insertion simulation changed inventory");
                check(handler.insertItem(i, jars.get(i).copy(), false).isEmpty(), "Jar input rejected a populated captive jar");
                check(handler.extractItem(i, 1, false).isEmpty(), "Automation removed a retained jar");
            }
            int bonus = 2 * Config.DRYGMY_UNIQUE_BONUS.get() + Math.min(Config.DRYGMY_QUANTITY_CAP.get(), 3);
            int expected = Config.DRYGMY_BASE_ITEM.get() + bonus, cost = Config.DRYGMY_MANA_COST.get(); source(m, cost * 3);
            int xp = 0;
            for (var item : jars) xp += ((LivingEntity) MobJarItem.fromItem(item.copy(), h.getLevel())).getExperienceReward(h.getLevel(), ANFakePlayer.getPlayer(h.getLevel()));
            xp /= 4;
            int greater = xp > 3 ? xp / 12 : 0, lesser = xp > 3 ? (xp % 12 + 2) / 3 : 0;
            int base = m.drygmyCycleTicks(); ticks(m, base - 1);
            check(count(m, Items.GOLD_NUGGET) == 0 && m.sourceTank().getStored() == cost * 3L, "Harvest completed or charged Source early");
            m.onUpdateServer();
            check(count(m, Items.GOLD_NUGGET) == expected && m.observedPrimary() == bonus && m.observedSecondary() == 3 && m.observedTertiary() == 2,
                  "Internal harvest does not match native quantity and diversity bonuses");
            check(count(m, ItemsRegistry.GREATER_EXPERIENCE_GEM) == greater && count(m, ItemsRegistry.EXPERIENCE_GEM) == lesser, "Experience gems differ from the native formula");
            m.outputs().forEach(slot -> slot.setStack(ItemStack.EMPTY));
            m.getComponent().addUpgrades(Upgrade.SPEED, 4); m.getComponent().addUpgrades(Upgrade.ENERGY, 4);
            int faster = m.drygmyCycleTicks(); check(faster < base, "Speed upgrades did not shorten the harvest cycle");
            ticks(m, faster - 1); check(count(m, Items.GOLD_NUGGET) == 0, "Upgraded harvest completed before its cycle"); m.onUpdateServer();
            check(count(m, Items.GOLD_NUGGET) == expected && m.sourceTank().getStored() == cost,
                  "Upgraded harvest changed yield or charged extra Source");
            check(count(m, ItemsRegistry.GREATER_EXPERIENCE_GEM) == greater && count(m, ItemsRegistry.EXPERIENCE_GEM) == lesser, "Repeated baby-zombie XP queries accumulated a multiplier");
            for (int i = 0; i < jars.size(); i++) check(ItemStack.matches(m.jarSlots.get(i).getStack(), jars.get(i)), "Captive jar data or item count changed");
            for (var slot : m.jarSlots) slot.setStack(ItemStack.EMPTY);
            m.jarSlots.get(0).setStack(jar(h, EntityType.IRON_GOLEM, false));
            long power = m.energy().getEnergy(); m.onUpdateServer();
            check(m.status() == SourceMachine.NO_JAR_CREATURE && m.energy().getEnergy() == power && m.sourceTank().getStored() == cost,
                  "Native Drygmy blacklist was bypassed");
            h.succeed();
        } finally { m.energy().setEnergy(0); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void legacyInventoriesAndBlockedHarvestSurviveWorldAndItemReloads(GameTestHelper h) {
        var m = machine(h); SourceMachine restored = null;
        try {
            m.inputs.get(0).setStack(new ItemStack(Items.DIAMOND)); m.inputs.get(1).setStack(new ItemStack(Items.GOLD_INGOT, 5));
            m.outputs().getFirst().setStack(new ItemStack(Items.IRON_INGOT, 7));
            m.getInventorySlots(null).get(13).setStack(new ItemStack(mekanism.common.registries.MekanismItems.ENERGY_TABLET.get()));
            m.getComponent().addUpgrades(Upgrade.SPEED, 2); m.cycleMode(); source(m, 4_000);
            m.getConfig().getConfig(TransmissionType.ITEM).setDataType(DataType.INPUT, RelativeSide.TOP);
            var oldItems = m.getInventorySlots(null).stream().limit(14).map(slot -> slot.getStack().copy()).toList();
            var oldDrop = drop(h, m); oldDrop.set(ContainerType.ITEM.getComponentType(), new AttachedItems(oldItems));
            restored = new SourceMachine(m.getBlockPos(), m.getBlockState()); restored.setLevel(h.getLevel()); restored.applyComponentsFromItemStack(oldDrop);
            check(restored.getInventorySlots(null).size() == 22 && restored.jarSlots.stream().allMatch(slot -> slot.isEmpty()), "Old inventory did not gain eight empty jar slots");
            for (int i = 0; i < 14; i++) check(ItemStack.matches(restored.getInventorySlots(null).get(i).getStack(), oldItems.get(i)), "Old item slot " + i + " moved or lost data");
            check(restored.mode() == 1 && restored.getComponent().getUpgrades(Upgrade.SPEED) == 2 && restored.sourceTank().getStored() == 4_000
                  && restored.getConfig().getConfig(TransmissionType.ITEM).getDataType(RelativeSide.TOP) == DataType.INPUT, "Old settings or upgrades changed during migration");
            m.energy().setEnergy(0); h.getLevel().setBlockEntity(restored); m = restored;
            m.inputs.forEach(slot -> slot.setStack(ItemStack.EMPTY)); m.cycleMode();
            m.getInventorySlots(null).get(13).setStack(ItemStack.EMPTY);
            m.jarSlots.get(0).setStack(jar(h, EntityType.CHICKEN, false));
            m.outputs().forEach(slot -> slot.setStack(new ItemStack(Items.COBBLESTONE, 64)));
            long power = m.energy().getEnergy(), source = m.sourceTank().getStored(); m.onUpdateServer();
            check(m.status() == SourceMachine.OUTPUT_FULL && m.energy().getEnergy() == power && m.sourceTank().getStored() == source, "Blocked harvest consumed resources");
            var planned = m.drygmyHarvest.copy(); ticks(m, 8); check(planned.equals(m.drygmyHarvest), "Blocked output rerolled loot");
            var world = m.saveWithoutMetadata(h.getLevel().registryAccess());
            restored = new SourceMachine(m.getBlockPos(), m.getBlockState()); restored.setLevel(h.getLevel()); restored.loadAdditional(world, h.getLevel().registryAccess());
            check(planned.equals(restored.drygmyHarvest) && ItemStack.matches(restored.jarSlots.get(0).getStack(), m.jarSlots.get(0).getStack()), "World reload lost jar contents or the prepared harvest");
            m.energy().setEnergy(0); h.getLevel().setBlockEntity(restored); m = restored; m.outputs().forEach(slot -> slot.setStack(ItemStack.EMPTY));
            int cycle = m.drygmyCycleTicks(), half = Math.max(1, cycle / 2); ticks(m, half);
            world = m.saveWithoutMetadata(h.getLevel().registryAccess());
            restored = new SourceMachine(m.getBlockPos(), m.getBlockState()); restored.setLevel(h.getLevel()); restored.loadAdditional(world, h.getLevel().registryAccess());
            check(restored.progress() == m.progress() && restored.progress() > 0, "World reload reset a matching harvest's progress");
            m.energy().setEnergy(0); h.getLevel().setBlockEntity(restored); m = restored; ticks(m, cycle - half);
            check(count(m, Items.GOLD_NUGGET) == Config.DRYGMY_BASE_ITEM.get() + Config.DRYGMY_UNIQUE_BONUS.get() + Math.min(Config.DRYGMY_QUANTITY_CAP.get(), 1)
                  && m.sourceTank().getStored() == source - Config.DRYGMY_MANA_COST.get(), "Restored harvest did not finish once with the remaining time and cost");
            h.succeed();
        } finally { m.energy().setEnergy(0); if (restored != null) restored.energy().setEnergy(0); }
    }

    @GameTest(template = "empty", batch = "ars_drygmy_queue", timeoutTicks = 90)
    public static void paidOverflowSurvivesPickupAndDrainsToAChestAfterJarsAreRemoved(GameTestHelper h) {
        var m = machine(h); int previous = Config.DRYGMY_BASE_ITEM.get();
        try {
            Config.DRYGMY_BASE_ITEM.set(512);
            int expected = 512 + Config.DRYGMY_UNIQUE_BONUS.get() + Math.min(Config.DRYGMY_QUANTITY_CAP.get(), 1);
            m.jarSlots.get(0).setStack(jar(h, EntityType.CHICKEN, false)); source(m, Config.DRYGMY_MANA_COST.get());
            ticks(m, m.drygmyCycleTicks());
            check(count(m, Items.GOLD_NUGGET) == 384 && m.drygmyPendingItems() == expected - 384 && m.sourceTank().isEmpty(),
                  "Oversized harvest did not pay once and retain the full overflow");
            m.jarSlots.get(0).setStack(ItemStack.EMPTY);
            var item = drop(h, m); var restored = new SourceMachine(m.getBlockPos(), m.getBlockState()); restored.setLevel(h.getLevel()); restored.applyComponentsFromItemStack(item);
            check(restored.internalDrygmy() && restored.drygmyPendingItems() == expected - 384, "Pickup discarded paid output when no jars remained");
            m.energy().setEnergy(0); h.getLevel().setBlockEntity(restored);
            var chestPos = restored.getBlockPos().relative(RelativeSide.RIGHT.getDirection(restored.getDirection()));
            h.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            h.runAfterDelay(60, () -> {
                try {
                    var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(chestPos); int count = 0;
                    for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.GOLD_NUGGET)) count += chest.getItem(i).getCount();
                    check(count == expected && restored.drygmyPendingItems() == 0 && restored.sourceTank().isEmpty(),
                          "Paid overflow was lost, duplicated, or demanded a second Source charge"); h.succeed();
                } finally { restored.energy().setEnergy(0); }
            });
        } finally { Config.DRYGMY_BASE_ITEM.set(previous); m.energy().setEnergy(0); }
    }
}
