package com.dynamicmart.cart_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.cart_service.client.CatalogClient;
import com.dynamicmart.cart_service.entity.Wishlist;
import com.dynamicmart.cart_service.entity.WishlistItem;
import com.dynamicmart.cart_service.repository.WishlistItemRepository;
import com.dynamicmart.cart_service.repository.WishlistRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WishlistServiceTest {
    private final WishlistRepository wishlists = mock(WishlistRepository.class);
    private final WishlistItemRepository items = mock(WishlistItemRepository.class);
    private final CatalogClient catalog = mock(CatalogClient.class);
    private final WishlistService service = new WishlistService(wishlists, items, catalog);

    @Test
    void addingTheSameProductTwiceDoesNotCreateADuplicate() {
        UUID customerId = UUID.randomUUID(); UUID productId = UUID.randomUUID();
        Wishlist wishlist = new Wishlist(customerId, Instant.now());
        when(wishlists.findByCustomerId(customerId)).thenReturn(Optional.of(wishlist));
        when(items.existsByWishlistIdAndProductId(wishlist.getId(), productId)).thenReturn(true);
        when(items.findAllByWishlistIdOrderByCreatedAtDesc(wishlist.getId())).thenReturn(List.of());

        var result = service.add(customerId, productId);

        verify(catalog).requirePublicProduct(productId);
        verify(items, never()).save(org.mockito.ArgumentMatchers.any());
        assertThat(result.items()).isEmpty();
    }

    @Test
    void removingAProductOnlyUsesTheCurrentCustomersWishlist() {
        UUID customerId = UUID.randomUUID(); UUID productId = UUID.randomUUID();
        Wishlist wishlist = new Wishlist(customerId, Instant.now());
        when(wishlists.findByCustomerId(customerId)).thenReturn(Optional.of(wishlist));
        when(items.findAllByWishlistIdOrderByCreatedAtDesc(wishlist.getId())).thenReturn(List.of(
                new WishlistItem(wishlist.getId(), UUID.randomUUID(), Instant.now())));

        var result = service.remove(customerId, productId);

        verify(items).deleteByWishlistIdAndProductId(wishlist.getId(), productId);
        assertThat(result.items()).hasSize(1);
    }
}
