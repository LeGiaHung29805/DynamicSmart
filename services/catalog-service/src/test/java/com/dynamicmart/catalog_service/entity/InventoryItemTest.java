package com.dynamicmart.catalog_service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryItemTest {
    @Test
    void reserveCommitAndReleaseNeverMakeInventoryNegative() {
        InventoryItem inventory = new InventoryItem(UUID.randomUUID(), 5);

        inventory.reserve(3);
        assertEquals(2, inventory.getAvailableQuantity());

        inventory.release(1);
        assertEquals(3, inventory.getAvailableQuantity());

        inventory.commit(2);
        assertEquals(3, inventory.getOnHandQuantity());
        assertEquals(0, inventory.getReservedQuantity());
    }

    @Test
    void rejectsOversellAndInvalidAdjustment() {
        InventoryItem inventory = new InventoryItem(UUID.randomUUID(), 1);

        assertThrows(IllegalStateException.class, () -> inventory.reserve(2));
        inventory.reserve(1);
        assertThrows(IllegalStateException.class, () -> inventory.adjust(-1));
        assertEquals(1, inventory.getOnHandQuantity());
        assertEquals(1, inventory.getReservedQuantity());
    }
}
