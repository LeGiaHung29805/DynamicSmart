package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.OrderFinalizationGateway;
import com.dynamicmart.order_service.client.OrderFinalizationGateway.FinalizationRequest;
import com.dynamicmart.order_service.client.OrderFinalizationGateway.PurchasedCartItem;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderFinalizationService {
    private final OrderSagaRepository sagas;
    private final CheckoutSessionItemRepository items;
    private final OrderFinalizationGateway gateway;

    public OrderFinalizationService(OrderSagaRepository sagas, CheckoutSessionItemRepository items,
                                    OrderFinalizationGateway gateway) {
        this.sagas = sagas;
        this.items = items;
        this.gateway = gateway;
    }

    public void finalizeIfConfirmed(CustomerOrder order) {
        if (order.getStatus() != OrderStatus.CONFIRMED) return;
        var saga = sagas.findByOrderId(order.getId()).orElseThrow(() -> missing("Không tìm thấy Saga của Order."));
        if (saga.getInventoryReservationId() == null) throw missing("Order chưa có Inventory reservation.");
        var purchased = items.findAllByCheckoutSessionId(order.getCheckoutSessionId()).stream()
                .filter(value -> value.getSourceCartItemId() != null)
                .map(value -> new PurchasedCartItem(value.getSourceCartItemId(), value.getQuantity(),
                        Objects.requireNonNull(value.getSourceCartItemVersion())))
                .toList();
        if (!purchased.isEmpty()) {
            gateway.finalizeConfirmed(new FinalizationRequest(order.getId(), order.getCustomerId(),
                    saga.getCorrelationId(), saga.getId(), saga.getInventoryReservationId(), purchased));
        }
    }

    private OrderException missing(String message) {
        return new OrderException(HttpStatus.CONFLICT, "ORDER_FINALIZATION_STATE_MISSING", message);
    }
}
