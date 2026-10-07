package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.PaymentClient.OrderPaymentContextRequest;
import com.dynamicmart.order_service.client.PaymentClient.PaymentResponse;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Creates the Payment-owned order context without holding an Order database transaction. */
@Service
public class OrderPaymentCreationService {
    private final PaymentClient payments;
    private final OrderPaymentCheckpointService checkpoints;

    public OrderPaymentCreationService(PaymentClient payments, OrderPaymentCheckpointService checkpoints) {
        this.payments = payments;
        this.checkpoints = checkpoints;
    }

    public PaymentCheckpoint ensurePayment(PaymentCommand command) {
        if (command.sagaStatus() == SagaStatus.PAYMENT_REQUESTED) {
            if (command.existingPaymentId() == null) {
                throw conflict("PAYMENT_CHECKPOINT_INCOMPLETE", "Saga PAYMENT_REQUESTED thiếu paymentId.");
            }
            PaymentResponse response = payments.getPayment(command.existingPaymentId());
            validateResponse(command, response);
            return new PaymentCheckpoint(response.id(), response.expiresAt(), response.redirectUrl(), false);
        }
        if (command.sagaStatus() == SagaStatus.COMPLETED
                || command.sagaStatus() == SagaStatus.FINALIZING_RESERVATIONS
                || command.sagaStatus() == SagaStatus.INVENTORY_COMMITTED) {
            if (command.finalTotalVnd() > 0 && command.existingPaymentId() == null) {
                throw conflict("PAYMENT_CHECKPOINT_INCOMPLETE", "Saga đang hoàn tất reservation nhưng thiếu paymentId.");
            }
            if (command.existingPaymentId() != null) {
                PaymentResponse response = payments.getPayment(command.existingPaymentId());
                validateResponse(command, response);
                return new PaymentCheckpoint(response.id(), response.expiresAt(), response.redirectUrl(), false);
            }
            return new PaymentCheckpoint(
                    command.existingPaymentId(), command.existingPaymentDueAt(), null, command.finalTotalVnd() == 0);
        }
        if (command.sagaStatus() != SagaStatus.ORDER_CREATED) {
            throw conflict("PAYMENT_CHECKPOINT_NOT_READY", "Order Saga chưa sẵn sàng tạo Payment.");
        }
        if (command.finalTotalVnd() == 0) {
            if ("PAYMENT_NOT_REQUIRED".equals(command.sagaStep())) {
                return new PaymentCheckpoint(null, null, null, true);
            }
            checkpoints.markPaymentNotRequired(command.sagaId(), command.orderId());
            return new PaymentCheckpoint(null, null, null, true);
        }

        PaymentResponse response = payments.createPayment(new OrderPaymentContextRequest(
                command.orderId(), command.customerId(), command.finalTotalVnd(),
                command.paymentTiming(), command.paymentMethod(), command.correlationId()));
        validateResponse(command, response);
        checkpoints.markPaymentRequested(
                command.sagaId(), command.orderId(), response.id(), response.expiresAt());
        return new PaymentCheckpoint(response.id(), response.expiresAt(), response.redirectUrl(), false);
    }

    private void validateResponse(PaymentCommand command, PaymentResponse response) {
        if (response == null || response.id() == null
                || !Objects.equals(response.orderId(), command.orderId())
                || response.amountVnd() != command.finalTotalVnd()
                || !command.paymentTiming().name().equals(response.timing())
                || !command.paymentMethod().name().equals(response.method())
                || response.status() == null || response.status().isBlank()) {
            throw new OrderException(
                    HttpStatus.BAD_GATEWAY,
                    "PAYMENT_CONTEXT_MISMATCH",
                    "Payment Service trả context không khớp Order đã tạo.");
        }
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public record PaymentCommand(
            UUID sagaId,
            SagaStatus sagaStatus,
            String sagaStep,
            UUID existingPaymentId,
            Instant existingPaymentDueAt,
            UUID orderId,
            UUID customerId,
            long finalTotalVnd,
            PaymentTiming paymentTiming,
            PaymentMethod paymentMethod,
            UUID correlationId) {
    }

    public record PaymentCheckpoint(UUID paymentId, Instant paymentDueAt, String redirectUrl, boolean paymentNotRequired) {
    }
}
