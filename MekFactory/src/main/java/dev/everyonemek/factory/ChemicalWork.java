package dev.everyonemek.factory;

import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.math.MathUtils;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToObjectCachedRecipe.ChemicalUsageMultiplier;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.StatUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Secondary input paid on each actual work step, with the unpaid draw retained across pauses and reloads. */
public final class ChemicalWork {
    public final ChemicalStack ingredient;
    public final boolean advanced, randomized;
    public boolean durationBased;
    public long used, pending = -1;
    private int pendingSpeed = -1, pendingChemical = -1;

    public ChemicalWork(ChemicalStack ingredient, boolean advanced, boolean randomized) {
        this.ingredient = ingredient.copy(); this.advanced = advanced; this.randomized = randomized;
    }
    public ChemicalWork copy() {
        var copy = new ChemicalWork(ingredient, advanced, randomized); copy.used = used; copy.pending = pending;
        copy.durationBased = durationBased;
        copy.pendingSpeed = pendingSpeed; copy.pendingChemical = pendingChemical; return copy;
    }
    public int slot(ResourceBank inputs) {
        for (int i = 0; i < inputs.chemicalTanks(); i++) {
            var stack = inputs.chemical(i);
            if (ChemicalStack.isSameChemical(stack, ingredient) && stack.getAmount() >= ingredient.getAmount()) return i;
        }
        return -1;
    }
    public long multiplier(Controller c, int baseTicks, int ticks, int progress) {
        int speedCount = Profiles.upgrades(c, Upgrade.SPEED), chemicalCount = advanced ? Profiles.upgrades(c, Upgrade.CHEMICAL) : 0;
        if (pending >= 0 && pendingSpeed == speedCount && pendingChemical == chemicalCount) return pending;
        pendingSpeed = speedCount; pendingChemical = chemicalCount;
        double speed = speedCount / (double) Upgrade.SPEED.getMax();
        double chemical = chemicalCount / (double) Upgrade.CHEMICAL.getMax();
        double factor = MekanismConfig.general.maxUpgradeMultiplier.get();
        // Same speed/chemical upgrade formulas used by MekanismUtils and its native cached recipes.
        if (randomized) pending = Math.max(0, StatUtils.inversePoisson(Math.pow(factor, advanced ? 2 * speed - chemical : speed)));
        else {
            long total = durationBased ? ticks : advanced ? Math.round(baseTicks * Math.pow(factor, speed - chemical)) : baseTicks;
            pending = ChemicalUsageMultiplier.constantUse(() -> total, () -> ticks).getToUse(used, progress);
        }
        c.markForSave(); return pending;
    }
    public long amountPerUnit(long multiplier) { return MathUtils.multiplyClamped(ingredient.getAmount(), multiplier); }
    public void consume(ResourceBank inputs, int slot, int units, long multiplier) {
        long amount = Math.multiplyExact(amountPerUnit(multiplier), units);
        if (amount > 0) { var stored = inputs.chemical(slot); inputs.chemical(slot, stored.copyWithAmount(stored.getAmount() - amount)); }
        used = MathUtils.addClamped(used, multiplier); pending = -1;
    }
    public CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag(); tag.put("ingredient", ingredient.saveOptional(registries));
        tag.putBoolean("advanced", advanced); tag.putBoolean("randomized", randomized); tag.putBoolean("duration_based", durationBased);
        tag.putLong("used", used); tag.putLong("pending", pending);
        tag.putInt("pending_speed", pendingSpeed); tag.putInt("pending_chemical", pendingChemical); return tag;
    }
    public static ChemicalWork load(CompoundTag tag, HolderLookup.Provider registries) {
        var stack = ChemicalStack.parseOptional(registries, tag.getCompound("ingredient"));
        if (stack.isEmpty()) return null;
        var work = new ChemicalWork(stack, tag.getBoolean("advanced"), tag.getBoolean("randomized"));
        work.durationBased = tag.getBoolean("duration_based");
        work.used = Math.max(0, tag.getLong("used")); work.pending = tag.contains("pending") ? Math.max(-1, tag.getLong("pending")) : -1;
        work.pendingSpeed = tag.contains("pending_speed") ? Math.clamp(tag.getInt("pending_speed"), -1, Upgrade.SPEED.getMax()) : -1;
        work.pendingChemical = tag.contains("pending_chemical") ? Math.clamp(tag.getInt("pending_chemical"), -1, Upgrade.CHEMICAL.getMax()) : -1;
        return work;
    }
}
