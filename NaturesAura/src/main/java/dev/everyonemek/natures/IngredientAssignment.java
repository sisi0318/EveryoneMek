package dev.everyonemek.natures;

/** Capacity-aware matching: overlapping tags must not greedily steal a later ingredient's only input. */
public final class IngredientAssignment {
    private IngredientAssignment() { }

    /** Integral max flow handles counted ingredients and overlapping tags without exponential search. */
    public static int[] matchQuantities(boolean[][] accepts, int[] available, int[] required) {
        int rows = accepts.length, slots = available.length, sink = rows + slots + 1, size = sink + 1;
        if (required.length != rows) throw new IllegalArgumentException();
        int[][] capacity = new int[size][size];
        long total = 0;
        for (int i = 0; i < rows; i++) {
            if (required[i] < 0 || accepts[i].length != slots) throw new IllegalArgumentException();
            capacity[0][i + 1] = required[i]; total += required[i];
            for (int s = 0; s < slots; s++) if (accepts[i][s]) capacity[i + 1][rows + s + 1] = Math.max(0, available[s]);
        }
        for (int s = 0; s < slots; s++) capacity[rows + s + 1][sink] = Math.max(0, available[s]);
        long flow = 0;
        while (flow < total) {
            int[] parent = new int[size], queue = new int[size];
            java.util.Arrays.fill(parent, -1); parent[0] = 0;
            int head = 0, tail = 1;
            while (head < tail && parent[sink] == -1) {
                int node = queue[head++];
                for (int next = 1; next < size; next++) if (parent[next] == -1 && capacity[node][next] > 0) {
                    parent[next] = node; queue[tail++] = next;
                }
            }
            if (parent[sink] == -1) return null;
            int amount = Integer.MAX_VALUE;
            for (int n = sink; n != 0; n = parent[n]) amount = Math.min(amount, capacity[parent[n]][n]);
            for (int n = sink; n != 0; n = parent[n]) { capacity[parent[n]][n] -= amount; capacity[n][parent[n]] += amount; }
            flow += amount;
        }
        int[] used = new int[slots];
        for (int s = 0; s < slots; s++) used[s] = capacity[sink][rows + s + 1];
        return used;
    }

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
