package dev.everyonemek.botania;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ManaMachineMenu extends MekanismTileContainer<ManaMachine> {
    public CompoundTag state = new CompoundTag();
    private CompoundTag lastSent;
    private int revision, ticks;
    private boolean accepted = true;
    private final Player player;
    public ManaMachineMenu(int id, Inventory inventory, ManaMachine tile) { super(ManaContent.MENU, id, inventory, tile); player = inventory.player; }
    @Override protected int getInventoryXOffset() { return 29; }
    @Override protected int getInventoryYOffset() { return 192; }
    @Override public boolean stillValid(Player player) {
        return super.stillValid(player) && canPlayerAccess(player) && player.level() == tile.getLevel()
              && !tile.isRemoved() && player.distanceToSqr(tile.getBlockPos().getCenter()) <= 64;
    }
    public void handleSettings(Player sender, int action, String value) {
        if (sender.containerMenu != this || !stillValid(sender) || tile.getLevel().isClientSide) return;
        accepted = tile.applySetting(action, value); revision++; ticks = 0; broadcastChanges();
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer server) || ticks++ % 20 != 0) return;
        CompoundTag snapshot = new CompoundTag(); snapshot.putInt("revision", revision); snapshot.putBoolean("accepted", accepted);
        snapshot.putString("recipe", tile.recipeLock()); snapshot.putInt("mode", tile.mode()); snapshot.putInt("side", tile.poolSide()); snapshot.putInt("target", tile.targetPercent());
        var choices = new ListTag();
        if (!tile.kind().random() && !tile.kind().controller()) for (var choice : ManaWork.choices(tile)) {
            if (choices.size() >= 1024) break;
            var entry = new CompoundTag(); entry.putString("id", choice.id().toString()); entry.put("icon", choice.icon().saveOptional(tile.getLevel().registryAccess())); choices.add(entry);
        }
        snapshot.put("choices", choices);
        if (!snapshot.equals(lastSent)) {
            lastSent = snapshot.copy(); PacketDistributor.sendToPlayer(server, new FlowerPackets.Snapshot(containerId, snapshot));
        }
    }
}
