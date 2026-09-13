package dev.everyonemek.botania;

import mekanism.api.*;
import mekanism.api.chemical.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

/** A live view of an existing store. Saved resources never belong to the adapter. */
public record ManaAccess(Level level, BlockPos pos, Direction side, BlockEntity original) {
    public static ManaAccess at(Level level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return null;
        var tile = level.getBlockEntity(pos);
        return tile instanceof ManaMachine machine && machine.kind().chemical || ManaTransfer.pool(level, pos) != null
              ? new ManaAccess(level, pos.immutable(), side, tile) : null;
    }
    public boolean live() { return level.hasChunkAt(pos) && level.getBlockEntity(pos) == original && !original.isRemoved(); }
    private IChemicalHandler handler() { return live() ? level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), pos, side) : null; }
    public long stored() {
        if (!live()) return 0;
        if (original instanceof ManaPoolBlockEntity pool) return pool.getCurrentMana();
        var handler = handler(); long amount = 0;
        if (handler != null) for (int i = 0; i < handler.getChemicalTanks(); i++) if (handler.getChemicalInTank(i).is(ManaContent.MANA)) amount += handler.getChemicalInTank(i).getAmount();
        return amount;
    }
    public long insert(long amount, boolean simulate) {
        if (amount <= 0 || !live()) return 0;
        if (original instanceof ManaPoolBlockEntity pool) {
            if (!ManaTransfer.canGive(pool)) return 0;
            int accepted = (int) Math.min(amount, pool.getMaxMana() - pool.getCurrentMana());
            if (!simulate && accepted > 0) { pool.receiveMana(accepted); pool.setChanged(); } return accepted;
        }
        var handler = handler(); if (handler == null) return 0;
        long wanted = Math.min(amount, ManaMachine.MANA_CAPACITY);
        return wanted - handler.insertChemical(new ChemicalStack(ManaContent.MANA, wanted), simulate ? Action.SIMULATE : Action.EXECUTE).getAmount();
    }
    public long extract(long amount, boolean simulate) {
        if (amount <= 0 || !live()) return 0;
        if (original instanceof ManaPoolBlockEntity pool) {
            if (!ManaTransfer.canTake(pool)) return 0;
            int taken = (int) Math.min(amount, pool.getCurrentMana());
            if (!simulate && taken > 0) { pool.receiveMana(-taken); pool.setChanged(); } return taken;
        }
        var handler = handler(); return handler == null ? 0 : handler.extractChemical(new ChemicalStack(ManaContent.MANA, Math.min(amount, ManaMachine.MANA_CAPACITY)),
              simulate ? Action.SIMULATE : Action.EXECUTE).getAmount();
    }
    public long refund(long amount) {
        if (amount <= 0 || !live()) return 0;
        if (original instanceof ManaPoolBlockEntity pool) {
            int accepted = (int) Math.min(amount, pool.getMaxMana() - pool.getCurrentMana()); pool.receiveMana(accepted); pool.setChanged(); return accepted;
        }
        var machine = (ManaMachine) original;
        return amount - machine.mana().insert(new ChemicalStack(ManaContent.MANA, amount), Action.EXECUTE, AutomationType.INTERNAL).getAmount();
    }
}
