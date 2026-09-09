package dev.everyonemek.natures;

import mekanism.api.text.ILangEntry;

public enum MachineKind implements ILangEntry {
    AURA_GENERATOR("universal_aura_generator"),
    FOREST_RITUAL("universal_forest_ritual"),
    NATURAL_ALTAR("universal_natural_altar"),
    OFFERING("universal_offering"),
    AURA_BOTTLER("aura_bottler"),
    AURA_CONTROLLER("aura_controller"),
    ANIMAL_SPAWNER("universal_animal_spawner");

    public final String id;

    MachineKind(String id) { this.id = id; }

    public int inputCount() { return this == ANIMAL_SPAWNER ? 9 : this == FOREST_RITUAL ? 10 : this == AURA_GENERATOR || this == AURA_CONTROLLER ? 0 : this == AURA_BOTTLER ? 1 : 2; }
    public int outputCount() { return inputCount() == 0 || this == ANIMAL_SPAWNER ? 0 : 4; }
    public boolean hasWorkArea() { return this == ANIMAL_SPAWNER; }
    public boolean hasChemicalTank() { return this == AURA_GENERATOR || usesEnvironmentAura(); }
    public boolean usesEnvironmentAura() { return this == NATURAL_ALTAR || this == AURA_BOTTLER || this == AURA_CONTROLLER || this == ANIMAL_SPAWNER; }
    public boolean supportsRange() { return this == AURA_CONTROLLER || hasWorkArea(); }
    public boolean outputsChemical() { return this == AURA_GENERATOR || this == AURA_CONTROLLER; }

    @Override
    public String getTranslationKey() { return "description." + NaturesMekanism.ID + "." + id; }
}
