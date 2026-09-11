package dev.everyonemek.forbidden;

/** Transient heat state; native fuel storage is never replaced with synthetic fuel. */
public interface ClibanoHeating {
    void forbiddenmekanism$beginHeatingTick();
    void forbiddenmekanism$endHeatingTick();
    boolean forbiddenmekanism$controlsHeat();
    boolean forbiddenmekanism$isHeating();

    static long energyPerTick(Controller controller) {
        long base = mekanism.common.util.UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertTo(MachineConfig.CLIBANO_HEAT_FE.get());
        double efficiency = Math.pow(mekanism.common.config.MekanismConfig.general.maxUpgradeMultiplier.get(),
              mekanism.common.util.MekanismUtils.fractionUpgrades(controller, mekanism.api.Upgrade.ENERGY));
        return Math.max(1, mekanism.api.math.MathUtils.ceilToLong(base / efficiency));
    }
}
