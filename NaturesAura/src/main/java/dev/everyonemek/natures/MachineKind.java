package dev.everyonemek.natures;

import mekanism.api.text.ILangEntry;

public enum MachineKind implements ILangEntry {
    AURA_GENERATOR("universal_aura_generator"),
    FOREST_RITUAL("universal_forest_ritual"),
    NATURAL_ALTAR("universal_natural_altar"),
    OFFERING("universal_offering");

    public final String id;

    MachineKind(String id) { this.id = id; }

    @Override
    public String getTranslationKey() { return "description." + NaturesMekanism.ID + "." + id; }
}
