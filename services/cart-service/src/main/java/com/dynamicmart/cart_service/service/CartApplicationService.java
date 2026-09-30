package com.dynamicmart.cart_service.service;

import com.dynamicmart.cart_service.dto.request.AddCartItemRequest;
import com.dynamicmart.cart_service.dto.request.UpdateCartItemRequest;
import com.dynamicmart.cart_service.dto.response.CartItemResponse;
import com.dynamicmart.cart_service.dto.response.CartResponse;
import com.dynamicmart.cart_service.client.CatalogClient;
import com.dynamicmart.cart_service.entity.Cart;
import com.dynamicmart.cart_service.entity.CartItem;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.CartItemRepository;
import com.dynamicmart.cart_service.repository.CartRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartApplicationService {
    private final CartRepository carts; private final CartItemRepository items; private final CatalogClient catalog;
    public CartApplicationService(CartRepository carts, CartItemRepository items, CatalogClient catalog) {
        this.carts = carts; this.items = items; this.catalog = catalog;
    }

    @Transactional
    public CartResponse get(UUID customerId) { return response(activeCart(customerId)); }

    @Transactional
    public CartResponse add(UUID customerId, AddCartItemRequest request) {
        Cart cart = activeCart(customerId); Instant now = Instant.now();
        CartItem item = items.findByCartIdAndVariantId(cart.getId(), request.variantId()).orElse(null);
        int next = item == null ? request.quantity() : item.getQuantity() + request.quantity();
        if (next > 99) throw invalidQuantity();
        catalog.requirePurchasable(request.productId(), request.variantId(), next);
        if (item == null) items.save(new CartItem(cart.getId(), request.productId(), request.variantId(), next, now));
        else {
            item.change(next, true, now);
        }
        cart.touch(now); return response(cart);
    }

    @Transactional
    public CartResponse update(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = activeCart(customerId); CartItem item = requireItem(cart, itemId);
        if (item.getVersion() != request.version()) throw new CartException(HttpStatus.CONFLICT, "CART_ITEM_VERSION_CONFLICT", "Dòng giỏ hàng đã thay đổi. Vui lòng tải lại.");
        if (request.quantity() == null && request.selected() == null) throw new CartException(HttpStatus.BAD_REQUEST, "CART_ITEM_CHANGE_EMPTY", "Không có thay đổi cho dòng giỏ hàng.");
        int quantity = request.quantity() == null ? item.getQuantity() : request.quantity();
        if (request.quantity() != null) catalog.requirePurchasable(item.getProductId(), item.getVariantId(), quantity);
        item.change(quantity, request.selected(), Instant.now());
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
        var cartItems = items.findAllByCartIdOrderByCreatedAtAsc(cart.getId());
        Map<UUID, CatalogClient.VariantSnapshot> snapshots;
        try {
            snapshots = catalog.validate(cartItems.stream()
                            .map(value -> new CatalogClient.VariantQuantity(value.getVariantId(), value.getQuantity())).toList())
                    .stream().collect(Collectors.toMap(CatalogClient.VariantSnapshot::variantId, value -> value));
        } catch (CartException exception) {
            snapshots = Map.of();
        }
        Map<UUID, CatalogClient.VariantSnapshot> catalogSnapshots = snapshots;
        return new CartResponse(cart.getId(), cart.getCustomerId(), cartItems.stream().map(value -> {
            var snapshot = catalogSnapshots.get(value.getVariantId());
            boolean available = snapshot != null && snapshot.purchasable() && snapshot.availableQuantity() >= value.getQuantity();
            String reason = snapshot == null ? "Không thể kiểm tra trạng thái Variant." :
                    (available ? null : (snapshot.unavailableReason() == null ? "Không đủ tồn kho." : snapshot.unavailableReason()));
            long listPrice = snapshot == null || snapshot.price() == null ? 0 : snapshot.price().listPriceVnd();
            long salePrice = snapshot == null || snapshot.price() == null ? 0 : snapshot.price().effectivePriceVnd();
            return new CartItemResponse(value.getId(), value.getProductId(), value.getVariantId(),
                    snapshot == null ? null : snapshot.productName(), snapshot == null ? null : snapshot.variantName(),
                    snapshot == null ? null : snapshot.imageUrl(), value.getQuantity(), value.getVersion(),
                    value.isSelected(), available, snapshot == null ? 0 : snapshot.availableQuantity(), reason,
                    listPrice, salePrice, value.getUpdatedAt());
        }).toList(), cart.getUpdatedAt());
    }
    private CartException invalidQuantity() { return new CartException(HttpStatus.UNPROCESSABLE_ENTITY, "CART_QUANTITY_INVALID", "Số lượng phải từ 1 đến 99."); }
}
