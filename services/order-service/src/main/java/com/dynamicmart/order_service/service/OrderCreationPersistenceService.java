package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderAddress;
import com.dynamicmart.order_service.entity.OrderItem;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderShippingSnapshot;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OrderStatusHistory;
import com.dynamicmart.order_service.entity.OrderVoucherSnapshot;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderAddressRepository;
import com.dynamicmart.order_service.repository.OrderItemRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.repository.OrderShippingSnapshotRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OrderVoucherSnapshotRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationContextReader.QuoteSnapshot;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import com.dynamicmart.order_service.service.OrderReservationService.ReservationResult;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Atomically freezes the immutable Order aggregate after every remote reservation has succeeded. */
@Service
public class OrderCreationPersistenceService {
    private static final String ORDER_AGGREGATE = "ORDER";
    private static final String ORDER_CREATED_EVENT = "OrderCreated";

    private final CheckoutSessionRepository sessions;
    private final OrderSagaRepository sagas;
    private final CheckoutShippingQuoteRepository quotes;
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderAddressRepository orderAddresses;
    private final OrderVoucherSnapshotRepository orderVouchers;
    private final OrderShippingSnapshotRepository orderShipping;
    private final OrderStatusHistoryRepository histories;
    private final OutboxEventRepository outbox;
    private final OrderStateMachine stateMachine;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OrderCreationPersistenceService(
            CheckoutSessionRepository sessions,
            OrderSagaRepository sagas,
            CheckoutShippingQuoteRepository quotes,
            CustomerOrderRepository orders,
            OrderItemRepository orderItems,
            OrderAddressRepository orderAddresses,
            OrderVoucherSnapshotRepository orderVouchers,
            OrderShippingSnapshotRepository orderShipping,
            OrderStatusHistoryRepository histories,
            OutboxEventRepository outbox,
            OrderStateMachine stateMachine,
            ObjectMapper objectMapper,
            Clock clock) {
        this.sessions = sessions;
        this.sagas = sagas;
        this.quotes = quotes;
        this.orders = orders;
        this.orderItems = orderItems;
        this.orderAddresses = orderAddresses;
        this.orderVouchers = orderVouchers;
        this.orderShipping = orderShipping;
        this.histories = histories;
        this.outbox = outbox;
        this.stateMachine = stateMachine;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public PersistedOrder persist(ValidatedOrderInput input, ReservationResult reservations) {
        CreationContext context = requireInput(input, reservations);
        Instant now = Instant.now(clock);
        CheckoutSession session = sessions.findOwnedForUpdate(context.checkoutSessionId(), context.customerId())
                .orElseThrow(() -> notFound("CHECKOUT_SESSION_NOT_FOUND", "Không tìm thấy Checkout Session."));
        OrderSaga saga = sagas.findByIdForUpdate(context.sagaId())
                .orElseThrow(() -> notFound("ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga."));
        requireSameSaga(context, session, saga);

        CustomerOrder existing = orders.findByCheckoutSessionId(context.checkoutSessionId()).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getId(), saga.getOrderId())
                    || !Objects.equals(existing.getId(), session.getCompletedOrderId())) {
                throw conflict("ORDER_CREATION_STATE_MISMATCH",
                        "Order đã tồn tại nhưng Checkout/Saga checkpoint không đồng nhất.");
            }
            return new PersistedOrder(existing.getId(), existing.getOrderNumber(), existing.getStatus(), true);
        }

        requireReadyForPersistence(session, saga, reservations);
        CheckoutShippingQuote quote = quotes.findByIdForUpdate(context.quote().checkoutQuoteId())
                .orElseThrow(() -> conflict("SHIPPING_QUOTE_STALE", "Không tìm thấy Checkout Quote để tạo Order."));
        requireSameActiveQuote(context, quote, now);

        UUID orderId = UUID.randomUUID();
        String orderNumber = orderNumber(orderId);
        OrderStatus status = stateMachine.initialStatus(
                context.paymentTiming(), context.paymentMethod(), input.pricing().finalTotalVnd());
        CustomerOrder order = createOrder(orderId, orderNumber, status, input, now);
        orders.save(order);
        orderItems.saveAll(createItems(orderId, input, now));
        orderAddresses.save(OrderAddress.create(
                UUID.randomUUID(), orderId, input.address().addressId(), input.address().recipientName(),
                input.address().phone(), input.address().addressLine(), input.address().provinceId(),
                input.address().wardId(), input.address().provinceName(), input.address().wardName(), now));
        orderVouchers.saveAll(input.vouchers().stream().map(voucher -> OrderVoucherSnapshot.create(
                UUID.randomUUID(), orderId, voucher.voucherId(), voucher.voucherCode(), voucher.scope(),
                voucher.discountMethod(), voucher.discountValue(), voucher.eligibleSubtotalVnd(),
                voucher.discountAmountVnd(), voucher.shippingDiscountVnd(), now)).toList());
        orderShipping.save(createShipping(orderId, quote, now));
        histories.save(OrderStatusHistory.initial(UUID.randomUUID(), orderId, status, context.correlationId(), now));
        outbox.save(OutboxEvent.pending(
                UUID.randomUUID(), ORDER_AGGREGATE, orderId, ORDER_CREATED_EVENT, 1,
                orderCreatedPayload(order, context, now), context.correlationId(), now));

        quote.consume(now);
        session.complete(orderId, now);
        saga.markOrderCreated(orderId, now);
        return new PersistedOrder(orderId, orderNumber, status, false);
    }

    private CreationContext requireInput(ValidatedOrderInput input, ReservationResult reservations) {
        if (input == null || input.context() == null || input.address() == null || input.pricing() == null
                || reservations == null || reservations.inventoryReservationId() == null) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "INVALID_ORDER_CREATION_INPUT",
                    "Validated input và Inventory reservation là bắt buộc.");
        }
        return input.context();
    }

    private void requireSameSaga(CreationContext context, CheckoutSession session, OrderSaga saga) {
        if (!Objects.equals(session.getId(), context.checkoutSessionId())
                || !Objects.equals(session.getCustomerId(), context.customerId())
                || !Objects.equals(saga.getCheckoutSessionId(), session.getId())
                || !Objects.equals(saga.getCorrelationId(), context.correlationId())) {
            throw conflict("ORDER_CREATION_STATE_MISMATCH", "Checkout, Saga và validated input không đồng nhất.");
        }
    }

    private void requireReadyForPersistence(
            CheckoutSession session, OrderSaga saga, ReservationResult reservations) {
        if (session.getStatus() != CheckoutStatus.ACTIVE || session.getCompletedOrderId() != null) {
            throw conflict("CHECKOUT_SESSION_NOT_ACTIVE", "Checkout Session không còn sẵn sàng để tạo Order.");
        }
        if (saga.getStatus() != SagaStatus.INVENTORY_RESERVED
                || !Objects.equals(saga.getVoucherReservationId(), reservations.voucherReservationId())
                || !Objects.equals(saga.getInventoryReservationId(), reservations.inventoryReservationId())) {
            throw conflict("RESERVATION_CHECKPOINT_MISMATCH",
                    "Reservation không khớp checkpoint Saga trước khi tạo Order.");
        }
    }

    private void requireSameActiveQuote(CreationContext context, CheckoutShippingQuote quote, Instant now) {
        QuoteSnapshot expected = context.quote();
        if (quote.getStatus() != ShippingQuoteStatus.ACTIVE || !quote.getExpiresAt().isAfter(now)
                || !Objects.equals(quote.getCheckoutSessionId(), context.checkoutSessionId())
                || !Objects.equals(quote.getQuoteId(), expected.providerQuoteId())
                || !Objects.equals(quote.getProvider(), expected.provider())
                || !Objects.equals(quote.getInputFingerprint(), expected.inputFingerprint())
                || quote.getFeeVnd() != expected.feeVnd()
                || quote.getShippingDiscountVnd() != expected.shippingDiscountVnd()
                || quote.getPayableFeeVnd() != expected.payableFeeVnd()
                || quote.getServiceId() != expected.serviceId()
                || !Objects.equals(quote.getTotalWeightGrams(), expected.totalWeightGrams())
                || !Objects.equals(quote.getPackageLengthCm(), expected.packageLengthCm())
                || !Objects.equals(quote.getPackageWidthCm(), expected.packageWidthCm())
                || !Objects.equals(quote.getPackageHeightCm(), expected.packageHeightCm())
                || !Objects.equals(quote.getToProvinceId(), expected.provinceId())
                || !Objects.equals(quote.getToWardId(), expected.wardId())) {
            throw conflict("SHIPPING_QUOTE_STALE", "Checkout Quote đã thay đổi hoặc hết hạn trước khi tạo Order.");
        }
    }

    private CustomerOrder createOrder(
            UUID orderId, String orderNumber, OrderStatus status, ValidatedOrderInput input, Instant now) {
        var context = input.context();
        var money = input.pricing();
        return CustomerOrder.create(
                orderId, orderNumber, context.checkoutSessionId(), context.customerId(), status,
                context.paymentTiming(), context.paymentMethod(), money.itemsListSubtotalVnd(),
                money.directSaleDiscountVnd(), money.itemsSubtotalVnd(), money.productDiscountVnd(),
                money.orderDiscountVnd(), money.shippingFeeVnd(), money.shippingDiscountVnd(),
                money.finalTotalVnd(), null, now);
    }

    private List<OrderItem> createItems(UUID orderId, ValidatedOrderInput input, Instant now) {
        Map<UUID, LineDiscount> discounts = new HashMap<>();
        input.lineDiscounts().forEach(line -> discounts.put(line.variantId(), line));
        return input.context().items().stream().map(item -> {
            LineDiscount discount = discounts.get(item.variantId());
            long productDiscount = discount == null ? 0 : discount.productDiscountVnd();
            long orderDiscount = discount == null ? 0 : discount.orderDiscountVnd();
            long lineTotal;
            try {
                lineTotal = Math.subtractExact(
                        Math.subtractExact(Math.multiplyExact(item.unitPriceVnd(), item.quantity()), productDiscount),
                        orderDiscount);
            } catch (ArithmeticException exception) {
                throw conflict("ORDER_LINE_TOTAL_INVALID", "Tổng tiền snapshot theo dòng vượt giới hạn hỗ trợ.");
            }
            if (lineTotal < 0) {
                throw conflict("ORDER_LINE_TOTAL_INVALID", "Tổng tiền snapshot theo dòng không được âm.");
            }
            return OrderItem.create(
                    UUID.randomUUID(), orderId, item.productId(), item.variantId(), item.sku(), item.productName(),
                    item.variantName(), item.imageUrl(), item.listPriceVnd(), item.directSalePromotionId(),
                    item.directSaleDiscountVnd(), item.unitPriceVnd(), item.quantity(), productDiscount,
                    orderDiscount, lineTotal, item.weightGrams(), now);
        }).toList();
    }

    private OrderShippingSnapshot createShipping(UUID orderId, CheckoutShippingQuote quote, Instant now) {
        return OrderShippingSnapshot.create(
                UUID.randomUUID(), orderId, quote.getQuoteId(), quote.getId(), quote.getProvider(),
                quote.getInputFingerprint(), quote.getServiceId(), quote.getServiceName(), quote.getFeeVnd(),
                quote.getShippingDiscountVnd(), quote.getPayableFeeVnd(), quote.getEta(), quote.getEtaText(),
                quote.getTotalWeightGrams(), quote.getPackageLengthCm(), quote.getPackageWidthCm(),
                quote.getPackageHeightCm(), quote.getToProvinceId(), quote.getToWardId(),
                quote.getToProvinceName(), quote.getToWardName(), quote.getQuotedAt(),
                quote.getRawResponseRedacted(), now);
    }

    private String orderCreatedPayload(CustomerOrder order, CreationContext context, Instant now) {
        return objectMapper.writeValueAsString(new OrderCreatedPayload(
                order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                order.getPaymentTiming(), order.getPaymentMethod(), order.getFinalTotalVnd(),
                context.checkoutSessionId(), context.cartId(), now));
    }

    private String orderNumber(UUID orderId) {
        return "ORD-" + orderId.toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private OrderException notFound(String code, String message) {
        return new OrderException(HttpStatus.NOT_FOUND, code, message);
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public record PersistedOrder(UUID orderId, String orderNumber, OrderStatus status, boolean replay) {
    }

    private record OrderCreatedPayload(
            UUID orderId,
            String orderNumber,
            UUID customerId,
            OrderStatus status,
            com.dynamicmart.order_service.entity.PaymentTiming paymentTiming,
            com.dynamicmart.order_service.entity.PaymentMethod paymentMethod,
            long finalTotalVnd,
            UUID checkoutSessionId,
            UUID cartId,
            Instant createdAt) {
    }
}
