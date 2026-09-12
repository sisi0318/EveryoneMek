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
    public static final int SET_MODE = 9, SET_DIRECTION = 10, SET_PRIORITY = 11, DETECT_POOL = 12,
          ADD_MEMBER = 13, REMOVE_MEMBER = 14, FILL_TARGET = 15, CONNECT_AS = 16;
    public final BlockPos position;
    public final Level level;
    private final Player player;
    private final BlockEntity tile;
    public CompoundTag state = new CompoundTag();
    private CompoundTag lastSent;
    public String feedback = "";
    private int settingsRevision, settingsAction = -1;
    private String settingsValue = "";
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
                if (action == ADD_MEMBER || action == REMOVE_MEMBER) {
                    UUID id = UUID.fromString(value);
                    if (id.equals(network.owner)) return false;
                    if (action == REMOVE_MEMBER) {
                        if (network.members.remove(id) == null) return false;
                    } else {
                        ServerPlayer member = server.getServer().getPlayerList().getPlayer(id);
                        if (member == null || network.members.size() >= 32 || network.members.containsKey(id)) return false;
                        network.members.put(id, member.getGameProfile().getName());
                    }
                    data.invalidate(); return true;
                }
                return false;
            }
            if (action == CONNECT_AS) {
                String[] parts = value.split(",", -1);
                if (parts.length != 2) return false;
                UUID id = UUID.fromString(parts[0]); int mode = Integer.parseInt(parts[1]);
                if (mode < NetworkPlant.SUPPLY || mode > NetworkPlant.RELAY) return false;
                // Validate membership/capacity before changing either the network or the mode.
                if (!data.join(plant, id, sender.getUUID())) return false;
                plant.mode = mode; plant.setChanged(); data.invalidate(); return true;
            }
            if (plant.mode == NetworkPlant.RELAY && (action == 2 || action == 3 || action == SET_DIRECTION || action == DETECT_POOL)
                  || (action == 4 || action == SET_PRIORITY || action == FILL_TARGET) && plant.mode != NetworkPlant.RECEIVE) return false;
            if (action == 1) plant.mode = (plant.mode + 1) % 3;
            else if (action == 2) level.setBlockAndUpdate(position, plant.getBlockState().setValue(BlockStateProperties.FACING, Direction.values()[(plant.direction().ordinal() + 1) % 6]));
            else if (action == 3) {
                int amount = Integer.parseInt(value);
                if (amount < 0 || amount > 2_000_000_000 || plant.mode == NetworkPlant.RELAY) return false;
                if (plant.mode == NetworkPlant.SUPPLY) plant.reserve = amount; else plant.target = amount;
            } else if (action == 4) plant.priority = (plant.priority + 1) % 3;
            else if (action == 5) return data.join(plant, UUID.fromString(value), sender.getUUID());
            else if (action == 8) { data.removed(plant); plant.network = null; }
            else if (action == SET_MODE) {
                int mode = Integer.parseInt(value); if (mode < 0 || mode > 2) return false; plant.mode = mode;
            } else if (action == SET_DIRECTION) {
                int direction = Integer.parseInt(value); if (direction < 0 || direction > 5) return false;
                level.setBlockAndUpdate(position, plant.getBlockState().setValue(BlockStateProperties.FACING, Direction.values()[direction]));
            } else if (action == SET_PRIORITY) {
                int priority = Integer.parseInt(value); if (priority < 0 || priority > 2) return false; plant.priority = priority;
            } else if (action == DETECT_POOL) {
                if (!plant.detectPool()) return false;
            } else if (action == FILL_TARGET) {
                var pool = ManaEndpoint.at(plant); if (pool == null) return false;
                plant.target = pool.getMaxMana();
            }
            else return false;
            plant.status = plant.network == null ? "unlinked" : "waiting"; plant.setChanged(); data.invalidate(); return true;
        } catch (IllegalArgumentException ignored) { return false; }
    }
    public void handleSettings(Player sender, int action, String value) {
        feedback = apply(sender, action, value) ? "" : action == DETECT_POOL ? "detect_failed" : "setting_failed";
        settingsAction = action; settingsValue = value; settingsRevision++;
    }
    public CompoundTag snapshot() {
        CompoundTag tag = new CompoundTag();
        if (tile == null || !(level instanceof ServerLevel server)) return tag;
        tag.putString("feedback", feedback);
        tag.putInt("settingsRevision", settingsRevision); tag.putInt("settingsAction", settingsAction); tag.putString("settingsValue", settingsValue);
        tag.putBoolean("enabled", Flowers.enabled(tile)); tag.putInt("fe", Flowers.storedFE(tile));
        tag.putInt("fePerMana", Balance.FE_PER_MANA.get()); tag.putInt("rate", Balance.LOTUS_RATE.get());
        if (tile instanceof ManaLotus lotus) {
            tag.putInt("kind", 0); tag.putInt("mana", lotus.getMana()); tag.putInt("maxMana", lotus.getMaxMana());
            if (lotus.findBoundTile() != null) tag.putLong("binding", lotus.getBindingPos().asLong());
            tag.putString("status", switch (lotus.status) { case 1 -> "paused"; case 2 -> "unbound"; case 3 -> "full"; case 4 -> "no_energy"; default -> "working"; });
        } else if (tile instanceof FunctionalFlowerBlockEntity flower) {
            tag.putString("flowerId", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()).toString());
            tag.putInt("kind", 1); tag.putInt("mana", flower.getMana()); tag.putInt("maxMana", flower.getMaxMana());
            tag.putString("status", !Flowers.enabled(tile) ? "paused" : flower.getMana() + Flowers.storedFE(tile) / Balance.FE_PER_MANA.get() < Flowers.workReserve(tile) ? "no_energy" : "ready");
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
                final var visible = network;
                tag.putUUID("network", visible.id); tag.putString("name", visible.name); tag.putInt("nodes", visible.nodes.size());
                tag.putInt("delivered", visible.lastDelivered); tag.putInt("fee", visible.lastFee);
                tag.putString("members", String.join(", ", visible.members.values().stream().sorted().toList()));
                tag.putInt("memberCount", visible.members.size());
                ListTag members = new ListTag();
                visible.members.entrySet().stream().sorted(java.util.Map.Entry.comparingByValue()).forEach(member -> {
                    CompoundTag entry = new CompoundTag(); entry.putUUID("id", member.getKey()); entry.putString("name", member.getValue()); members.add(entry);
                });
                tag.put("memberEntries", members);
                if (plant.core()) {
                    ListTag online = new ListTag();
                    server.getServer().getPlayerList().getPlayers().stream().filter(p -> !p.getUUID().equals(visible.owner) && !visible.members.containsKey(p.getUUID()))
                          .sorted(java.util.Comparator.comparing(p -> p.getGameProfile().getName())).forEach(p -> {
                              CompoundTag entry = new CompoundTag(); entry.putUUID("id", p.getUUID()); entry.putString("name", p.getGameProfile().getName()); online.add(entry);
                          });
                    tag.put("onlinePlayers", online);
                    ListTag connections = new ListTag();
                    visible.nodes.stream().sorted().forEach(pos -> {
                        CompoundTag entry = new CompoundTag(); entry.putLong("pos", pos.asLong()); entry.putString("name", pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
                        entry.putString("status", "unloaded");
                        if (server.hasChunkAt(pos) && server.getBlockEntity(pos) instanceof NetworkPlant node && Objects.equals(node.network, visible.id)) {
                            entry.putInt("mode", node.mode); entry.putString("status", Flowers.enabled(node) ? node.status : "paused");
                            entry.putInt("moved", node.moved);
                        }
                        connections.add(entry);
                    });
                    tag.put("connections", connections);
                }
            }
            ListTag choices = new ListTag();
            for (ManaNetworks.Network choice : data.accessible(player.getUUID())) {
                CompoundTag entry = new CompoundTag(); entry.putUUID("id", choice.id); entry.putString("name", choice.name);
                entry.putInt("nodes", choice.nodes.size());
                entry.putString("location", choice.core == null ? "" : choice.core.getX() + ", " + choice.core.getY() + ", " + choice.core.getZ());
                entry.putBoolean("online", choice.core != null && server.hasChunkAt(choice.core)
                      && server.getBlockEntity(choice.core) instanceof NetworkPlant core && Objects.equals(core.network, choice.id) && Flowers.enabled(core));
                choices.add(entry);
            }
            tag.put("choices", choices);
            if (!plant.core() && server.hasChunkAt(plant.targetPos())) {
                var pool = ManaEndpoint.at(plant);
                if (pool != null) {
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
