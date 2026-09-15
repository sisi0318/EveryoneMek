package dev.everyonemek.overloadcore.client;

import java.util.Objects;

/** One held-Shift hover session, timed independently of game ticks and frame rate. */
public final class TooltipReveal {
    public static final long LINE_DELAY_MS = 160;
    public static final long WRITE_MS = 140;
    private Object screen, target;
    private long started;
    private boolean active, seen;

    public void beginFrame(boolean shiftDown) {
        seen = false;
        if (!shiftDown) reset();
    }

    public long sample(Object screen, Object target, long now) {
        if (!active || this.screen != screen || !Objects.equals(this.target, target) || now < started) {
            this.screen = screen;
            this.target = target;
            started = now;
            active = true;
        }
        seen = true;
        return now - started;
    }

    public void endFrame() {
        if (!seen) reset();
    }

    public void reset() {
        screen = null;
        target = null;
        active = false;
        seen = false;
    }

    public static int visibleLines(long elapsed, int total) {
        return total <= 0 ? 0 : (int) Math.min(total, Math.max(0, elapsed) / LINE_DELAY_MS + 1);
    }

    public static int visibleGlyphs(long elapsed, int line, int total) {
        long age = elapsed - line * LINE_DELAY_MS;
        if (total <= 0 || age < 0) return 0;
        if (age >= WRITE_MS) return total;
        return Math.max(1, (int) (total * age / WRITE_MS));
    }

    public static boolean writing(long elapsed, int line) {
        long age = elapsed - line * LINE_DELAY_MS;
        return age >= 0 && age < WRITE_MS;
    }
}
