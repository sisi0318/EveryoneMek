package dev.everyonemek.natures;

/** Capacity-aware matching: overlapping tags must not greedily steal a later ingredient's only input. */
public final class IngredientAssignment {
    private IngredientAssignment() { }

    public static int[] match(boolean[][] accepts, int[] available) {
        int[] used = new int[available.length];
        return assign(accepts, available, used, 0) ? used : null;
    }

    private static boolean assign(boolean[][] accepts, int[] available, int[] used, int ingredient) {
        if (ingredient == accepts.length) return true;
        for (int slot = 0; slot < available.length; slot++) {
            if (accepts[ingredient][slot] && used[slot] < available[slot]) {
                used[slot]++;
                if (assign(accepts, available, used, ingredient + 1)) return true;
                used[slot]--;
            }
        }
        return false;
    }

    /** Total resource due at this progress; differences sum exactly to the recipe cost. */
    public static long due(long total, int progress, int duration) {
        if (duration <= 0 || progress < 0 || total < 0) throw new IllegalArgumentException();
        return total * Math.min(progress, duration) / duration;
    }
}
