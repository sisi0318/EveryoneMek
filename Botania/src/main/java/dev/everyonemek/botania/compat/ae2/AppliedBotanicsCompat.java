package dev.everyonemek.botania.compat.ae2;

import appbot.ae2.SafeMana;
import appeng.api.config.Actionable;
import dev.everyonemek.botania.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

public final class AppliedBotanicsCompat {
    public static java.util.List<net.minecraft.network.chat.Component> cellTooltip(net.minecraft.world.item.ItemStack stack) {
        if (!(stack.getItem() instanceof appbot.item.cell.IManaCellItem cell)) return java.util.List.of();
        long stored = Math.max(0, stack.getOrDefault(appbot.AppliedBotanicsForge.MANA.get(), 0L));
        return ManaStorageItem.cellTooltip(stored, ManaCellCapacity.maximum(cell.getTotalBytes()));
    }
    public static appeng.api.stacks.AEKey poolKey() { return appbot.ae2.ManaKey.KEY; }
    public static int insert(ManaPoolBlockEntity pool, int amount, boolean simulate) { return ((SafeMana) pool).insert(amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE); }
    public static int extract(ManaPoolBlockEntity pool, int amount, boolean simulate) { return ((SafeMana) pool).extract(amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE); }
    public static ManaReceiver receiver(ManaMachine machine, Direction side) {
        return machine.kind() == ManaMachineKind.GREENHOUSE ? new GreenhousePort(machine, side) : new MachinePort(machine, side == null ? Direction.UP : side);
    }
    public static final class GreenhousePort extends GreenhouseSparkPort implements SafeMana {
        public GreenhousePort(ManaMachine machine, Direction side) { super(machine, side); }
        @Override public int insert(int amount, Actionable action) { var access = access(); return access == null ? 0 : (int) access.insert(amount, action == Actionable.SIMULATE); }
        @Override public int extract(int amount, Actionable action) { var access = access(); return access == null ? 0 : (int) access.extract(amount, action == Actionable.SIMULATE); }
    }
    public record MachinePort(ManaMachine machine, Direction side) implements ManaReceiver, SafeMana {
        private MachineSparkPort port() { return new MachineSparkPort(machine, side); }
        @Override public Level getManaReceiverLevel() { return machine.getLevel(); }
        @Override public BlockPos getManaReceiverPos() { return machine.getBlockPos(); }
        @Override public int getCurrentMana() { return port().getCurrentMana(); }
        @Override public boolean isFull() { return port().isFull(); }
        @Override public boolean canReceiveManaFromBursts() { return port().canReceiveManaFromBursts(); }
        @Override public void receiveMana(int mana) { port().receiveMana(mana); }
        @Override public int insert(int amount, Actionable action) {
            var access = ManaAccess.at(machine.getLevel(), machine.getBlockPos(), side);
            return access == null || access.original() != machine ? 0 : (int) access.insert(amount, action == Actionable.SIMULATE);
        }
        @Override public int extract(int amount, Actionable action) {
            var access = ManaAccess.at(machine.getLevel(), machine.getBlockPos(), side);
            return access == null || access.original() != machine ? 0 : (int) access.extract(amount, action == Actionable.SIMULATE);
        }
    }
    private AppliedBotanicsCompat() { }
}
