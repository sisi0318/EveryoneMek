package dev.everyonemek.natures;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IngredientAssignmentTest {
    @Test
    void countedOverlappingIngredientsReassignWithoutReusingItems() {
        boolean[][] accepts = {{true, true}, {true, false}};
        assertArrayEquals(new int[]{64, 64}, IngredientAssignment.matchQuantities(accepts, new int[]{64, 64}, new int[]{64, 64}));
        assertNull(IngredientAssignment.matchQuantities(accepts, new int[]{63, 65}, new int[]{64, 64}));
        assertArrayEquals(new int[]{3, 4}, IngredientAssignment.matchQuantities(new boolean[][]{{true, true}}, new int[]{3, 8}, new int[]{7}));
    }
    @Test
    void overlappingTagsDoNotStealTheOnlyExactIngredient() {
        assertArrayEquals(new int[]{1, 1}, IngredientAssignment.match(new boolean[][]{{true, true}, {true, false}}, new int[]{1, 1}));
    }

    @Test
    void repeatedIngredientCanComeFromOneStackButCannotReuseOneItem() {
        boolean[][] required = {{true}, {true}, {true}};
        assertArrayEquals(new int[]{3}, IngredientAssignment.match(required, new int[]{3}));
        assertNull(IngredientAssignment.match(required, new int[]{2}));
    }

    @Test
    void impossibleMixturesDoNotMatchEvenWithEnoughTotalItems() {
        assertNull(IngredientAssignment.match(new boolean[][]{{true, false}, {true, false}}, new int[]{1, 10}));
    }

    @Test
    void unrelatedStockRemainsUntouched() {
        assertArrayEquals(new int[]{0, 1, 0}, IngredientAssignment.match(new boolean[][]{{false, true, false}}, new int[]{64, 64, 64}));
    }

    @Test
    void auraRoundingNeverCreatesAnExtraPerTickCharge() {
        for (int ticks : new int[]{1, 3, 80, 200, 1200}) {
            for (long aura : new long[]{0, 1, 20_000, Integer.MAX_VALUE}) {
                long charged = 0;
                for (int tick = 1; tick <= ticks; tick++) {
                    long next = IngredientAssignment.due(aura, tick, ticks);
                    assertTrue(next >= charged);
                    charged += next - charged;
                }
                assertEquals(aura, charged);
            }
        }
    }

    @Test
    void reloadedProgressChargesOnlyTheRemainingAura() {
        long paid = IngredientAssignment.due(20_003, 37, 80);
        long remainder = IngredientAssignment.due(20_003, 80, 80) - paid;
        assertEquals(20_003, paid + remainder);
    }
}
