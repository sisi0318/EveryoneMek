package dev.everyonemek.ars;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IngredientAssignmentTest {
    @Test void overlappingTagsLeaveTheOnlyValidSlotForTheSpecificIngredient() {
        assertArrayEquals(new int[]{1, 1}, IngredientAssignment.matchQuantities(
              new boolean[][]{{true, true}, {true, false}}, new int[]{1, 1}, new int[]{1, 1}));
    }
    @Test void repeatedPedestalsRequireTheirFullQuantityWithoutMutatingAvailability() {
        int[] available = {2, 1};
        boolean[][] accepts = {{true, false}, {true, false}, {false, true}};
        assertArrayEquals(new int[]{2, 1}, IngredientAssignment.matchQuantities(accepts, available, new int[]{1, 1, 1}));
        assertNull(IngredientAssignment.matchQuantities(accepts, new int[]{1, 1}, new int[]{1, 1, 1}));
        assertArrayEquals(new int[]{2, 1}, available);
    }
}
