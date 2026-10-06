package com.dynamicmart.order_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.config.JwtProperties;
import com.dynamicmart.order_service.config.SecurityConfig;
import com.dynamicmart.order_service.config.SecurityErrorWriter;
import com.dynamicmart.order_service.dto.response.OrderDetailResponse;
import com.dynamicmart.order_service.dto.response.OrderPageResponse;
import com.dynamicmart.order_service.dto.response.OrderTimelineResponse;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.GlobalExceptionHandler;
import com.dynamicmart.order_service.service.OrderQueryService;
import com.dynamicmart.order_service.service.OrderQueryService.Query;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = OrderQueryController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorWriter.class,
        CurrentCustomer.class,
        GlobalExceptionHandler.class,
        OrderQueryControllerTests.TestBeans.class
})
class OrderQueryControllerTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("40000000-0000-0000-0000-000000000003");
    private static final Instant CREATED_FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant CREATED_TO = Instant.parse("2026-10-02T23:59:59Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderQueryService queries;

    @Test
    void customerListUsesJwtIdentityAndForwardsAllowListedQueryInputs() throws Exception {
        Query query = new Query(1, 10, "CONFIRMED", "ORD-01", CREATED_FROM, CREATED_TO, "updatedAt,asc");
        when(queries.listCustomerOrders(CUSTOMER_ID, query)).thenReturn(pageResponse());

        mockMvc.perform(get("/api/v1/orders")
                        .with(jwtFor(CUSTOMER_ID, "ROLE_CUSTOMER"))
                        .queryParam("page", "1")
                        .queryParam("size", "10")
                        .queryParam("status", "CONFIRMED")
                        .queryParam("orderNumber", "ORD-01")
                        .queryParam("createdFrom", CREATED_FROM.toString())
                        .queryParam("createdTo", CREATED_TO.toString())
                        .queryParam("sort", "updatedAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.sort").value("createdAt,desc"));

        verify(queries).listCustomerOrders(CUSTOMER_ID, query);
    }

    @Test
    void customerDetailAndTimelineUseOwnedQueryMethods() throws Exception {
        when(queries.getCustomerOrder(CUSTOMER_ID, ORDER_ID)).thenReturn(detailResponse());
        when(queries.getCustomerTimeline(CUSTOMER_ID, ORDER_ID))
                .thenReturn(new OrderTimelineResponse(ORDER_ID, List.of()));

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .with(jwtFor(CUSTOMER_ID, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-01"))
                .andExpect(jsonPath("$.availableActions[0]").value("CONFIRM_RECEIVED"));

        mockMvc.perform(get("/api/v1/orders/{orderId}/timeline", ORDER_ID)
                        .with(jwtFor(CUSTOMER_ID, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()));

        verify(queries).getCustomerOrder(CUSTOMER_ID, ORDER_ID);
        verify(queries).getCustomerTimeline(CUSTOMER_ID, ORDER_ID);
    }

    @Test
    void adminCanFilterAcrossCustomersAndReadAdminDetail() throws Exception {
        Query query = new Query(0, 20, null, null, null, null, "createdAt,desc");
        when(queries.listAdminOrders(query, CUSTOMER_ID)).thenReturn(pageResponse());
        when(queries.getAdminOrder(ORDER_ID)).thenReturn(detailResponse());

        mockMvc.perform(get("/api/v1/orders/admin")
                        .with(jwtFor(ADMIN_ID, "ROLE_ADMIN"))
                        .queryParam("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId").value(CUSTOMER_ID.toString()));

        mockMvc.perform(get("/api/v1/orders/admin/{orderId}", ORDER_ID)
                        .with(jwtFor(ADMIN_ID, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()));

        verify(queries).listAdminOrders(query, CUSTOMER_ID);
        verify(queries).getAdminOrder(ORDER_ID);
    }

    @Test
    void rolesCannotCrossCustomerAndAdminReadApis() throws Exception {
        Mockito.clearInvocations(queries);

        mockMvc.perform(get("/api/v1/orders/admin")
                        .with(jwtFor(CUSTOMER_ID, "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .with(jwtFor(ADMIN_ID, "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        verify(queries, never()).listAdminOrders(any(), any());
        verify(queries, never()).getCustomerOrder(any(), any());
    }

    @Test
    void malformedPathIdentifierUsesStableErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/orders/not-a-uuid")
                        .with(jwtFor(CUSTOMER_ID, "ROLE_CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.errors[0].field").value("orderId"));
    }

    private OrderPageResponse pageResponse() {
        return new OrderPageResponse(
                List.of(new OrderPageResponse.OrderSummary(
                        ORDER_ID, "ORD-01", CUSTOMER_ID, OrderStatus.SHIPPING,
                        PaymentTiming.PREPAID, PaymentMethod.VNPAY, 100_000, "VND",
                        CREATED_TO, CREATED_FROM, CREATED_TO)),
                0, 20, 1, 1, true, true, "createdAt,desc");
    }

    private OrderDetailResponse detailResponse() {
        return new OrderDetailResponse(
                ORDER_ID, "ORD-01", CUSTOMER_ID, OrderStatus.SHIPPING,
                PaymentTiming.PREPAID, PaymentMethod.VNPAY,
                new OrderDetailResponse.MoneyBreakdown(100_000, 0, 100_000, 0, 0, 0, 0, 100_000, "VND"),
                CREATED_TO, null, null, null, null, CREATED_FROM, null,
                CREATED_FROM, CREATED_TO, List.of("CONFIRM_RECEIVED"), List.of(),
                new OrderDetailResponse.Address("A", "0900000000", "Địa chỉ", 1, 2, "Tỉnh", "Phường"),
                List.of(), null, List.of());
    }

    private RequestPostProcessor jwtFor(UUID subject, String role) {
        return jwt()
                .jwt(jwt -> jwt.subject(subject.toString()))
                .authorities(new SimpleGrantedAuthority(role));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        OrderQueryService orderQueryService() {
            return Mockito.mock(OrderQueryService.class);
        }

        @Bean
        JwtProperties jwtProperties() {
            String secret = Base64.getEncoder().encodeToString(
                    "01234567890123456789012345678901".getBytes());
            return new JwtProperties("dynamicmart-identity-service", secret);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
