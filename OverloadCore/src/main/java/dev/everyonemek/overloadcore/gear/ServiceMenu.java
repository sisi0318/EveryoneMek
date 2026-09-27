package dev.everyonemek.overloadcore.gear;

import java.util.UUID;
import java.util.function.Consumer;
import dev.everyonemek.overloadcore.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.gear.config.ModuleConfig;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.tile.TileEntityModificationStation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Edits worn originals without moving them out of Curios or releasing their binding. */
public final class ServiceMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final long session;
    private final Player owner;
    private final TileEntityModificationStation station;
    public ItemStack core = ItemStack.EMPTY, ward = ItemStack.EMPTY;
    public long energy, cost;
    public int status;
    private ItemStack clientInput = ItemStack.EMPTY;
    private long lastAction = Long.MIN_VALUE, lastPublish = Long.MIN_VALUE;
    private GearMenus.Snapshot lastSent;
    public static final String[] STATES = {"ready", "missing", "no_module", "unsupported", "maxed", "energy", "space", "changed", "busy", "done", "disabled"};

    public ServiceMenu(int id, Inventory inventory, BlockPos pos, long session) {
        super(GearMenus.SERVICE.get(), id); this.owner = inventory.player; this.pos = pos.immutable(); this.session = session;
        station = owner.level().getBlockEntity(pos) instanceof TileEntityModificationStation s ? s : null;
        addSlot(new Slot(new StationInput(), 0, 13, 46) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof mekanism.common.content.gear.IModuleItem; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 31 + col * 18, 185 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 31 + col * 18, 243));
    }
    public static ServiceMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf b) { return new ServiceMenu(id, inventory, b.readBlockPos(), b.readLong()); }
    public static void open(ServerPlayer p, TileEntityModificationStation station) {
        if (station.isRemoved() || p.distanceToSqr(station.getBlockPos().getCenter()) > 64
              || !IBlockSecurityUtils.INSTANCE.canAccess(p, p.level(), station.getBlockPos(), station)) return;
        long nonce = p.getRandom().nextLong(); var pos = station.getBlockPos();
        p.openMenu(new SimpleMenuProvider((id, inv, player) -> new ServiceMenu(id, inv, pos, nonce), CoreContent.text("service.title")),
              b -> { b.writeBlockPos(pos); b.writeLong(nonce); });
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && station != null && !station.isRemoved() && player.level().hasChunkAt(pos)
              && player.level().getBlockEntity(pos) == station && player.distanceToSqr(pos.getCenter()) <= 64
              && IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), pos, station);
    }
    public static UUID identity(ItemStack stack) {
        if (stack.is(CoreContent.WARD)) return stack.get(CoreContent.WARD_SEAL);
        var data = stack.get(CoreContent.DATA); return data != null && data.hasUUID("instance") ? data.getUUID("instance") : null;
    }
    public ItemStack view(int target) { return target == 0 ? core : ward; }
    private ItemStack actual(ServerPlayer p, int target) {
        if (target == 0) { CoreBinding.restore(p); var s = CoreBinding.worn(p); return CoreBinding.matches(p, s) ? s : ItemStack.EMPTY; }
        return WardCustody.ensure(p) ? WardRuntime.worn(p) : ItemStack.EMPTY;
    }
    private long operationCost() { return Math.max(1, station.getEnergyContainer().getEnergyPerTick()) * Math.max(1, station.ticksRequired); }
    public boolean action(ServerPlayer player, GearMenus.Edit request) {
        if (player.containerMenu != this || request.menu() != containerId || request.session() != session || !stillValid(player)
              || request.target() < 0 || request.target() > 1 || request.upgrade() < 0 || request.upgrade() >= GearUpgrade.values().length) return false;
        long now = player.level().getGameTime(); if (now == lastAction) return false; lastAction = now;
        ItemStack item = actual(player, request.target());
        if (item.isEmpty() || request.identity() == null || !request.identity().equals(identity(item))) return fail(7);
        var upgrade = GearUpgrade.values()[request.upgrade()]; var type = EquipmentModules.get(upgrade);
        if ((upgrade.targets & (request.target() == 0 ? 8 : 4)) == 0) return fail(3);
        var container = ModuleHelper.get().getModuleContainer(item);
        if (container == null) return fail(3);
        int count = container.installedCount(type);
        var input = station.getInventorySlots(null).getFirst();
        if (!station.containerSlot.isEmpty()) return fail(8);
        if (!station.canFunction()) return fail(10);
        Consumer<ItemStack> change;
        int installed = 0;
        if (request.operation() == 0) {
            if (!input.getStack().is(CoreContent.UPGRADE_ITEMS.get(upgrade))) return fail(2);
            if (!container.canInstall(item, type)) return fail(count >= upgrade.maximum ? 4 : 3);
            installed = Math.min(input.getCount(), upgrade.maximum - count); int adding = installed;
            if (station.getEnergyContainer().getEnergy() < operationCost()) return fail(5);
            change = copy -> ModuleHelper.get().getModuleContainer(copy).addModule(player.registryAccess(), copy, type, adding);
        } else if (request.operation() == 1) {
            if (count == 0) return fail(2);
            if (!hasSpace(player, new ItemStack(CoreContent.UPGRADE_ITEMS.get(upgrade).get()))) return fail(6);
            change = copy -> ModuleHelper.get().getModuleContainer(copy).removeModule(player.registryAccess(), copy, type, 1);
        } else if (request.operation() == 2) {
            if (count == 0) return fail(2);
            var enabled = container.get(type).<Boolean>getConfigOrThrow(ModuleConfig.ENABLED_KEY);
            change = copy -> ModuleHelper.get().getModuleContainer(copy).replaceModuleConfig(player.registryAccess(), copy, type, enabled.with(!enabled.get()));
        } else return false;
        boolean success = request.target() == 0 ? CoreBinding.updateEquipment(player, item, change) : WardCustody.update(player, item, change);
        if (!success) return fail(7);
        if (request.operation() == 0) {
            input.shrinkStack(installed, Action.EXECUTE);
            station.getEnergyContainer().extract(operationCost(), Action.EXECUTE, AutomationType.INTERNAL);
        } else if (request.operation() == 1) player.getInventory().placeItemBackInInventory(new ItemStack(CoreContent.UPGRADE_ITEMS.get(upgrade).get()));
        station.markForSave(); player.getInventory().setChanged(); status = 9; publish(); return true;
    }
    private static boolean hasSpace(Player p, ItemStack module) {
        return p.getInventory().items.stream().anyMatch(s -> s.isEmpty() || ItemStack.isSameItemSameComponents(s, module) && s.getCount() < s.getMaxStackSize());
    }
    private boolean fail(int reason) { status = reason; publish(); return false; }
    private void publish() {
        if (!(owner instanceof ServerPlayer p) || !stillValid(p)) return;
        core = actual(p, 0).copy(); ward = actual(p, 1).copy(); energy = station.getEnergyContainer().getEnergy(); cost = operationCost();
        var packet = new GearMenus.Snapshot(containerId, session, core, ward, energy, cost, status);
        if (lastSent == null || !ItemStack.matches(core,lastSent.core()) || !ItemStack.matches(ward,lastSent.ward())
              || energy != lastSent.energy() || cost != lastSent.cost() || status != lastSent.status()) {
            PacketDistributor.sendToPlayer(p, packet); lastSent = packet;
        }
        lastPublish = p.level().getGameTime();
    }
    @Override public void broadcastChanges() { super.broadcastChanges(); if (owner instanceof ServerPlayer && (lastPublish == Long.MIN_VALUE || owner.level().getGameTime() - lastPublish >= 5)) publish(); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { if (stillValid(player)) super.clicked(slot, button, type, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var copy = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, stack); return copy;
    }
    private final class StationInput implements Container {
        public int getContainerSize() { return 1; }
        public boolean isEmpty() { return getItem(0).isEmpty(); }
        public ItemStack getItem(int i) { return owner.level().isClientSide ? clientInput : station == null ? ItemStack.EMPTY : station.getInventorySlots(null).getFirst().getStack(); }
        public ItemStack removeItem(int i, int amount) { var result = getItem(0).split(amount); setChanged(); return result; }
        public ItemStack removeItemNoUpdate(int i) { var result = getItem(0); setItem(0, ItemStack.EMPTY); return result; }
        public void setItem(int i, ItemStack stack) { if (owner.level().isClientSide) clientInput = stack; else if (station != null) station.getInventorySlots(null).getFirst().setStack(stack); setChanged(); }
        public void setChanged() { if (station != null) station.markForSave(); }
        public boolean stillValid(Player player) { return ServiceMenu.this.stillValid(player); }
        public void clearContent() { setItem(0, ItemStack.EMPTY); }
    }
}
