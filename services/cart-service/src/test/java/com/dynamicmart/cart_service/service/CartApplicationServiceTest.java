package com.dynamicmart.cart_service.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dynamicmart.cart_service.client.CatalogClient;
import com.dynamicmart.cart_service.dto.request.AddCartItemRequest;
import com.dynamicmart.cart_service.entity.Cart;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.CartItemRepository;
import com.dynamicmart.cart_service.repository.CartRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartApplicationServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final CatalogClient catalog = mock(CatalogClient.class);
    private final CartApplicationService service = new CartApplicationService(carts, items, catalog);

    @Test
    void unavailableVariantIsNeverAddedToCart() {
        UUID customer = UUID.randomUUID(), product = UUID.randomUUID(), variant = UUID.randomUUID();
        Cart cart = new Cart(customer, Instant.now());
        when(carts.findByCustomerIdAndStatus(customer, "ACTIVE")).thenReturn(Optional.of(cart));
        when(items.findByCartIdAndVariantId(cart.getId(), variant)).thenReturn(Optional.empty());
        when(catalog.requirePurchasable(product, variant, 2)).thenThrow(new CartException(
                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "CART_VARIANT_UNAVAILABLE", "Hết hàng"));

        assertThatThrownBy(() -> service.add(customer, new AddCartItemRequest(product, variant, 2)))
                .isInstanceOf(CartException.class).hasMessageContaining("Hết hàng");
        verify(items, never()).save(any());
    }
}
