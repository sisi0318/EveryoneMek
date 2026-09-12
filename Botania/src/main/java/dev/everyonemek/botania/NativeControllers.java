package dev.everyonemek.botania;

import java.util.*;
import dev.everyonemek.botania.mixin.EnchanterAccess;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.recipe.ElvenTradeRecipe;
import vazkii.botania.api.state.BotaniaStateProperties;
import vazkii.botania.api.state.enums.AlfheimPortalState;
import vazkii.botania.common.block.block_entity.AlfheimPortalBlockEntity;
import vazkii.botania.common.block.block_entity.ManaEnchanterBlockEntity;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.xplat.BotaniaConfig;

public final class NativeControllers {
    private static final String JOB = "botanicalmekanism:enchant_job";
    public static List<ManaMachine> attached(BlockEntity nativeTile) {
        var level = nativeTile.getLevel(); List<ManaMachine> found = new ArrayList<>();
        if (level == null) return found;
        for (var side : Direction.values()) {
            var pos = nativeTile.getBlockPos().relative(side);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof ManaMachine machine && !machine.isRemoved()
                  && machine.kind().controller() && machine.targetPos().equals(nativeTile.getBlockPos())
                  && (nativeTile instanceof AlfheimPortalBlockEntity ? machine.kind() == ManaMachineKind.ELVEN : machine.kind() == ManaMachineKind.ENCHANTER)) found.add(machine);
        }
        return found;
    }
    public static boolean validEnchanter(ManaEnchanterBlockEntity tile) {
        if (!PlantSupport.areaLoaded(tile.getLevel(), tile.getBlockPos(), 6)) return false;
        var axis = tile.getBlockState().getValue(BotaniaStateProperties.ENCHANTER_DIRECTION);
        return EnchanterAccess.botanicalmekanism$formed().get().validate(tile.getLevel(), tile.getBlockPos().below(),
              axis == Direction.Axis.X ? Rotation.CLOCKWISE_90 : Rotation.NONE);
    }
    public static void tick(ManaMachine machine) {
        if (!PlantSupport.areaLoaded(machine.getLevel(), machine.targetPos(), 6)) { machine.status(ManaMachine.UNLOADED); return; }
        var target = machine.getLevel().getBlockEntity(machine.targetPos());
        if (target == null || (machine.kind() == ManaMachineKind.ELVEN ? target.getClass() != AlfheimPortalBlockEntity.class
              : target.getClass() != ManaEnchanterBlockEntity.class)) { machine.status(ManaMachine.STRUCTURE); return; }
        if (attached(target).size() != 1) { machine.status(ManaMachine.BUSY); return; }
        if (target instanceof AlfheimPortalBlockEntity portal) trade(machine, portal);
        else enchant(machine, (ManaEnchanterBlockEntity) target);
    }
    private static void trade(ManaMachine machine, AlfheimPortalBlockEntity portal) {
        var state = portal.getBlockState().getValue(BotaniaStateProperties.ALFPORTAL_STATE);
        if (state == AlfheimPortalState.OFF || portal.ticksOpen <= AlfheimPortalBlockEntity.TICKS_UNTIL_FULLY_OPENED
              || !AlfheimPortalBlockEntity.MULTIBLOCK.get().validate(machine.getLevel(), portal.getBlockPos(),
                    state == AlfheimPortalState.ON_X ? Rotation.CLOCKWISE_90 : Rotation.NONE)) { machine.status(ManaMachine.STRUCTURE); return; }
        if (machine.controller.getLong("next_trade") > machine.getLevel().getGameTime()) { machine.status(ManaMachine.READY); return; }
        var input = new ApothecaryWork.Input(machine.inputs.stream().map(slot -> slot.getStack().copy()).toList());
        ElvenTradeRecipe.AssemblyResult selected = null;
        // Match using the native counted-input contract and native ordering, including every output.
        for (var holder : machine.getLevel().getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.ELVEN_TRADE_TYPE)) {
            var recipe = holder.value();
            if (recipe.getClass() != vazkii.botania.common.crafting.ElvenTradeRecipe.class || recipe.isReturnRecipe()) continue;
            var result = recipe.tryAssemble(new ApothecaryWork.Input(input.getItems()), machine.getLevel().registryAccess()).orElse(null);
            if (result != null && !result.outputs().isEmpty() && (selected == null || result.compareTo(selected) < 0)) selected = result;
        }
        if (selected == null) { machine.status(ManaMachine.NO_RECIPE); return; }
        var merged = machine.mergeOutputs(selected.outputs()); if (merged == null) { machine.status(ManaMachine.OUTPUT_FULL); return; }
        var pylons = portal.locatePylons(true);
        if (pylons.size() < AlfheimPortalBlockEntity.MIN_REQUIRED_PYLONS) { machine.status(ManaMachine.STRUCTURE); return; }
        int perPool = Math.max(1, AlfheimPortalBlockEntity.MANA_COST / pylons.size());
        for (var pos : pylons) {
            var pool = ManaTransfer.pool(machine.getLevel(), pos.below());
            if (pool == null || pool.getCurrentMana() < perPool) { machine.status(ManaMachine.NO_MANA); return; }
        }
        for (var entry : selected.matchedInputSlots().int2IntEntrySet()) {
            if (entry.getIntKey() < 0 || entry.getIntKey() >= machine.inputs.size() || entry.getIntValue() <= 0
                  || machine.inputs.get(entry.getIntKey()).getCount() < entry.getIntValue()) { machine.status(ManaMachine.NO_RECIPE); return; }
        }
        if (machine.energy().extract(machine.energy().getEnergyPerTick(), Action.SIMULATE, AutomationType.INTERNAL) != machine.energy().getEnergyPerTick()) {
            machine.status(ManaMachine.NO_ENERGY); return;
        }
        // The real portal alone debits its real pools, including its native split/rounding rules.
        if (!portal.consumeMana(pylons, AlfheimPortalBlockEntity.MANA_COST, false)) { machine.status(ManaMachine.NO_MANA); return; }
        machine.spendEnergy(machine.energy().getEnergyPerTick());
        for (var entry : selected.matchedInputSlots().int2IntEntrySet()) machine.inputs.get(entry.getIntKey()).shrinkStack(entry.getIntValue(), Action.EXECUTE);
        machine.setOutputs(merged); selected.outputs().forEach(stack -> stack.onCraftedBySystem(machine.getLevel()));
        machine.controller.putLong("next_trade", machine.getLevel().getGameTime() + 4); machine.markForSave(); portal.setChanged(); machine.status(ManaMachine.WORKING);
    }
    private static void enchant(ManaMachine machine, ManaEnchanterBlockEntity enchanter) {
        if (!BotaniaConfig.common().enchanterEnabled() || !validEnchanter(enchanter)) { machine.status(ManaMachine.STRUCTURE); return; }
        var held = enchanter.itemToEnchant;
        UUID job = machine.controller.hasUUID("job") ? machine.controller.getUUID("job") : null;
        if (!held.isEmpty()) {
            var data = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (job == null || !data.hasUUID(JOB) || !job.equals(data.getUUID(JOB))) { machine.status(ManaMachine.BUSY); return; }
            if (enchanter.stage == ManaEnchanterBlockEntity.State.IDLE) {
                var product = held.copy(); data.remove(JOB);
                if (data.isEmpty()) product.remove(DataComponents.CUSTOM_DATA); else product.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                var merged = machine.mergeOutputs(List.of(product)); if (merged == null) { machine.status(ManaMachine.OUTPUT_FULL); return; }
                if (!machine.spendEnergy(machine.energy().getEnergyPerTick())) { machine.status(ManaMachine.NO_ENERGY); return; }
                enchanter.itemToEnchant = ItemStack.EMPTY; enchanter.sync(); machine.setOutputs(merged);
                machine.controller.remove("job"); machine.markForSave(); machine.status(ManaMachine.READY); return;
            }
            if (enchanter.stage == ManaEnchanterBlockEntity.State.GATHER_MANA) {
                int amount = (int) Math.min(1000, Math.min(machine.mana().getStored(), enchanter.getAvailableSpaceForMana()));
                if (amount > 0) {
                    int before = enchanter.getCurrentMana(); enchanter.receiveMana(amount);
                    machine.mana().extract(enchanter.getCurrentMana() - before, Action.EXECUTE, AutomationType.INTERNAL); enchanter.sync();
                }
                machine.status(enchanter.getAvailableSpaceForMana() > 0 && machine.mana().isEmpty() ? ManaMachine.NO_MANA : ManaMachine.WORKING);
            } else machine.status(ManaMachine.WORKING);
            return;
        }
        var input = machine.inputs.getFirst().getStack();
        if (input.isEmpty() || input.is(Items.BOOK) || !input.isEnchantable() || input.getCount() != 1) { machine.status(ManaMachine.NO_RECIPE); return; }
        if (enchanter.stage != ManaEnchanterBlockEntity.State.IDLE || machine.outputs.getFirst().getCount() != 0) { machine.status(ManaMachine.OUTPUT_FULL); return; }
        if (machine.energy().extract(machine.energy().getEnergyPerTick(), Action.SIMULATE, AutomationType.INTERNAL) != machine.energy().getEnergyPerTick()) {
            machine.status(ManaMachine.NO_ENERGY); return;
        }
        var marked = input.copy(); var data = marked.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.contains(JOB)) { machine.status(ManaMachine.ITEM_DENIED); return; }
        UUID newJob = UUID.randomUUID(); data.putUUID(JOB, newJob); marked.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        enchanter.itemToEnchant = marked;
        // The native wand entry and subsequent native ticks decide valid/conflicting enchantments and exact cost.
        if (!enchanter.onUsedByWand(null, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST), Direction.UP)) {
            enchanter.itemToEnchant = ItemStack.EMPTY; machine.status(ManaMachine.NO_EXTRA); return;
        }
        machine.spendEnergy(machine.energy().getEnergyPerTick()); machine.inputs.getFirst().setStackUnchecked(ItemStack.EMPTY);
        machine.controller.putUUID("job", newJob); machine.markForSave(); enchanter.sync(); machine.status(ManaMachine.WORKING);
    }
    /** Read-only book views: never spawned, saved, dropped, or consumed as a second inventory. */
    public static List<ItemEntity> books(ManaEnchanterBlockEntity enchanter, List<ItemEntity> worldBooks) {
        var attached = attached(enchanter);
        if (attached.size() != 1 || !attached.getFirst().canFunction() || !validEnchanter(enchanter)) return worldBooks;
        var result = new ArrayList<>(worldBooks);
        for (var slot : attached.getFirst().extras) if (slot.getStack().is(Items.ENCHANTED_BOOK)) {
            var pos = enchanter.getBlockPos(); result.add(new ItemEntity(enchanter.getLevel(), pos.getX(), pos.getY(), pos.getZ(), slot.getStack().copyWithCount(1)));
        }
        return result;
    }
    private NativeControllers() { }
}
