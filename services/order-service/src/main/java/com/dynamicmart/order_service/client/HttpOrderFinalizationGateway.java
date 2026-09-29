package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpOrderFinalizationGateway implements OrderFinalizationGateway {
    private final RestClient catalog;
    private final RestClient cart;

    public HttpOrderFinalizationGateway(
            @Qualifier("catalogRestClient") RestClient catalog,
            @Qualifier("cartRestClient") RestClient cart) {
        this.catalog = catalog;
        this.cart = cart;
    }

    @Override
    public void finalizeConfirmed(FinalizationRequest request) {
        try {
            catalog.post()
                    .uri("/api/v1/catalog/internal/inventory/reservations/{id}/commit", request.inventoryReservationId())
                    .header("Idempotency-Key", operationKey(request.orderId(), "INVENTORY_COMMIT").toString())
                    .body(new CommitInventory(request.orderId())).retrieve().toBodilessEntity();
            cart.post()
                    .uri("/api/v1/cart/internal/voucher-reservation-groups/{id}/consume", request.sagaId())
                    .body(new ConsumeVoucher(request.orderId())).retrieve().toBodilessEntity();
            cart.post().uri("/api/v1/cart/internal/order-confirmed")
                    .body(new OrderConfirmed(
                            operationKey(request.orderId(), "CART_CLEANUP"), request.correlationId(),
                            request.customerId(), "CART", request.items()))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_FINALIZATION_UNAVAILABLE",
                    "Chưa thể chốt tồn kho, voucher hoặc giỏ hàng; có thể thử lại an toàn.");
        }
    }

    private UUID operationKey(UUID orderId, String operation) {
        return UUID.nameUUIDFromBytes(("ORDER|" + orderId + "|" + operation).getBytes(StandardCharsets.UTF_8));
    }

    private record CommitInventory(UUID orderId) { }
    private record ConsumeVoucher(UUID orderId) { }
    private record OrderConfirmed(UUID eventId, UUID correlationId, UUID customerId, String source,
                                  List<PurchasedCartItem> items) { }
}
