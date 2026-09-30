package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.InternalCartDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.dynamicmart.cart_service.entity.Cart;
import com.dynamicmart.cart_service.entity.CartItem;
import com.dynamicmart.cart_service.repository.CartItemRepository;
import com.dynamicmart.cart_service.repository.CartRepository;
import com.dynamicmart.cart_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartCleanupServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
    private final CartCleanupService service = new CartCleanupService(carts, items, processed);

    @Test
    void removesOnlySelectedItemWhoseQuantityAndVersionMatchSnapshot() {
        UUID customer = UUID.randomUUID(); Cart cart = new Cart(customer, Instant.now());
        CartItem matching = new CartItem(cart.getId(), UUID.randomUUID(), UUID.randomUUID(), 2, Instant.now());
        CartItem changed = new CartItem(cart.getId(), UUID.randomUUID(), UUID.randomUUID(), 1, Instant.now());
        changed.change(2, true, Instant.now());
        when(carts.findByCustomerIdAndStatus(customer, "ACTIVE")).thenReturn(Optional.of(cart));
        when(items.findByIdAndCartId(matching.getId(), cart.getId())).thenReturn(Optional.of(matching));
        when(items.findByIdAndCartId(changed.getId(), cart.getId())).thenReturn(Optional.of(changed));
        var request = new OrderConfirmedRequest(UUID.randomUUID(), UUID.randomUUID(), customer, "CART", List.of(
                new PurchasedCartItem(matching.getId(), 2, 0), new PurchasedCartItem(changed.getId(), 1, 0)));

        CartCleanupResponse result = service.orderConfirmed(request);

        assertThat(result.removedCount()).isEqualTo(1);
        verify(items).delete(matching); verify(items, never()).delete(changed); verify(processed).save(any());
    }

    @Test
    void duplicateOrderConfirmedEventDoesNothing() {
        UUID eventId = UUID.randomUUID(); when(processed.existsById(eventId)).thenReturn(true);
        var result = service.orderConfirmed(new OrderConfirmedRequest(eventId, null, UUID.randomUUID(), "CART",
                List.of(new PurchasedCartItem(UUID.randomUUID(), 1, 0))));
        assertThat(result.duplicate()).isTrue(); verifyNoInteractions(carts, items);
    }
}
