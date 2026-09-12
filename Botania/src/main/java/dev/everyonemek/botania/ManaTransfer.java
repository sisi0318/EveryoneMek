package dev.everyonemek.botania;

import java.util.List;
import dev.everyonemek.botania.mixin.ManaPoolAccess;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;
import vazkii.botania.common.block.mana.ManaPoolBlock;

/** Transfers resources between real, bounded stores. No synthetic pool context. */
public final class ManaTransfer {
    public static final int RATE = 1000;
    public static ManaPoolBlockEntity pool(Level level, BlockPos position) {
        if (level == null || !level.hasChunkAt(position)) return null;
        var tile = level.getBlockEntity(position);
        return tile != null && !tile.isRemoved() && tile.getClass() == ManaPoolBlockEntity.class
              && tile.getBlockState().getBlock() instanceof ManaPoolBlock block && !block.isCreative() ? (ManaPoolBlockEntity) tile : null;
    }
    public static boolean canTake(ManaPoolBlockEntity pool) { return ((ManaPoolAccess) pool).botanicalmekanism$canSpare(); }
    public static boolean canGive(ManaPoolBlockEntity pool) { return ((ManaPoolAccess) pool).botanicalmekanism$canAccept(); }
    public static void tick(ManaMachine tile) {
        var pool = pool(tile.getLevel(), tile.targetPos());
        if (pool == null) { tile.status(ManaMachine.NO_POOL); return; }
        if (tile.kind() == ManaMachineKind.BRIDGE) bridge(tile, pool); else charger(tile, pool);
    }
    private static void bridge(ManaMachine tile, ManaPoolBlockEntity pool) {
        boolean take = tile.mode() == 0;
        long amount = Math.min(RATE, take ? Math.min(pool.getCurrentMana(), tile.mana().getNeeded())
              : Math.min(pool.getMaxMana() - pool.getCurrentMana(), tile.mana().getStored()));
        if (!(take ? canTake(pool) : canGive(pool))) { tile.status(ManaMachine.ITEM_DENIED); return; }
        if (amount <= 0) { tile.status(ManaMachine.READY); return; }
        if (!tile.spendEnergy(tile.energy().getEnergyPerTick())) { tile.status(ManaMachine.NO_ENERGY); return; }
        if (take) {
            int before = pool.getCurrentMana(); pool.receiveMana(-(int) amount);
            int taken = before - pool.getCurrentMana();
            var remainder = tile.mana().insert(new ChemicalStack(ManaContent.MANA, taken), Action.EXECUTE, AutomationType.INTERNAL);
            if (!remainder.isEmpty()) pool.receiveMana((int) remainder.getAmount());
        } else {
            int before = pool.getCurrentMana(); pool.receiveMana((int) amount);
            tile.mana().extract(pool.getCurrentMana() - before, Action.EXECUTE, AutomationType.INTERNAL);
        }
        pool.setChanged(); tile.markForSave(); tile.status(ManaMachine.WORKING);
    }
    private static void charger(ManaMachine tile, ManaPoolBlockEntity pool) {
        var source = tile.inputs.getFirst().getStack();
        if (source.isEmpty()) { tile.status(ManaMachine.NO_RECIPE); return; }
        if (source.getCount() != 1) { tile.status(ManaMachine.ITEM_DENIED); return; }
        var copy = source.copy(); var item = ManaItem.LOOKUP.find(copy);
        if (item == null || item.getMaxMana() <= 0 || item.getMana() < 0 || item.getMana() > item.getMaxMana()) { tile.status(ManaMachine.ITEM_DENIED); return; }
        boolean charge = tile.mode() == 0;
        int target = (int) ((long) item.getMaxMana() * tile.targetPercent() / 100);
        if (charge ? item.getMana() >= target : item.getMana() <= target) {
            var merged = tile.mergeOutputs(List.of(copy));
            if (merged == null) { tile.status(ManaMachine.OUTPUT_FULL); return; }
            tile.inputs.getFirst().setStackUnchecked(net.minecraft.world.item.ItemStack.EMPTY); tile.setOutputs(merged); tile.markForSave(); tile.status(ManaMachine.READY); return;
        }
        if (tile.mergeOutputs(List.of(copy)) == null) { tile.status(ManaMachine.OUTPUT_FULL); return; }
        if (charge ? !canTake(pool) || !item.canReceiveManaFromPool(pool)
              : !canGive(pool) || item.isNoExport() || !item.canDrainManaToPool(pool)) { tile.status(ManaMachine.ITEM_DENIED); return; }
        int amount = Math.min(RATE, charge ? Math.min(target - item.getMana(), pool.getCurrentMana())
              : Math.min(item.getMana() - target, pool.getMaxMana() - pool.getCurrentMana()));
        if (amount <= 0) { tile.status(ManaMachine.NO_MANA); return; }
        int before = item.getMana(); item.addMana(charge ? amount : -amount);
        int moved = charge ? item.getMana() - before : before - item.getMana();
        // A rejected or invalid item mutation never touches the original item or pool.
        if (moved <= 0 || moved > amount) { tile.status(ManaMachine.ITEM_DENIED); return; }
        if (!tile.spendEnergy(tile.energy().getEnergyPerTick())) { tile.status(ManaMachine.NO_ENERGY); return; }
        pool.receiveMana(charge ? -moved : moved); pool.setChanged();
        tile.inputs.getFirst().setStackUnchecked(copy); tile.markForSave(); tile.status(ManaMachine.WORKING);
    }
    private ManaTransfer() { }
}
