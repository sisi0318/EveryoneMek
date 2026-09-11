package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class MachineMenu extends MekanismTileContainer<Controller> {
    private final int nativeStart;
    public MachineMenu(int id, Inventory inventory, Controller tile) {
        super(Content.MENU, id, inventory, tile);
        nativeStart = slots.size();
        if (tile.kind().forge()) return;
        var handler = NativeInventory.menu(tile);
        for (int i = 0; i < handler.getSlots(); i++) {
            final int index = i;
            int[] coordinates = nativeCoordinates(tile.kind(), i);
            addSlot(new SlotItemHandler(handler, i, coordinates[0], coordinates[1]) {
                @Override public boolean mayPickup(Player player) {
                    return player.level().isClientSide || canConfigure(player) && NativeInventory.editable(tile, tile.binding.resolve(), index);
                }
                @Override public boolean mayPlace(ItemStack stack) {
                    return (inventory.player.level().isClientSide || canConfigure(inventory.player)) && handler.isItemValid(index, stack);
                }
                @Override public void setChanged() {
                    if (!tile.getLevel().isClientSide) {
                        var block = tile.binding.resolve();
                        if (NativeInventory.editable(tile, block, index)) { block.setStack(index, block.getStack(index).copy()); block.setChanged(); }
                    }
                    super.setChanged();
                }
            });
        }
    }
    public static int[] nativeCoordinates(MachineKind kind, int index) {
        return switch (index) {
            case 0 -> new int[]{112, 30};
            case 1 -> new int[]{112, 66};
            case 2 -> new int[]{130, 66};
            case 3 -> new int[]{148, 30};
            case 4 -> new int[]{166, 30};
            case 5 -> new int[]{148, 102};
            default -> new int[]{166, 102};
        };
    }
    @Override protected int getInventoryXOffset() { return 48; }
    @Override protected int getInventoryYOffset() { return 240; }
    public boolean canConfigure(Player player) {
        return !player.level().isClientSide && stillValid(player)
              && IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), tile.getBlockPos(), tile);
    }
    public boolean setRecipe(Player player, String recipe) { return canConfigure(player) && tile.setRecipe(recipe); }
    @Override public boolean stillValid(Player player) {
        return super.stillValid(player) && player.distanceToSqr(tile.getBlockPos().getX() + .5, tile.getBlockPos().getY() + .5, tile.getBlockPos().getZ() + .5) <= 64
              && (player.level().isClientSide || IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), tile.getBlockPos(), tile));
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!canConfigure(player) || index < 0 || index >= slots.size() || !slots.get(index).mayPickup(player)) return ItemStack.EMPTY;
        if (index < nativeStart) return super.quickMoveStack(player, index);
        var source = slots.get(index);
        ItemStack original = source.getItem().copy();
        if (original.isEmpty()) return ItemStack.EMPTY;
        var selected = getSelectedWindow(player.getUUID());
        ItemStack remainder = insertItem(hotBarSlots, original, true, selected);
        remainder = insertItem(mainInventorySlots, remainder, true, selected);
        remainder = insertItem(hotBarSlots, remainder, false, selected);
        remainder = insertItem(mainInventorySlots, remainder, false, selected);
        int moved = original.getCount() - remainder.getCount();
        if (moved == 0) return ItemStack.EMPTY;
        source.remove(moved); source.setChanged(); source.onTake(player, original.copyWithCount(moved));
        return original;
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!canConfigure(player) || !(player instanceof ServerPlayer serverPlayer)) return false;
        if (id >= 32 && id < 40 && tile.kind().forge()) return tile.uninstallResourceModule((id - 32) % 4, id >= 36);
        if (id == 0 && !tile.kind().forge()) return tile.binding.bindNearby(serverPlayer);
        if (id == 1) { tile.enabled = !tile.enabled; tile.markForSave(); return true; }
        if (id == 3 && tile.binding.resolve() instanceof ClibanoMainBlockEntity clibano) {
            clibano.awardUsedRecipesAndPopExperience(serverPlayer); clibano.setChanged(); return true;
        }
        return false;
    }
}
