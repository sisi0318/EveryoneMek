package dev.everyonemek.botania;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class NetworkPlant extends BlockEntity {
    public static final int SUPPLY = 0, RECEIVE = 1, RELAY = 2;
    public UUID network;
    public int mode = RECEIVE, reserve, target = 1_000_000, priority = 1;
    public String status = "unlinked";
    public int moved, hops;
    public NetworkPlant(BlockPos pos, BlockState state) { super(Content.NETWORK_TILE.get(), pos, state); }
    public boolean core() { return getBlockState().is(Content.CORE.get()); }
    public Direction direction() { return getBlockState().getValue(BlockStateProperties.FACING); }
    public BlockPos targetPos() { return worldPosition.relative(direction()); }
    public void tick() {
        if (!Flowers.live(this) || !(level instanceof ServerLevel server)) return;
        if (core()) ManaNetworks.get(server).tick(this);
        else if (network != null && Flowers.enabled(this)) {
            var data = ManaNetworks.get(server);
            var known = data.find(network);
            if (known != null && !known.nodes.contains(worldPosition)) data.join(this, network, Flowers.owner(this));
        }
    }
    public void saveItem(CompoundTag tag) {
        if (network != null) tag.putUUID("network", network);
        tag.putInt("mode", mode); tag.putInt("reserve", reserve); tag.putInt("target", target); tag.putInt("priority", priority);
        tag.putInt("direction", direction().ordinal());
    }
    public void loadItem(CompoundTag tag) {
        network = tag.hasUUID("network") ? tag.getUUID("network") : null;
        mode = Math.clamp(tag.getInt("mode"), SUPPLY, RELAY);
        reserve = Math.clamp(tag.getInt("reserve"), 0, 2_000_000_000);
        target = tag.contains("target") ? Math.clamp(tag.getInt("target"), 0, 2_000_000_000) : 1_000_000;
        priority = tag.contains("priority") ? Math.clamp(tag.getInt("priority"), 0, 2) : 1;
        if (level != null && tag.contains("direction"))
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockStateProperties.FACING, Direction.values()[Math.clamp(tag.getInt("direction"), 0, 5)]));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) { super.saveAdditional(tag, provider); saveItem(tag); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // Facing already belongs to the loaded block state; do not mutate the level while loading NBT.
        CompoundTag values = tag.copy(); values.remove("direction"); loadItem(values);
    }
}
