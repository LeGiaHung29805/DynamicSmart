package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.dto.request.CheckoutPreviewRequest;
import com.dynamicmart.order_service.dto.request.CreateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.request.CreateOrderRequest;
import com.dynamicmart.order_service.dto.request.UpdateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.response.CreateOrderResponse;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.service.OrderCreationOrchestrator.CreationResult;
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
    private final OrderSagaRepository sagas;
    private final OrderCreationOrchestrator orderCreation;

    public CreateOrderFacade(
            CheckoutSessionService sessions,
            CheckoutPreviewService previews,
            OrderSagaRepository sagas,
            OrderCreationOrchestrator orderCreation) {
        this.sessions = sessions;
        this.previews = previews;
        this.sagas = sagas;
        this.orderCreation = orderCreation;
    }

    public CreateOrderResponse create(UUID customerId, UUID idempotencyKey, CreateOrderRequest request) {
        requireIdempotencyKey(idempotencyKey);
        OrderSaga existing = sagas.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return response(orderCreation.create(customerId, existing.getCheckoutSessionId(), idempotencyKey));
        }

        var session = sessions.create(customerId,
                new CreateCheckoutSessionRequest(CheckoutSource.CART, request.cartId(), null, null));
        sessions.update(customerId, session.id(), new UpdateCheckoutSessionRequest(
                request.addressId(), request.paymentTiming(), request.paymentMethod()));
        previews.preview(customerId, session.id(), new CheckoutPreviewRequest(
                request.merchandiseVoucherId(), request.shippingVoucherId(), null));

        return response(orderCreation.create(customerId, session.id(), idempotencyKey));
    }

    private CreateOrderResponse response(CreationResult result) {
        return new CreateOrderResponse(
                result.orderId(), result.orderNumber(), result.status(), result.sagaId(),
                result.paymentId(), result.paymentDueAt(), result.replay());
    }

    private void requireIdempotencyKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                    "Header Idempotency-Key là bắt buộc.");
        }
    }
}
