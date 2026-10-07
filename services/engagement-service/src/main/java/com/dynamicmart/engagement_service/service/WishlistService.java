package com.dynamicmart.engagement_service.service;

import com.dynamicmart.engagement_service.dto.request.AddWishlistItemRequest;
import com.dynamicmart.engagement_service.dto.response.WishlistResponse;
import com.dynamicmart.engagement_service.entity.Wishlist;
import com.dynamicmart.engagement_service.entity.WishlistItem;
import com.dynamicmart.engagement_service.mapper.WishlistMapper;
import com.dynamicmart.engagement_service.repository.WishlistItemRepository;
import com.dynamicmart.engagement_service.repository.WishlistRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {
    private final WishlistRepository wishlists;
    private final WishlistItemRepository items;
    private final WishlistMapper mapper;

    public WishlistService(WishlistRepository wishlists, WishlistItemRepository items, WishlistMapper mapper) {
        this.wishlists = wishlists;
        this.items = items;
        this.mapper = mapper;
    }

    @Transactional
    public WishlistResponse get(UUID customerId) {
        Wishlist wishlist = requireWishlist(customerId);
        return response(wishlist);
    }

    @Transactional
    public WishlistResponse add(UUID customerId, AddWishlistItemRequest request) {
        Wishlist wishlist = requireWishlist(customerId);
        Instant now = Instant.now();
        if (!items.existsByWishlistIdAndProductId(wishlist.getId(), request.productId())) {
            items.save(new WishlistItem(wishlist.getId(), request.productId(), now));
        }
        wishlist.touch(now);
        return response(wishlist);
    }

    @Transactional
    public void remove(UUID customerId, UUID productId) {
        Wishlist wishlist = requireWishlist(customerId);
        items.findByWishlistIdAndProductId(wishlist.getId(), productId).ifPresent(items::delete);
        wishlist.touch(Instant.now());
    }

    private Wishlist requireWishlist(UUID customerId) {
        return wishlists.findByCustomerId(customerId)
                .orElseGet(() -> wishlists.save(new Wishlist(customerId, Instant.now())));
    }

    private WishlistResponse response(Wishlist wishlist) {
        return mapper.toResponse(wishlist, items.findAllByWishlistIdOrderByCreatedAtDesc(wishlist.getId()));
    }
}
