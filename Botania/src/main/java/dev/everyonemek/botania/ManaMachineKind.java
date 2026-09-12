package dev.everyonemek.botania;

public enum ManaMachineKind implements mekanism.api.text.IHasTranslationKey {
    BRIDGE("mana_bridge", 0, 0, 0, true),
    CHARGER("mana_charger", 1, 0, 1, false),
    INFUSER("mana_infuser", 1, 1, 6, true),
    RUNIC("runic_forge", 16, 1, 6, true),
    PURE("pure_converter", 1, 0, 6, false),
    TERRA("terra_condenser", 16, 0, 6, true),
    BREWERY("botanical_brewery", 16, 1, 6, true),
    ORE("ore_processor", 1, 1, 6, true),
    METAMORPHIC("metamorphic_stone", 1, 0, 6, true),
    ELVEN("elven_trade_controller", 16, 0, 8, false),
    ENCHANTER("mana_enchanter_controller", 1, 16, 1, true);

    public final String id;
    public final int inputs, extras, outputs;
    public final boolean chemical;
    ManaMachineKind(String id, int inputs, int extras, int outputs, boolean chemical) {
        this.id = id; this.inputs = inputs; this.extras = extras; this.outputs = outputs; this.chemical = chemical;
    }
    public boolean controller() { return this == ELVEN || this == ENCHANTER; }
    public boolean random() { return this == ORE || this == METAMORPHIC; }
    @Override public String getTranslationKey() { return "description.botanicalmekanism." + id; }
}
