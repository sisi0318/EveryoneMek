package dev.everyonemek.botania;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class SparkControllerMenu extends AbstractContainerMenu {
    private final MechanicalSparkEntity anchor, master;
    private final Container modules;
    private final ContainerData stats;
    public SparkControllerMenu(int id, Inventory inventory, int anchorId, int masterId) {
        super(MechanicalSparks.MENU.get(), id);
        var level = inventory.player.level();
        anchor = level.getEntity(anchorId) instanceof MechanicalSparkEntity spark ? spark : null;
        master = level.getEntity(masterId) instanceof MechanicalSparkEntity spark ? spark : null;
        modules = !level.isClientSide && master != null ? master.modules : new SimpleContainer(2);
        stats = level.isClientSide ? new SimpleContainerData(4) : new ContainerData() {
            @Override public int get(int index) {
                var net = anchor == null ? null : MechanicalSparkNetworks.network(anchor);
                if (net == null) return 0;
                return switch (index) { case 0 -> net.range(); case 1 -> net.multiplier(); case 2 -> net.members(); default -> net.conflict() ? 2 : net.master() == null ? 1 : 0; };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 4; }
        };
        addDataSlots(stats);
        for (int i = 0; i < 2; i++) {
            final int slot = i;
            addSlot(new Slot(modules, i, 53 + i * 54, 32) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(slot == 0 ? MechanicalSparks.RANGE.get() : MechanicalSparks.EFFICIENCY.get()); }
                @Override public int getMaxStackSize() { return 8; }
                @Override public int getMaxStackSize(ItemStack stack) { return 8; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 101 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 159));
    }
    public int stat(int index) { return stats.get(index); }
    @Override public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true;
        return anchor != null && master != null && master.isMaster() && anchor.distanceToSqr(player) <= 64 && anchor.accessible(player) && master.accessible(player)
              && (anchor == master || MechanicalSparkNetworks.network(anchor).master() == master);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var before = stack.copy();
        if (index < 2) { if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY; }
        else {
            int target = stack.is(MechanicalSparks.RANGE.get()) ? 0 : stack.is(MechanicalSparks.EFFICIENCY.get()) ? 1 : -1;
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); return before;
    }
}
