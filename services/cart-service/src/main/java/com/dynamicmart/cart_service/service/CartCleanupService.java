package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.InternalCartDtos.*;

import com.dynamicmart.cart_service.entity.Cart;
import com.dynamicmart.cart_service.entity.CartItem;
import com.dynamicmart.cart_service.entity.ProcessedEvent;
import com.dynamicmart.cart_service.repository.CartItemRepository;
import com.dynamicmart.cart_service.repository.CartRepository;
import com.dynamicmart.cart_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartCleanupService {
    private final CartRepository carts; private final CartItemRepository items; private final ProcessedEventRepository processed;
    public CartCleanupService(CartRepository carts, CartItemRepository items, ProcessedEventRepository processed) {
        this.carts = carts; this.items = items; this.processed = processed;
    }
    @Transactional
    public CartCleanupResponse orderConfirmed(OrderConfirmedRequest request) {
        if (processed.existsById(request.eventId())) return new CartCleanupResponse(0, true);
        int removed = 0;
        if ("CART".equals(request.source())) {
            Cart cart = carts.findByCustomerIdAndStatus(request.customerId(), "ACTIVE").orElse(null);
            if (cart != null) for (PurchasedCartItem snapshot : request.items()) {
                CartItem item = items.findByIdAndCartId(snapshot.id(), cart.getId()).orElse(null);
                if (item != null && item.isSelected() && item.getQuantity() == snapshot.quantity() && item.getVersion() == snapshot.version()) {
                    items.delete(item); removed++;
                }
            }
            if (removed > 0) cart.touch(Instant.now());
        }
        processed.save(new ProcessedEvent(request.eventId(), "OrderConfirmed", "order-service", request.correlationId(), Instant.now()));
        return new CartCleanupResponse(removed, false);
    }
}
