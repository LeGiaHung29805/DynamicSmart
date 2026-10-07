package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.dto.response.OrderDetailResponse;
import com.dynamicmart.order_service.dto.response.OrderPageResponse;
import com.dynamicmart.order_service.dto.response.OrderTimelineResponse;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderAddress;
import com.dynamicmart.order_service.entity.OrderActorType;
import com.dynamicmart.order_service.entity.OrderItem;
import com.dynamicmart.order_service.entity.OrderShippingSnapshot;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OrderStatusHistory;
import com.dynamicmart.order_service.entity.OrderVoucherSnapshot;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderAddressRepository;
import com.dynamicmart.order_service.repository.OrderItemRepository;
import com.dynamicmart.order_service.repository.OrderShippingSnapshotRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OrderVoucherSnapshotRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "orderNumber", "orderNumber",
            "status", "status",
            "finalTotalVnd", "finalTotalVnd");

    private final CustomerOrderRepository orders;
    private final OrderItemRepository items;
    private final OrderAddressRepository addresses;
    private final OrderVoucherSnapshotRepository vouchers;
    private final OrderShippingSnapshotRepository shippingSnapshots;
    private final OrderStatusHistoryRepository histories;

    public OrderQueryService(
            CustomerOrderRepository orders,
            OrderItemRepository items,
            OrderAddressRepository addresses,
            OrderVoucherSnapshotRepository vouchers,
            OrderShippingSnapshotRepository shippingSnapshots,
            OrderStatusHistoryRepository histories) {
        this.orders = orders;
        this.items = items;
        this.addresses = addresses;
        this.vouchers = vouchers;
        this.shippingSnapshots = shippingSnapshots;
        this.histories = histories;
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listCustomerOrders(UUID customerId, Query query) {
        requireId(customerId);
        return list(query, customerId);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listAdminOrders(Query query, UUID customerId) {
        return list(query, customerId);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getCustomerOrder(UUID customerId, UUID orderId) {
        requireId(customerId);
        requireId(orderId);
        CustomerOrder order = orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(this::notFound);
        return detail(order, false);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getAdminOrder(UUID orderId) {
        requireId(orderId);
        CustomerOrder order = orders.findById(orderId).orElseThrow(this::notFound);
        return detail(order, true);
    }

    @Transactional(readOnly = true)
    public OrderTimelineResponse getCustomerTimeline(UUID customerId, UUID orderId) {
        requireId(customerId);
        requireId(orderId);
        CustomerOrder order = orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(this::notFound);
        return new OrderTimelineResponse(order.getId(), timeline(order.getId(), false));
    }

    @Transactional(readOnly = true)
    public OrderTimelineResponse getAdminTimeline(UUID orderId) {
        requireId(orderId);
        CustomerOrder order = orders.findById(orderId).orElseThrow(this::notFound);
        return new OrderTimelineResponse(order.getId(), timeline(order.getId(), true));
    }

    @Transactional(readOnly = true)
    public com.dynamicmart.order_service.controller.OrderInternalController.ReviewEligibility checkReviewEligibility(UUID customerId, UUID orderItemId) {
        requireId(customerId);
        requireId(orderItemId);

        OrderItem item = items.findById(orderItemId).orElse(null);
        if (item == null) {
            return new com.dynamicmart.order_service.controller.OrderInternalController.ReviewEligibility(
                    false, null, orderItemId, customerId, null, null, "Không tìm thấy dòng đơn hàng.");
        }

        CustomerOrder order = orders.findById(item.getOrderId()).orElse(null);
        if (order == null || !order.getCustomerId().equals(customerId)) {
            return new com.dynamicmart.order_service.controller.OrderInternalController.ReviewEligibility(
                    false, item.getOrderId(), orderItemId, customerId, item.getProductId(), item.getVariantId(), "Sản phẩm không thuộc đơn hàng của bạn.");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            return new com.dynamicmart.order_service.controller.OrderInternalController.ReviewEligibility(
                    false, order.getId(), orderItemId, customerId, item.getProductId(), item.getVariantId(), "Đơn hàng chưa hoàn thành.");
        }

        return new com.dynamicmart.order_service.controller.OrderInternalController.ReviewEligibility(
                true, order.getId(), orderItemId, customerId, item.getProductId(), item.getVariantId(), null);
    }

    private OrderPageResponse list(Query rawQuery, UUID customerId) {
        Query query = rawQuery == null ? Query.defaults() : rawQuery;
        validate(query);
        OrderStatus status = parseStatus(query.status());
        Sort sort = parseSort(query.sort());
        Specification<CustomerOrder> specification = (root, ignored, criteria) -> criteria.conjunction();
        if (customerId != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.equal(root.get("customerId"), customerId));
        }
        if (status != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.equal(root.get("status"), status));
        }
        if (query.orderNumber() != null && !query.orderNumber().isBlank()) {
            String pattern = "%" + query.orderNumber().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, ignored, criteria) ->
                    criteria.like(criteria.lower(root.get("orderNumber")), pattern));
        }
        if (query.createdFrom() != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
        }
        if (query.createdTo() != null) {
            specification = specification.and((root, ignored, criteria) ->
                    criteria.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
        }

        Page<CustomerOrder> result = orders.findAll(
                specification, PageRequest.of(query.page(), query.size(), sort));
        return new OrderPageResponse(
                result.getContent().stream().map(this::summary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast(), normalizedSort(query.sort()));
    }

    private OrderDetailResponse detail(CustomerOrder order, boolean admin) {
        List<OrderItem> orderItems = items.findAllByOrderId(order.getId()).stream()
                .sorted(Comparator.comparing(OrderItem::getCreatedAt).thenComparing(OrderItem::getId))
                .toList();
        if (orderItems.isEmpty()) {
            throw snapshotIncomplete("Thiếu snapshot sản phẩm của Order.");
        }
        OrderAddress address = addresses.findByOrderId(order.getId())
                .orElseThrow(() -> snapshotIncomplete("Thiếu snapshot địa chỉ của Order."));
        List<OrderVoucherSnapshot> orderVouchers = vouchers.findAllByOrderId(order.getId()).stream()
                .sorted(Comparator.comparing(OrderVoucherSnapshot::getCreatedAt)
                        .thenComparing(OrderVoucherSnapshot::getId))
                .toList();
        OrderShippingSnapshot shipping = shippingSnapshots.findByOrderId(order.getId())
                .orElseThrow(() -> snapshotIncomplete("Thiếu snapshot giao hàng của Order."));

        return new OrderDetailResponse(
                order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                order.getPaymentTiming(), order.getPaymentMethod(), money(order), order.getPaymentDueAt(),
                order.getPaymentSucceededAt(), order.getShipmentDeliveredAt(), order.getCancelReason(),
                order.getCancelledAt(), order.getConfirmedAt(), order.getCompletedAt(), order.getCreatedAt(),
                order.getUpdatedAt(), availableActions(order, admin),
                orderItems.stream().map(this::item).toList(), address(address),
                orderVouchers.stream().map(this::voucher).toList(),
                shipping(shipping), timeline(order.getId(), admin));
    }

    private OrderPageResponse.OrderSummary summary(CustomerOrder order) {
        return new OrderPageResponse.OrderSummary(
                order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus(),
                order.getPaymentTiming(), order.getPaymentMethod(), order.getFinalTotalVnd(), order.getCurrency(),
                order.getPaymentDueAt(), order.getCreatedAt(), order.getUpdatedAt());
    }

    private OrderDetailResponse.MoneyBreakdown money(CustomerOrder order) {
        return new OrderDetailResponse.MoneyBreakdown(
                order.getItemsListSubtotalVnd(), order.getDirectSaleDiscountVnd(), order.getItemsSubtotalVnd(),
                order.getProductDiscountVnd(), order.getOrderDiscountVnd(), order.getShippingFeeVnd(),
                order.getShippingDiscountVnd(), order.getFinalTotalVnd(), order.getCurrency());
    }

    private OrderDetailResponse.Item item(OrderItem item) {
        return new OrderDetailResponse.Item(
                item.getId(), item.getProductId(), item.getVariantId(), item.getSku(), item.getProductName(),
                item.getVariantName(), item.getImageUrl(), item.getListPriceVnd(), item.getDirectSalePromotionId(),
                item.getDirectSaleDiscountVnd(), item.getUnitPriceVnd(), item.getQuantity(),
                item.getProductDiscountVnd(), item.getOrderDiscountVnd(), item.getLineTotalVnd(),
                item.getWeightGrams());
    }

    private OrderDetailResponse.Address address(OrderAddress address) {
        return new OrderDetailResponse.Address(
                address.getRecipientName(), address.getPhone(), address.getAddressLine(), address.getProvinceId(),
                address.getWardId(), address.getProvinceName(), address.getWardName());
    }

    private OrderDetailResponse.Voucher voucher(OrderVoucherSnapshot voucher) {
        return new OrderDetailResponse.Voucher(
                voucher.getVoucherId(), voucher.getVoucherCode(), voucher.getScope(), voucher.getDiscountMethod(),
                voucher.getDiscountValue(), voucher.getEligibleSubtotalVnd(), voucher.getDiscountAmountVnd(),
                voucher.getShippingDiscountVnd());
    }

    private OrderDetailResponse.Shipping shipping(OrderShippingSnapshot shipping) {
        return new OrderDetailResponse.Shipping(
                shipping.getQuoteId(), shipping.getProvider(), shipping.getServiceId(), shipping.getServiceName(),
                shipping.getFeeVnd(), shipping.getShippingDiscountVnd(), shipping.getPayableFeeVnd(),
                shipping.getEta(), shipping.getEtaText(), shipping.getTotalWeightGrams(),
                shipping.getPackageLengthCm(), shipping.getPackageWidthCm(), shipping.getPackageHeightCm(),
                shipping.getToProvinceId(), shipping.getToWardId(), shipping.getToProvinceName(),
                shipping.getToWardName(), shipping.getQuotedAt());
    }

    private List<OrderDetailResponse.TimelineEntry> timeline(UUID orderId, boolean admin) {
        return histories.findAllByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .sorted(Comparator.comparing(OrderStatusHistory::getCreatedAt)
                        .thenComparing(OrderStatusHistory::getId))
                .map(history -> new OrderDetailResponse.TimelineEntry(
                        history.getId(), history.getFromStatus(), history.getToStatus(), history.getActorType(),
                        visibleActorId(history, admin), history.getReason(),
                        admin ? history.getCorrelationId() : null, history.getCreatedAt()))
                .toList();
    }

    private UUID visibleActorId(OrderStatusHistory history, boolean admin) {
        return admin || history.getActorType() == OrderActorType.CUSTOMER
                ? history.getActorId() : null;
    }

    private List<String> availableActions(CustomerOrder order, boolean admin) {
        if (admin) {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                return List.of("PACK");
            }
            if (order.getStatus() == OrderStatus.PACKING) {
                return List.of("SHIP");
            }
            if (order.getStatus() == OrderStatus.SHIPPING && order.getPaymentTiming() == PaymentTiming.POSTPAID) {
                return List.of("HANDOVER");
            }
            return List.of();
        }
        OrderStatus receivable = order.getPaymentTiming() == PaymentTiming.POSTPAID
                ? OrderStatus.HANDOVER_PENDING : OrderStatus.SHIPPING;
        return order.getStatus() == receivable ? List.of("CONFIRM_RECEIVED") : List.of();
    }

    private void validate(Query query) {
        if (query.page() < 0) {
            throw invalid("INVALID_PAGE", "Page không được âm.");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw invalid("INVALID_PAGE_SIZE", "Size phải nằm trong khoảng 1 đến 100.");
        }
        if (query.createdFrom() != null && query.createdTo() != null
                && query.createdFrom().isAfter(query.createdTo())) {
            throw invalid("INVALID_CREATED_RANGE", "createdFrom không được sau createdTo.");
        }
        if (query.orderNumber() != null && query.orderNumber().trim().length() > 32) {
            throw invalid("INVALID_ORDER_NUMBER_FILTER", "Bộ lọc orderNumber không được quá 32 ký tự.");
        }
        parseSort(query.sort());
    }

    private OrderStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("INVALID_ORDER_STATUS", "Trạng thái Order không hợp lệ.");
        }
    }

    private Sort parseSort(String rawSort) {
        String normalized = normalizedSort(rawSort);
        String[] parts = normalized.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.containsKey(parts[0])) {
            throw invalid("INVALID_ORDER_SORT",
                    "Sort phải dùng một trong createdAt, updatedAt, orderNumber, status, finalTotalVnd.");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw invalid("INVALID_ORDER_SORT", "Chiều sort chỉ nhận asc hoặc desc.");
        }
        Sort result = Sort.by(direction, SORT_FIELDS.get(parts[0]));
        return "id".equals(SORT_FIELDS.get(parts[0])) ? result : result.and(Sort.by(direction, "id"));
    }

    private String normalizedSort(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return "createdAt,desc";
        }
        String[] parts = rawSort.trim().split(",", -1);
        if (parts.length != 2) {
            return rawSort.trim();
        }
        return parts[0].trim() + "," + parts[1].trim().toLowerCase(Locale.ROOT);
    }

    private void requireId(UUID id) {
        if (id == null) {
            throw invalid("ORDER_QUERY_INPUT_REQUIRED", "Định danh truy vấn Order là bắt buộc.");
        }
    }

    private OrderException notFound() {
        return new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy Order.");
    }

    private OrderException snapshotIncomplete(String message) {
        return new OrderException(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_SNAPSHOT_INCOMPLETE", message);
    }

    private OrderException invalid(String code, String message) {
        return new OrderException(HttpStatus.BAD_REQUEST, code, message);
    }

    public record Query(
            int page,
            int size,
            String status,
            String orderNumber,
            Instant createdFrom,
            Instant createdTo,
            String sort) {
        public static Query defaults() {
            return new Query(0, 20, null, null, null, null, "createdAt,desc");
        }
    }
}
