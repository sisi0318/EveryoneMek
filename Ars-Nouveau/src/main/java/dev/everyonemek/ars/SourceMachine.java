package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
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
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
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
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import com.hollingsworth.arsnouveau.common.items.ExperienceGem;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SourceMachine extends TileEntityConfigurableMachine {
    public static final long SOURCE_CAPACITY = 1_000_000;
    public static final int RUNNING = 0, REDSTONE = 1, OUTPUT_FULL = 2, MISSING_INPUT = 3, MISSING_MATERIALS = 4,
          NO_RECIPE = 5, NEED_SOURCE = 6, NEED_ENERGY = 7, NO_CONTAINER = 8, TRANSFER_BLOCKED = 9, DISABLED = 10,
          BAD_LOCK = 11, NEED_EXPERIENCE = 12, NEED_BOOK = 13, NO_POTION_JAR = 14, INVALID_POTION = 15,
          NO_TARGET = 16, NO_CREATURE = 17, NO_ENVIRONMENT = 18, IDLE = 19, HEAT_FULL = 20,
          UNSUPPORTED_RITUAL = 21, TARGET_DISABLED = 22, NEED_HEAT = 23, NEED_POTION = 24,
          NEED_SELECTION = 25, BOOK_TIER_LOW = 26, WAITING_START = 27, RITUAL_CONDITIONS = 28, NO_JAR_CREATURE = 29;
    public static final int EXPERIENCE_CAPACITY = 1_000_000, HEAT_CAPACITY = 1_000;
    // Initialized by the superclass callbacks; field initializers here would overwrite these containers.
    public List<BasicInventorySlot> inputs;
    private List<OutputInventorySlot> outputs;
    private EnergyInventorySlot energySlot;
    private BasicInventorySlot experienceSlot;
    List<BasicInventorySlot> jarSlots;
    CompoundTag drygmyHarvest;
    DrygmyHarvest.Samples drygmySamples;
    private int drygmyPendingItems;
    private MachineEnergyContainer<SourceMachine> energy;
    private IChemicalTank sourceTank;
    private int progress, duration = 1, status = MISSING_INPUT;
    private CompoundTag signature;
    private int mode;
    private RelativeSide arsSide = RelativeSide.FRONT;
    private String recipeLock = "", selectedRecipe = "";
    private boolean readingData;
    private int experience, heat;
    private int observedPrimary = -1, observedSecondary = -1, observedTertiary = -1;
    private net.neoforged.neoforge.items.IItemHandler collectionHandler;
    // Read-only native supply lookup is throttled; never persisted or used to spend resources.
    BlockEntity checkedSourceTarget;
    long nextSourceCheck;
    int checkedSourceCost;
    boolean nativeSourceAvailable;
    com.hollingsworth.arsnouveau.api.ritual.AbstractRitual requestedRitual;
    java.util.UUID ritualPlayer;

    public SourceMachine(BlockPos pos, BlockState state) {
        super(Content.MACHINES.get(((MachineBlock) state.getBlock()).kind), pos, state);
        int normalStart = kind() == MachineKind.RITUAL_CONTROLLER ? 2 : inputs.size() == 1 ? 0 : 1;
        var item = configComponent.setupItemIOConfig(kind().processesItems()
                    ? new ArrayList<IInventorySlot>(inputs.subList(normalStart, inputs.size())) : List.of(),
              new ArrayList<IInventorySlot>(outputs), energySlot, false);
        for (RelativeSide side : RelativeSide.values()) item.setDataType(DataType.INPUT, side);
        if (kind().processesItems()) {
            item.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, false, inputs.getFirst()));
            item.setDataType(DataType.EXTRA, RelativeSide.BACK);
            if (kind() == MachineKind.RITUAL_CONTROLLER)
                item.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, false, inputs.get(0), inputs.get(1)));
        }
        item.setDataType(DataType.OUTPUT, RelativeSide.RIGHT);
        item.setDataType(DataType.ENERGY, RelativeSide.BOTTOM);
        item.setEjecting(true);
        if (experienceSlot != null) {
            item.addSlotInfo(DataType.INPUT_2, new InventorySlotInfo(true, false, experienceSlot));
            item.setDataType(DataType.INPUT_2, RelativeSide.TOP);
        }
        if (!jarSlots.isEmpty()) {
            item.addSlotInfo(DataType.INPUT_2, new InventorySlotInfo(true, false, new ArrayList<IInventorySlot>(jarSlots)));
            item.setDataType(DataType.INPUT_2, RelativeSide.TOP);
        }
        var power = configComponent.setupInputConfig(TransmissionType.ENERGY, energy);
        for (RelativeSide side : RelativeSide.values()) power.setDataType(DataType.INPUT, side);
        if (kind().usesSource()) {
        var chemical = kind() == MachineKind.SOURCE_CONVERTER
              ? configComponent.setupIOConfig(TransmissionType.CHEMICAL, sourceTank, RelativeSide.RIGHT)
              : kind().outputsSource()
                    ? configComponent.setupOutputConfig(TransmissionType.CHEMICAL, sourceTank)
                    : configComponent.setupInputConfig(TransmissionType.CHEMICAL, sourceTank);
        for (RelativeSide side : RelativeSide.values()) chemical.setDataType(
              kind().outputsSource() && kind() != MachineKind.SOURCE_CONVERTER ? DataType.OUTPUT : DataType.INPUT, side);
        chemical.setEjecting(kind().outputsSource());
        }
        ejectorComponent = new TileComponentEjector(this);
        if (kind().outputsSource() && kind().processesItems())
            ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL, TransmissionType.ITEM);
        else ejectorComponent.setOutputData(configComponent, kind().outputsSource() ? TransmissionType.CHEMICAL : TransmissionType.ITEM);
        if (kind() == MachineKind.SOURCE_CONVERTER) {
            reserveArsSide();
            configComponent.addConfigChangeListener(TransmissionType.CHEMICAL, side -> {
                if (!readingData && side == arsSide.getDirection(getDirection())) reserveArsSide();
            });
        }
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
        var builder = ChemicalTankHelper.forSideWithConfig(this);
        sourceTank = kind().outputsSource() && kind() != MachineKind.SOURCE_CONVERTER ? BasicChemicalTank.output(SOURCE_CAPACITY, listener)
              : kind() == MachineKind.SOURCE_CONVERTER
                    ? BasicChemicalTank.createModern(SOURCE_CAPACITY, stack -> stack.is(Content.SOURCE), listener)
                    : BasicChemicalTank.inputModern(kind().usesSource() ? SOURCE_CAPACITY : 0, stack -> stack.is(Content.SOURCE), listener);
        if (kind().usesSource()) builder.addTank(sourceTank);
        return builder.build();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        var builder = InventorySlotHelper.forSideWithConfig(this);
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        jarSlots = new ArrayList<>();
        for (int i = 0; i < kind().inputCount(); i++) {
            final int index = i;
            int x = i == 0 ? 104 : 18 + (i - 1) % 4 * 18;
            int y = i == 0 ? 39 : 30 + (i - 1) / 4 * 18;
            if (kind() == MachineKind.RITUAL_CONTROLLER && i > 0) {
                x = i == 1 ? 104 : 18 + (i - 2) % 4 * 18;
                y = i == 1 ? 60 : 30 + (i - 2) / 4 * 18;
            }
            if (kind() == MachineKind.DRYGMY_STATION && i > 0) {
                x = 18 + (i - 1) % 3 * 18; y = 125 + (i - 1) / 3 * 18;
            }
            var slot = BasicInventorySlot.at((stack, automation) -> automation != AutomationType.EXTERNAL,
                  (stack, automation) -> automation != AutomationType.EXTERNAL || !kind().creatureController()
                        || index > 0 && !internalDrygmy() && WorldControllers.acceptsCollected(this, stack),
                  stack -> kind().advanced() ? AdvancedRecipes.accepts(getLevel(), kind(), index, stack)
                        : RecipeAdapter.accepts(getLevel(), kind(), index, stack), listener, x, y);
            inputs.add(slot);
            builder.addSlot(slot);
        }
        for (int i = 0; i < kind().outputCount(); i++) {
            int columns = kind().outputCount() > 4 ? 3 : 2;
            var slot = OutputInventorySlot.at(listener, 164 + i % columns * 18, 30 + i / columns * 18);
            outputs.add(slot);
            builder.addSlot(slot);
        }
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energy, this::getLevel, listener, 206, 78));
        if (kind() == MachineKind.GLYPH_SCRIBE) {
            experienceSlot = BasicInventorySlot.at((stack, automation) -> automation != AutomationType.EXTERNAL,
                  (stack, automation) -> true, stack -> stack.getItem() instanceof ExperienceGem, listener, 206, 60);
            builder.addSlot(experienceSlot);
        }
        // Append after every old slot, including energy, so existing world and item inventories keep their indices.
        if (kind() == MachineKind.DRYGMY_STATION) for (int i = 0; i < DrygmyHarvest.JAR_SLOTS; i++) {
            var slot = DrygmyHarvest.slot(listener, 18 + i % 4 * 18, 30 + i / 4 * 18);
            jarSlots.add(slot); builder.addSlot(slot);
        }
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean update = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        setActive(false);
        if (!canFunction()) { status = REDSTONE; return update; }
        if (experienceSlot != null && experienceSlot.getStack().getItem() instanceof ExperienceGem gem) {
            int points = gem.getValue();
            if (points > 0 && points <= EXPERIENCE_CAPACITY - experience) {
                experience += points;
                experienceSlot.shrinkStack(1, Action.EXECUTE);
                markForSave();
            }
        }
        switch (kind()) {
            case SOURCE_GENERATOR -> tickGenerator();
            case SOURCE_CONVERTER -> tickConverter();
            case IMBUEMENT_CHAMBER, ENCHANTING_APPARATUS -> tickRecipe();
            default -> tickAdvanced();
        }
        return update;
    }

    private int rate(int base) {
        return (int) Math.min(Integer.MAX_VALUE, (long) base * MekanismUtils.getOperationsPerTick(this, 1, 1));
    }

    public int sourceRate() {
        return rate(kind() == MachineKind.SOURCE_GENERATOR ? MachineConfig.GENERATOR_RATE.get() : MachineConfig.CONVERTER_RATE.get());
    }

    private long transferEnergy(int actual, int fullRate) {
        return Math.max(1, (long) Math.ceil(energy.getEnergyPerTick() * (actual / (double) fullRate)));
    }

    private boolean hasEnergy(long amount) {
        if (energy.extract(amount, Action.SIMULATE, AutomationType.INTERNAL) == amount) return true;
        status = NEED_ENERGY;
        return false;
    }

    private void spendEnergy(long amount) { energy.extract(amount, Action.EXECUTE, AutomationType.INTERNAL); }

    private void tickGenerator() {
        int rate = sourceRate();
        int amount = (int) Math.min(rate, sourceTank.getNeeded());
        if (amount == 0) { status = OUTPUT_FULL; return; }
        long cost = transferEnergy(amount, rate);
        if (!hasEnergy(cost)) return;
        spendEnergy(cost);
        sourceTank.insert(new ChemicalStack(Content.SOURCE, amount), Action.EXECUTE, AutomationType.INTERNAL);
        status = RUNNING;
        setActive(true);
    }

    private void reserveArsSide() {
        if (kind() != MachineKind.SOURCE_CONVERTER) return;
        var info = configComponent.getConfig(TransmissionType.CHEMICAL);
        if (info != null && info.getDataType(arsSide) != DataType.NONE) {
            info.setDataType(DataType.NONE, arsSide);
            if (level != null) configComponent.sideChanged(TransmissionType.CHEMICAL, arsSide);
        }
    }

    private void tickConverter() {
        if (mode == 2) { status = DISABLED; return; }
        Direction direction = arsSide.getDirection(getDirection());
        BlockPos target = worldPosition.relative(direction);
        if (!level.hasChunkAt(target)) { status = NO_CONTAINER; return; }
        ISourceCap other = level.getCapability(CapabilityRegistry.SOURCE_CAPABILITY, target, direction.getOpposite());
        if (other == null) { status = NO_CONTAINER; return; }
        int rate = sourceRate();
        int available = (int) Math.min(rate, mode == 0 ? sourceTank.getStored() : sourceTank.getNeeded());
        if (available == 0) { status = mode == 0 ? NEED_SOURCE : OUTPUT_FULL; return; }
        int planned = Math.clamp(mode == 0 ? other.receiveSource(available, true) : other.extractSource(available, true), 0, available);
        if (planned == 0) { status = TRANSFER_BLOCKED; return; }
        if (!hasEnergy(transferEnergy(planned, rate))) return;
        int actual = Math.clamp(mode == 0 ? other.receiveSource(planned, false) : other.extractSource(planned, false), 0, planned);
        if (actual == 0) { status = TRANSFER_BLOCKED; return; }
        if (mode == 0) sourceTank.extract(actual, Action.EXECUTE, AutomationType.INTERNAL);
        else sourceTank.insert(new ChemicalStack(Content.SOURCE, actual), Action.EXECUTE, AutomationType.INTERNAL);
        spendEnergy(transferEnergy(actual, rate));
        status = RUNNING;
        setActive(true);
    }

    private void tickRecipe() {
        RecipeAdapter.Plan plan = RecipeAdapter.find(this);
        if (plan == null) {
            resetWork();
            selectedRecipe = "";
            status = RecipeAdapter.missingStatus(this);
            return;
        }
        selectedRecipe = plan.id().toString();
        int ticks = Math.max(1, MekanismUtils.getTicks(this, plan.ticks()));
        if (!plan.signature().equals(signature) || duration != ticks) {
            resetWork();
            signature = plan.signature();
            duration = ticks;
        }
        List<ItemStack> merged = mergeOutputs(plan.outputs());
        if (merged == null) { status = OUTPUT_FULL; return; }
        if (sourceTank.getStored() < plan.source()) { status = NEED_SOURCE; return; }
        if (!hasEnergy(energy.getEnergyPerTick())) return;
        spendEnergy(energy.getEnergyPerTick());
        status = RUNNING;
        setActive(true);
        if (++progress >= duration) {
            sourceTank.extract(plan.source(), Action.EXECUTE, AutomationType.INTERNAL);
            for (int i = 0; i < inputs.size(); i++) inputs.get(i).shrinkStack(plan.consume()[i], Action.EXECUTE);
            for (int i = 0; i < outputs.size(); i++) outputs.get(i).setStackUnchecked(merged.get(i));
            resetWork();
        }
        markForSave();
    }

    List<ItemStack> mergeOutputs(List<ItemStack> products) {
        var merged = outputs.stream().map(slot -> slot.getStack().copy()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (ItemStack product : products) {
            int remaining = product.getCount();
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < merged.size() && remaining > 0; i++) {
                ItemStack current = merged.get(i);
                if (pass == 0 ? current.isEmpty() : !current.isEmpty()) continue;
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, product)) continue;
                int moved = Math.min(remaining, Math.min(64, product.getMaxStackSize()) - current.getCount());
                if (moved <= 0) continue;
                merged.set(i, product.copyWithCount(current.getCount() + moved));
                remaining -= moved;
            }
            if (remaining > 0) return null;
        }
        return merged;
    }

    private void resetWork() { progress = 0; signature = null; }
    public MachineEnergyContainer<SourceMachine> energy() { return energy; }
    public IChemicalTank sourceTank() { return sourceTank; }
    public List<OutputInventorySlot> outputs() { return outputs; }
    public double progress() { return progress / (double) Math.max(1, duration); }
    public int status() { return status; }
    public int mode() { return mode; }
    public RelativeSide arsSide() { return arsSide; }
    public String recipeLock() { return recipeLock; }
    public String selectedRecipe() { return selectedRecipe; }
    public int experience() { return experience; }
    public int heat() { return heat; }
    public int observedPrimary() { return observedPrimary; }
    public int observedSecondary() { return observedSecondary; }
    public int observedTertiary() { return observedTertiary; }
    public boolean internalDrygmy() { return kind() == MachineKind.DRYGMY_STATION && (DrygmyHarvest.hasJars(this) || drygmyPendingItems > 0 || DrygmyHarvest.hasPending(drygmyHarvest)); }
    public int drygmyCycleTicks() { return Math.max(1, MekanismUtils.getTicks(this, MachineConfig.DRYGMY_HARVEST_TICKS.get())); }
    public int drygmyPendingItems() { return drygmyPendingItems; }
    void cacheDrygmyHarvest(CompoundTag tag) {
        if (drygmyHarvest == tag) return;
        drygmyHarvest = tag; drygmyPendingItems = DrygmyHarvest.hasPending(tag) ? Math.max(0, tag.getInt("pending_count")) : 0;
        markForSave();
    }
    void observe(int first, int second, int third) { observedPrimary = first; observedSecondary = second; observedTertiary = third; }
    void status(int value) { status = value; }
    net.neoforged.neoforge.items.IItemHandler collectionCapability(Direction side) {
        if (side != null) return ITEM_HANDLER_PROVIDER.getCapability(this, side);
        if (collectionHandler == null) collectionHandler = new NativeCollectionHandler(this);
        return collectionHandler;
    }

    private void tickAdvanced() {
        AdvancedRecipes.Work work = internalDrygmy() ? DrygmyHarvest.plan(this)
              : kind().worldController() ? WorldControllers.plan(this) : AdvancedRecipes.plan(this);
        if (work == null) { resetWork(); selectedRecipe = ""; return; }
        selectedRecipe = work.id();
        int ticks = Math.max(1, MekanismUtils.getTicks(this, work.ticks()));
        if (!work.signature().equals(signature) || duration != ticks) {
            resetWork(); signature = work.signature(); duration = ticks;
        }
        if (mergeOutputs(work.maximumOutputs()) == null) { status = OUTPUT_FULL; return; }
        if (sourceTank.getStored() < work.sourceCost()) { status = NEED_SOURCE; return; }
        if (sourceTank.getNeeded() < work.sourceGain()) { status = OUTPUT_FULL; return; }
        if (experience < work.experienceCost()) { status = NEED_EXPERIENCE; return; }
        if ((long) heat + work.heatChange() > HEAT_CAPACITY) { status = HEAT_FULL; return; }
        if (heat + work.heatChange() < 0) { status = NEED_HEAT; return; }
        if (!hasEnergy(energy.getEnergyPerTick())) return;
        spendEnergy(energy.getEnergyPerTick()); status = RUNNING; setActive(true);
        if (++progress >= duration) {
            // Random products are rolled only after a complete operation has reserved its maximum output.
            List<ItemStack> merged = mergeOutputs(work.outputs().get());
            if (merged == null) throw new IllegalStateException("Recipe exceeded its reserved output: " + work.id());
            work.commit().run();
            for (int i = 0; i < inputs.size(); i++) inputs.get(i).shrinkStack(work.consume()[i], Action.EXECUTE);
            sourceTank.extract(work.sourceCost(), Action.EXECUTE, AutomationType.INTERNAL);
            if (work.sourceGain() > 0) sourceTank.insert(new ChemicalStack(Content.SOURCE, work.sourceGain()), Action.EXECUTE, AutomationType.INTERNAL);
            experience -= work.experienceCost(); heat += work.heatChange();
            for (int i = 0; i < outputs.size(); i++) outputs.get(i).setStackUnchecked(merged.get(i));
            resetWork();
        }
        markForSave();
    }

    public boolean depositExperience(Player player) {
        if (kind() != MachineKind.GLYPH_SCRIBE) return false;
        int points = Math.min(100, Math.min(ScribesTile.getTotalPlayerExperience(player), EXPERIENCE_CAPACITY - experience));
        if (points <= 0) return false;
        player.giveExperiencePoints(-points); experience += points; markForSave(); return true;
    }

    public boolean requestRitualStart(Player player) {
        if (kind() != MachineKind.RITUAL_CONTROLLER) return false;
        BlockPos target = getBlockPos().relative(RelativeSide.FRONT.getDirection(getDirection()));
        if (!getLevel().hasChunkAt(target) || !(getLevel().getBlockEntity(target) instanceof
              com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile brazier)
              || brazier.ritual == null || brazier.ritual.isRunning() || brazier.isOff || brazier.isDecorative) return false;
        requestedRitual = brazier.ritual; ritualPlayer = player.getUUID(); resetWork();
        return true;
    }

    public void cycleMode() {
        if (kind() == MachineKind.SOURCE_CONVERTER) {
            mode = (mode + 1) % 3;
            // Every Chemical face follows the selected transfer direction; the Ars face stays reserved.
            var info = configComponent.getConfig(TransmissionType.CHEMICAL);
            for (RelativeSide side : RelativeSide.values()) {
                info.setDataType(side == arsSide ? DataType.NONE : mode == 1 ? DataType.OUTPUT : DataType.INPUT, side);
                configComponent.sideChanged(TransmissionType.CHEMICAL, side);
            }
        } else if (kind().modes() > 1) {
            mode = (mode + 1) % kind().modes();
            recipeLock = "";
        } else return;
        requestedRitual = null; ritualPlayer = null;
        resetWork();
        markForSave();
    }

    public void cycleArsSide() {
        if (kind() != MachineKind.SOURCE_CONVERTER) return;
        RelativeSide previous = arsSide;
        arsSide = RelativeSide.values()[(arsSide.ordinal() + 1) % RelativeSide.values().length];
        var info = configComponent.getConfig(TransmissionType.CHEMICAL);
        info.setDataType(mode == 1 ? DataType.OUTPUT : DataType.INPUT, previous);
        configComponent.sideChanged(TransmissionType.CHEMICAL, previous);
        reserveArsSide();
        markForSave();
    }

    public boolean setRecipeLock(String value) {
        if (!kind().recipeSelectable() || value.length() > 256) return false;
        if (!value.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null || !(kind().advanced() ? AdvancedRecipes.isSupportedRecipe(this, id) : RecipeAdapter.isSupportedRecipe(this, id))) return false;
        }
        recipeLock = value;
        resetWork();
        markForSave();
        return true;
    }

    private CompoundTag settings() {
        var tag = new CompoundTag();
        tag.putInt("mode", mode);
        tag.putInt("ars_side", arsSide.ordinal());
        tag.putString("recipe_lock", recipeLock);
        tag.putInt("experience", experience);
        tag.putInt("heat", heat);
        if (kind() == MachineKind.DRYGMY_STATION && drygmyHarvest != null) tag.put("drygmy_harvest", drygmyHarvest.copy());
        return tag;
    }

    private void readSettings(CompoundTag tag) {
        mode = Math.clamp(tag.getInt("mode"), 0, kind().modes() - 1);
        experience = Math.clamp(tag.getInt("experience"), 0, EXPERIENCE_CAPACITY);
        heat = Math.clamp(tag.getInt("heat"), 0, HEAT_CAPACITY);
        drygmyHarvest = kind() == MachineKind.DRYGMY_STATION && tag.contains("drygmy_harvest", net.minecraft.nbt.Tag.TAG_COMPOUND)
              ? tag.getCompound("drygmy_harvest").copy() : null;
        drygmyPendingItems = DrygmyHarvest.hasPending(drygmyHarvest) ? Math.max(0, drygmyHarvest.getInt("pending_count")) : 0;
        drygmySamples = null;
        arsSide = RelativeSide.values()[Math.clamp(tag.getInt("ars_side"), 0, RelativeSide.values().length - 1)];
        String id = tag.getString("recipe_lock");
        recipeLock = id.length() <= 256 && ResourceLocation.tryParse(id) != null ? id : "";
        reserveArsSide();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(Content.SETTINGS, settings());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        readingData = true;
        try { super.applyImplicitComponents(input); } finally { readingData = false; }
        CompoundTag tag = input.get(Content.SETTINGS);
        if (tag != null) readSettings(tag);
    }

    @Override
    public void applyInventorySlots(BlockEntity.DataComponentInput input, List<IInventorySlot> slots,
          mekanism.common.attachments.containers.item.AttachedItems attached) {
        // Mek only restores equal-sized item lists. Upgrade the known 0.2.x layout before delegating,
        // leaving the original seven inputs, six outputs and energy item exactly where they were.
        if (kind() == MachineKind.DRYGMY_STATION && attached.size() == 14 && slots.size() == 14 + DrygmyHarvest.JAR_SLOTS) {
            var expanded = new ArrayList<>(attached.containers());
            for (int i = 0; i < DrygmyHarvest.JAR_SLOTS; i++) expanded.add(ItemStack.EMPTY);
            attached = new mekanism.common.attachments.containers.item.AttachedItems(expanded);
        }
        super.applyInventorySlots(input, slots, attached);
    }

    @Override
    public List<Component> getInfo(Upgrade upgrade) { return UpgradeUtils.getMultScaledInfo(this, upgrade); }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> progress, v -> progress = v));
        container.track(SyncableInt.create(() -> duration, v -> duration = v));
        container.track(SyncableInt.create(() -> status, v -> status = v));
        container.track(SyncableInt.create(() -> experience, v -> experience = v));
        container.track(SyncableInt.create(() -> heat, v -> heat = v));
        container.track(SyncableInt.create(() -> observedPrimary, v -> observedPrimary = v));
        container.track(SyncableInt.create(() -> observedSecondary, v -> observedSecondary = v));
        container.track(SyncableInt.create(() -> observedTertiary, v -> observedTertiary = v));
        container.track(SyncableInt.create(() -> drygmyPendingItems, v -> drygmyPendingItems = v));
        container.track(SyncableInt.create(() -> mode, v -> mode = v));
        container.track(SyncableInt.create(() -> arsSide.ordinal(), v -> arsSide = RelativeSide.values()[Math.clamp(v, 0, 5)]));
        container.track(new RecipeSelectionSync(() -> List.of(recipeLock, selectedRecipe), values -> {
            if (values.size() == 2) { recipeLock = values.get(0); selectedRecipe = values.get(1); }
        }));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("machine_settings", settings());
        tag.putInt("work_progress", progress);
        tag.putInt("work_duration", duration);
        if (signature != null) tag.put("work_signature", signature);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        readingData = true;
        try { super.loadAdditional(tag, provider); } finally { readingData = false; }
        readSettings(tag.getCompound("machine_settings"));
        duration = Math.clamp(tag.getInt("work_duration"), 1, 100_000);
        progress = Math.clamp(tag.getInt("work_progress"), 0, duration - 1);
        signature = tag.contains("work_signature") ? tag.getCompound("work_signature") : null;
    }
}
