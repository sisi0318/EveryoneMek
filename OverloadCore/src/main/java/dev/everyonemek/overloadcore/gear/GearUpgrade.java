package dev.everyonemek.overloadcore.gear;

/** Explicit installation contracts; unrelated Mek equipment does not inherit support. */
public enum GearUpgrade {
    HEAT_SINK("phase_heat_sink", 4, 1),
    MAGNETIC("magnetic_compensation", 4, 2),
    CAPACITOR("ward_capacitor", 4, 4),
    AFTERGUARD("afterguard_stabilizer", 4, 4),
    RESERVOIR("residual_reservoir", 4, 8),
    RESONANCE("resonant_discharge", 4, 16 | 64),
    ACCELERATOR("charge_accelerator", 4, 32 | 64),
    MAGAZINE("rail_magazine", 4, 32),
    FOCUS("rail_focus", 4, 32),
    PIERCING("rail_piercing", 3, 32),
    BLADE_FIELD("blade_field", 3, 64);

    public final String id;
    public final int maximum, targets;
    GearUpgrade(String id, int maximum, int targets) { this.id = id; this.maximum = maximum; this.targets = targets; }
}
