package dev.everyonemek.forbidden;

import mekanism.api.text.ILangEntry;

public enum MachineKind implements ILangEntry {
    FORGE("forge_controller"), CLIBANO("clibano_controller");
    public final String id;
    MachineKind(String id) { this.id = id; }
    public boolean forge() { return this == FORGE; }
    public int supplies() { return forge() ? 4 : 2; }
    @Override public String getTranslationKey() { return "description.forbiddenmekanism." + id; }
}
