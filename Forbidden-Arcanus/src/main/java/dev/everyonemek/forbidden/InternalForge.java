package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.ModBlockPatterns;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.Ritual;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.CreateItemResult;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.TransmuteInputResult;
import com.stal111.forbidden_arcanus.common.item.enhancer.*;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import java.util.*;
import mekanism.api.*;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

/** Owns the forge's resources and processing; all recipe definitions remain in the native registry. */
public final class InternalForge {
    private final Controller machine;
    private CompoundTag signature;
    private static final String[] MODULE_PROGRESS_KEYS = {"glow_progress", "soul_progress", "blood_progress", "experience_progress"};
    private final int[] moduleProgress = new int[4];
    private static final int[] MODULE_YIELDS = {100, 1, 150, 100};
    public static int moduleYield(int resource) { return MODULE_YIELDS[resource]; }

    public InternalForge(Controller machine) {
        this.machine = machine;
        machine.nativeTier = 1;
        refreshCapacities();
    }
    public static boolean supports(Ritual ritual) {
        return ritual.result() instanceof CreateItemResult || ritual.result() instanceof TransmuteInputResult;
    }
    public boolean hasPlatform() {
        var level = machine.getLevel();
        var pos = machine.getBlockPos();
        if (level == null || machine.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != machine) return false;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
            if (!level.hasChunkAt(pos.offset(x, -1, z))) return false;
        return ModBlockPatterns.BASE_HEPHAESTUS_PATTERN.matches(level, pos.offset(4, -1, -4), Direction.SOUTH, Direction.UP) != null;
    }
    private void refreshCapacities() {
        HephaestusForgeLevel.getFromIndex(machine.nativeTier).getMaxEssences()
              .forEach((type, value) -> machine.capacities[type.ordinal()] = value);
    }
    public boolean upgradeTo(int tier) {
        if (tier < 2 || tier > 5 || machine.nativeTier != tier - 1) return false;
        machine.nativeTier = tier;
        refreshCapacities();
        machine.markForSave();
        return true;
    }
    public HolderSet<EnhancerDefinition> enhancers() {
        return HolderSet.direct(machine.enhancers.stream()
              .flatMap(slot -> EnhancerHelper.getEnhancerHolder(machine.getLevel().registryAccess(), slot.getStack()).stream()).toList());
    }
    public EssencesDefinition cost(Ritual ritual) {
        var modifiers = enhancers().stream().flatMap(holder -> holder.value().getEffects(EnhancerTarget.HEPHAESTUS_FORGE))
              .filter(EssenceModifier.class::isInstance).map(EssenceModifier.class::cast).toList();
        var cost = ritual.requirements().essences().applyModifiers(modifiers);
        return new EssencesDefinition(Math.max(0, cost.aureal()), Math.max(0, cost.souls()),
              Math.max(0, cost.blood()), Math.max(0, cost.experience()));
    }
    public int moduleInterval() { return Math.max(1, MekanismUtils.getTicks(machine, 100)); }
    private boolean hasEnergy(long amount) {
        return machine.energy().extract(amount, Action.SIMULATE, AutomationType.INTERNAL) == amount;
    }
    private void generateResources() {
        for (int resource = 0; resource < 4; resource++) {
            int modules = machine.resourceModuleCount(resource);
            if (modules == 0) { moduleProgress[resource] = 0; continue; }
            int free = machine.capacities[resource] - machine.observed[resource];
            long pulseEnergy = Math.max(1, machine.energy().getEnergyPerTick());
            int active = (int) Math.min(Math.min(modules, (free + moduleYield(resource) - 1) / moduleYield(resource)),
                  machine.energy().getEnergy() / pulseEnergy);
            if (free <= 0 || active <= 0) continue;
            machine.resourceRates[resource] = Math.min(free, active * moduleYield(resource)) * 20.0 / moduleInterval();
            if (++moduleProgress[resource] >= moduleInterval()) {
                int amount = Math.min(free, active * moduleYield(resource));
                machine.energy().extract(pulseEnergy * active, Action.EXECUTE, AutomationType.INTERNAL);
                machine.observed[resource] += amount;
                if (machine.observed[resource] == machine.capacities[resource]) machine.resourceRates[resource] = 0;
                moduleProgress[resource] = 0;
            }
            machine.markForSave();
        }
    }
    private void fillResources() {
        var level = machine.getLevel();
        var inputs = level.registryAccess().registryOrThrow(FARegistries.FORGE_INPUT);
        for (var type : EssenceType.values()) {
            int index = type.ordinal();
            var slot = machine.supplies.get(index);
            if (slot.isEmpty()) continue;
            var input = inputs.stream().filter(value -> value.canInput(type, slot.getStack())).findFirst().orElse(null);
            if (input == null) {
                if (machine.storeOutput(slot.getStack())) slot.setEmpty();
                continue;
            }
            int free = machine.capacities[index] - machine.observed[index];
            if (free <= 0) continue;
            // Native inputs can mutate components or use randomness. Evaluate only an actual tick, on a copy.
            ItemStack working = slot.getStack().copy();
            int amount = Math.min(free, Math.max(0, input.getInputValue(working, level.random).amount()));
            if (amount == 0) continue;
            ItemStack remainder = input.finishInput(working, amount);
            machine.observed[index] += amount;
            // Keep an emptied container here until output space exists, so nothing is lost when outputs fill.
            slot.setStackUnchecked(remainder);
            machine.markForSave();
        }
    }
    private record Work(Holder.Reference<Ritual> holder, int[] allocation, ItemStack output, EssencesDefinition cost) { }
    private Work select() {
        var stock = machine.stock.stream().map(slot -> slot.getStack()).toList();
        Work fallback = null;
        for (var holder : Recipes.rituals(machine.getLevel())) {
            if (!machine.recipeLock.isEmpty() && !machine.recipeLock.equals(holder.key().location().toString())) continue;
            Ritual ritual = holder.value();
            if (!supports(ritual)) continue;
            int[] allocation = Recipes.allocate(Recipes.ingredients(ritual), stock);
            if (allocation == null) continue;
            ItemStack output = ritual.result().getResultItem(stock.get(allocation[0]).copyWithCount(1));
            if (output.isEmpty()) continue;
            var work = new Work(holder, allocation, output.copy(), cost(ritual));
            if (fallback == null) fallback = work;
            if (problem(work) == Controller.IDLE) return work;
        }
        return fallback;
    }
    private int problem(Work work) {
        var requirements = work.holder.value().requirements();
        if (!requirements.tier().test(machine.nativeTier)) return Controller.NEED_TIER;
        if (!requirements.checkRequirements(machine.nativeTier, enhancers())) return Controller.NEED_ENHANCER;
        int[] statuses = {Controller.NEED_AUREAL, Controller.NEED_SOULS, Controller.NEED_BLOOD, Controller.NEED_EXPERIENCE};
        for (var type : EssenceType.values())
            if (machine.observed[type.ordinal()] < work.cost.get(type)) return statuses[type.ordinal()];
        return machine.canStore(work.output) ? Controller.IDLE : Controller.OUTPUT_FULL;
    }
    private CompoundTag signature(Work work) {
        var lookup = machine.getLevel().registryAccess();
        var tag = new CompoundTag();
        tag.putString("recipe", work.holder.key().location().toString());
        tag.putInt("duration", machine.duration);
        tag.putIntArray("cost", Arrays.stream(EssenceType.values()).mapToInt(work.cost::get).toArray());
        tag.put("output", work.output.save(lookup));
        var materials = new ListTag();
        for (int index : work.allocation) materials.add(machine.stock.get(index).getStack().copyWithCount(1).save(lookup));
        tag.put("materials", materials);
        var enhancers = new ListTag();
        for (var slot : machine.enhancers) enhancers.add(slot.getStack().saveOptional(lookup));
        tag.put("enhancers", enhancers);
        return tag;
    }
    public void tick() {
        Arrays.fill(machine.resourceRates, 0);
        if (!hasPlatform()) { machine.status = Controller.STRUCTURE; return; }
        if (!machine.enabled || !machine.canFunction()) { machine.status = Controller.PAUSED; return; }
        if (!hasEnergy(machine.energy().getEnergyPerTick())) { machine.status = Controller.NEED_ENERGY; return; }
        fillResources();
        generateResources();
        Work work = select();
        if (work == null) {
            resetProgress(); machine.selectedRecipe = ""; machine.status = Controller.NEED_MATERIALS; return;
        }
        machine.selectedRecipe = work.holder.key().location().toString();
        machine.duration = Math.max(1, MekanismUtils.getTicks(machine, work.holder.value().duration()));
        CompoundTag next = signature(work);
        if (!next.equals(signature)) { machine.progress = 0; signature = next; machine.markForSave(); }
        int problem = problem(work);
        if (problem != Controller.IDLE) { machine.status = problem; return; }
        if (!hasEnergy(machine.energy().getEnergyPerTick())) { machine.status = Controller.NEED_ENERGY; return; }
        machine.energy().extract(machine.energy().getEnergyPerTick(), Action.EXECUTE, AutomationType.INTERNAL);
        machine.status = Controller.RUNNING;
        machine.progress++;
        if (machine.progress >= machine.duration) {
            // This server tick owns the complete transaction; neither inputs nor resources were removed earlier.
            if (!machine.storeOutput(work.output)) { machine.status = Controller.OUTPUT_FULL; return; }
            for (int index : work.allocation) machine.stock.get(index).shrinkStack(1, Action.EXECUTE);
            work.cost.forEach((type, amount) -> machine.observed[type.ordinal()] -= amount);
            resetProgress();
        }
        machine.markForSave();
    }
    public void resetProgress() {
        machine.progress = 0; signature = null; machine.markForSave();
    }
    public void save(CompoundTag tag) {
        tag.putInt("forge_tier", machine.nativeTier);
        tag.putIntArray("essences", Arrays.copyOf(machine.observed, 4));
        tag.putInt("progress", machine.progress);
        for (int resource = 0; resource < 4; resource++) tag.putInt(MODULE_PROGRESS_KEYS[resource], moduleProgress[resource]);
        if (signature != null) tag.put("work", signature.copy());
    }
    public void load(CompoundTag tag) {
        machine.nativeTier = Math.clamp(tag.getInt("forge_tier"), 1, 5);
        refreshCapacities();
        int[] saved = tag.getIntArray("essences");
        for (int i = 0; i < 4; i++) machine.observed[i] = Math.clamp(i < saved.length ? saved[i] : 0, 0, machine.capacities[i]);
        signature = tag.contains("work", 10) ? tag.getCompound("work").copy() : null;
        machine.duration = signature == null ? 1 : Math.clamp(signature.getInt("duration"), 1, Integer.MAX_VALUE);
        machine.progress = signature == null ? 0 : Math.clamp(tag.getInt("progress"), 0, machine.duration - 1);
        for (int resource = 0; resource < 4; resource++) moduleProgress[resource] = Math.clamp(tag.getInt(MODULE_PROGRESS_KEYS[resource]), 0, 99);
    }
}
