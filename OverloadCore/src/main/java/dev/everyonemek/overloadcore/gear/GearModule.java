package dev.everyonemek.overloadcore.gear;

import mekanism.api.gear.ICustomModule;

/** Stateless module data. Actions are dispatched at their actual work/damage/use boundary. */
public record GearModule(GearUpgrade upgrade) implements ICustomModule<GearModule> { }
