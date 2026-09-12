package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.*;
import mekanism.api.chemical.*;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.*;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.*;
import mekanism.common.inventory.slot.*;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ManaMachine extends TileEntityConfigurableMachine {
    public static final int MANA_CAPACITY = 1_000_000;
    public static final int WORKING = 0, NO_RECIPE = 1, NO_EXTRA = 2, NO_MANA = 3, NO_ENERGY = 4, OUTPUT_FULL = 5,
          REDSTONE = 6, STRUCTURE = 7, NO_POOL = 8, ITEM_DENIED = 9, READY = 10, UNLOADED = 11, BUSY = 12, NEEDS_CEILING = 13;
    // These containers are constructed during the superclass constructor.
    public List<BasicInventorySlot> inputs, extras;
    public List<OutputInventorySlot> outputs;
    private EnergyInventorySlot energySlot;
    private MachineEnergyContainer<ManaMachine> energy;
    private IChemicalTank mana;
    private int progress, duration = 1, status = NO_RECIPE;
    private CompoundTag signature;
    private int mode, poolSide, targetPercent = 100;
    private String recipeLock = "";
    private boolean readingSettings, reservingSide;
    // Controller bookkeeping holds identifiers only; native devices own their in-flight resources.
    CompoundTag controller = new CompoundTag();

    public ManaMachine(BlockPos pos, BlockState state) {
        super(ManaContent.MACHINES.get(((ManaMachineBlock) state.getBlock()).kind), pos, state);
        var item = configComponent.setupItemIOConfig(new ArrayList<IInventorySlot>(inputs), new ArrayList<IInventorySlot>(outputs), energySlot, false);
        for (RelativeSide side : RelativeSide.values()) item.setDataType(DataType.INPUT, side);
        if (!extras.isEmpty()) {
            item.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, false, new ArrayList<IInventorySlot>(extras)));
            item.setDataType(DataType.EXTRA, RelativeSide.BACK);
        }
        item.setDataType(DataType.OUTPUT, RelativeSide.RIGHT); item.setDataType(DataType.ENERGY, RelativeSide.BOTTOM); item.setEjecting(true);
        var power = configComponent.setupInputConfig(TransmissionType.ENERGY, energy);
        for (RelativeSide side : RelativeSide.values()) power.setDataType(DataType.INPUT, side);
        if (kind().chemical) {
            var chemical = kind() == ManaMachineKind.BRIDGE ? configComponent.setupIOConfig(TransmissionType.CHEMICAL, mana, RelativeSide.RIGHT)
                  : configComponent.setupInputConfig(TransmissionType.CHEMICAL, mana);
            for (RelativeSide side : RelativeSide.values()) chemical.setDataType(kind() == ManaMachineKind.BRIDGE ? DataType.OUTPUT : DataType.INPUT, side);
            chemical.setEjecting(kind() == ManaMachineKind.BRIDGE);
        }
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM, TransmissionType.CHEMICAL);
        if (kind() == ManaMachineKind.BRIDGE) {
            reservePoolSide();
            configComponent.addConfigChangeListener(TransmissionType.CHEMICAL, side -> {
                if (!readingSettings && !reservingSide && side == poolDirection()) reservePoolSide();
            });
        }
    }
    public ManaMachineKind kind() { return ((ManaMachineBlock) getBlockState().getBlock()).kind; }
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        var builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energy = MachineEnergyContainer.input(this, listener)); return builder.build();
    }
    @Override public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        var builder = ChemicalTankHelper.forSideWithConfig(this);
        mana = kind() == ManaMachineKind.BRIDGE ? BasicChemicalTank.createModern(MANA_CAPACITY, stack -> stack.is(ManaContent.MANA), listener)
              : BasicChemicalTank.inputModern(kind().chemical ? MANA_CAPACITY : 0, stack -> stack.is(ManaContent.MANA), listener);
        if (kind().chemical) builder.addTank(mana); return builder.build();
    }
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        var builder = InventorySlotHelper.forSideWithConfig(this);
        inputs = new ArrayList<>(); extras = new ArrayList<>(); outputs = new ArrayList<>();
        for (int i = 0; i < kind().inputs; i++) {
            var slot = BasicInventorySlot.at((stack, automation) -> automation != AutomationType.EXTERNAL, (stack, automation) -> true,
                  stack -> ManaWork.accepts(kind(), getLevel(), stack, false), listener,
                  kind() == ManaMachineKind.ENCHANTER ? 106 : 16 + i % 4 * 18, kind() == ManaMachineKind.ENCHANTER ? 50 : 32 + i / 4 * 18);
            inputs.add(slot); builder.addSlot(slot);
        }
        for (int i = 0; i < kind().extras; i++) {
            var slot = BasicInventorySlot.at((stack, automation) -> automation != AutomationType.EXTERNAL, (stack, automation) -> true,
                  stack -> ManaWork.accepts(kind(), getLevel(), stack, true), listener,
                  kind().extras == 1 ? 106 : 16 + i % 4 * 18, kind().extras == 1 ? 86 : 32 + i / 4 * 18);
            extras.add(slot); builder.addSlot(slot);
        }
        for (int i = 0; i < kind().outputs; i++) {
            var slot = OutputInventorySlot.at(listener, 152 + i % 3 * 18, 32 + i / 3 * 18); outputs.add(slot); builder.addSlot(slot);
        }
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energy, this::getLevel, listener, 206, 86)); return builder.build();
    }
    @Override protected boolean onUpdateServer() {
        boolean update = super.onUpdateServer(); energySlot.fillContainerOrConvert(); setActive(false);
        SparkExpansion.supplyMachine(this);
        if (!canFunction()) { status = REDSTONE; return update; }
        if (kind() == ManaMachineKind.BRIDGE || kind() == ManaMachineKind.CHARGER) { ManaTransfer.tick(this); return update; }
        if (kind().controller()) { NativeControllers.tick(this); return update; }
        if (kind() == ManaMachineKind.TERRA && !ManaWork.platform(this)) { status = STRUCTURE; return update; }
        if (kind() == ManaMachineKind.ORE && extras.getFirst().getStack().is(vazkii.botania.common.block.BotaniaBlocks.ORECHID_IGNEM.asItem())
              && !level.dimensionType().hasCeiling()) { status = NEEDS_CEILING; return update; }
        ManaWork.Plan plan = ManaWork.find(this);
        if (plan == null) {
            resetWork(); status = !extras.isEmpty() && kind() != ManaMachineKind.INFUSER && extras.getFirst().isEmpty()
                  && inputs.stream().anyMatch(slot -> !slot.isEmpty()) ? NO_EXTRA : NO_RECIPE; return update;
        }
        int ticks = Math.max(1, MekanismUtils.getTicks(this, plan.ticks()));
        long cost = energy.getEnergyPerTick();
        CompoundTag current = plan.signature(this); current.putLong("energy_per_tick", cost);
        if (!current.equals(signature) || duration != ticks) { resetWork(); signature = current; duration = ticks; }
        if (!plan.extraValid()) { status = NO_EXTRA; return update; }
        if (!plan.canOutput(this)) { status = OUTPUT_FULL; return update; }
        if (mana.getStored() < plan.maxMana()) { status = NO_MANA; return update; }
        if (!spendEnergy(cost)) { status = NO_ENERGY; return update; }
        status = WORKING; setActive(true);
        if (++progress >= duration) { plan.commit(this); resetWork(); }
        markForSave(); return update;
    }
    private void resetWork() { if (progress != 0 || signature != null) markForSave(); progress = 0; signature = null; }
    public boolean spendEnergy(long amount) {
        if (energy.extract(amount, Action.SIMULATE, AutomationType.INTERNAL) != amount) return false;
        energy.extract(amount, Action.EXECUTE, AutomationType.INTERNAL); return true;
    }
    public List<ItemStack> mergeOutputs(List<ItemStack> products) {
        var merged = outputs.stream().map(slot -> slot.getStack().copy()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (ItemStack product : products) {
            if (product.isEmpty()) continue;
            int remaining = product.getCount();
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < merged.size() && remaining > 0; i++) {
                ItemStack current = merged.get(i);
                if (pass == 0 ? current.isEmpty() : !current.isEmpty()) continue;
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, product)) continue;
                int amount = Math.min(remaining, Math.min(outputs.get(i).getLimit(product), product.getMaxStackSize()) - current.getCount());
                if (amount > 0) { merged.set(i, product.copyWithCount(current.getCount() + amount)); remaining -= amount; }
            }
            if (remaining > 0) return null;
        }
        return merged;
    }
    public void setOutputs(List<ItemStack> merged) { for (int i = 0; i < outputs.size(); i++) outputs.get(i).setStackUnchecked(merged.get(i)); }
    public MachineEnergyContainer<ManaMachine> energy() { return energy; }
    public IChemicalTank mana() { return mana; }
    public int status() { return status; }
    public void status(int value) { status = value; setActive(value == WORKING); }
    public int progressTicks() { return progress; }
    public double progress() { return (double) progress / duration; }
    public int mode() { return mode; }
    public int poolSide() { return poolSide; }
    public int targetPercent() { return targetPercent; }
    public String recipeLock() { return recipeLock; }
    public Direction poolDirection() { return RelativeSide.values()[poolSide].getDirection(getDirection()); }
    public BlockPos targetPos() { return getBlockPos().relative(poolDirection()); }
    public boolean applySetting(int action, String value) {
        try {
            if (action == 0 && (kind() == ManaMachineKind.BRIDGE || kind() == ManaMachineKind.CHARGER)) {
                int next = Integer.parseInt(value); if (next < 0 || next > 1) return false;
                if (next != mode && kind() == ManaMachineKind.CHARGER) targetPercent = next == 0 ? 100 : 0;
                mode = next;
                if (kind() == ManaMachineKind.BRIDGE) {
                    var chemical = configComponent.getConfig(TransmissionType.CHEMICAL);
                    for (var side : RelativeSide.values()) chemical.setDataType(mode == 0 ? DataType.OUTPUT : DataType.INPUT, side);
                }
                reservePoolSide();
            } else if (action == 1) {
                int next = Integer.parseInt(value); if (next < 0 || next >= RelativeSide.values().length) return false;
                int previous = poolSide;
                poolSide = next; reservePoolSide();
                if (kind() == ManaMachineKind.BRIDGE && previous != next) configComponent.getConfig(TransmissionType.CHEMICAL)
                      .setDataType(mode == 0 ? DataType.OUTPUT : DataType.INPUT, RelativeSide.values()[previous]);
            } else if (action == 2 && kind() == ManaMachineKind.CHARGER) {
                int next = Integer.parseInt(value); if (next < 0 || next > 100) return false; targetPercent = next;
            } else if (action == 3 && !kind().random() && !kind().controller()) {
                if (!value.isEmpty() && ManaWork.choices(this).stream().noneMatch(choice -> choice.id().toString().equals(value))) return false;
                recipeLock = value;
            } else return false;
            resetWork(); markForSave(); return true;
        } catch (IllegalArgumentException ignored) { return false; }
    }
    private void reservePoolSide() {
        if (kind() != ManaMachineKind.BRIDGE || configComponent == null || reservingSide) return;
        reservingSide = true;
        try { configComponent.getConfig(TransmissionType.CHEMICAL).setDataType(DataType.NONE, RelativeSide.values()[poolSide]); }
        finally { reservingSide = false; }
    }
    private CompoundTag settings() {
        var tag = new CompoundTag(); tag.putInt("mode", mode); tag.putInt("pool_side", poolSide); tag.putInt("target", targetPercent);
        tag.putString("recipe", recipeLock); tag.put("controller", controller.copy()); return tag;
    }
    private void readSettings(CompoundTag tag) {
        mode = Math.clamp(tag.getInt("mode"), 0, 1); poolSide = Math.clamp(tag.getInt("pool_side"), 0, RelativeSide.values().length - 1);
        targetPercent = tag.contains("target") ? Math.clamp(tag.getInt("target"), 0, 100) : 100;
        String id = tag.getString("recipe"); recipeLock = id.length() <= 256 && ResourceLocation.tryParse(id) != null ? id : "";
        controller = tag.getCompound("controller").copy(); reservePoolSide();
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) { super.collectImplicitComponents(builder); builder.set(ManaContent.SETTINGS, settings()); }
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        readingSettings = true; try { super.applyImplicitComponents(input); } finally { readingSettings = false; }
        var settings = input.get(ManaContent.SETTINGS); if (settings != null) readSettings(settings);
    }
    @Override public List<Component> getInfo(Upgrade upgrade) { return UpgradeUtils.getMultScaledInfo(this, upgrade); }
    @Override public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> progress, v -> progress = v)); container.track(SyncableInt.create(() -> duration, v -> duration = v));
        container.track(SyncableInt.create(() -> status, v -> status = v)); container.track(SyncableInt.create(() -> mode, v -> mode = v));
        container.track(SyncableInt.create(() -> poolSide, v -> poolSide = v)); container.track(SyncableInt.create(() -> targetPercent, v -> targetPercent = v));
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); tag.put("machine_settings", settings()); tag.putInt("progress", progress); tag.putInt("duration", duration);
        if (signature != null) tag.put("work", signature);
    }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        readingSettings = true; try { super.loadAdditional(tag, provider); } finally { readingSettings = false; }
        readSettings(tag.getCompound("machine_settings")); duration = Math.clamp(tag.getInt("duration"), 1, 2_000_000);
        progress = Math.clamp(tag.getInt("progress"), 0, duration - 1); signature = tag.contains("work") ? tag.getCompound("work") : null;
    }
}
