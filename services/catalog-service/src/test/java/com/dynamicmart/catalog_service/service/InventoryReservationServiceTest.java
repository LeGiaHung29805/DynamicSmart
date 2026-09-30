package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.dto.request.InventoryReservationItemRequest;
import com.dynamicmart.catalog_service.dto.request.ReserveInventoryRequest;
import com.dynamicmart.catalog_service.dto.response.InventoryReservationResponse;
import com.dynamicmart.catalog_service.entity.Category;
import com.dynamicmart.catalog_service.entity.InventoryItem;
import com.dynamicmart.catalog_service.entity.InventoryOperationType;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.InventoryReservationItemRepository;
import com.dynamicmart.catalog_service.repository.InventoryReservationRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {
    @Mock private InventoryReservationRepository reservationRepository;
    @Mock private InventoryReservationItemRepository reservationItemRepository;
    @Mock private InventoryItemRepository inventoryRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InventoryOperationIdempotency idempotency;
    @Mock private CatalogOutboxService outboxService;

    private InventoryReservationService service;
    private UUID variantId;
    private InventoryItem inventory;

    @BeforeEach
    void setUp() {
        service = new InventoryReservationService(reservationRepository, reservationItemRepository,
                inventoryRepository, variantRepository, productRepository, categoryRepository,
                idempotency, outboxService);
        variantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant(variantId, productId, "LAST-ONE", null,
                1_000_000, 1000, 10, 10, 10, 0);
        variant.activate();
        Product product = new Product(productId, categoryId, "Last item", "last-item",
                null, null, 1000, 10, 10, 10);
        product.publish(Instant.now().minusSeconds(60));
        Category category = new Category(categoryId, null, "PHONE", "Phone", "phone", null, 0);
        inventory = new InventoryItem(variantId, 1);

        when(idempotency.hash(any())).thenReturn("hash");
        when(reservationRepository.findByCheckoutSessionIdForUpdate(any())).thenReturn(Optional.empty());
        when(variantRepository.findAllByIdIn(anyList())).thenReturn(List.of(variant));
        when(variantRepository.findAllByIdInForShare(anyList())).thenReturn(List.of(variant));
        when(productRepository.findAllByIdInForShare(anyList())).thenReturn(List.of(product));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
        when(inventoryRepository.findAllByVariantIdInForUpdate(anyList())).thenReturn(List.of(inventory));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void retryWithSameOperationDoesNotReserveTwice() {
        UUID operationKey = UUID.randomUUID();
        ReserveInventoryRequest request = request(UUID.randomUUID());
        when(idempotency.replay(eq(operationKey), eq(InventoryOperationType.RESERVE), eq("hash"),
                eq(InventoryReservationResponse.class))).thenReturn(Optional.empty());

        InventoryReservationResponse first = service.reserve(operationKey, request);
        when(idempotency.replay(eq(operationKey), eq(InventoryOperationType.RESERVE), eq("hash"),
                eq(InventoryReservationResponse.class))).thenReturn(Optional.of(first));

        InventoryReservationResponse retried = service.reserve(operationKey, request);

        assertEquals(first, retried);
        assertEquals(1, inventory.getReservedQuantity());
        assertEquals(0, inventory.getAvailableQuantity());
    }

    @Test
    void secondBuyerCannotReserveLastAvailableItem() {
        UUID firstOperation = UUID.randomUUID();
        UUID secondOperation = UUID.randomUUID();
        when(idempotency.replay(any(), eq(InventoryOperationType.RESERVE), eq("hash"),
                eq(InventoryReservationResponse.class))).thenReturn(Optional.empty());

        service.reserve(firstOperation, request(UUID.randomUUID()));
        CatalogException exception = assertThrows(CatalogException.class,
                () -> service.reserve(secondOperation, request(UUID.randomUUID())));

        assertEquals("INSUFFICIENT_INVENTORY", exception.getCode());
        assertEquals(1, inventory.getReservedQuantity());
    }

    private ReserveInventoryRequest request(UUID checkoutSessionId) {
        return new ReserveInventoryRequest(checkoutSessionId, Instant.now().plusSeconds(900),
                List.of(new InventoryReservationItemRequest(variantId, 1)));
    }
}
