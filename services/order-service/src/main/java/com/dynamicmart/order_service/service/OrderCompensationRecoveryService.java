package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.InventoryReservationGateway.ReleaseInventoryRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ReleaseVoucherRequest;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Resumes reverse-order release when a process stopped while the Saga was COMPENSATING. */
@Service
public class OrderCompensationRecoveryService {
    private final InventoryReservationGateway inventory;
    private final VoucherReservationGateway vouchers;
    private final OrderSagaCheckpointService checkpoints;
    private final OrderSagaRepository sagas;

    public OrderCompensationRecoveryService(
            InventoryReservationGateway inventory,
            VoucherReservationGateway vouchers,
            OrderSagaCheckpointService checkpoints,
            OrderSagaRepository sagas) {
        this.inventory = inventory;
        this.vouchers = vouchers;
        this.checkpoints = checkpoints;
        this.sagas = sagas;
    }

    public void resume(UUID sagaId) {
        OrderSaga saga = sagas.findById(sagaId)
                .orElseThrow(() -> new OrderException(
                        HttpStatus.NOT_FOUND, "ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga."));
        if (saga.getStatus() == SagaStatus.COMPENSATED) {
            return;
        }
        if (saga.getStatus() != SagaStatus.COMPENSATING) {
            throw new OrderException(
                    HttpStatus.CONFLICT,
                    "SAGA_NOT_COMPENSATING",
                    "Chỉ Saga COMPENSATING mới được phục hồi compensation.");
        }

        try {
            if (saga.getInventoryReservationId() != null) {
                inventory.release(new ReleaseInventoryRequest(
                        OrderReservationService.operationKey(sagaId, "INVENTORY_RELEASE"), sagaId,
                        saga.getCorrelationId(), saga.getInventoryReservationId(), recoveryReason(saga)));
            }
            if (saga.getVoucherReservationId() != null) {
                vouchers.release(new ReleaseVoucherRequest(
                        OrderReservationService.operationKey(sagaId, "VOUCHER_RELEASE"), sagaId,
                        saga.getCorrelationId(), saga.getVoucherReservationId(), recoveryReason(saga)));
            }
            checkpoints.markCompensated(sagaId);
        } catch (RuntimeException failure) {
            try {
                checkpoints.recordCompensationFailure(sagaId, failure);
            } catch (RuntimeException checkpointFailure) {
                failure.addSuppressed(checkpointFailure);
            }
            throw failure;
        }
    }

    private String recoveryReason(OrderSaga saga) {
        return saga.getLastErrorCode() == null || saga.getLastErrorCode().isBlank()
                ? "ORDER_SAGA_RECOVERY"
                : saga.getLastErrorCode();
    }
}
