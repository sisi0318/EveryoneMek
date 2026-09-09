package dev.everyonemek.natures;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import java.util.Locale;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Moves existing Aura between the world and its Chemical tank, with a dead band between limits. */
public final class AuraControllerLogic {
    public enum Mode {
        BALANCE, RECOVER, RELEASE, HOLD;
        public String translationKey() { return "gui.naturesmekanism.control_mode." + name().toLowerCase(Locale.ROOT); }
    }
    public static final int MIN_LIMIT = -1_000_000, MAX_LIMIT = 100_000_000;
    private final AuraMachine machine;
    private int lower = 1_200_000, upper = 2_200_000;
    private Mode mode = Mode.BALANCE;

    public AuraControllerLogic(AuraMachine machine) { this.machine = machine; }
    public int lower() { return lower; }
    public int upper() { return upper; }
    public Mode mode() { return mode; }

    public void setLower(int value) { lower = Math.clamp(value, MIN_LIMIT, upper); machine.markForSave(); }
    public void setUpper(int value) { upper = Math.clamp(value, lower, MAX_LIMIT); machine.markForSave(); }
    public void cycleMode() { mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length]; machine.markForSave(); }

    public void tick() {
        var level = machine.getLevel();
        BlockPos pos = machine.getBlockPos();
        int radius = machine.environmentRadius();
        int ambient = IAuraChunk.getAuraInArea(level, pos, radius);
        machine.setEnvironmentAura(ambient);
        if (mode == Mode.HOLD) { machine.setStatus(10); return; }
        boolean recover = ambient > upper && mode != Mode.RELEASE;
        boolean release = ambient < lower && mode != Mode.RECOVER;
        if (!recover && !release) { machine.setStatus(9); return; }
        var tank = machine.auraTank();
        long rate = (long) MachineConfig.CONTROLLER_RATE.get() * MekanismUtils.getOperationsPerTick(machine, 1, 1);
        long needed = recover ? (long) ambient - upper : (long) lower - ambient;
        int amount = (int) Math.min(Math.min(rate, needed), recover ? tank.getNeeded() : tank.getStored());
        if (amount <= 0) { machine.setStatus(recover ? 3 : 5); return; }
        BlockPos spot = recover ? IAuraChunk.getHighestSpot(level, pos, radius, pos) : IAuraChunk.getLowestSpot(level, pos, radius, pos);
        if (!level.hasChunkAt(spot)) { machine.setStatus(11); return; }
        IAuraChunk chunk = IAuraChunk.getAuraChunk(level, spot);
        int possible = recover ? chunk.drainAura(spot, amount, false, true) : chunk.storeAura(spot, amount, false, true);
        if (possible != amount || !machine.payEnergy()) return;
        if (recover) {
            chunk.drainAura(spot, amount, false, false);
            tank.insert(new ChemicalStack(Content.AURA, amount), Action.EXECUTE, AutomationType.INTERNAL);
        } else {
            chunk.storeAura(spot, amount, false, false);
            tank.extract(amount, Action.EXECUTE, AutomationType.INTERNAL);
        }
        machine.setEnvironmentAura(ambient + (recover ? -amount : amount));
        machine.setStatus(recover ? 12 : 13);
        machine.setActive(true);
        machine.markForSave();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("lower", lower);
        tag.putInt("upper", upper);
        tag.putInt("mode", mode.ordinal());
        return tag;
    }

    public void load(CompoundTag tag) {
        lower = Math.clamp(tag.contains("lower") ? tag.getInt("lower") : 1_200_000, MIN_LIMIT, MAX_LIMIT);
        upper = Math.clamp(tag.contains("upper") ? tag.getInt("upper") : 2_200_000, lower, MAX_LIMIT);
        int id = tag.getInt("mode");
        mode = id >= 0 && id < Mode.values().length ? Mode.values()[id] : Mode.BALANCE;
    }

    public void track(MekanismContainer container) {
        container.track(SyncableInt.create(() -> lower, v -> lower = v));
        container.track(SyncableInt.create(() -> upper, v -> upper = v));
        container.track(SyncableInt.create(() -> mode.ordinal(), v -> mode = Mode.values()[Math.clamp(v, 0, Mode.values().length - 1)]));
    }
}
