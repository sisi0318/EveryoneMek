package dev.everyonemek.botania.compat.ae2;

import appeng.api.behaviors.*;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import dev.everyonemek.botania.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class ManaBusStorage implements MEStorage, StackImportStrategy, StackExportStrategy {
    private final ManaAccess access;
    private final Runnable changed;
    private final boolean extractableOnly;
    public ManaBusStorage(ManaAccess access, boolean extractableOnly, Runnable changed) { this.access = access; this.extractableOnly = extractableOnly; this.changed = changed; }
    public static LiveBus at(ServerLevel level, BlockPos pos, Direction side) { return new LiveBus(level, pos.immutable(), side); }
    public record LiveBus(ServerLevel level, BlockPos pos, Direction side) implements StackImportStrategy, StackExportStrategy {
        private ManaBusStorage current() { return new ManaBusStorage(ManaAccess.at(level, pos, side), false, () -> {}); }
        @Override public boolean transfer(StackTransferContext context) { return current().transfer(context); }
        @Override public long transfer(StackTransferContext context, AEKey key, long amount) { return current().transfer(context, key, amount); }
        @Override public long push(AEKey key, long amount, Actionable mode) { return current().push(key, amount, mode); }
    }
    @Override public Component getDescription() { return ManaKeys.current().getDisplayName(); }
    @Override public void getAvailableStacks(KeyCounter out) {
        if (access != null) {
            long amount = extractableOnly ? access.extract(Long.MAX_VALUE, true) : access.stored();
            if (amount > 0) out.add(ManaKeys.current(), amount);
        }
    }
    @Override public long insert(AEKey key, long amount, Actionable action, IActionSource source) {
        if (access == null || key != ManaKeys.current()) return 0;
        long moved = access.insert(amount, action == Actionable.SIMULATE); if (moved > 0 && action == Actionable.MODULATE) changed.run(); return moved;
    }
    @Override public long extract(AEKey key, long amount, Actionable action, IActionSource source) {
        if (access == null || key != ManaKeys.current()) return 0;
        long moved = access.extract(amount, action == Actionable.SIMULATE); if (moved > 0 && action == Actionable.MODULATE) changed.run(); return moved;
    }
    private void recover(long amount) {
        if (amount <= 0 || access == null) return;
        var pos = access.pos();
        net.minecraft.world.Containers.dropItemStack(access.level(), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, ManaStorageItem.recovery(amount));
    }
    @Override public boolean transfer(StackTransferContext context) {
        if (access == null || !context.hasOperationsLeft() || !context.isKeyTypeEnabled(ManaKeys.type())
              || context.isInFilter(ManaKeys.current()) == context.isInverted()) return false;
        var inventory = context.getInternalStorage().getInventory(); var source = context.getActionSource();
        long limit = Math.min(context.getOperationsRemaining() * (long) ManaKeys.type().getAmountPerOperation(), access.extract(Long.MAX_VALUE, true));
        long accepted = StorageHelper.poweredInsert(context.getEnergySource(), inventory, ManaKeys.current(), limit, source, Actionable.SIMULATE);
        if (accepted <= 0) return false;
        long taken = access.extract(accepted, false);
        long inserted = StorageHelper.poweredInsert(context.getEnergySource(), inventory, ManaKeys.current(), taken, source, Actionable.MODULATE);
        if (inserted < taken) recover(taken - inserted - access.refund(taken - inserted));
        if (inserted > 0) context.reduceOperationsRemaining(Math.max(1, inserted / ManaKeys.type().getAmountPerOperation()));
        return inserted > 0;
    }
    @Override public long transfer(StackTransferContext context, AEKey key, long amount) {
        if (access == null || key != ManaKeys.current() || amount <= 0 || !context.isKeyTypeEnabled(ManaKeys.type())) return 0;
        var inventory = context.getInternalStorage().getInventory(); var source = context.getActionSource();
        long available = StorageHelper.poweredExtraction(context.getEnergySource(), inventory, key, amount, source, Actionable.SIMULATE);
        long accepted = access.insert(available, true);
        if (accepted <= 0) return 0;
        long taken = StorageHelper.poweredExtraction(context.getEnergySource(), inventory, key, accepted, source, Actionable.MODULATE);
        long inserted = access.insert(taken, false);
        if (inserted < taken) recover(taken - inserted - inventory.insert(key, taken - inserted, Actionable.MODULATE, source));
        return inserted;
    }
    @Override public long push(AEKey key, long amount, Actionable mode) { return insert(key, amount, mode, IActionSource.empty()); }
}
