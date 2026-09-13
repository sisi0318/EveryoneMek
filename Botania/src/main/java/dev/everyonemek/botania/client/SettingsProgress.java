package dev.everyonemek.botania.client;

/** Tracks the last submitted setting even when the server combines several replies into one snapshot. */
public final class SettingsProgress {
    private boolean initialized;
    private int sent, acknowledged;
    public void acknowledge(int revision) {
        if (!initialized) { initialized = true; sent = acknowledged = revision; return; }
        if (revision - acknowledged >= 0) acknowledged = revision;
        if (revision - sent > 0) sent = revision;
    }
    public boolean ready() { return initialized; }
    public int submit() {
        if (!initialized) throw new IllegalStateException("Settings have not arrived yet");
        return ++sent;
    }
    public boolean waiting() { return initialized && acknowledged != sent; }
    public boolean confirmed(int revision) { return initialized && acknowledged - revision >= 0; }
}
