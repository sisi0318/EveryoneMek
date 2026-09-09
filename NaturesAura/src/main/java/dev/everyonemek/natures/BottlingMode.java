package dev.everyonemek.natures;

import java.util.List;
import java.util.Locale;

public enum BottlingMode {
    AUTO, AURA, VACUUM, SUNLIGHT, GHOST, DARKNESS;

    private static final List<BottlingMode> NATURAL = List.of(AUTO, AURA, VACUUM);
    private static final List<BottlingMode> SIMULATED = List.of(SUNLIGHT, GHOST, DARKNESS, VACUUM);

    public static BottlingMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : AUTO;
    }

    public BottlingMode next(boolean simulationInstalled) {
        List<BottlingMode> choices = simulationInstalled ? SIMULATED : NATURAL;
        return choices.get((choices.indexOf(this) + 1) % choices.size());
    }

    public boolean requiresSimulation() { return this == SUNLIGHT || this == GHOST || this == DARKNESS; }
    public String translationKey() { return "gui.naturesmekanism.bottling_mode." + name().toLowerCase(Locale.ROOT); }
}
