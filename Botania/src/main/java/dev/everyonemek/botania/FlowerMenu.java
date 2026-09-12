package dev.everyonemek.botania;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;

public final class FlowerMenu extends AbstractContainerMenu {
    public final BlockPos position;
    public final Level level;
    private final Player player;
    private final BlockEntity tile;
    public CompoundTag state = new CompoundTag();
    private CompoundTag lastSent;
    public FlowerMenu(int id, Inventory inventory, BlockPos position) {
        super(Content.MENU.get(), id); this.position = position; level = inventory.player.level(); player = inventory.player;
        tile = level.getBlockEntity(position);
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.level() == level && player.distanceToSqr(position.getCenter()) <= 64
              && (level.isClientSide || tile != null && Flowers.live(tile) && Flowers.owns(player, tile));
    }
    public boolean apply(Player sender, int action, String value) {
        if (!(level instanceof ServerLevel server) || sender.containerMenu != this || !stillValid(sender)) return false;
        ManaNetworks data = ManaNetworks.get(server);
        if (action == 0) { Flowers.data(tile).putBoolean("paused", Flowers.enabled(tile)); tile.setChanged(); data.invalidate(); return true; }
        if (!(tile instanceof NetworkPlant plant)) return false;
        try {
            if (plant.core()) {
                if (!data.activate(plant)) return false;
                ManaNetworks.Network network = data.find(plant.network);
                if (action == 6) {
                    String name = value.trim();
                    if (name.isEmpty() || name.length() > 32 || name.chars().anyMatch(Character::isISOControl)) return false;
                    network.name = name; data.setDirty(); return true;
                }
                if (action == 7) {
                    String name = value.trim();
                    if (name.isEmpty() || name.length() > 16) return false;
                    var existing = network.members.entrySet().stream().filter(entry -> entry.getValue().equalsIgnoreCase(name)).findFirst();
                    if (existing.isPresent()) network.members.remove(existing.get().getKey());
                    else {
                        ServerPlayer member = server.getServer().getPlayerList().getPlayerByName(name);
                        if (member == null || member.getUUID().equals(network.owner) || network.members.size() >= 32) return false;
                        network.members.put(member.getUUID(), member.getGameProfile().getName());
                    }
                    data.invalidate(); return true;
                }
                return false;
            }
            if (plant.mode == NetworkPlant.RELAY && (action == 2 || action == 3) || action == 4 && plant.mode != NetworkPlant.RECEIVE) return false;
            if (action == 1) plant.mode = (plant.mode + 1) % 3;
            else if (action == 2) level.setBlockAndUpdate(position, plant.getBlockState().setValue(BlockStateProperties.FACING, Direction.values()[(plant.direction().ordinal() + 1) % 6]));
            else if (action == 3) {
                int amount = Integer.parseInt(value);
                if (amount < 0 || amount > 2_000_000_000 || plant.mode == NetworkPlant.RELAY) return false;
                if (plant.mode == NetworkPlant.SUPPLY) plant.reserve = amount; else plant.target = amount;
            } else if (action == 4) plant.priority = (plant.priority + 1) % 3;
            else if (action == 5) return data.join(plant, UUID.fromString(value), sender.getUUID());
            else if (action == 8) { data.removed(plant); plant.network = null; }
            else return false;
            plant.status = plant.network == null ? "unlinked" : "waiting"; plant.setChanged(); data.invalidate(); return true;
        } catch (IllegalArgumentException ignored) { return false; }
    }
    public CompoundTag snapshot() {
        CompoundTag tag = new CompoundTag();
        if (tile == null || !(level instanceof ServerLevel server)) return tag;
        tag.putBoolean("enabled", Flowers.enabled(tile)); tag.putInt("fe", Flowers.storedFE(tile));
        tag.putInt("fePerMana", Balance.FE_PER_MANA.get()); tag.putInt("rate", Balance.LOTUS_RATE.get());
        if (tile instanceof ManaLotus lotus) {
            tag.putInt("kind", 0); tag.putInt("mana", lotus.getMana()); tag.putInt("maxMana", lotus.getMaxMana());
            tag.putString("status", switch (lotus.status) { case 1 -> "paused"; case 2 -> "unbound"; case 3 -> "full"; case 4 -> "no_energy"; default -> "working"; });
        } else if (tile instanceof FunctionalFlowerBlockEntity flower) {
            tag.putInt("kind", 1); tag.putInt("mana", flower.getMana()); tag.putInt("maxMana", flower.getMaxMana());
            tag.putString("status", !Flowers.enabled(tile) ? "paused" : Flowers.storedFE(tile) < Balance.FE_PER_MANA.get() && flower.getMana() == 0 ? "no_energy" : "ready");
        } else if (tile instanceof NetworkPlant plant) {
            ManaNetworks data = ManaNetworks.get(server);
            if (plant.core()) data.activate(plant);
            ManaNetworks.Network network = data.find(plant.network);
            boolean denied = network != null && !network.permits(player.getUUID());
            if (denied) network = null;
            tag.putInt("kind", plant.core() ? 2 : 3); tag.putInt("mode", plant.mode); tag.putInt("direction", plant.direction().ordinal());
            tag.putInt("priority", plant.priority); tag.putInt("limit", plant.mode == NetworkPlant.SUPPLY ? plant.reserve : plant.target);
            tag.putInt("moved", plant.moved); tag.putInt("hops", plant.hops);
            String status = denied ? "denied" : plant.status;
            if (!Flowers.enabled(plant)) status = "paused";
            else if (network != null && (network.core == null || !server.hasChunkAt(network.core)
                  || !(server.getBlockEntity(network.core) instanceof NetworkPlant core) || !core.core()
                  || !Objects.equals(core.network, network.id) || !Flowers.enabled(core))) status = "core_offline";
            tag.putString("status", status);
            if (network != null) {
                tag.putUUID("network", network.id); tag.putString("name", network.name); tag.putInt("nodes", network.nodes.size());
                tag.putInt("delivered", network.lastDelivered); tag.putInt("fee", network.lastFee);
                tag.putString("members", String.join(", ", network.members.values().stream().sorted().toList()));
                tag.putInt("memberCount", network.members.size());
            }
            ListTag choices = new ListTag();
            for (ManaNetworks.Network choice : data.accessible(player.getUUID())) {
                CompoundTag entry = new CompoundTag(); entry.putUUID("id", choice.id); entry.putString("name", choice.name); choices.add(entry);
            }
            tag.put("choices", choices);
            if (!plant.core() && server.hasChunkAt(plant.targetPos())) {
                if (server.getBlockEntity(plant.targetPos()) instanceof ManaPoolBlockEntity pool) {
                    tag.putInt("mana", pool.getCurrentMana()); tag.putInt("maxMana", pool.getMaxMana());
                }
            }
        }
        return tag;
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        CompoundTag next = snapshot();
        if (!next.equals(lastSent)) { PacketDistributor.sendToPlayer(serverPlayer, new FlowerPackets.Snapshot(containerId, next)); lastSent = next; }
    }
}
