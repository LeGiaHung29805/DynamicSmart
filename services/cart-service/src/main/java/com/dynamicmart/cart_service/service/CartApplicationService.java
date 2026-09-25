package com.dynamicmart.cart_service.service;

import com.dynamicmart.cart_service.dto.request.AddCartItemRequest;
import com.dynamicmart.cart_service.dto.request.UpdateCartItemRequest;
import com.dynamicmart.cart_service.dto.response.CartItemResponse;
import com.dynamicmart.cart_service.dto.response.CartResponse;
import com.dynamicmart.cart_service.entity.Cart;
import com.dynamicmart.cart_service.entity.CartItem;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.CartItemRepository;
import com.dynamicmart.cart_service.repository.CartRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartApplicationService {
    private final CartRepository carts; private final CartItemRepository items;
    public CartApplicationService(CartRepository carts, CartItemRepository items) { this.carts = carts; this.items = items; }

    @Transactional
    public CartResponse get(UUID customerId) { return response(activeCart(customerId)); }

    @Transactional
    public CartResponse add(UUID customerId, AddCartItemRequest request) {
        Cart cart = activeCart(customerId); Instant now = Instant.now();
        CartItem item = items.findByCartIdAndVariantId(cart.getId(), request.variantId()).orElse(null);
        if (item == null) items.save(new CartItem(cart.getId(), request.productId(), request.variantId(), request.quantity(), now));
        else {
            int next = item.getQuantity() + request.quantity();
            if (next > 99) throw invalidQuantity();
            item.change(next, true, now);
        }
        cart.touch(now); return response(cart);
    }

    @Transactional
    public CartResponse update(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = activeCart(customerId); CartItem item = requireItem(cart, itemId);
        if (item.getVersion() != request.version()) throw new CartException(HttpStatus.CONFLICT, "CART_ITEM_VERSION_CONFLICT", "Dòng giỏ hàng đã thay đổi. Vui lòng tải lại.");
        if (request.quantity() == null && request.selected() == null) throw new CartException(HttpStatus.BAD_REQUEST, "CART_ITEM_CHANGE_EMPTY", "Không có thay đổi cho dòng giỏ hàng.");
        item.change(request.quantity() == null ? item.getQuantity() : request.quantity(), request.selected(), Instant.now());
        cart.touch(Instant.now()); return response(cart);
    }

    @Transactional
    public void remove(UUID customerId, UUID itemId) {
        Cart cart = activeCart(customerId); items.delete(requireItem(cart, itemId)); cart.touch(Instant.now());
    }

    private Cart activeCart(UUID customerId) {
        return carts.findByCustomerIdAndStatus(customerId, "ACTIVE").orElseGet(() -> carts.save(new Cart(customerId, Instant.now())));
    }
    private CartItem requireItem(Cart cart, UUID itemId) {
        return items.findByIdAndCartId(itemId, cart.getId()).orElseThrow(() -> new CartException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Không tìm thấy dòng giỏ hàng."));
    }
    private CartResponse response(Cart cart) {
        return new CartResponse(cart.getId(), cart.getCustomerId(), items.findAllByCartIdOrderByCreatedAtAsc(cart.getId()).stream()
                .map(value -> new CartItemResponse(value.getId(), value.getProductId(), value.getVariantId(), value.getQuantity(),
                        value.getVersion(), value.isSelected(), value.getUpdatedAt())).toList(), cart.getUpdatedAt());
    }
    private CartException invalidQuantity() { return new CartException(HttpStatus.UNPROCESSABLE_ENTITY, "CART_QUANTITY_INVALID", "Số lượng phải từ 1 đến 99."); }
}
