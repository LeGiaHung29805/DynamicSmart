package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.dto.request.AdjustInventoryRequest;
import com.dynamicmart.catalog_service.entity.InventoryItem;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.InventoryAdjustmentRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentServiceTest {
    @Mock private InventoryItemRepository inventoryRepository;
    @Mock private InventoryAdjustmentRepository adjustmentRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PostgresAdvisoryLock advisoryLock;
    @Mock private CatalogOutboxService outboxService;

    @InjectMocks
    private InventoryAdjustmentService adjustmentService;

    @Test
    void cannotReduceOnHandBelowReservedQuantity() {
        UUID variantId = UUID.randomUUID();
        UUID operationKey = UUID.randomUUID();
        InventoryItem inventory = new InventoryItem(variantId, 5);
        inventory.reserve(4);
        when(adjustmentRepository.findByOperationKey(operationKey)).thenReturn(Optional.empty());
        when(variantRepository.existsById(variantId)).thenReturn(true);
        when(inventoryRepository.findByVariantIdForUpdate(variantId)).thenReturn(Optional.of(inventory));

        CatalogException exception = assertThrows(CatalogException.class,
                () -> adjustmentService.adjust(variantId, operationKey, UUID.randomUUID(),
                        new AdjustInventoryRequest(-2, "Stock count correction")));

        assertEquals("INVENTORY_ADJUSTMENT_REJECTED", exception.getCode());
        assertEquals(5, inventory.getOnHandQuantity());
        assertEquals(4, inventory.getReservedQuantity());
    }
}
