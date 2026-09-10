package dev.everyonemek.ars;

import mekanism.api.text.ILangEntry;

public enum MachineKind implements ILangEntry {
    SOURCE_GENERATOR("source_generator"),
    SOURCE_CONVERTER("source_converter"),
    IMBUEMENT_CHAMBER("imbuement_chamber"),
    ENCHANTING_APPARATUS("enchanting_apparatus"),
    SOURCE_EXTRACTOR("source_extractor"),
    MAGIC_CRUSHER("magic_crusher"),
    GLYPH_SCRIBE("glyph_scribe"),
    POTION_MIXER("potion_mixer"),
    POTION_BOTTLER("potion_bottler"),
    DRYGMY_STATION("drygmy_station"),
    WHIRLISPRIG_STATION("whirlisprig_station"),
    RITUAL_CONTROLLER("ritual_controller");

    public final String id;
    MachineKind(String id) { this.id = id; }
    public boolean processesItems() { return inputCount() > 0; }
    public boolean advanced() { return ordinal() >= SOURCE_EXTRACTOR.ordinal(); }
    public boolean worldController() { return this == DRYGMY_STATION || this == WHIRLISPRIG_STATION || this == RITUAL_CONTROLLER; }
    public boolean creatureController() { return this == DRYGMY_STATION || this == WHIRLISPRIG_STATION; }
    public boolean usesSource() { return this != MAGIC_CRUSHER && this != GLYPH_SCRIBE && this != POTION_BOTTLER; }
    public boolean recipeSelectable() { return this == IMBUEMENT_CHAMBER || this == ENCHANTING_APPARATUS || this == MAGIC_CRUSHER || this == GLYPH_SCRIBE; }
    public int inputCount() {
        return switch (this) {
            case SOURCE_GENERATOR, SOURCE_CONVERTER, POTION_MIXER -> 0;
            case MAGIC_CRUSHER, POTION_BOTTLER -> 1;
            case SOURCE_EXTRACTOR -> 3;
            case DRYGMY_STATION, WHIRLISPRIG_STATION -> 7;
            default -> 9;
        };
    }
    public int outputCount() {
        return switch (this) {
            case MAGIC_CRUSHER, DRYGMY_STATION, WHIRLISPRIG_STATION -> 6;
            default -> processesItems() ? 4 : 0;
        };
    }
    public int modes() {
        return switch (this) {
            case SOURCE_CONVERTER, SOURCE_EXTRACTOR, RITUAL_CONTROLLER -> 3;
            case ENCHANTING_APPARATUS, POTION_BOTTLER, DRYGMY_STATION, WHIRLISPRIG_STATION -> 2;
            default -> 1;
        };
    }
    public boolean outputsSource() { return this == SOURCE_GENERATOR || this == SOURCE_CONVERTER || this == SOURCE_EXTRACTOR; }

    @Override
    public String getTranslationKey() { return "description." + ArsMekanism.ID + "." + id; }
}
