package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.*;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.capabilities.holder.fluid.*;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.*;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class MechanicalApothecary extends TileEntityConfigurableMachine {
    public static final int WATER_CAPACITY = 16000, WATER_PER_CRAFT = 1000;
    public static final int WORKING = 0, NO_RECIPE = 1, NO_REAGENT = 2, NO_WATER = 3, NO_ENERGY = 4, OUTPUT_FULL = 5, REDSTONE = 6;
    // Created by superclass callbacks: never overwrite these with subclass field initializers.
    public List<BasicInventorySlot> inputs;
    public BasicInventorySlot reagent;
    public List<OutputInventorySlot> outputs;
    private EnergyInventorySlot energySlot;
    public FluidInventorySlot waterInput;
    public OutputInventorySlot bucketOutput;
    private MachineEnergyContainer<MechanicalApothecary> energy;
    private IExtendedFluidTank water;
    private int progress, duration = 1, status = NO_RECIPE;
    private CompoundTag signature;
    net.minecraft.resources.ResourceLocation lastRecipe;

    public MechanicalApothecary(BlockPos pos, BlockState state) {
        super(ApothecaryContent.BLOCK, pos, state);
        var inputSlots = new ArrayList<IInventorySlot>(); inputSlots.add(waterInput); inputSlots.addAll(inputs);
        var outputSlots = new ArrayList<IInventorySlot>(outputs); outputSlots.add(bucketOutput);
        var items = configComponent.setupItemIOConfig(inputSlots, outputSlots, energySlot, false);
        items.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, false, reagent));
        for (RelativeSide side : RelativeSide.values()) items.setDataType(DataType.INPUT, side);
        items.setDataType(DataType.EXTRA, RelativeSide.BACK);
        items.setDataType(DataType.OUTPUT, RelativeSide.RIGHT);
        items.setDataType(DataType.ENERGY, RelativeSide.BOTTOM); items.setEjecting(true);
        var power = configComponent.setupInputConfig(TransmissionType.ENERGY, energy);
        var fluid = configComponent.setupInputConfig(TransmissionType.FLUID, water);
        for (RelativeSide side : RelativeSide.values()) { power.setDataType(DataType.INPUT, side); fluid.setDataType(DataType.INPUT, side); }
        ejectorComponent = new TileComponentEjector(this); ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        var builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energy = MachineEnergyContainer.input(this, listener)); return builder.build();
    }
    @Override public IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        var builder = FluidTankHelper.forSideWithConfig(this);
        builder.addTank(water = BasicFluidTank.input(WATER_CAPACITY, stack -> stack.is(Fluids.WATER), listener)); return builder.build();
    }
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        var builder = InventorySlotHelper.forSideWithConfig(this); inputs = new ArrayList<>(); outputs = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            var slot = BasicInventorySlot.at((stack, automation) -> automation != AutomationType.EXTERNAL,
                  (stack, automation) -> true, stack -> ApothecaryWork.acceptsMaterial(getLevel(), stack), listener, 16 + i % 4 * 18, 32 + i / 4 * 18);
            inputs.add(slot); builder.addSlot(slot);
        }
        builder.addSlot(reagent = BasicInventorySlot.at((stack, automation) -> automation != AutomationType.EXTERNAL,
              (stack, automation) -> true, stack -> ApothecaryWork.acceptsReagent(getLevel(), stack), listener, 106, 86));
        for (int i = 0; i < 6; i++) {
            var slot = OutputInventorySlot.at(listener, 152 + i % 3 * 18, 35 + i / 3 * 18); outputs.add(slot); builder.addSlot(slot);
        }
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energy, this::getLevel, listener, 206, 86));
        // Append after the original 24 slots, so stored items keep their existing indices.
        builder.addSlot(waterInput = FluidInventorySlot.fill(water, listener, 152, 86));
        builder.addSlot(bucketOutput = OutputInventorySlot.at(listener, 180, 86));
        return builder.build();
    }
    @Override protected boolean onUpdateServer() {
        boolean update = super.onUpdateServer(); energySlot.fillContainerOrConvert(); setActive(false);
        waterInput.fillTank(bucketOutput);
        if (!canFunction()) { status = REDSTONE; return update; }
        ApothecaryWork.Plan plan = ApothecaryWork.find(this);
        if (plan == null) { resetWork(); status = NO_RECIPE; return update; }
        int ticks = Math.max(1, MekanismUtils.getTicks(this, plan.option().ticks()));
        long baseCost = mekanism.common.util.UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertFrom(plan.option().fePerTick());
        long cost = Math.max(1, (long) Math.ceil((double) baseCost * energy.getEnergyPerTick() / Math.max(1, energy.getBaseEnergyPerTick())));
        CompoundTag current = plan.signature(); current.putLong("energy_per_tick", cost);
        if (!current.equals(signature) || duration != ticks) { resetWork(); signature = current; duration = ticks; }
        if (!plan.option().reagent().test(reagent.getStack())) { status = NO_REAGENT; return update; }
        List<ItemStack> merged = mergeOutputs(plan.products());
        if (merged == null) { status = OUTPUT_FULL; return update; }
        if (water.getFluidAmount() < WATER_PER_CRAFT) { status = NO_WATER; return update; }
        if (energy.extract(cost, Action.SIMULATE, AutomationType.INTERNAL) != cost) { status = NO_ENERGY; return update; }
        energy.extract(cost, Action.EXECUTE, AutomationType.INTERNAL);
        status = WORKING; setActive(true);
        if (++progress >= duration) {
            water.extract(WATER_PER_CRAFT, Action.EXECUTE, AutomationType.INTERNAL);
            for (int i = 0; i < 16; i++) inputs.get(i).shrinkStack(plan.consume()[i], Action.EXECUTE);
            reagent.shrinkStack(1, Action.EXECUTE);
            for (int i = 0; i < outputs.size(); i++) outputs.get(i).setStackUnchecked(merged.get(i));
            resetWork();
        }
        markForSave(); return update;
    }
    private void resetWork() { if (progress != 0 || signature != null) markForSave(); progress = 0; signature = null; }
    public List<ItemStack> mergeOutputs(List<ItemStack> products) {
        var merged = outputs.stream().map(slot -> slot.getStack().copy()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (ItemStack product : products) {
            int remaining = product.getCount();
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < merged.size() && remaining > 0; i++) {
                ItemStack current = merged.get(i);
                if (pass == 0 ? current.isEmpty() : !current.isEmpty()) continue;
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, product)) continue;
                int amount = Math.min(remaining, Math.min(outputs.get(i).getLimit(product), product.getMaxStackSize()) - current.getCount());
                if (amount <= 0) continue;
                merged.set(i, product.copyWithCount(current.getCount() + amount)); remaining -= amount;
            }
            if (remaining > 0) return null;
        }
        return merged;
    }
    public MachineEnergyContainer<MechanicalApothecary> energy() { return energy; }
    public IExtendedFluidTank water() { return water; }
    public int status() { return status; }
    public int progressTicks() { return progress; }
    public double progress() { return (double) progress / duration; }
    @Override public void applyInventorySlots(net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput input, List<IInventorySlot> slots,
          mekanism.common.attachments.containers.item.AttachedItems attached) {
        if (attached.size() == 24 && slots.size() == 26) {
            var expanded = new ArrayList<>(attached.containers()); expanded.add(ItemStack.EMPTY); expanded.add(ItemStack.EMPTY);
            attached = new mekanism.common.attachments.containers.item.AttachedItems(expanded);
        }
        super.applyInventorySlots(input, slots, attached);
    }
    @Override public List<Component> getInfo(Upgrade upgrade) { return UpgradeUtils.getMultScaledInfo(this, upgrade); }
    @Override public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> progress, v -> progress = v));
        container.track(SyncableInt.create(() -> duration, v -> duration = v));
        container.track(SyncableInt.create(() -> status, v -> status = v));
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); tag.putInt("progress", progress); tag.putInt("duration", duration);
        if (signature != null) tag.put("work", signature);
    }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); duration = Math.clamp(tag.getInt("duration"), 1, 10000);
        progress = Math.clamp(tag.getInt("progress"), 0, duration - 1); signature = tag.contains("work") ? tag.getCompound("work") : null;
    }
}
