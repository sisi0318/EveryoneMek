package dev.everyonemek.botania;

/** Expanded cell storage is independent of AE's resource accounting and transfer units. */
public enum ManaCellTier {
    K1(1, .5), K4(4, 1), K16(16, 1.5), K64(64, 2), K256(256, 2.5);

    public static final int BYTES_PER_K = 1024;
    public static final int CELL_MANA_PER_BYTE = 8000;
    // Appbot's ME planning density; physical cells use CELL_MANA_PER_BYTE above.
    public static final int MANA_PER_BYTE = 500;
    public static final int MANA_PER_OPERATION = 500;
    public static final int MANA_PER_POOL = 1_000_000;
    public static final long SLOT_CAPACITY = 10_000;
    public final int kilobytes;
    public final double idleDrain;
    public final long capacity;
    ManaCellTier(int kilobytes, double idleDrain) {
        this.kilobytes = kilobytes; this.idleDrain = idleDrain;
        capacity = kilobytes * (long) BYTES_PER_K * CELL_MANA_PER_BYTE;
    }
    public String id() { return this == K1 ? "mana_storage_cell" : "mana_storage_cell_" + kilobytes + "k"; }
}
