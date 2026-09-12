package dev.everyonemek.botania;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WirelessFeeTest {
    @Test void packetSplittingKeepsTheSameCostAndPrepaidCredit() {
        int fees = 0, credit = 0;
        for (int i = 0; i < 100; i++) {
            var next = WirelessFee.forDelivery(1, 2, credit);
            fees += next.fee(); credit = next.remainingCredit();
        }
        assertEquals(new WirelessFee(fees, credit), WirelessFee.forDelivery(100, 2, 0));
        assertEquals(4, fees); assertEquals(0, credit);
        assertEquals(new WirelessFee(8, 0), WirelessFee.forDelivery(100, 4, 0));
        assertEquals(new WirelessFee(0, 37), WirelessFee.forDelivery(0, 2, 37));
        assertThrows(IllegalArgumentException.class, () -> WirelessFee.forDelivery(1, 5, 0));
    }
}
