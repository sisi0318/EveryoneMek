package dev.everyonemek.botania;

/** Applied Botanics alpha.3 uses decimal kilobytes and 500 mana per byte. */
public enum ManaCellTier {
    K1(1, .5), K4(4, 1), K16(16, 1.5), K64(64, 2), K256(256, 2.5);

    public static final int BYTES_PER_K = 1000;
    public static final int MANA_PER_BYTE = 500;
    public static final int MANA_PER_OPERATION = 500;
    public static final int MANA_PER_POOL = 1_000_000;
    public static final long SLOT_CAPACITY = 10_000;
    public final int kilobytes;
    public final double idleDrain;
    public final long capacity;
    ManaCellTier(int kilobytes, double idleDrain) {
        this.kilobytes = kilobytes; this.idleDrain = idleDrain;
        capacity = kilobytes * (long) BYTES_PER_K * MANA_PER_BYTE;
    }
    public String id() { return this == K1 ? "mana_storage_cell" : "mana_storage_cell_" + kilobytes + "k"; }
}
