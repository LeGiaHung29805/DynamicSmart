package com.dynamicmart.engagement_service.mapper;

import com.dynamicmart.engagement_service.dto.response.WishlistItemResponse;
import com.dynamicmart.engagement_service.dto.response.WishlistResponse;
import com.dynamicmart.engagement_service.entity.Wishlist;
import com.dynamicmart.engagement_service.entity.WishlistItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {
    public WishlistResponse toResponse(Wishlist wishlist, List<WishlistItem> items) {
        return new WishlistResponse(wishlist.getId(), wishlist.getCustomerId(),
                items.stream().map(this::toItem).toList(), wishlist.getUpdatedAt());
    }

    private WishlistItemResponse toItem(WishlistItem item) {
        return new WishlistItemResponse(item.getId(), item.getProductId(), item.getCreatedAt());
    }
}
