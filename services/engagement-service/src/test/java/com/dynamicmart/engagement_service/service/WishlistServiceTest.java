package com.dynamicmart.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.engagement_service.dto.request.AddWishlistItemRequest;
import com.dynamicmart.engagement_service.entity.Wishlist;
import com.dynamicmart.engagement_service.entity.WishlistItem;
import com.dynamicmart.engagement_service.mapper.WishlistMapper;
import com.dynamicmart.engagement_service.repository.WishlistItemRepository;
import com.dynamicmart.engagement_service.repository.WishlistRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WishlistServiceTest {
    private final WishlistRepository wishlists = org.mockito.Mockito.mock(WishlistRepository.class);
    private final WishlistItemRepository items = org.mockito.Mockito.mock(WishlistItemRepository.class);
    private final WishlistService service = new WishlistService(wishlists, items, new WishlistMapper());

    @Test
    void duplicateProductIsNotInsertedAgain() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Wishlist wishlist = new Wishlist(customerId, Instant.now());
        WishlistItem existing = new WishlistItem(wishlist.getId(), productId, Instant.now());
        when(wishlists.findByCustomerId(customerId)).thenReturn(Optional.of(wishlist));
        when(items.existsByWishlistIdAndProductId(wishlist.getId(), productId)).thenReturn(true);
        when(items.findAllByWishlistIdOrderByCreatedAtDesc(wishlist.getId())).thenReturn(List.of(existing));

        var response = service.add(customerId, new AddWishlistItemRequest(productId));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(productId);
        verify(items, never()).save(any(WishlistItem.class));
    }

    @Test
    void removeUsesOnlyWishlistOwnedByAuthenticatedCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Wishlist wishlist = new Wishlist(customerId, Instant.now());
        WishlistItem item = new WishlistItem(wishlist.getId(), productId, Instant.now());
        when(wishlists.findByCustomerId(customerId)).thenReturn(Optional.of(wishlist));
        when(items.findByWishlistIdAndProductId(wishlist.getId(), productId)).thenReturn(Optional.of(item));

        service.remove(customerId, productId);

        verify(items).findByWishlistIdAndProductId(wishlist.getId(), productId);
        verify(items).delete(item);
    }

    @Test
    void missingWishlistIsCreatedForCurrentCustomerOnly() {
        UUID customerId = UUID.randomUUID();
        when(wishlists.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(wishlists.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findAllByWishlistIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        var response = service.get(customerId);

        assertThat(response.customerId()).isEqualTo(customerId);
        verify(wishlists).findByCustomerId(customerId);
    }
}
