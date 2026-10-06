package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.PurchasedCartItem;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderOperationLog;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderOperationLogRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Enqueues Cart cleanup only after reservation finalization reached its durable final checkpoint. */
@Service
public class OrderConfirmationEventService {
    private static final String OPERATION_TYPE = "ENQUEUE_ORDER_CONFIRMED";

    private final CustomerOrderRepository orders;
    private final OrderSagaRepository sagas;
    private final CheckoutSessionRepository sessions;
    private final CheckoutSessionItemRepository items;
    private final OrderOperationLogRepository operationLogs;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OrderConfirmationEventService(
            CustomerOrderRepository orders,
            OrderSagaRepository sagas,
            CheckoutSessionRepository sessions,
            CheckoutSessionItemRepository items,
            OrderOperationLogRepository operationLogs,
            OutboxEventRepository outbox,
            ObjectMapper objectMapper,
            Clock clock) {
        this.orders = orders;
        this.sagas = sagas;
        this.sessions = sessions;
        this.items = items;
        this.operationLogs = operationLogs;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public EnqueueResult enqueueIfEligible(UUID orderId) {
        CustomerOrder order = orders.findForUpdate(orderId)
                .orElseThrow(() -> notFound("ORDER_NOT_FOUND", "Không tìm thấy Order."));
        CheckoutSession session = sessions.findById(order.getCheckoutSessionId())
                .orElseThrow(() -> conflict("CHECKOUT_SESSION_NOT_FOUND", "Order thiếu Checkout Session nguồn."));
        if (session.getSource() != CheckoutSource.CART) {
            return new EnqueueResult(null, false, true);
        }

        UUID operationKey = operationKey(orderId);
        OrderOperationLog existing = operationLogs.findById(operationKey).orElse(null);
        if (existing != null) {
            requireSameOperation(existing, orderId);
            ConfirmationCheckpoint checkpoint = objectMapper.readValue(
                    existing.getResultPayload(), ConfirmationCheckpoint.class);
            return new EnqueueResult(checkpoint.eventId(), true, false);
        }

        requireConfirmed(order);
        OrderSaga saga = sagas.findByOrderId(orderId)
                .orElseThrow(() -> conflict("ORDER_SAGA_NOT_FOUND", "Order thiếu Create Order Saga."));
        if (saga.getStatus() != SagaStatus.INVENTORY_COMMITTED
                && saga.getStatus() != SagaStatus.COMPLETED) {
            throw conflict("ORDER_CONFIRMATION_NOT_READY",
                    "Reservation chưa hoàn tất nên chưa thể phát OrderConfirmed.");
        }

        List<PurchasedCartItem> purchased = items.findAllByCheckoutSessionId(session.getId()).stream()
                .map(this::purchasedItem)
                .toList();
        if (purchased.isEmpty()) {
            throw conflict("ORDER_CONFIRMED_ITEMS_MISSING",
                    "Checkout CART không có item snapshot để dọn Giỏ hàng.");
        }

        Instant now = Instant.now(clock);
        UUID eventId = UUID.randomUUID();
        OrderConfirmedPayload payload = new OrderConfirmedPayload(
                order.getId(), order.getCustomerId(), CheckoutSource.CART.name(), purchased,
                order.getConfirmedAt() == null ? now : order.getConfirmedAt());
        outbox.save(OutboxEvent.pending(
                eventId, "ORDER", order.getId(), "OrderConfirmed", 1,
                objectMapper.writeValueAsString(payload), saga.getCorrelationId(), now));
        operationLogs.save(OrderOperationLog.completed(
                operationKey, OPERATION_TYPE, orderId,
                objectMapper.writeValueAsString(new ConfirmationCheckpoint(eventId)), now));
        return new EnqueueResult(eventId, false, false);
    }

    private PurchasedCartItem purchasedItem(CheckoutSessionItem item) {
        if (item.getSourceCartItemId() == null || item.getSourceCartItemVersion() == null) {
            throw conflict("ORDER_CONFIRMED_ITEM_CONTRACT_INVALID",
                    "Checkout CART thiếu CartItem ID/version snapshot.");
        }
        return new PurchasedCartItem(
                item.getSourceCartItemId(), item.getQuantity(), item.getSourceCartItemVersion());
    }

    private void requireConfirmed(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw conflict("ORDER_NOT_CONFIRMED", "Order chưa được xác nhận hoặc đã bị hủy.");
        }
    }

    private void requireSameOperation(OrderOperationLog existing, UUID orderId) {
        if (!OPERATION_TYPE.equals(existing.getOperationType())
                || !Objects.equals(orderId, existing.getOrderId())) {
            throw conflict("ORDER_CONFIRMATION_OPERATION_MISMATCH",
                    "Checkpoint OrderConfirmed không khớp Order.");
        }
    }

    static UUID operationKey(UUID orderId) {
        return UUID.nameUUIDFromBytes(("ORDER_CONFIRMED:" + orderId).getBytes(StandardCharsets.UTF_8));
    }

    private OrderException notFound(String code, String message) {
        return new OrderException(HttpStatus.NOT_FOUND, code, message);
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public record EnqueueResult(UUID eventId, boolean replay, boolean skippedBuyNow) {
    }

    public record OrderConfirmedPayload(
            UUID orderId,
            UUID customerId,
            String source,
            List<PurchasedCartItem> items,
            Instant confirmedAt) {
    }

    private record ConfirmationCheckpoint(UUID eventId) {
    }
}
