package dev.everyonemek.overloadcore.gear;

/** Explicit installation contracts; unrelated Mek equipment does not inherit support. */
public enum GearUpgrade {
    HEAT_SINK("phase_heat_sink", 4, 1),
    MAGNETIC("magnetic_compensation", 4, 2),
    CAPACITOR("ward_capacitor", 4, 4),
    AFTERGUARD("afterguard_stabilizer", 4, 4),
    // Decode old stacks only. No installation target, recipe or creative entry.
    RESERVOIR("residual_reservoir", 4, 0),
    RESONANCE("resonant_discharge", 4, 16),
    ACCELERATOR("charge_accelerator", 4, 16),
    // Preserve installed modules and registry ID; this is now the rail recovery unit.
    MAGAZINE("rail_magazine", 4, 16),
    FOCUS("rail_focus", 4, 16),
    PIERCING("rail_piercing", 3, 16),
    BLADE_FIELD("blade_field", 3, 16),
    POLARIZATION("polarization", 3, 16),
    DEFLECTOR("magnetic_deflector", 1, 1);

    public final String id;
    public final int maximum, targets;
    GearUpgrade(String id, int maximum, int targets) { this.id = id; this.maximum = maximum; this.targets = targets; }
}
