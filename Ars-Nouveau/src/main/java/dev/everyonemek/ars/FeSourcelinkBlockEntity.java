package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.api.client.ITooltipProvider;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.block.ITickable;
import com.hollingsworth.arsnouveau.common.block.tile.ModdedTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class FeSourcelinkBlockEntity extends ModdedTile implements ITickable, ITooltipProvider {
    public static final int ENERGY_CAPACITY = 4_000_000;
    public static final int NO_JAR = 0, FULL = 1, NEED_ENERGY = 2, RUNNING = 3;
    private static final Direction[] JAR_DIRECTIONS = {Direction.UP, Direction.DOWN};
    private int storedFE;
    private int status = NO_JAR;
    private int lastSyncedFE = -1;
    private int lastSyncedStatus = -1;
    private final IEnergyStorage energy = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            if (!isLive() || level.isClientSide || amount <= 0) return 0;
            int accepted = Math.min(amount, ENERGY_CAPACITY - storedFE);
            if (!simulate && accepted > 0) { storedFE += accepted; setChanged(); }
            return accepted;
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return isLive() ? storedFE : 0; }
        @Override public int getMaxEnergyStored() { return ENERGY_CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return isLive(); }
    };

    public FeSourcelinkBlockEntity(BlockPos pos, BlockState state) { super(Content.FE_SOURCELINK_TILE.get(), pos, state); }
    public IEnergyStorage energy() { return energy; }
    public int storedFE() { return storedFE; }
    public int status() { return status; }

    private boolean isLive() {
        return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this;
    }

    @Override public void tick() {
        if (!isLive() || level.isClientSide) return;
        status = NO_JAR;
        int rate = MachineConfig.GENERATOR_RATE.get();
        int fullCost = MachineConfig.GENERATOR_FE.get();
        for (Direction direction : JAR_DIRECTIONS) {
            BlockPos jarPos = worldPosition.relative(direction);
            if (!level.hasChunkAt(jarPos) || !(level.getBlockEntity(jarPos) instanceof SourceJarTile)) continue;
            var jar = level.getCapability(CapabilityRegistry.SOURCE_CAPABILITY, jarPos, direction.getOpposite());
            if (jar == null) continue;
            status = FULL;
            int accepted = Math.clamp(jar.receiveSource(rate, true), 0, rate);
            if (accepted == 0) continue;
            status = NEED_ENERGY;
            if (storedFE < cost(fullCost, rate, accepted)) break;
            // Only the real jar owns Source; a simulated transfer never changes either resource.
            int actual = Math.clamp(jar.receiveSource(accepted, false), 0, accepted);
            if (actual > 0) {
                storedFE -= cost(fullCost, rate, actual);
                status = RUNNING;
                setChanged();
                if (level.getGameTime() % 20 == 0)
                    ParticleUtil.spawnFollowProjectile(level, worldPosition, jarPos, ParticleColor.defaultParticleColor());
            } else status = FULL;
            break;
        }
        if (status != lastSyncedStatus || storedFE != lastSyncedFE && level.getGameTime() % 20 == 0) {
            lastSyncedFE = storedFE;
            lastSyncedStatus = status;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private static int cost(int fullCost, int rate, int amount) {
        return (int) (((long) fullCost * amount + rate - 1) / rate);
    }

    public MutableComponent energyText() { return Component.translatable("gui.arsmekanism.fe_energy", storedFE, ENERGY_CAPACITY); }
    public Component statusText() { return Component.translatable("gui.arsmekanism.fe_sourcelink_status." + status); }
    @Override public void getTooltip(List<Component> tooltip) { tooltip.add(energyText()); tooltip.add(statusText()); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("fe_energy", storedFE);
        tag.putInt("fe_status", status);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        storedFE = Math.clamp(tag.getInt("fe_energy"), 0, ENERGY_CAPACITY);
        status = Math.clamp(tag.getInt("fe_status"), NO_JAR, RUNNING);
    }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(Content.FE_ENERGY, storedFE);
    }

    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        storedFE = Math.clamp(input.getOrDefault(Content.FE_ENERGY, 0), 0, ENERGY_CAPACITY);
    }
}
