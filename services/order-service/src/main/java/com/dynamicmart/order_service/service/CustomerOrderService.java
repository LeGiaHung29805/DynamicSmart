package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.dto.response.CustomerOrderResponse;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderActorType;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OrderStatusHistory;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class CustomerOrderService {
    private final CustomerOrderRepository orders;
    private final OrderStatusHistoryRepository histories;
    private final OutboxEventRepository outbox;
    private final PaymentClient payments;
    private final OrderStateMachine stateMachine;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CustomerOrderService(CustomerOrderRepository orders, OrderStatusHistoryRepository histories,
            OutboxEventRepository outbox, PaymentClient payments, OrderStateMachine stateMachine,
            ObjectMapper objectMapper, Clock clock) {
        this.orders = orders;
        this.histories = histories;
        this.outbox = outbox;
        this.payments = payments;
        this.stateMachine = stateMachine;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CustomerOrderResponse get(UUID orderId, UUID customerId) {
        return response(orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(this::notFound));
    }

    @Transactional
    public CustomerOrderResponse confirmReceived(UUID orderId, UUID customerId) {
        CustomerOrder order = orders.findForUpdate(orderId).orElseThrow(this::notFound);
        if (!order.getCustomerId().equals(customerId)) throw notFound();
        if (order.getStatus() == OrderStatus.COMPLETED) return response(order);

        boolean paymentSucceeded = order.getPaymentSucceededAt() != null
                || order.getPaymentTiming() == PaymentTiming.NOT_REQUIRED;
        boolean cod = order.getPaymentTiming() == PaymentTiming.POSTPAID
                && order.getPaymentMethod() == PaymentMethod.COD;
        List<OrderStatus> transitions = stateMachine.customerConfirmReceived(
                order.getStatus(), order.getPaymentTiming(), paymentSucceeded || cod);
        if (cod) {
            payments.collectCodForReceivedOrder(orderId);
            paymentSucceeded = true;
        }

        Instant now = Instant.now(clock);
        UUID correlationId = UUID.randomUUID();
        boolean deliveredNow = false;
        for (OrderStatus next : transitions) {
            OrderStatus previous = order.getStatus();
            order.setStatus(next);
            order.setUpdatedAt(now);
            if (next == OrderStatus.DELIVERED) {
                order.setShipmentDeliveredAt(now);
                deliveredNow = true;
            }
            if (next == OrderStatus.COMPLETED) order.setCompletedAt(now);
            histories.save(OrderStatusHistory.transition(UUID.randomUUID(), orderId, previous, next,
                    OrderActorType.CUSTOMER, customerId, "CUSTOMER_CONFIRMED_RECEIVED", correlationId, now));
        }
        if (paymentSucceeded && order.getPaymentSucceededAt() == null) order.setPaymentSucceededAt(now);
        orders.save(order);
        if (deliveredNow) {
            outbox.save(OutboxEvent.pending(UUID.randomUUID(), "ORDER", orderId, "ShipmentDelivered", 1,
                    objectMapper.writeValueAsString(new ShipmentDeliveredPayload(orderId, customerId, now)),
                    correlationId, now));
        }
        return response(order);
    }

    private CustomerOrderResponse response(CustomerOrder order) {
        boolean canConfirm = order.getStatus() == (order.getPaymentTiming() == PaymentTiming.POSTPAID
                ? OrderStatus.HANDOVER_PENDING : OrderStatus.SHIPPING);
        return new CustomerOrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getPaymentTiming(), order.getPaymentMethod(), order.getFinalTotalVnd(), order.getCurrency(),
                canConfirm, order.getShipmentDeliveredAt(), order.getCompletedAt(), order.getCreatedAt());
    }

    private OrderException notFound() {
        return new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng.");
    }

    private record ShipmentDeliveredPayload(UUID orderId, UUID customerId, Instant deliveredAt) { }
}
