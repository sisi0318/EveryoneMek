package dev.everyonemek.botania;

import dev.everyonemek.botania.client.SettingsProgress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingsProgressTest {
    @Test void rapidEditsWaitForTheirOwnAcknowledgementAndAllowCombinedReplies() {
        var progress = new SettingsProgress();
        assertFalse(progress.ready()); progress.acknowledge(20);
        int first = progress.submit(), second = progress.submit();
        progress.acknowledge(first);
        assertTrue(progress.waiting()); assertTrue(progress.confirmed(first)); assertFalse(progress.confirmed(second));
        progress.acknowledge(20); // A stale snapshot cannot undo an acknowledgement.
        assertTrue(progress.confirmed(first));
        int third = progress.submit(); progress.acknowledge(third);
        assertFalse(progress.waiting()); assertTrue(progress.confirmed(second));
        var rollover = new SettingsProgress(); rollover.acknowledge(Integer.MAX_VALUE - 1);
        int beforeWrap = rollover.submit(), afterWrap = rollover.submit();
        rollover.acknowledge(beforeWrap); assertTrue(rollover.waiting()); assertFalse(rollover.confirmed(afterWrap));
        rollover.acknowledge(afterWrap); assertFalse(rollover.waiting());
    }
}
