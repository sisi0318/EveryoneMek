package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AuraMachine extends TileEntityConfigurableMachine {
    public static final long AURA_CAPACITY = 1_000_000;
    public List<BasicInventorySlot> inputs;
    private List<OutputInventorySlot> outputs;
    private EnergyInventorySlot energySlot;
    private GoldModuleSlot goldModuleSlot;
    private MachineEnergyContainer<AuraMachine> energy;
    private IChemicalTank auraTank;
    private int progress, duration = 20, batch;
    private long paidAura;
    private CompoundTag workSignature;
    private boolean environmentOutput;
    private int status;
    private int environmentAura;
    private int environmentRefreshTicks;

    public AuraMachine(BlockPos pos, BlockState state) {
        super(Content.MACHINES.get(((MachineBlock) state.getBlock()).kind), pos, state);
        List<BasicInventorySlot> recipeInputs = kind() == MachineKind.FOREST_RITUAL ? inputs.subList(0, 8) : inputs;
        var item = configComponent.setupItemIOConfig(new ArrayList<IInventorySlot>(recipeInputs),
              new ArrayList<IInventorySlot>(outputs), energySlot, false);
        for (RelativeSide side : RelativeSide.values()) item.setDataType(DataType.INPUT, side);
        if (kind() == MachineKind.FOREST_RITUAL) {
            item.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, false, inputs.get(8), inputs.get(9)));
            item.setDataType(DataType.EXTRA, RelativeSide.BACK);
        }
        item.setDataType(DataType.OUTPUT, RelativeSide.RIGHT);
        item.setDataType(DataType.ENERGY, RelativeSide.BOTTOM);
        item.setEjecting(true);
        var power = configComponent.setupInputConfig(TransmissionType.ENERGY, energy);
        for (RelativeSide side : RelativeSide.values()) power.setDataType(DataType.INPUT, side);
        if (auraTank != null) {
            var chemical = kind() == MachineKind.AURA_GENERATOR
                  ? configComponent.setupOutputConfig(TransmissionType.CHEMICAL, auraTank)
                  : configComponent.setupInputConfig(TransmissionType.CHEMICAL, auraTank);
            for (RelativeSide side : RelativeSide.values()) chemical.setDataType(
                  kind() == MachineKind.AURA_GENERATOR ? DataType.OUTPUT : DataType.INPUT, side);
            chemical.setEjecting(kind() == MachineKind.AURA_GENERATOR);
        }
        ejectorComponent = new TileComponentEjector(this);
        if (kind() == MachineKind.AURA_GENERATOR)
            ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL);
        else ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    public MachineKind kind() { return ((MachineBlock) getBlockState().getBlock()).kind; }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        var builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energy = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        if (kind() != MachineKind.AURA_GENERATOR && kind() != MachineKind.NATURAL_ALTAR) return null;
        var builder = ChemicalTankHelper.forSideWithConfig(this);
        auraTank = kind() == MachineKind.AURA_GENERATOR
              ? BasicChemicalTank.output(AURA_CAPACITY, listener)
              : BasicChemicalTank.inputModern(AURA_CAPACITY, stack -> stack.is(Content.AURA), listener);
        builder.addTank(auraTank);
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        var builder = InventorySlotHelper.forSideWithConfig(this);
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        int count = kind() == MachineKind.FOREST_RITUAL ? 10 : kind() == MachineKind.AURA_GENERATOR ? 0 : 2;
        for (int i = 0; i < count; i++) {
            final int slot = i;
            int x = kind() == MachineKind.FOREST_RITUAL ? (i < 8 ? 18 + i % 4 * 18 : 104) : 42 + i * 40;
            int y = kind() == MachineKind.FOREST_RITUAL ? (i < 8 ? 30 + i / 4 * 18 : 30 + (i - 8) * 22) : 40;
            var input = BasicInventorySlot.at((stack, automation) -> automation != AutomationType.EXTERNAL,
                  (stack, automation) -> true, stack -> RecipeAdapter.accepts(getLevel(), kind(), slot, stack), listener, x, y);
            inputs.add(input);
            builder.addSlot(input);
        }
        if (count > 0) for (int i = 0; i < 4; i++) {
            var output = OutputInventorySlot.at(listener, 160 + i % 2 * 18, 30 + i / 2 * 18);
            outputs.add(output);
            builder.addSlot(output);
        }
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energy, this::getLevel, listener, 206, 78));
        if (kind() == MachineKind.FOREST_RITUAL) {
            // Append after all existing slots so 0.1.0 saves and dropped machine inventories keep their indices.
            builder.addSlot(goldModuleSlot = new GoldModuleSlot(() -> {
                listener.onContentsChanged();
                refreshModuleEnergy();
            }));
        }
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean update = super.onUpdateServer();
        refreshModuleEnergy();
        energySlot.fillContainerOrConvert();
        setActive(false);
        if (kind() == MachineKind.NATURAL_ALTAR && --environmentRefreshTicks <= 0) {
            environmentAura = readEnvironmentAura();
            environmentRefreshTicks = 10;
        }
        if (!canFunction()) { status = 1; return update; }
        if (kind() == MachineKind.OFFERING && !OfferingFlowers.complete(level, worldPosition)) {
            status = 2;
            return update;
        }
        if (kind() == MachineKind.AURA_GENERATOR) {
            if (environmentOutput) emitAura();
            tickGenerator();
        } else {
            tickRecipe();
        }
        return update;
    }

    private void tickGenerator() {
        duration = Math.max(1, MekanismUtils.getTicks(this, 20));
        long nominal = (long) MachineConfig.AURA_PER_CYCLE.get() * MekanismUtils.getOperationsPerTick(this, 20, 1);
        long generated = Math.min(nominal, auraTank.getNeeded());
        if (generated <= 0) { status = 3; return; }
        if (!payEnergy()) return;
        setActive(true);
        status = 0;
        if (++progress >= duration) {
            auraTank.insert(new ChemicalStack(Content.AURA, generated), Action.EXECUTE, AutomationType.INTERNAL);
            progress = 0;
        }
        markForSave();
    }

    private void emitAura() {
        if (auraTank.isEmpty()) return;
        // Avoid loading chunks just to spread aura.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            if (!level.hasChunkAt(worldPosition.offset(x * 16, 0, z * 16))) return;
        int inArea = IAuraChunk.getAuraInArea(level, worldPosition, 16);
        long room = (long) MachineConfig.EMIT_LIMIT.get() - inArea;
        int amount = (int) Math.min(Math.min(auraTank.getStored(), MachineConfig.EMIT_RATE.get()), Math.max(0L, room));
        if (amount == 0) return;
        BlockPos spot = IAuraChunk.getLowestSpot(level, worldPosition, 16, worldPosition);
        int stored = IAuraChunk.getAuraChunk(level, spot).storeAura(spot, amount, false, false);
        auraTank.extract(stored, Action.EXECUTE, AutomationType.INTERNAL);
    }

    private void tickRecipe() {
        RecipeAdapter.Plan plan = RecipeAdapter.find(this, batch);
        if (plan == null && batch > 0) { resetWork(); plan = RecipeAdapter.find(this, 0); }
        if (plan == null) { resetWork(); status = 4; return; }
        if (!plan.signature().equals(workSignature)) {
            resetWork();
            workSignature = plan.signature();
            duration = Math.max(1, MekanismUtils.getTicks(this, plan.ticks()));
            batch = plan.batch();
        }
        if (!canFit(plan.outputs(), true)) { status = 3; return; }
        int next = Math.min(progress + 1, duration);
        long totalDue = IngredientAssignment.due(plan.aura(), next, duration);
        long toPay = Math.max(0, totalDue - paidAura);
        long fromTank = auraTank == null ? 0 : Math.min(toPay, auraTank.getStored());
        long fromEnvironment = toPay - fromTank;
        EnvironmentDraw draw = fromEnvironment > 0 ? prepareEnvironmentDraw(fromEnvironment) : null;
        if (fromEnvironment > 0 && draw == null) { status = 5; return; }
        if (!payEnergy()) return;
        // Both sources have been checked before spending energy or modifying either source.
        // NaturesAura's aimForZero=false contract drains the exact requested amount.
        if (draw != null) {
            draw.chunk().drainAura(draw.pos(), draw.amount(), false, false);
            environmentAura = Math.max(0, environmentAura - draw.amount());
        }
        if (fromTank > 0) auraTank.extract(fromTank, Action.EXECUTE, AutomationType.INTERNAL);
        paidAura += toPay;
        progress = next;
        status = 0;
        setActive(true);
        if (progress >= duration) {
            List<ItemStack> result = new ArrayList<>(plan.outputs());
            if (kind() == MachineKind.OFFERING && OfferingFlowers.allWitherRoses(level, worldPosition))
                result.add(new ItemStack(Items.BLACK_DYE, 4 + level.random.nextInt(5)));
            List<ItemStack> merged = mergeOutputs(result);
            if (merged == null) throw new IllegalStateException("Reserved output space changed within the server tick");
            for (int i = 0; i < plan.consume().length; i++) inputs.get(i).shrinkStack(plan.consume()[i], Action.EXECUTE);
            for (int i = 0; i < outputs.size(); i++) outputs.get(i).setStackUnchecked(merged.get(i));
            resetWork();
        }
        markForSave();
    }

    private record EnvironmentDraw(IAuraChunk chunk, BlockPos pos, int amount) { }

    private int readEnvironmentAura() {
        // The API queries NaturesAura's indexed chunks; it does not request neighbouring chunks to load.
        return Math.max(0, IAuraChunk.getAuraInArea(level, worldPosition, MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get()));
    }

    private EnvironmentDraw prepareEnvironmentDraw(long amount) {
        if (kind() != MachineKind.NATURAL_ALTAR || amount > Integer.MAX_VALUE) return null;
        environmentAura = readEnvironmentAura();
        if (environmentAura < amount) return null;
        BlockPos spot = IAuraChunk.getHighestSpot(level, worldPosition, MachineConfig.ALTAR_ENVIRONMENT_RADIUS.get(), worldPosition);
        if (!level.hasChunkAt(spot)) return null;
        IAuraChunk chunk = IAuraChunk.getAuraChunk(level, spot);
        return chunk.drainAura(spot, (int) amount, false, true) == amount ? new EnvironmentDraw(chunk, spot, (int) amount) : null;
    }

    private boolean payEnergy() {
        long required = energy.getEnergyPerTick();
        if (energy.extract(required, Action.SIMULATE, AutomationType.INTERNAL) != required) { status = 6; return false; }
        energy.extract(required, Action.EXECUTE, AutomationType.INTERNAL);
        return true;
    }

    public boolean canFit(List<ItemStack> stacks, boolean reserveBonus) {
        List<ItemStack> result = new ArrayList<>(stacks);
        if (reserveBonus && kind() == MachineKind.OFFERING && OfferingFlowers.allWitherRoses(level, worldPosition))
            result.add(new ItemStack(Items.BLACK_DYE, 8));
        return mergeOutputs(result) != null;
    }

    private List<ItemStack> mergeOutputs(List<ItemStack> stacks) {
        List<ItemStack> merged = new ArrayList<>();
        for (var slot : outputs) merged.add(slot.getStack().copy());
        for (ItemStack produced : stacks) {
            int remaining = produced.getCount();
            // Fill compatible stacks before occupying empty slots.
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < merged.size() && remaining > 0; i++) {
                ItemStack current = merged.get(i);
                if (pass == 0 ? current.isEmpty() : !current.isEmpty()) continue;
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, produced)) continue;
                int move = Math.min(remaining, produced.getMaxStackSize() - current.getCount());
                if (move <= 0) continue;
                merged.set(i, produced.copyWithCount(current.getCount() + move));
                remaining -= move;
            }
            if (remaining > 0) return null;
        }
        return merged;
    }

    private void resetWork() { progress = 0; paidAura = 0; batch = 0; workSignature = null; }

    public void toggleEnvironmentOutput() { environmentOutput = !environmentOutput; markForSave(); }
    public boolean environmentOutput() { return environmentOutput; }
    public double progress() { return progress / (double) Math.max(1, duration); }
    public int status() { return status; }
    public int environmentAura() { return environmentAura; }
    public IChemicalTank auraTank() { return auraTank; }
    public MachineEnergyContainer<AuraMachine> energy() { return energy; }
    public GoldModuleSlot goldModuleSlot() { return goldModuleSlot; }
    public boolean hasInfiniteGold() { return goldModuleSlot != null && goldModuleSlot.getStack().is(Content.INFINITE_GOLD_MODULE); }

    private void refreshModuleEnergy() {
        if (kind() != MachineKind.FOREST_RITUAL || energy == null || getComponent() == null || level != null && level.isClientSide) return;
        long normal = MekanismUtils.getEnergyPerTick(this, energy.getBaseEnergyPerTick());
        energy.setEnergyPerTick(hasInfiniteGold() ? MathUtils.multiplyClamped(normal, MachineConfig.INFINITE_GOLD_POWER_MULTIPLIER.get()) : normal);
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        refreshModuleEnergy();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(Content.ENVIRONMENT_OUTPUT, environmentOutput);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        environmentOutput = Boolean.TRUE.equals(input.get(Content.ENVIRONMENT_OUTPUT));
    }

    @Override
    public List<Component> getInfo(Upgrade upgrade) { return UpgradeUtils.getMultScaledInfo(this, upgrade); }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> progress, v -> progress = v));
        container.track(SyncableInt.create(() -> duration, v -> duration = v));
        container.track(SyncableInt.create(() -> status, v -> status = v));
        if (kind() == MachineKind.NATURAL_ALTAR)
            container.track(SyncableInt.create(() -> environmentAura, v -> environmentAura = v));
        container.track(SyncableBoolean.create(() -> environmentOutput, v -> environmentOutput = v));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("work_progress", progress);
        tag.putInt("work_duration", duration);
        tag.putInt("work_batch", batch);
        tag.putLong("work_aura", paidAura);
        tag.putBoolean("environment_output", environmentOutput);
        if (workSignature != null) tag.put("work_signature", workSignature);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        duration = Math.clamp(tag.getInt("work_duration"), 1, 1_000_000);
        progress = Math.clamp(tag.getInt("work_progress"), 0, duration);
        batch = Math.clamp(tag.getInt("work_batch"), 0, 16);
        paidAura = Math.max(0, tag.getLong("work_aura"));
        environmentOutput = tag.getBoolean("environment_output");
        workSignature = tag.contains("work_signature") ? tag.getCompound("work_signature") : null;
    }
}
