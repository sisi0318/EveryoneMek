package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.item.enhancer.EnhancerHelper;
import dev.everyonemek.forbidden.mixin.ClibanoAccess;
import dev.everyonemek.forbidden.mixin.UpgradeSlotAccess;
import java.util.*;
import mekanism.api.*;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.*;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableDouble;
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
          PAUSED = 6, NEED_MATERIALS = 7, NEED_TIER = 8, CONDITIONS = 9, OUTPUT_FULL = 10,
          NEED_ENHANCER = 11, NEED_AUREAL = 12, NEED_SOULS = 13, NEED_BLOOD = 14, NEED_FUEL = 15,
          NEED_SOUL = 16, NEED_EXPERIENCE = 17, RECIPE_CONFLICT = 18;
    // These fields are initialized during superclass callbacks, before the subclass constructor runs.
    public List<BasicInventorySlot> stock, supplies, enhancers;
    public List<OutputInventorySlot> outputs;
    public ResourceModuleSlot module;
    public List<ResourceModuleSlot> resourceModules;
    public EnergyInventorySlot energySlot;
    private MachineEnergyContainer<Controller> energy;
    public final Binding binding = new Binding(this);
    public final InternalForge forge;
    public int status = NO_TARGET, progress, duration = 1, nativeTier;
    public final int[] observed = new int[10], capacities = new int[4];
    public final double[] resourceRates = new double[4];
    private final int[] clientModuleCounts = new int[4];
    public String recipeLock = "", selectedRecipe = "";
    public boolean enabled = true;
    private int cooldown;

    public Controller(BlockPos pos, BlockState state) {
        super(Content.MACHINES.get(((MachineBlock) state.getBlock()).kind), pos, state);
        forge = kind().forge() ? new InternalForge(this) : null;
        if (forge != null) {
            var input = (UpgradeSlotAccess) getComponent().getUpgradeSlot();
            var valid = input.forbiddenmekanism$validator();
            var insert = input.forbiddenmekanism$canInsert();
            input.forbiddenmekanism$validator(stack -> valid.test(stack) || Content.resourceModuleIndex(stack) >= 0);
            input.forbiddenmekanism$canInsert((stack, automation) -> insert.test(stack, automation)
                  || automation != AutomationType.EXTERNAL && Content.resourceModuleIndex(stack) >= 0);
            var output = (UpgradeSlotAccess) getComponent().getUpgradeOutputSlot();
            var outputValid = output.forbiddenmekanism$validator();
            output.forbiddenmekanism$validator(stack -> outputValid.test(stack) || Content.resourceModuleIndex(stack) >= 0);
        }
        var items = configComponent.setupItemIOConfig(new ArrayList<IInventorySlot>(stock),
              new ArrayList<IInventorySlot>(outputs), energySlot, false);
        items.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, false,
              kind().forge() ? new ArrayList<IInventorySlot>(supplies) : List.of(supplies.get(0))));
        if (!kind().forge()) items.addSlotInfo(DataType.INPUT_2, new InventorySlotInfo(true, false, List.of(supplies.get(1))));
        for (RelativeSide side : RelativeSide.values()) items.setDataType(DataType.INPUT, side);
        items.setDataType(DataType.EXTRA, RelativeSide.BACK);
        if (!kind().forge()) items.setDataType(DataType.INPUT_2, RelativeSide.TOP);
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
                  s -> NativeInventory.acceptsSupply(getLevel(), kind(), index, s), listener,
                  kind().forge() ? 112 + i * 18 : 18 + i * 18, kind().forge() ? 66 : 102);
            supplies.add(slot); builder.addSlot(slot);
        }
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energy, this::getLevel, listener, 218, kind().forge() ? 66 : 84));
        if (kind().forge()) {
            enhancers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                var slot = new BasicInventorySlot(1, (s, a) -> a != AutomationType.EXTERNAL,
                      (s, a) -> a != AutomationType.EXTERNAL,
                      s -> getLevel() != null && EnhancerHelper.getEnhancer(getLevel().registryAccess(), s).isPresent(),
                      listener, 112 + i * 18, 30) { };
                enhancers.add(slot); builder.addSlot(slot);
            }
            resourceModules = new ArrayList<>();
            for (int resource = 0; resource < 4; resource++) {
                var slot = new ResourceModuleSlot(listener, resource);
                resourceModules.add(slot); builder.addSlot(slot);
            }
            module = resourceModules.getFirst();
        }
        return builder.build();
    }
    public int glowModules() { return resourceModuleCount(0); }
    public int resourceModuleCount(int resource) {
        if (resourceModules == null || resource < 0 || resource >= resourceModules.size()) return 0;
        if (level != null && level.isClientSide) return clientModuleCounts[resource];
        var slot = resourceModules.get(resource);
        return slot.getStack().is(Content.resourceModule(resource)) ? Math.min(8, slot.getCount()) : 0;
    }
    public boolean uninstallResourceModule(int resource, boolean all) {
        if (!kind().forge() || resource < 0 || resource >= 4) return false;
        var installed = resourceModules.get(resource);
        var output = getComponent().getUpgradeOutputSlot();
        ItemStack offered = installed.extractItem(all ? 8 : 1, Action.SIMULATE, AutomationType.INTERNAL);
        if (offered.isEmpty()) return false;
        int accepted = offered.getCount() - output.insertItem(offered, Action.SIMULATE, AutomationType.INTERNAL).getCount();
        if (accepted == 0) return false;
        output.insertItem(installed.extractItem(accepted, Action.EXECUTE, AutomationType.INTERNAL), Action.EXECUTE, AutomationType.INTERNAL);
        markForSave();
        return true;
    }
    public MachineEnergyContainer<Controller> energy() { return energy; }
    @Override protected boolean onUpdateServer() {
        boolean update = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        if (forge != null) {
            forge.tick();
            setActive(status == RUNNING);
            return update;
        }
        setActive(false);
        ClibanoEmbedding.autoConnect(this);
        ClibanoPorts.eject(this);
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
        if (target instanceof ClibanoMainBlockEntity clibano) changed |= ClibanoAutomation.tick(this, clibano);
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
        if (target instanceof ClibanoMainBlockEntity clibano) {
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
        if (forge != null && !recipeLock.equals(value)) forge.resetProgress();
        recipeLock = value; markForSave(); return true;
    }
    private CompoundTag settings(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        if (forge != null) forge.save(tag); else binding.save(tag);
        tag.putString("recipe", recipeLock); tag.putBoolean("enabled", enabled);
        return tag;
    }
    private void readSettings(CompoundTag tag, HolderLookup.Provider provider) {
        if (forge != null) forge.load(tag); else binding.load(tag);
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        String id = tag.getString("recipe"); recipeLock = id.length() <= 256 && ResourceLocation.tryParse(id) != null ? id : "";
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
    @Override public void applyInventorySlots(BlockEntity.DataComponentInput input, List<IInventorySlot> slots,
          mekanism.common.attachments.containers.item.AttachedItems attached) {
        // Mek skips unequal item lists. Preserve the released 0.2.0 layout and append only the three new slots.
        if (kind().forge() && attached.size() == 23 && slots.size() == 26) {
            var expanded = new ArrayList<>(attached.containers());
            for (int i = 0; i < 3; i++) expanded.add(ItemStack.EMPTY);
            attached = new mekanism.common.attachments.containers.item.AttachedItems(expanded);
        }
        super.applyInventorySlots(input, slots, attached);
    }
    @Override public List<Component> getInfo(Upgrade upgrade) { return UpgradeUtils.getMultScaledInfo(this, upgrade); }
    @Override public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> status, v -> status = v));
        container.track(SyncableInt.create(() -> progress, v -> progress = v));
        container.track(SyncableInt.create(() -> duration, v -> duration = Math.max(1, v)));
        container.track(SyncableInt.create(() -> nativeTier, v -> nativeTier = v));
        container.track(SyncableInt.create(() -> enabled ? 1 : 0, v -> enabled = v != 0));
        container.track(SyncableInt.create(() -> binding.embedded ? 1 : 0, v -> binding.embedded = v != 0));
        container.track(mekanism.common.inventory.container.sync.SyncableLong.create(() -> binding.target == null ? Long.MIN_VALUE : binding.target.asLong(),
              v -> binding.target = v == Long.MIN_VALUE ? null : BlockPos.of(v)));
        for (int i = 0; i < observed.length; i++) { final int n = i; container.track(SyncableInt.create(() -> observed[n], v -> observed[n] = v)); }
        for (int i = 0; i < capacities.length; i++) { final int n = i; container.track(SyncableInt.create(() -> capacities[n], v -> capacities[n] = v)); }
        if (kind().forge()) for (int i = 0; i < 4; i++) {
            final int n = i;
            container.track(SyncableInt.create(() -> resourceModuleCount(n), v -> clientModuleCounts[n] = Math.clamp(v, 0, 8)));
            container.track(SyncableDouble.create(() -> resourceRates[n], v -> resourceRates[n] = Math.max(0, v)));
        }
        container.track(new RecipeSelectionSync(() -> List.of(recipeLock, selectedRecipe, binding.label()), values -> {
            if (values.size() == 3) { recipeLock = values.get(0); selectedRecipe = values.get(1); binding.clientLabel = values.get(2); }
        }));
    }
}
