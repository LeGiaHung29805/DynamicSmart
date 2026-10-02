package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderActorType;
import com.dynamicmart.order_service.entity.OrderAddress;
import com.dynamicmart.order_service.entity.OrderItem;
import com.dynamicmart.order_service.entity.OrderShippingSnapshot;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.OrderStatusHistory;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderAddressRepository;
import com.dynamicmart.order_service.repository.OrderItemRepository;
import com.dynamicmart.order_service.repository.OrderShippingSnapshotRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OrderVoucherSnapshotRepository;
import com.dynamicmart.order_service.service.OrderQueryService.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@SuppressWarnings({"unchecked", "rawtypes"})
class OrderQueryServiceTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Test
    void customerListAlwaysAddsOwnerPredicateAndStableTieBreakerSort() {
        Fixture fixture = fixture();
        CustomerOrder order = order(OrderStatus.CONFIRMED, PaymentTiming.PREPAID);
        when(fixture.orders.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        var response = fixture.service.listCustomerOrders(CUSTOMER_ID,
                new Query(0, 20, "confirmed", "ORD", null, null, "updatedAt,asc"));

        assertEquals(1, response.content().size());
        assertEquals("updatedAt,asc", response.sort());
        ArgumentCaptor<Specification<CustomerOrder>> specification = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(fixture.orders).findAll(specification.capture(), pageable.capture());
        assertEquals("updatedAt: ASC,id: ASC", pageable.getValue().getSort().toString());

        Root<CustomerOrder> root = Mockito.mock(Root.class);
        CriteriaQuery<?> criteriaQuery = Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder criteria = Mockito.mock(CriteriaBuilder.class);
        Path<Object> customerPath = Mockito.mock(Path.class);
        Path<Object> statusPath = Mockito.mock(Path.class);
        Path<String> orderNumberPath = Mockito.mock(Path.class);
        Predicate predicate = Mockito.mock(Predicate.class);
        when(criteria.conjunction()).thenReturn(predicate);
        when(root.get("customerId")).thenReturn((Path) customerPath);
        when(root.get("status")).thenReturn((Path) statusPath);
        when(root.get("orderNumber")).thenReturn((Path) orderNumberPath);
        when(criteria.equal(any(), any())).thenReturn(predicate);
        when(criteria.lower(orderNumberPath)).thenReturn(orderNumberPath);
        when(criteria.like(any(), any(String.class))).thenReturn(predicate);
        when(criteria.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);

        specification.getValue().toPredicate(root, criteriaQuery, criteria);

        verify(criteria).equal(customerPath, CUSTOMER_ID);
        verify(criteria).equal(statusPath, OrderStatus.CONFIRMED);
    }

    @Test
    void rejectsInvalidFiltersBeforeQueryingDatabase() {
        Fixture fixture = fixture();

        OrderException size = assertThrows(OrderException.class, () -> fixture.service.listCustomerOrders(
                CUSTOMER_ID, new Query(0, 101, null, null, null, null, "createdAt,desc")));
        OrderException sort = assertThrows(OrderException.class, () -> fixture.service.listCustomerOrders(
                CUSTOMER_ID, new Query(0, 20, null, null, null, null, "customerId,desc")));
        OrderException status = assertThrows(OrderException.class, () -> fixture.service.listCustomerOrders(
                CUSTOMER_ID, new Query(0, 20, "UNKNOWN", null, null, null, "createdAt,desc")));
        OrderException range = assertThrows(OrderException.class, () -> fixture.service.listCustomerOrders(
                CUSTOMER_ID, new Query(0, 20, null, null, NOW, NOW.minusSeconds(1), "createdAt,desc")));

        assertEquals("INVALID_PAGE_SIZE", size.getCode());
        assertEquals("INVALID_ORDER_SORT", sort.getCode());
        assertEquals("INVALID_ORDER_STATUS", status.getCode());
        assertEquals("INVALID_CREATED_RANGE", range.getCode());
        verify(fixture.orders, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void customerDetailUsesOwnedLookupAndMapsImmutableSnapshots() {
        Fixture fixture = fixture();
        CustomerOrder order = order(OrderStatus.SHIPPING, PaymentTiming.PREPAID);
        OrderAddress address = OrderAddress.create(
                UUID.randomUUID(), ORDER_ID, UUID.randomUUID(), "Nguyễn Văn A", "0900000000",
                "1 Đường A", 201, 301, "Hà Nội", "Phường A", NOW);
        OrderItem item = OrderItem.create(
                UUID.randomUUID(), ORDER_ID, UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "Laptop",
                "16GB", "https://example.test/image.png", 10_000_000, null, 500_000,
                9_500_000, 1, 100_000, 0, 9_400_000, 1500, NOW);
        when(fixture.orders.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.of(order));
        when(fixture.items.findAllByOrderId(ORDER_ID)).thenReturn(List.of(item));
        when(fixture.addresses.findByOrderId(ORDER_ID)).thenReturn(Optional.of(address));
        when(fixture.vouchers.findAllByOrderId(ORDER_ID)).thenReturn(List.of());
        when(fixture.shipping.findByOrderId(ORDER_ID)).thenReturn(Optional.of(shippingSnapshot()));
        when(fixture.histories.findAllByOrderIdOrderByCreatedAtAsc(ORDER_ID)).thenReturn(List.of());

        var response = fixture.service.getCustomerOrder(CUSTOMER_ID, ORDER_ID);

        assertEquals("ORD-20261002-001", response.orderNumber());
        assertEquals("Laptop", response.items().get(0).productName());
        assertEquals("Nguyễn Văn A", response.address().recipientName());
        assertEquals(List.of("CONFIRM_RECEIVED"), response.availableActions());
        assertEquals(9_400_000, response.money().finalTotalVnd());
        verify(fixture.orders).findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID);
        verify(fixture.orders, never()).findById(ORDER_ID);
    }

    @Test
    void customerCannotDiscoverAnotherCustomersOrder() {
        Fixture fixture = fixture();
        when(fixture.orders.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

        OrderException exception = assertThrows(
                OrderException.class, () -> fixture.service.getCustomerOrder(CUSTOMER_ID, ORDER_ID));

        assertEquals("ORDER_NOT_FOUND", exception.getCode());
        verify(fixture.orders, never()).findById(ORDER_ID);
        verify(fixture.items, never()).findAllByOrderId(any());
    }

    @Test
    void customerTimelineRedactsInternalActorAndCorrelationIdentifiers() {
        Fixture fixture = fixture();
        CustomerOrder order = order(OrderStatus.PACKING, PaymentTiming.PREPAID);
        UUID correlationId = UUID.randomUUID();
        OrderStatusHistory history = OrderStatusHistory.transition(
                UUID.randomUUID(), ORDER_ID, OrderStatus.CONFIRMED, OrderStatus.PACKING,
                OrderActorType.ADMIN, ADMIN_ID, "ADMIN_PACK", correlationId, null, NOW);
        when(fixture.orders.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.of(order));
        when(fixture.histories.findAllByOrderIdOrderByCreatedAtAsc(ORDER_ID)).thenReturn(List.of(history));

        var customerTimeline = fixture.service.getCustomerTimeline(CUSTOMER_ID, ORDER_ID);

        assertNull(customerTimeline.timeline().get(0).actorId());
        assertNull(customerTimeline.timeline().get(0).correlationId());

        when(fixture.orders.findById(ORDER_ID)).thenReturn(Optional.of(order));
        var adminTimeline = fixture.service.getAdminTimeline(ORDER_ID);
        assertEquals(ADMIN_ID, adminTimeline.timeline().get(0).actorId());
        assertEquals(correlationId, adminTimeline.timeline().get(0).correlationId());
    }

    @Test
    void incompleteRequiredSnapshotFailsExplicitly() {
        Fixture fixture = fixture();
        CustomerOrder order = order(OrderStatus.CONFIRMED, PaymentTiming.PREPAID);
        when(fixture.orders.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(fixture.items.findAllByOrderId(ORDER_ID)).thenReturn(List.of());
        when(fixture.addresses.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.getAdminOrder(ORDER_ID));

        assertEquals("ORDER_SNAPSHOT_INCOMPLETE", exception.getCode());
    }

    private CustomerOrder order(OrderStatus status, PaymentTiming timing) {
        PaymentMethod method = timing == PaymentTiming.PREPAID ? PaymentMethod.VNPAY : PaymentMethod.COD;
        CustomerOrder order = CustomerOrder.create(
                ORDER_ID, "ORD-20261002-001", UUID.randomUUID(), CUSTOMER_ID, status, timing, method,
                10_000_000, 500_000, 9_500_000, 100_000, 0, 50_000, 50_000,
                9_400_000, timing == PaymentTiming.PREPAID ? NOW.plusSeconds(900) : null, NOW);
        order.setUpdatedAt(NOW.plusSeconds(60));
        return order;
    }

    private OrderShippingSnapshot shippingSnapshot() {
        return OrderShippingSnapshot.create(
                UUID.randomUUID(), ORDER_ID, UUID.randomUUID(), UUID.randomUUID(), "GHN", "a".repeat(64),
                53320, "Standard", 50_000, 50_000, 0, NOW.plusSeconds(86_400), "Ngày mai",
                1500, 20, 15, 10, 201, 301, "Hà Nội", "Phường A", NOW,
                null, NOW);
    }

    private Fixture fixture() {
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        OrderItemRepository items = Mockito.mock(OrderItemRepository.class);
        OrderAddressRepository addresses = Mockito.mock(OrderAddressRepository.class);
        OrderVoucherSnapshotRepository vouchers = Mockito.mock(OrderVoucherSnapshotRepository.class);
        OrderShippingSnapshotRepository shipping = Mockito.mock(OrderShippingSnapshotRepository.class);
        OrderStatusHistoryRepository histories = Mockito.mock(OrderStatusHistoryRepository.class);
        return new Fixture(
                new OrderQueryService(orders, items, addresses, vouchers, shipping, histories),
                orders, items, addresses, vouchers, shipping, histories);
    }

    private record Fixture(
            OrderQueryService service,
            CustomerOrderRepository orders,
            OrderItemRepository items,
            OrderAddressRepository addresses,
            OrderVoucherSnapshotRepository vouchers,
            OrderShippingSnapshotRepository shipping,
            OrderStatusHistoryRepository histories) {
    }
}
