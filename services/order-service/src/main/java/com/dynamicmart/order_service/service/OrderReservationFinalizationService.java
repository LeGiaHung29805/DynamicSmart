package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.InventoryReservationGateway.CommitInventoryRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ConsumeVoucherRequest;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Finalizes reservations only after an Order is confirmed; remote operations must be idempotent. */
@Service
public class OrderReservationFinalizationService {
    private final InventoryReservationGateway inventory;
    private final VoucherReservationGateway vouchers;
    private final OrderReservationFinalizationCheckpointService checkpoints;
    private final OrderSagaRepository sagas;
    private final CustomerOrderRepository orders;

    public OrderReservationFinalizationService(
            InventoryReservationGateway inventory,
            VoucherReservationGateway vouchers,
            OrderReservationFinalizationCheckpointService checkpoints,
            OrderSagaRepository sagas,
            CustomerOrderRepository orders) {
        this.inventory = inventory;
        this.vouchers = vouchers;
        this.checkpoints = checkpoints;
        this.sagas = sagas;
        this.orders = orders;
    }

    public FinalizationResult finalizeIfConfirmed(UUID sagaId, UUID orderId) {
        OrderSaga saga = saga(sagaId);
        CustomerOrder order = order(orderId);
        requireSameOrder(saga, order);
        requirePaymentCheckpoint(saga, order);

        if (saga.getStatus() == SagaStatus.COMPLETED) {
            return new FinalizationResult(true, false);
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            return new FinalizationResult(false, true);
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw conflict(
                    "RESERVATION_FINALIZATION_ORDER_STATE_INVALID",
                    "Chỉ Order CONFIRMED mới được hoàn tất reservation.");
        }
        if (saga.getInventoryReservationId() == null) {
            throw conflict(
                    "INVENTORY_RESERVATION_CHECKPOINT_MISSING",
                    "Saga thiếu Inventory reservation ID.");
        }

        SagaStatus status = saga.getStatus();
        if (status == SagaStatus.PAYMENT_REQUESTED || status == SagaStatus.ORDER_CREATED) {
            checkpoints.begin(sagaId, orderId);
            status = SagaStatus.FINALIZING_RESERVATIONS;
        }

        try {
            if (status == SagaStatus.FINALIZING_RESERVATIONS) {
                inventory.commit(new CommitInventoryRequest(
                        OrderReservationService.operationKey(sagaId, "INVENTORY_COMMIT"),
                        sagaId, saga.getCorrelationId(), orderId, saga.getInventoryReservationId()));
                checkpoints.markInventoryCommitted(sagaId, orderId);
                status = SagaStatus.INVENTORY_COMMITTED;
            }
            if (status == SagaStatus.INVENTORY_COMMITTED) {
                if (saga.getVoucherReservationId() != null) {
                    vouchers.consume(new ConsumeVoucherRequest(
                            OrderReservationService.operationKey(sagaId, "VOUCHER_CONSUME"),
                            sagaId, saga.getCorrelationId(), orderId, saga.getVoucherReservationId()));
                }
                checkpoints.markCompleted(sagaId, orderId);
                return new FinalizationResult(true, false);
            }
        } catch (RuntimeException failure) {
            try {
                checkpoints.recordFailure(sagaId, orderId, failure);
            } catch (RuntimeException checkpointFailure) {
                failure.addSuppressed(checkpointFailure);
            }
            throw failure;
        }
        throw conflict(
                "RESERVATION_FINALIZATION_NOT_READY",
                "Saga chưa sẵn sàng hoàn tất reservation từ trạng thái " + status + ".");
    }

    private OrderSaga saga(UUID sagaId) {
        return sagas.findById(sagaId)
                .orElseThrow(() -> new OrderException(
                        HttpStatus.NOT_FOUND, "ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga."));
    }

    private CustomerOrder order(UUID orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new OrderException(
                        HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy Order."));
    }

    private void requireSameOrder(OrderSaga saga, CustomerOrder order) {
        if (!Objects.equals(saga.getOrderId(), order.getId())) {
            throw conflict("ORDER_SAGA_ORDER_MISMATCH", "Order không thuộc Create Order Saga.");
        }
    }

    private void requirePaymentCheckpoint(OrderSaga saga, CustomerOrder order) {
        if (order.getFinalTotalVnd() > 0 && saga.getPaymentId() == null) {
            throw conflict(
                    "PAYMENT_CHECKPOINT_INCOMPLETE",
                    "Order có số tiền cần thanh toán nhưng Saga thiếu paymentId.");
        }
        if (order.getFinalTotalVnd() == 0 && saga.getPaymentId() != null) {
            throw conflict(
                    "PAYMENT_CHECKPOINT_MISMATCH",
                    "Order miễn phí không được gắn Payment context.");
        }
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public record FinalizationResult(boolean completed, boolean waitingForPayment) {
    }
}
