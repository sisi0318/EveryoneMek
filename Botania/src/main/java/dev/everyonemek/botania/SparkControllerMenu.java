package dev.everyonemek.botania;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class SparkControllerMenu extends AbstractContainerMenu {
    public static final int CHANNEL_SLOT = 38;
    private final MechanicalSparkEntity anchor, master;
    private final Container modules;
    private final ContainerData stats;
    public SparkControllerMenu(int id, Inventory inventory, int anchorId, int masterId) {
        super(MechanicalSparks.MENU.get(), id);
        var level = inventory.player.level();
        anchor = level.getEntity(anchorId) instanceof MechanicalSparkEntity spark ? spark : null;
        master = level.getEntity(masterId) instanceof MechanicalSparkEntity spark ? spark : null;
        modules = !level.isClientSide && master != null ? master.modules : new SimpleContainer(3);
        stats = level.isClientSide ? new SimpleContainerData(9) : new ContainerData() {
            @Override public int get(int index) {
                var net = anchor == null ? null : MechanicalSparkNetworks.network(anchor);
                if (net == null) return 0;
                if (index >= 4 && master != null) {
                    var me = master.meLink().stats();
                    return switch (index) { case 4 -> me.capacity(); case 5 -> me.used(); case 6 -> me.links(); case 7 -> me.state(); default -> me.power(); };
                }
                return switch (index) { case 0 -> net.range(); case 1 -> net.multiplier(); case 2 -> net.members(); default -> net.conflict() ? 2 : net.master() == null ? 1 : 0; };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 9; }
        };
        addDataSlots(stats);
        for (int i = 0; i < 2; i++) {
            final int slot = i;
            addSlot(new Slot(modules, i, 35 + i * 45, 32) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(MechanicalSparks.module(slot)); }
                @Override public int getMaxStackSize() { return 8; }
                @Override public int getMaxStackSize(ItemStack stack) { return 8; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 131 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 189));
        // Append after the existing menu slots, so previous player-slot indices stay valid.
        addSlot(new Slot(modules, 2, 125, 32) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(MechanicalSparks.CHANNEL.get()); }
            @Override public int getMaxStackSize() { return 4; }
            @Override public int getMaxStackSize(ItemStack stack) { return 4; }
        });
    }
    public int stat(int index) { return stats.get(index); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (player.level().isClientSide || player.containerMenu == this && stillValid(player)) super.clicked(slot, button, type, player);
    }
    @Override public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true;
        return anchor != null && master != null && master.isMaster() && anchor.distanceToSqr(player) <= 64 && anchor.accessible(player) && master.accessible(player)
              && (anchor == master || MechanicalSparkNetworks.network(anchor).master() == master);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (player.containerMenu != this || !stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var before = stack.copy();
        if (index < 2 || index == CHANNEL_SLOT) { if (!moveItemStackTo(stack, 2, CHANNEL_SLOT, true)) return ItemStack.EMPTY; }
        else {
            int target = MechanicalSparks.moduleSlot(stack); if (target == 2) target = CHANNEL_SLOT;
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); return before;
    }
}
