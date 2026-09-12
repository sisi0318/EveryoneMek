package dev.everyonemek.botania;

/** Charges prepaid transport capacity, so splitting a transfer does not change its total fee. */
public record WirelessFee(int fee, int remainingCredit) {
    public static WirelessFee forDelivery(int delivered, int hops, int credit) {
        if (delivered < 0 || hops < 0 || hops > 4 || credit < 0 || credit >= 50) throw new IllegalArgumentException("Invalid wireless accounting input");
        long work = (long) delivered * hops;
        int fee = Math.toIntExact((Math.max(0, work - credit) + 49) / 50);
        return new WirelessFee(fee, Math.toIntExact(credit + 50L * fee - work));
    }
}
