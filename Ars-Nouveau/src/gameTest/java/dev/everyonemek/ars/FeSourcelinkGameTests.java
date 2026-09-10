package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsMekanism.ID)
@PrefixGameTestTemplate(false)
public final class FeSourcelinkGameTests {
    private static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }
    private static FeSourcelinkBlockEntity place(GameTestHelper h, BlockPos relative) {
        h.setBlock(relative, Content.FE_SOURCELINK.get());
        return (FeSourcelinkBlockEntity) h.getBlockEntity(relative);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void verticalJarsStopWhenMissingFullOrUnpoweredAndChargeExactFE(GameTestHelper h) {
        BlockPos relative = new BlockPos(5, 2, 5);
        var tile = place(h, relative);
        try {
            var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, tile.getBlockPos(), Direction.WEST);
            check(energy != null && energy.canReceive() && !energy.canExtract(), "FE input capability missing");
            check(energy.receiveEnergy(9_000, true) == 9_000 && tile.storedFE() == 0, "FE simulation changed the buffer");
            h.setBlock(relative.east(), BlockRegistry.SOURCE_JAR.defaultBlockState());
            energy.receiveEnergy(20_000, false); tile.tick();
            check(tile.status() == FeSourcelinkBlockEntity.NO_JAR && tile.storedFE() == 20_000, "Horizontal jar was filled or missing jar consumed FE");
            h.setBlock(relative.above(), BlockRegistry.SOURCE_JAR.defaultBlockState());
            var upper = h.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, tile.getBlockPos().above(), Direction.DOWN);
            check(upper != null, "Upper jar capability missing");
            upper.setSource(upper.getMaxSource() - 3);
            check(upper.receiveSource(20, true) == 3 && upper.getSource() == upper.getMaxSource() - 3, "Native jar simulation changed Source");
            int before = tile.storedFE(); tile.tick();
            int partialCost = (int) Math.ceil(MachineConfig.GENERATOR_FE.get() * 3D / MachineConfig.GENERATOR_RATE.get());
            check(upper.getSource() == upper.getMaxSource() && before - tile.storedFE() == partialCost, "Partial jar fill duplicated Source or charged wrong FE");
            before = tile.storedFE(); tile.tick();
            check(tile.status() == FeSourcelinkBlockEntity.FULL && tile.storedFE() == before, "Full jar consumed FE");
            h.setBlock(relative.below(), BlockRegistry.SOURCE_JAR.defaultBlockState());
            var lower = h.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, tile.getBlockPos().below(), Direction.UP);
            check(lower != null, "Lower jar capability missing");
            tile.tick();
            check(lower.getSource() == MachineConfig.GENERATOR_RATE.get(), "Full upper jar did not fall back to the lower jar");
            upper.setSource(0); before = lower.getSource(); tile.tick();
            check(upper.getSource() == MachineConfig.GENERATOR_RATE.get() && lower.getSource() == before, "Upper jar priority or per-tick output incorrect");
            h.setBlock(relative.above(), Blocks.AIR); before = lower.getSource(); tile.tick();
            check(lower.getSource() == before + MachineConfig.GENERATOR_RATE.get(), "Jar below the channel did not work");
            tile.loadAdditional(new CompoundTag(), h.getLevel().registryAccess());
            before = lower.getSource(); tile.tick();
            check(tile.status() == FeSourcelinkBlockEntity.NEED_ENERGY && lower.getSource() == before, "Unpowered channel generated Source");
            energy.receiveEnergy(MachineConfig.GENERATOR_FE.get() - 1, false); tile.tick();
            check(lower.getSource() == before, "Insufficient energy produced a full operation");
            h.succeed();
        } finally { h.setBlock(relative, Blocks.AIR); }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void energySurvivesWorldAndItemRoundTripsAndRemovedHandlersStop(GameTestHelper h) {
        BlockPos relative = new BlockPos(5, 2, 5);
        var original = place(h, relative);
        try {
            var cached = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, original.getBlockPos(), Direction.NORTH);
            check(cached != null, "FE capability missing");
            check(cached.receiveEnergy(Integer.MAX_VALUE, true) == FeSourcelinkBlockEntity.ENERGY_CAPACITY && original.storedFE() == 0, "Capacity simulation overflowed or mutated FE");
            cached.receiveEnergy(123_456, false);
            var restored = new FeSourcelinkBlockEntity(original.getBlockPos(), original.getBlockState());
            restored.loadAdditional(original.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            check(restored.storedFE() == 123_456, "World save lost FE");
            ItemStack drop = Block.getDrops(original.getBlockState(), h.getLevel(), original.getBlockPos(), original, null, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst();
            drop = ItemStack.parse(h.getLevel().registryAccess(), drop.save(h.getLevel().registryAccess())).orElseThrow();
            check(drop.is(Content.FE_SOURCELINK_ITEM) && drop.getOrDefault(Content.FE_ENERGY, 0) == 123_456, "Dropped item lost stored FE");
            h.setBlock(relative, Blocks.AIR);
            check(cached.receiveEnergy(1_000, false) == 0 && !cached.canReceive() && cached.getEnergyStored() == 0, "Removed block still accepted power through cached handler");
            var placed = place(h, relative); placed.applyComponentsFromItemStack(drop);
            check(placed.storedFE() == 123_456, "Replacing the channel lost dropped FE");
            check(cached.receiveEnergy(1_000, false) == 0 && placed.storedFE() == 123_456, "Old handler charged the replacement block");
            var invalid = new CompoundTag(); invalid.putInt("fe_energy", -1); invalid.putInt("fe_status", Integer.MAX_VALUE);
            placed.loadAdditional(invalid, h.getLevel().registryAccess());
            check(placed.storedFE() == 0 && placed.status() == FeSourcelinkBlockEntity.RUNNING, "Invalid saved values were not clamped");
            drop.set(Content.FE_ENERGY, Integer.MAX_VALUE); placed.applyComponentsFromItemStack(drop);
            check(placed.storedFE() == FeSourcelinkBlockEntity.ENERGY_CAPACITY, "Item energy overflow was not clamped");
            h.succeed();
        } finally { h.setBlock(relative, Blocks.AIR); }
    }

    @GameTest(template = "empty", batch = "ars_fe_logistics", timeoutTicks = 140)
    public static void realMekCablePowersVerticalJarForNativeArsConsumers(GameTestHelper h) {
        BlockPos relative = new BlockPos(5, 1, 5);
        var tile = place(h, relative);
        h.setBlock(relative.above(), BlockRegistry.SOURCE_JAR.defaultBlockState());
        h.setBlock(new BlockPos(2, 1, 5), MekanismBlocks.BASIC_ENERGY_CUBE.get());
        var cube = (TileEntityEnergyCube) h.getBlockEntity(new BlockPos(2, 1, 5));
        cube.getEnergyContainers(null).getFirst().setEnergy(1_000_000);
        cube.getConfig().getConfig(TransmissionType.ENERGY).setDataType(DataType.OUTPUT, RelativeSide.fromDirections(cube.getDirection(), Direction.EAST));
        cube.getConfig().getConfig(TransmissionType.ENERGY).setEjecting(true);
        h.setBlock(new BlockPos(3, 1, 5), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        h.setBlock(new BlockPos(4, 1, 5), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        h.runAfterDelay(110, () -> {
            try {
                var jar = h.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, tile.getBlockPos().above(), Direction.DOWN);
                check(jar != null && jar.getSource() >= 200 && tile.storedFE() > 0, "Real cable failed to power the channel and fill its upper jar");
                check(cube.getEnergyContainers(null).getFirst().getEnergy() < 1_000_000, "Energy cube did not supply FE");
                int before = jar.getSource();
                check(SourceUtil.takeSourceMultiple(tile.getBlockPos().above(), h.getLevel(), 0, 200) != null && jar.getSource() == before - 200,
                      "Native Ars consumer could not use the generated Source");
                h.succeed();
            } finally { cube.getEnergyContainers(null).getFirst().setEnergy(0); h.setBlock(relative, Blocks.AIR); }
        });
    }
}
