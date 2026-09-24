package com.dynamicmart.catalog_service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryReservationTest {
    @Test
    void terminalTransitionCannotBeAppliedTwice() {
        InventoryReservation reservation = new InventoryReservation(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(900));

        reservation.release("PAYMENT_FAILED", Instant.now());

        assertEquals(InventoryReservationStatus.RELEASED, reservation.getStatus());
        assertThrows(IllegalStateException.class,
                () -> reservation.release("PAYMENT_FAILED", Instant.now()));
    }
}
