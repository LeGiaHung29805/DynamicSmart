package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.PaymentClient.OrderPaymentContextRequest;
import com.dynamicmart.order_service.dto.request.CheckoutPreviewRequest;
import com.dynamicmart.order_service.dto.request.CreateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.request.CreateOrderRequest;
import com.dynamicmart.order_service.dto.request.UpdateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.response.CreateOrderResponse;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.service.OrderCreationAdmissionService.AdmissionResult;
import com.dynamicmart.order_service.service.OrderCreationPersistenceService.PersistedOrder;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Runs the public one-shot checkout contract through Hiếu's session/Saga architecture.
 * Every Address, Cart, Catalog and Voucher value is resolved again by a trusted gateway.
 */
@Service
public class CreateOrderFacade {
    private final CheckoutSessionService sessions;
    private final CheckoutPreviewService previews;
    private final OrderCreationAdmissionService admissions;
    private final OrderCreationRevalidationService revalidation;
    private final OrderReservationService reservations;
    private final OrderCreationPersistenceService persistence;
    private final PaymentClient payments;
    private final OrderSagaRepository sagas;
    private final CustomerOrderRepository orders;
    private final OrderFinalizationService finalization;

    public CreateOrderFacade(
            CheckoutSessionService sessions,
            CheckoutPreviewService previews,
            OrderCreationAdmissionService admissions,
            OrderCreationRevalidationService revalidation,
            OrderReservationService reservations,
            OrderCreationPersistenceService persistence,
            PaymentClient payments,
            OrderSagaRepository sagas,
            CustomerOrderRepository orders,
            OrderFinalizationService finalization) {
        this.sessions = sessions;
        this.previews = previews;
        this.admissions = admissions;
        this.revalidation = revalidation;
        this.reservations = reservations;
        this.persistence = persistence;
        this.payments = payments;
        this.sagas = sagas;
        this.orders = orders;
        this.finalization = finalization;
    }

    public CreateOrderResponse create(UUID customerId, UUID idempotencyKey, CreateOrderRequest request) {
        requireIdempotencyKey(idempotencyKey);
        CreateOrderResponse replay = replayCompleted(customerId, idempotencyKey);
        if (replay != null) {
            return replay;
        }

        var session = sessions.create(customerId,
                new CreateCheckoutSessionRequest(CheckoutSource.CART, request.cartId(), null, null));
        sessions.update(customerId, session.id(), new UpdateCheckoutSessionRequest(
                request.addressId(), request.paymentTiming(), request.paymentMethod()));
        previews.preview(customerId, session.id(), new CheckoutPreviewRequest(
                request.merchandiseVoucherId(), request.shippingVoucherId(), null));

        AdmissionResult admitted = admissions.admit(customerId, session.id(), idempotencyKey);
        var validated = revalidation.revalidate(customerId, admitted.sagaId());
        var reserved = reservations.reserve(validated);
        PersistedOrder order = persistence.persist(validated, reserved);
        return paymentResponse(customerId, admitted.correlationId(), order);
    }

    private CreateOrderResponse replayCompleted(UUID customerId, UUID idempotencyKey) {
        OrderSaga saga = sagas.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (saga == null) {
            return null;
        }
        if (saga.getOrderId() == null) {
            throw new OrderException(HttpStatus.CONFLICT, "ORDER_CREATION_IN_PROGRESS",
                    "Yêu cầu tạo đơn đang xử lý hoặc cần được bù trừ; vui lòng thử lại sau.");
        }
        CustomerOrder order = orders.findByIdAndCustomerId(saga.getOrderId(), customerId)
                .orElseThrow(() -> new OrderException(HttpStatus.CONFLICT, "ORDER_REPLAY_OWNER_MISMATCH",
                        "Idempotency-Key không thuộc đơn hàng của khách hàng hiện tại."));
        return paymentResponse(customerId, saga.getCorrelationId(),
                new PersistedOrder(order.getId(), order.getOrderNumber(), order.getStatus(), true));
    }

    private CreateOrderResponse paymentResponse(UUID customerId, UUID correlationId, PersistedOrder persisted) {
        CustomerOrder order = orders.findByIdAndCustomerId(persisted.orderId(), customerId)
                .orElseThrow(() -> new OrderException(HttpStatus.CONFLICT, "ORDER_NOT_FOUND_AFTER_CREATION",
                        "Không thể tải Order vừa tạo."));
        finalization.finalizeIfConfirmed(order);
        if (order.getFinalTotalVnd() == 0
                || order.getPaymentTiming() == PaymentTiming.NOT_REQUIRED
                || order.getPaymentMethod() == PaymentMethod.FREE) {
            return new CreateOrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), null);
        }
        var payment = payments.createPayment(new OrderPaymentContextRequest(
                order.getId(), customerId, order.getFinalTotalVnd(), order.getPaymentTiming(),
                order.getPaymentMethod(), correlationId));
        return new CreateOrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), payment.redirectUrl());
    }

    private void requireIdempotencyKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                    "Header Idempotency-Key là bắt buộc.");
        }
    }
}
