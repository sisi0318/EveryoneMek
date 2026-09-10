package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import dev.everyonemek.forbidden.mixin.ClibanoAccess;
import dev.everyonemek.forbidden.mixin.RitualAccess;
import java.util.*;
import mekanism.api.*;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.*;
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
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valhelsia.valhelsia_core.api.common.block.entity.neoforge.ValhelsiaContainerBlockEntity;

public final class Controller extends TileEntityConfigurableMachine {
    public static final int IDLE = 0, RUNNING = 1, NO_TARGET = 2, STRUCTURE = 3, OCCUPIED = 4, NEED_ENERGY = 5,
          PAUSED = 6, NEED_MATERIALS = 7, NEED_HAMMER = 8, CONDITIONS = 9, OUTPUT_FULL = 10,
          MANUAL_ITEMS = 11, RECIPE_CONFLICT = 12, INTERRUPTED = 13, UPGRADED = 14, NEED_FUEL = 15, NEED_SOUL = 16;
    // These fields are initialized during superclass callbacks, before the subclass constructor runs.
    public List<BasicInventorySlot> stock, supplies;
    public List<OutputInventorySlot> outputs;
    public BasicInventorySlot hammer;
    public HammerModuleSlot module;
    public EnergyInventorySlot energySlot;
    private MachineEnergyContainer<Controller> energy;
    public final Binding binding = new Binding(this);
    public int status = NO_TARGET, phase, progress, duration = 1, nativeTier;
    public final int[] observed = new int[10], capacities = new int[4];
    public String recipeLock = "", selectedRecipe = "";
    public boolean enabled = true;
    public UUID batch;
    public ItemStack expected = ItemStack.EMPTY;
    public int startingTier;
    public String batchRecipe = "";
    public CompoundTag preparedSignature;
    private int cooldown;

