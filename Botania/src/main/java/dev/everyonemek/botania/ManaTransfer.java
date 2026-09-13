package dev.everyonemek.botania;

import java.util.List;
import dev.everyonemek.botania.mixin.ManaPoolAccess;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;
import vazkii.botania.common.block.mana.ManaPoolBlock;

/** Transfers resources through the original pools and machine stores. */
public final class ManaTransfer {
    public static final int RATE = 1000;
    public static ManaPoolBlockEntity pool(Level level, BlockPos position) {
        if (level == null || !level.hasChunkAt(position)) return null;
        var tile = level.getBlockEntity(position);
        return tile != null && !tile.isRemoved() && (tile.getClass() == ManaPoolBlockEntity.class || AppliedBotanics.isPool(tile))
              && tile.getBlockState().getBlock() instanceof ManaPoolBlock ? (ManaPoolBlockEntity) tile : null;
    }
    public static boolean canTake(ManaPoolBlockEntity pool) { return ((ManaPoolAccess) pool).botanicalmekanism$canSpare(); }
    public static boolean canGive(ManaPoolBlockEntity pool) { return ((ManaPoolAccess) pool).botanicalmekanism$canAccept(); }
    public static int space(ManaPoolBlockEntity pool) {
        if (!Flowers.live(pool) || !canGive(pool)) return 0;
        return AppliedBotanics.isPool(pool) ? dev.everyonemek.botania.compat.ae2.AppliedBotanicsCompat.insert(pool, Integer.MAX_VALUE, true)
              : Math.max(0, pool.getMaxMana() - pool.getCurrentMana());
    }
    public static int give(ManaPoolBlockEntity pool, int amount, boolean simulate) {
        if (amount <= 0 || !Flowers.live(pool) || !canGive(pool)) return 0;
        if (AppliedBotanics.isPool(pool)) return dev.everyonemek.botania.compat.ae2.AppliedBotanicsCompat.insert(pool, amount, simulate);
        int accepted = Math.min(amount, space(pool));
        if (simulate || accepted == 0) return accepted;
        int before = pool.getCurrentMana(); pool.receiveMana(accepted); return Math.max(0, pool.getCurrentMana() - before);
    }
    public static boolean creative(ManaPoolBlockEntity pool) { return pool.getBlockState().getBlock() instanceof ManaPoolBlock block && block.isCreative(); }
    public static int take(ManaPoolBlockEntity pool, int amount, boolean simulate) {
        if (amount <= 0 || !canTake(pool) || !Flowers.live(pool)) return 0;
        if (AppliedBotanics.isPool(pool)) return dev.everyonemek.botania.compat.ae2.AppliedBotanicsCompat.extract(pool, amount, simulate);
        int taken = Math.min(amount, Math.max(0, pool.getCurrentMana()));
        // The Everlasting Pool always reports full. Its supply does not reduce that value.
        if (simulate || creative(pool)) return taken;
        int before = pool.getCurrentMana(); pool.receiveMana(-taken);
        return Math.max(0, before - pool.getCurrentMana());
    }
    public static int refund(ManaPoolBlockEntity pool, int amount) {
        if (amount <= 0 || !Flowers.live(pool)) return 0;
        if (AppliedBotanics.isPool(pool)) return dev.everyonemek.botania.compat.ae2.AppliedBotanicsCompat.insert(pool, amount, false);
        if (creative(pool)) return amount;
        int before = pool.getCurrentMana();
        pool.receiveMana(Math.min(amount, Math.max(0, pool.getMaxMana() - before)));
        return Math.max(0, pool.getCurrentMana() - before);
    }
    /** Adjacent pool IO shares one budget per machine and world tick; draining chargers only export. */
    public static int fillFromAdjacentPools(ManaMachine tile) {
        if (!tile.kind().chemical || !Flowers.live(tile) || tile.getLevel().isClientSide) return 0;
        if (tile.kind() == ManaMachineKind.CHARGER && tile.mode() == 1) return drainToAdjacentPools(tile);
        if (tile.mana().getNeeded() == 0) return 0;
        int moved = 0;
        var sides = RelativeSide.values();
        int start = (int) Math.floorMod(tile.getLevel().getGameTime(), sides.length);
        for (int offset = 0; offset < sides.length && tile.poolPullRemaining() > 0; offset++) {
            var direction = sides[(start + offset) % sides.length].getDirection(tile.getDirection());
            var pos = tile.getBlockPos().relative(direction);
            var pool = pool(tile.getLevel(), pos);
            if (pool == null || !canTake(pool) || pool.getCurrentMana() <= 0) continue;
            var receiver = tile.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), tile.getBlockPos(), direction);
            if (receiver == null) continue;
            int wanted = Math.min(tile.poolPullRemaining(), pool.getCurrentMana());
            int accepted = wanted - (int) receiver.insertChemical(new ChemicalStack(ManaContent.MANA, wanted), Action.SIMULATE).getAmount();
            if (accepted <= 0) continue;
            int taken = take(pool, accepted, false);
            if (taken <= 0) continue;
            int remainder = (int) receiver.insertChemical(new ChemicalStack(ManaContent.MANA, taken), Action.EXECUTE).getAmount();
            if (remainder > 0) refund(pool, remainder);
            tile.recordPoolPull(taken - remainder); moved += taken - remainder;
        }
        return moved;
    }
    public static void tick(ManaMachine tile) {
        if (tile.kind() == ManaMachineKind.CHARGER) { chargerBuffer(tile); return; }
        var pool = pool(tile.getLevel(), tile.targetPos());
        if (pool == null) { tile.status(ManaMachine.NO_POOL); return; }
        bridge(tile, pool);
    }
    private static int drainToAdjacentPools(ManaMachine tile) {
        var config = tile.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.CHEMICAL);
        if (tile.mana().isEmpty() || config == null || !config.isEjecting()) return 0;
        int moved = 0; var sides = RelativeSide.values();
        int start = (int) Math.floorMod(tile.getLevel().getGameTime(), sides.length);
        for (int i = 0; i < sides.length && tile.poolPullRemaining() > 0; i++) {
            var direction = sides[(start + i) % sides.length].getDirection(tile.getDirection());
            var target = pool(tile.getLevel(), tile.getBlockPos().relative(direction));
            if (target == null) continue;
            var source = ManaAccess.at(tile.getLevel(), tile.getBlockPos(), direction);
            int available = source == null ? 0 : (int) source.extract(tile.poolPullRemaining(), true);
            int accepted = give(target, available, true);
            if (accepted <= 0) continue;
            int taken = (int) source.extract(accepted, false);
            int delivered = give(target, taken, false);
            if (delivered < taken) source.refund(taken - delivered);
            tile.recordPoolPull(delivered); moved += delivered;
        }
        return moved;
    }
    private static void bridge(ManaMachine tile, ManaPoolBlockEntity pool) {
        boolean take = tile.mode() == 0;
        long amount = Math.min(take ? tile.poolPullRemaining() : RATE, take ? Math.min(pool.getCurrentMana(), tile.mana().getNeeded())
              : Math.min(space(pool), tile.mana().getStored()));
        if (!(take ? canTake(pool) : canGive(pool))) { tile.status(ManaMachine.ITEM_DENIED); return; }
        if (amount <= 0) { tile.status(ManaMachine.READY); return; }
        if (!tile.spendEnergy(tile.energy().getEnergyPerTick())) { tile.status(ManaMachine.NO_ENERGY); return; }
        if (take) {
            int taken = take(pool, (int) amount, false);
            var remainder = tile.mana().insert(new ChemicalStack(ManaContent.MANA, taken), Action.EXECUTE, AutomationType.INTERNAL);
            if (!remainder.isEmpty()) refund(pool, (int) remainder.getAmount());
            tile.recordPoolPull(taken - (int) remainder.getAmount());
        } else {
            tile.mana().extract(give(pool, (int) amount, false), Action.EXECUTE, AutomationType.INTERNAL);
        }
        pool.setChanged(); tile.markForSave(); tile.status(ManaMachine.WORKING);
    }
    private static void chargerBuffer(ManaMachine tile) {
        var source = tile.inputs.getFirst().getStack();
        if (source.isEmpty()) { tile.status(ManaMachine.NO_RECIPE); return; }
        if (source.getCount() != 1) { tile.status(ManaMachine.ITEM_DENIED); return; }
        var copy = source.copy(); var item = ManaItem.LOOKUP.find(copy);
        if (item == null || item.getMaxMana() <= 0 || item.getMana() < 0 || item.getMana() > item.getMaxMana()) { tile.status(ManaMachine.ITEM_DENIED); return; }
        boolean charge = tile.mode() == 0; int target = (int) ((long) item.getMaxMana() * tile.targetPercent() / 100);
        var merged = tile.mergeOutputs(List.of(copy));
        if (merged == null) { tile.status(ManaMachine.OUTPUT_FULL); return; }
        if (charge ? item.getMana() >= target : item.getMana() <= target) {
            tile.inputs.getFirst().setStackUnchecked(net.minecraft.world.item.ItemStack.EMPTY); tile.setOutputs(merged); tile.markForSave(); tile.status(ManaMachine.READY); return;
        }
        if (charge ? !item.canReceiveManaFromPool(tile) : item.isNoExport() || !item.canDrainManaToPool(tile)) { tile.status(ManaMachine.ITEM_DENIED); return; }
        int amount = (int) Math.min(RATE, charge ? Math.min(target - item.getMana(), tile.mana().getStored())
              : Math.min(item.getMana() - target, tile.mana().getNeeded()));
        if (amount <= 0) { tile.status(charge ? ManaMachine.NO_MANA : ManaMachine.MANA_FULL); return; }
        int before = item.getMana(); item.addMana(charge ? amount : -amount);
        int moved = charge ? item.getMana() - before : before - item.getMana();
        if (moved <= 0 || moved > amount) { tile.status(ManaMachine.ITEM_DENIED); return; }
        if (!tile.spendEnergy(tile.energy().getEnergyPerTick())) { tile.status(ManaMachine.NO_ENERGY); return; }
        tile.receiveMana(charge ? -moved : moved);
        tile.inputs.getFirst().setStackUnchecked(copy); tile.markForSave(); tile.status(ManaMachine.WORKING);
    }
    private ManaTransfer() { }
}
