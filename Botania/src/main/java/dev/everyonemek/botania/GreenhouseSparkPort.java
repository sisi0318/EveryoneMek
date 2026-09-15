package dev.everyonemek.botania;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaPool;

/** Only the generating greenhouse advertises a pool source; its configured face owns all IO. */
public class GreenhouseSparkPort implements ManaPool {
    protected final ManaMachine machine;
    protected final Direction side;
    public GreenhouseSparkPort(ManaMachine machine, Direction side) { this.machine = machine; this.side = side == null ? Direction.UP : side; }
    protected ManaAccess access() {
        if (!Flowers.live(machine)) return null;
        var access = ManaAccess.at(machine.getLevel(), machine.getBlockPos(), side);
        return access != null && access.original() == machine ? access : null;
    }
    @Override public Level getManaReceiverLevel() { return machine.getLevel(); }
    @Override public BlockPos getManaReceiverPos() { return machine.getBlockPos(); }
    @Override public int getMaxMana() { return ManaMachine.MANA_CAPACITY; }
    @Override public int getCurrentMana() { var access = access(); return access == null ? 0 : (int) access.extract(Integer.MAX_VALUE, true); }
    @Override public boolean isFull() { return new MachineSparkPort(machine, side).isFull(); }
    @Override public boolean canReceiveManaFromBursts() { return new MachineSparkPort(machine, side).canReceiveManaFromBursts(); }
    @Override public boolean isOutputtingPower() { return true; }
    @Override public void receiveMana(int amount) {
        if (machine.getLevel() == null || machine.getLevel().isClientSide) return;
        var access = access(); if (access == null) return;
        if (amount > 0) access.insert(amount, false); else if (amount < 0) access.extract(-(long) amount, false);
    }
}
