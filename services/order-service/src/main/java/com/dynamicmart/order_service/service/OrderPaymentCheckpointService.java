package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists Payment progress only after the remote Payment call has returned successfully. */
@Service
public class OrderPaymentCheckpointService {
    private final OrderSagaRepository sagas;
    private final CustomerOrderRepository orders;
    private final Clock clock;

    public OrderPaymentCheckpointService(
            OrderSagaRepository sagas,
            CustomerOrderRepository orders,
            Clock clock) {
        this.sagas = sagas;
        this.orders = orders;
        this.clock = clock;
    }

    @Transactional
    public void markPaymentRequested(UUID sagaId, UUID orderId, UUID paymentId, Instant paymentDueAt) {
        if (paymentId == null) {
            throw conflict("PAYMENT_ID_REQUIRED", "Payment Service không trả paymentId.");
        }
        OrderSaga saga = lockSaga(sagaId);
        CustomerOrder order = lockOrder(orderId);
        requireSameOrder(saga, order);
        if (saga.getStatus() == SagaStatus.PAYMENT_REQUESTED) {
            if (!Objects.equals(saga.getPaymentId(), paymentId)) {
                throw conflict("PAYMENT_CHECKPOINT_MISMATCH", "Retry trả về paymentId khác checkpoint đã lưu.");
            }
            return;
        }
        if (saga.getStatus() != SagaStatus.ORDER_CREATED || order.getFinalTotalVnd() <= 0) {
            throw invalidState(saga);
        }
        Instant now = Instant.now(clock);
        saga.markPaymentRequested(paymentId, now);
        order.setPaymentDueAt(paymentDueAt);
        order.setUpdatedAt(now);
    }

    @Transactional
    public void markPaymentNotRequired(UUID sagaId, UUID orderId) {
        OrderSaga saga = lockSaga(sagaId);
        CustomerOrder order = lockOrder(orderId);
        requireSameOrder(saga, order);
        if ("PAYMENT_NOT_REQUIRED".equals(saga.getCurrentStep())) {
            return;
        }
        if (saga.getStatus() != SagaStatus.ORDER_CREATED
                || order.getFinalTotalVnd() != 0
                || order.getPaymentTiming() != PaymentTiming.NOT_REQUIRED
                || order.getPaymentMethod() != PaymentMethod.FREE) {
            throw invalidState(saga);
        }
        saga.markPaymentNotRequired(Instant.now(clock));
    }

    private OrderSaga lockSaga(UUID sagaId) {
        return sagas.findByIdForUpdate(sagaId)
                .orElseThrow(() -> notFound("ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga."));
    }

    private CustomerOrder lockOrder(UUID orderId) {
        return orders.findForUpdate(orderId)
                .orElseThrow(() -> notFound("ORDER_NOT_FOUND", "Không tìm thấy Order để checkpoint Payment."));
    }

    private void requireSameOrder(OrderSaga saga, CustomerOrder order) {
        if (!Objects.equals(saga.getOrderId(), order.getId())) {
            throw conflict("PAYMENT_CHECKPOINT_ORDER_MISMATCH", "Saga và Order không đồng nhất ở bước Payment.");
        }
    }

    private OrderException invalidState(OrderSaga saga) {
        return conflict(
                "INVALID_PAYMENT_CHECKPOINT",
                "Không thể ghi Payment checkpoint từ trạng thái Saga " + saga.getStatus() + ".");
    }

    private OrderException notFound(String code, String message) {
        return new OrderException(HttpStatus.NOT_FOUND, code, message);
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }
}
