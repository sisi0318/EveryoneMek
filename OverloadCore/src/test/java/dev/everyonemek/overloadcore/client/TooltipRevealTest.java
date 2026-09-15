package dev.everyonemek.overloadcore.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TooltipRevealTest {
    @Test void linesAdvanceOnceAndStayVisibleWithoutCycling() {
        assertEquals(1, TooltipReveal.visibleLines(0, 14));
        assertEquals(1, TooltipReveal.visibleLines(159, 14));
        assertEquals(2, TooltipReveal.visibleLines(160, 14));
        assertEquals(14, TooltipReveal.visibleLines(2080, 14));
        assertEquals(14, TooltipReveal.visibleLines(60_000, 14));
        assertEquals(0, TooltipReveal.visibleLines(60_000, 0));
        assertEquals(0, TooltipReveal.visibleGlyphs(159, 1, 20));
        assertEquals(1, TooltipReveal.visibleGlyphs(160, 1, 20));
        assertEquals(20, TooltipReveal.visibleGlyphs(300, 1, 20));
        assertFalse(TooltipReveal.writing(300, 1));
    }

    @Test void releasingShiftOrLeavingTheTooltipRestartsTheAnimation() {
        var state = new TooltipReveal(); var screen = new Object();
        state.beginFrame(true);
        assertEquals(0, state.sample(screen, "pendant", 100)); state.endFrame();
        state.beginFrame(true);
        assertEquals(160, state.sample(screen, "pendant", 260)); state.endFrame();
        state.beginFrame(false); state.endFrame();
        state.beginFrame(true);
        assertEquals(0, state.sample(screen, "pendant", 400)); state.endFrame();
        state.beginFrame(true); state.endFrame(); // Mouse moved to an empty slot while still holding Shift.
        state.beginFrame(true);
        assertEquals(0, state.sample(screen, "pendant", 600));
    }

    @Test void switchingSlotsScreensOrClockEpochDoesNotInheritProgress() {
        var state = new TooltipReveal(); var screen = new Object();
        assertEquals(0, state.sample(screen, "slot-a", 100));
        assertEquals(200, state.sample(screen, "slot-a", 300));
        assertEquals(0, state.sample(screen, "slot-b", 320));
        assertEquals(0, state.sample(new Object(), "slot-b", 400));
        assertEquals(0, state.sample(screen, "slot-b", 500));
        assertEquals(0, state.sample(screen, "slot-b", 10));
    }
}
