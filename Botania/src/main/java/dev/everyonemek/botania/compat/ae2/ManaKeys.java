package dev.everyonemek.botania.compat.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import dev.everyonemek.botania.AppliedBotanics;

/** One canonical ME resource; registry aliases handle saved keys from either installation. */
public final class ManaKeys {
    public static AEKey current() { return AppliedBotanics.loaded() ? AppliedBotanicsCompat.poolKey() : ManaKey.INSTANCE; }
    public static AEKeyType type() { return current().getType(); }
    public static boolean matches(AEKey key) { return key == current(); }
    private ManaKeys() { }
}
