package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import io.netty.buffer.Unpooled;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NaturesMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ControllerGameTests {
    private static void check(boolean condition, String message) { if (!condition) throw new net.minecraft.gametest.framework.GameTestAssertException(message); }
    private static AuraMachine machine(GameTestHelper h, MachineKind kind, int x) {
        BlockPos pos = new BlockPos(x, 1, 5);
        h.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(pos, Content.MACHINES.get(kind).get());
        AuraMachine m = (AuraMachine) h.getBlockEntity(pos);
        m.energy().setEnergy(m.energy().getMaxEnergy());
        return m;
    }
    private static int ambient(AuraMachine m) { return IAuraChunk.getAuraInArea(m.getLevel(), m.getBlockPos(), m.environmentRadius()); }
    private static void ambient(AuraMachine m, int target) {
        check(AuraTestEnvironment.set(m, m.environmentRadius(), target) == target, "Controller environment fixture failed");
    }

    @GameTest(template = "empty", batch = "controller_balance", timeoutTicks = 35)
    public static void balanceTransfersOnlyExistingAuraAndStopsAtTheLimits(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.AURA_CONTROLLER, 5);
        ambient(m, 1_000_000);
        m.controller().setLower(1_001_500);
        m.controller().setUpper(1_002_500);
        m.auraTank().setStack(new ChemicalStack(Content.AURA, 4_000));
        long perTick = m.energy().getEnergyPerTick();
        h.runAfterDelay(6, () -> {
            check(ambient(m) == 1_001_500 && m.auraTank().getStored() == 2_500, "Release overshot the lower limit or duplicated Aura");
            check(m.status() == 9 && m.energy().getMaxEnergy() - m.energy().getEnergy() == perTick * 2, "Idle balance continued to consume power");
            ambient(m, 1_003_750);
        });
        h.runAfterDelay(14, () -> {
            check(ambient(m) == 1_002_500 && m.auraTank().getStored() == 3_750, "Recovery overshot the upper limit or lost Aura");
            check(m.energy().getMaxEnergy() - m.energy().getEnergy() == perTick * 4, "Recovery partial transfer charged incorrect ticks");
            check(!m.getActive(), "At-target controller remained active");
            m.energy().setEnergy(0);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "controller_full", timeoutTicks = 35)
    public static void recoveryChecksPowerAndFitsTheLastTankUnit(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.AURA_CONTROLLER, 5);
        ambient(m, 1_000);
        m.controller().setLower(0);
        m.controller().setUpper(0);
        m.auraTank().setStack(new ChemicalStack(Content.AURA, AuraMachine.AURA_CAPACITY - 1));
        m.energy().setEnergy(0);
        h.runAfterDelay(5, () -> {
            check(ambient(m) == 1_000 && m.auraTank().getNeeded() == 1, "Unpowered recovery drained the environment");
            m.energy().setEnergy(m.energy().getMaxEnergy());
        });
        h.runAfterDelay(12, () -> {
            check(m.auraTank().getNeeded() == 0 && ambient(m) == 999, "Recovery did not fill exactly the last tank unit");
            check(m.energy().getMaxEnergy() - m.energy().getEnergy() == m.energy().getEnergyPerTick(), "Full tank continued spending energy");
            m.auraTank().extract(1, Action.EXECUTE, AutomationType.INTERNAL);
        });
        h.runAfterDelay(20, () -> {
            check(m.auraTank().getNeeded() == 0 && ambient(m) == 998, "Partial tank extraction did not resume recovery");
            m.energy().setEnergy(0);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "controller_modes", timeoutTicks = 20)
    public static void directionalModesAndNegativeLimitsAreRespected(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.AURA_CONTROLLER, 5);
        m.controller().setLower(-100_000);
        m.controller().setUpper(-100_000);
        ambient(m, -99_500);
        m.controller().cycleMode(); // recover
        m.onUpdateServer();
        check(ambient(m) == -100_000 && m.auraTank().getStored() == 500, "Signed recovery target was clamped or duplicated Aura");
        m.controller().cycleMode(); // release
        ambient(m, -100_250);
        m.onUpdateServer();
        check(ambient(m) == -100_000 && m.auraTank().getStored() == 250, "Release did not stop at the negative lower target");
        m.controller().cycleMode(); // hold
        long energy = m.energy().getEnergy();
        ambient(m, 0);
        m.onUpdateServer();
        check(ambient(m) == 0 && m.energy().getEnergy() == energy && m.status() == 10, "Disabled controller changed resources");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void rangeLimitsSettingsAndDroppedStateStayConsistent(GameTestHelper h) {
        AuraMachine m = machine(h, MachineKind.AURA_CONTROLLER, 5);
        ItemStack range = new ItemStack(Content.RANGE_MODULE.get(), 8);
        check(m.rangeModuleSlot().insertItem(range, Action.EXECUTE, AutomationType.MANUAL).getCount() == 4, "Range module limit is not four");
        check(m.environmentRadius() == Math.min(64, MachineConfig.CONTROLLER_RADIUS.get() + 32), "Range modules did not extend the query radius");
        m.getComponent().addUpgrades(Upgrade.SPEED, 2);
        m.getComponent().addUpgrades(Upgrade.ENERGY, 2);
        long normal = MekanismUtils.getEnergyPerTick(m, m.energy().getBaseEnergyPerTick());
        check(m.energy().getEnergyPerTick() == normal * 5, "Range cost was lost when Mek upgrades changed");
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setPos(m.getBlockPos().getX() + .5, m.getBlockPos().getY() + .5, m.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(77, player.getInventory(), m);
        check(menu.applySetting(player, SetMachineSettingPayload.CONTROL_LOWER, -100_000), "Accessible controller rejected the numeric setting");
        check(menu.applySetting(player, SetMachineSettingPayload.CONTROL_UPPER, -200_000) && m.controller().upper() == -100_000, "Reversed limits were not clamped");
        check(!menu.applySetting(player, 999, 1), "Unknown setting was accepted");
        player.setPos(player.getX() + 100, player.getY(), player.getZ());
        check(!menu.applySetting(player, SetMachineSettingPayload.CONTROL_LOWER, 100), "Distant player changed a machine setting");
        var buffer = Unpooled.buffer();
        var payload = new SetMachineSettingPayload(77, SetMachineSettingPayload.CONTROL_LOWER, -123_456);
        SetMachineSettingPayload.STREAM_CODEC.encode(buffer, payload);
        check(payload.equals(SetMachineSettingPayload.STREAM_CODEC.decode(buffer)), "Signed setting payload did not round-trip");
        buffer.release();
        ItemStack drop = Block.getDrops(m.getBlockState(), h.getLevel(), m.getBlockPos(), m, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
        drop = ItemStack.parse(h.getLevel().registryAccess(), drop.save(h.getLevel().registryAccess())).orElseThrow();
        AuraMachine restored = new AuraMachine(m.getBlockPos(), m.getBlockState());
        restored.setLevel(h.getLevel());
        restored.applyComponentsFromItemStack(drop);
        check(restored.controller().lower() == -100_000 && restored.controller().upper() == -100_000 && restored.rangeModules() == 4,
              "Dropped controller lost limits or range modules");
        restored.rangeModuleSlot().extractItem(4, Action.EXECUTE, AutomationType.MANUAL);
        check(restored.environmentRadius() == MachineConfig.CONTROLLER_RADIUS.get() && restored.energy().getEnergyPerTick() == normal,
              "Removing range upgrades did not restore normal range and power");
        m.energy().setEnergy(0);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "controller_tubes", timeoutTicks = 130)
    public static void controllerAcceptsAndOutputsAuraThroughRealPressurizedTubes(GameTestHelper h) {
        machine(h, MachineKind.AURA_GENERATOR, 2);
        AuraMachine controller = machine(h, MachineKind.AURA_CONTROLLER, 4);
        AuraMachine altar = machine(h, MachineKind.NATURAL_ALTAR, 6);
        controller.controller().cycleMode();
        controller.controller().cycleMode();
        controller.controller().cycleMode(); // hold environment, keep normal logistics active
        var config = controller.getConfig().getConfig(TransmissionType.CHEMICAL);
        config.setDataType(DataType.INPUT, RelativeSide.fromDirections(controller.getDirection(), Direction.WEST));
        config.setDataType(DataType.OUTPUT, RelativeSide.fromDirections(controller.getDirection(), Direction.EAST));
        h.setBlock(new BlockPos(3, 1, 5), MekanismBlocks.BASIC_PRESSURIZED_TUBE.get());
        h.setBlock(new BlockPos(5, 1, 5), MekanismBlocks.BASIC_PRESSURIZED_TUBE.get());
        h.runAfterDelay(100, () -> {
            check(altar.auraTank().getStored() > 0, "Controller bidirectional Chemical logistics did not pass Aura to the altar");
            h.succeed();
        });
    }
}
