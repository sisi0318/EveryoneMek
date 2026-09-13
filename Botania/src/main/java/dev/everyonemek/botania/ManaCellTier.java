package dev.everyonemek.botania;

/** Cell sizes use the same 1024-byte tiers and fluid density as ME fluid cells. */
public enum ManaCellTier {
    K1(1, .5), K4(4, 1), K16(16, 1.5), K64(64, 2), K256(256, 2.5);

    public static final int MANA_PER_BYTE = 8000;
    public final int kilobytes;
    public final double idleDrain;
    public final long capacity;
    ManaCellTier(int kilobytes, double idleDrain) {
        this.kilobytes = kilobytes; this.idleDrain = idleDrain;
        capacity = kilobytes * 1024L * MANA_PER_BYTE;
    }
    public String id() { return this == K1 ? "mana_storage_cell" : "mana_storage_cell_" + kilobytes + "k"; }
}