    public Controller(BlockPos pos, BlockState state) {
        super(Content.MACHINES.get(((MachineBlock) state.getBlock()).kind), pos, state);
        var items = configComponent.setupItemIOConfig(new ArrayList<IInventorySlot>(stock),
              new ArrayList<IInventorySlot>(outputs), energySlot, false);
        items.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, false,
              kind().forge() ? new ArrayList<IInventorySlot>(supplies) : List.of(supplies.get(0))));
        items.addSlotInfo(DataType.INPUT_2, new InventorySlotInfo(true, false,
              kind().forge() ? List.of(hammer) : List.of(supplies.get(1))));
        for (RelativeSide side : RelativeSide.values()) items.setDataType(DataType.INPUT, side);
        items.setDataType(DataType.EXTRA, RelativeSide.BACK);
        items.setDataType(DataType.INPUT_2, RelativeSide.TOP);
        items.setDataType(DataType.ENERGY, RelativeSide.BOTTOM);
        items.setDataType(DataType.OUTPUT, RelativeSide.RIGHT);
        items.setEjecting(true);
        var power = configComponent.setupInputConfig(TransmissionType.ENERGY, energy);
        for (RelativeSide side : RelativeSide.values()) power.setDataType(DataType.INPUT, side);
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }
    public MachineKind kind() { return ((MachineBlock) getBlockState().getBlock()).kind; }
    @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        var builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energy = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        var builder = InventorySlotHelper.forSideWithConfig(this);
        stock = new ArrayList<>(); outputs = new ArrayList<>(); supplies = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            var slot = BasicInventorySlot.at((s, a) -> a != AutomationType.EXTERNAL, (s, a) -> true,
                  s -> true, listener, 18 + i % 3 * 18, 30 + i / 3 * 18);
            stock.add(slot); builder.addSlot(slot);
        }
        for (int i = 0; i < 4; i++) {
            var slot = OutputInventorySlot.at(listener, 200 + i % 2 * 18, 30 + i / 2 * 18);
            outputs.add(slot); builder.addSlot(slot);
        }
        for (int i = 0; i < kind().supplies(); i++) {
            final int index = i;
            var slot = BasicInventorySlot.at((s, a) -> a != AutomationType.EXTERNAL, (s, a) -> true,
                  s -> NativeInventory.acceptsSupply(getLevel(), kind(), index, s), listener, 18 + i * 18, 102);
            supplies.add(slot); builder.addSlot(slot);
        }
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energy, this::getLevel, listener, 218, 84));
        if (kind().forge()) {
            hammer = new HammerSlot(listener);
            builder.addSlot(hammer);
            builder.addSlot(module = new HammerModuleSlot(listener));
        }
        return builder.build();
    }
    public boolean infiniteHammer() { return module != null && module.getStack().is(Content.INFINITE_HAMMER_MODULE); }
    public MachineEnergyContainer<Controller> energy() { return energy; }
    @Override protected boolean onUpdateServer() {
        boolean update = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        setActive(false);
        var target = binding.resolve();
        readNative(target);
        if (target == null) { status = binding.target == null ? NO_TARGET : STRUCTURE; return update; }
        if (!enabled || !canFunction()) { status = PAUSED; return update; }
        if (cooldown-- > 0) return update;
        cooldown = Math.max(1, 10 / MekanismUtils.getOperationsPerTick(this, 1, 1));
        if (energy.extract(energy.getEnergyPerTick(), Action.SIMULATE, AutomationType.INTERNAL) != energy.getEnergyPerTick()) {
            status = NEED_ENERGY; return update;
        }
        boolean changed = NativeInventory.supply(this, target);
        if (target instanceof HephaestusForgeBlockEntity forge) changed |= ForgeAutomation.tick(this, forge);
        else if (target instanceof ClibanoMainBlockEntity clibano) changed |= ClibanoAutomation.tick(this, clibano);
        if (changed) {
            energy.extract(energy.getEnergyPerTick(), Action.EXECUTE, AutomationType.INTERNAL);
            markForSave();
            target.setChanged();
            setActive(true);
        }
        return update;
    }
    private void readNative(ValhelsiaContainerBlockEntity<?> target) {
        Arrays.fill(observed, -1); Arrays.fill(capacities, 0); nativeTier = 0; progress = 0; duration = 1;
        if (target instanceof HephaestusForgeBlockEntity forge) {
            var block = (HephaestusForgeBlock) forge.getBlockState().getBlock();
            nativeTier = block.getLevel().getAsInt();
            for (int i = 0; i < 4; i++) observed[i] = forge.getHephaestusForgeData().get(i);
            block.getLevel().getMaxEssences().forEach((type, value) -> capacities[type.ordinal()] = value);
            var active = ((RitualAccess) forge.getRitualManager()).forbiddenmekanism$active();
            if (active != null) { progress = active.getCounter(); duration = active.getRitual().duration(); }
        } else if (target instanceof ClibanoMainBlockEntity clibano) {
            var data = ((ClibanoAccess) clibano).forbiddenmekanism$data();
            for (int i = 0; i < 10; i++) observed[i] = data.get(i);
            progress = observed[3]; duration = Math.max(1, observed[5]);
        }
    }
    public boolean canStore(ItemStack stack) { return mergedOutputs(stack) != null; }
    public boolean storeOutput(ItemStack stack) {
        var merged = mergedOutputs(stack);
        if (merged == null) return false;
        for (int i = 0; i < outputs.size(); i++) outputs.get(i).setStackUnchecked(merged.get(i));
        return true;
    }
    private List<ItemStack> mergedOutputs(ItemStack stack) {
        var result = outputs.stream().map(s -> s.getStack().copy()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        int remaining = stack.getCount();
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < result.size() && remaining > 0; i++) {
            ItemStack old = result.get(i);
            if (pass == 0 ? old.isEmpty() : !old.isEmpty()) continue;
            if (!old.isEmpty() && !ItemStack.isSameItemSameComponents(old, stack)) continue;
            int moved = Math.min(remaining, Math.min(64, stack.getMaxStackSize()) - old.getCount());
            if (moved > 0) { result.set(i, stack.copyWithCount(old.getCount() + moved)); remaining -= moved; }
        }
        return remaining == 0 ? result : null;
    }
    public boolean setRecipe(String value) {
        if (value.length() > 256 || !value.isEmpty() && !Recipes.exists(level, kind(), ResourceLocation.tryParse(value))) return false;
        // Finish an in-flight batch with the original recipe before the new selection takes effect.
        recipeLock = value; markForSave(); return true;
    }
    public void clearBatch() { phase = 0; batch = null; batchRecipe = ""; expected = ItemStack.EMPTY; preparedSignature = null; markForSave(); }
    private CompoundTag settings(HolderLookup.Provider provider) {
        var tag = new CompoundTag(); binding.save(tag);
        tag.putString("recipe", recipeLock); tag.putBoolean("enabled", enabled);
        tag.putInt("phase", phase); tag.putString("batch_recipe", batchRecipe); tag.putInt("starting_tier", startingTier);
        if (batch != null) tag.putUUID("batch", batch);
        if (preparedSignature != null) tag.put("prepared", preparedSignature.copy());
        if (!expected.isEmpty()) tag.put("expected", expected.save(provider));
        return tag;
    }
    private void readSettings(CompoundTag tag, HolderLookup.Provider provider) {
        binding.load(tag); enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        String id = tag.getString("recipe"); recipeLock = id.length() <= 256 && ResourceLocation.tryParse(id) != null ? id : "";
        phase = Math.clamp(tag.getInt("phase"), 0, 2); startingTier = Math.clamp(tag.getInt("starting_tier"), 0, 5);
        batch = tag.hasUUID("batch") ? tag.getUUID("batch") : null; batchRecipe = tag.getString("batch_recipe");
        expected = ItemStack.parseOptional(provider, tag.getCompound("expected"));
        preparedSignature = tag.contains("prepared", 10) ? tag.getCompound("prepared").copy() : null;
        if (batch == null) phase = 0;
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); tag.put("controller", settings(provider));
    }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); readSettings(tag.getCompound("controller"), provider);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder); builder.set(Content.SETTINGS, settings(level.registryAccess()));
    }
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        var tag = input.get(Content.SETTINGS);
        if (tag != null) readSettings(tag, level.registryAccess());
    }
    @Override public List<Component> getInfo(Upgrade upgrade) { return UpgradeUtils.getMultScaledInfo(this, upgrade); }
    @Override public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> status, v -> status = v));
        container.track(SyncableInt.create(() -> phase, v -> phase = v));
        container.track(SyncableInt.create(() -> progress, v -> progress = v));
        container.track(SyncableInt.create(() -> duration, v -> duration = Math.max(1, v)));
        container.track(SyncableInt.create(() -> nativeTier, v -> nativeTier = v));
        container.track(SyncableInt.create(() -> enabled ? 1 : 0, v -> enabled = v != 0));
        for (int i = 0; i < observed.length; i++) { final int n = i; container.track(SyncableInt.create(() -> observed[n], v -> observed[n] = v)); }
        for (int i = 0; i < capacities.length; i++) { final int n = i; container.track(SyncableInt.create(() -> capacities[n], v -> capacities[n] = v)); }
        container.track(new RecipeSelectionSync(() -> List.of(recipeLock, selectedRecipe, binding.label()), values -> {
            if (values.size() == 3) { recipeLock = values.get(0); selectedRecipe = values.get(1); binding.clientLabel = values.get(2); }
        }));
    }
}
