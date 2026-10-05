package com.dynamicmart.cart_service.service;

import com.dynamicmart.cart_service.client.CatalogClient;
import com.dynamicmart.cart_service.dto.response.WishlistResponse;
import com.dynamicmart.cart_service.entity.Wishlist;
import com.dynamicmart.cart_service.entity.WishlistItem;
import com.dynamicmart.cart_service.repository.WishlistItemRepository;
import com.dynamicmart.cart_service.repository.WishlistRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {
    private final WishlistRepository wishlists;
    private final WishlistItemRepository items;
    private final CatalogClient catalog;

    public WishlistService(WishlistRepository wishlists, WishlistItemRepository items, CatalogClient catalog) {
        this.wishlists = wishlists; this.items = items; this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public WishlistResponse get(UUID customerId) {
        return wishlists.findByCustomerId(customerId).map(this::response)
                .orElseGet(() -> new WishlistResponse(List.of()));
    }

    @Transactional
    public WishlistResponse add(UUID customerId, UUID productId) {
        catalog.requirePublicProduct(productId);
        Instant now = Instant.now();
        Wishlist wishlist = wishlists.findByCustomerId(customerId)
                .orElseGet(() -> wishlists.save(new Wishlist(customerId, now)));
        if (!items.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            items.save(new WishlistItem(wishlist.getId(), productId, now));
            wishlist.touch(now);
        }
        return response(wishlist);
    }

    @Transactional
    public WishlistResponse remove(UUID customerId, UUID productId) {
        Wishlist wishlist = wishlists.findByCustomerId(customerId).orElse(null);
        if (wishlist == null) return new WishlistResponse(List.of());
        items.deleteByWishlistIdAndProductId(wishlist.getId(), productId);
        wishlist.touch(Instant.now());
        return response(wishlist);
    }

    private WishlistResponse response(Wishlist wishlist) {
        return new WishlistResponse(items.findAllByWishlistIdOrderByCreatedAtDesc(wishlist.getId()).stream()
                .map(item -> new WishlistResponse.Item(item.getProductId(), item.getCreatedAt())).toList());
    }
}
