package dev.everyonemek.botania;

/** One storage calculation for Appbot's normal and portable cells, including existing items. */
public final class ManaCellCapacity {
    public static long bytes(long appbotBytes) {
        return appbotBytes / 1000 * ManaCellTier.BYTES_PER_K + appbotBytes % 1000 * ManaCellTier.BYTES_PER_K / 1000;
    }
    public static long maximum(long appbotBytes) { return bytes(appbotBytes) * ManaCellTier.CELL_MANA_PER_BYTE; }
    public static long usedBytes(long stored) {
        return stored / ManaCellTier.CELL_MANA_PER_BYTE + (stored % ManaCellTier.CELL_MANA_PER_BYTE == 0 ? 0 : 1);
    }
    private ManaCellCapacity() { }
}
