package com.dynamicmart.order_service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.config.JwtProperties;
import com.dynamicmart.order_service.config.SecurityConfig;
import com.dynamicmart.order_service.config.SecurityErrorWriter;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.exception.GlobalExceptionHandler;
import com.dynamicmart.order_service.service.OrderCreationOrchestrator;
import com.dynamicmart.order_service.service.OrderCreationOrchestrator.CreationResult;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
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

@WebMvcTest(controllers = OrderCreationController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorWriter.class,
        CurrentCustomer.class,
        GlobalExceptionHandler.class,
        OrderCreationControllerTests.TestBeans.class
})
class OrderCreationControllerTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final Instant PAYMENT_DUE_AT = Instant.parse("2026-09-24T08:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderCreationOrchestrator orderCreation;

    @Test
    void idempotencyKeyHeaderIsRequired() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/sessions/{sessionId}/orders", SESSION_ID)
                        .with(customerJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    void malformedIdempotencyKeyUsesStableErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/sessions/{sessionId}/orders", SESSION_ID)
                        .with(customerJwt())
                        .header("Idempotency-Key", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void newOrderReturnsCreatedAndUsesJwtCustomer() throws Exception {
        when(orderCreation.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY)).thenReturn(
                new CreationResult(
                        ORDER_ID, "ORD-NEW", OrderStatus.PENDING_PAYMENT, SAGA_ID,
                        PAYMENT_ID, PAYMENT_DUE_AT, "https://pay.example/checkout", false));

        mockMvc.perform(post("/api/v1/checkout/sessions/{sessionId}/orders", SESSION_ID)
                        .with(customerJwt())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.redirectUrl").value("https://pay.example/checkout"))
                .andExpect(jsonPath("$.replay").value(false));

        verify(orderCreation).create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);
    }

    @Test
    void committedRetryReturnsOkInsteadOfCreatingAgain() throws Exception {
        when(orderCreation.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY)).thenReturn(
                new CreationResult(
                        ORDER_ID, "ORD-EXISTING", OrderStatus.PENDING_PAYMENT, SAGA_ID,
                        PAYMENT_ID, PAYMENT_DUE_AT, null, true));

        mockMvc.perform(post("/api/v1/checkout/sessions/{sessionId}/orders", SESSION_ID)
                        .with(customerJwt())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replay").value(true));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        return jwt()
                .jwt(jwt -> jwt.subject(CUSTOMER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        OrderCreationOrchestrator orderCreationOrchestrator() {
            return Mockito.mock(OrderCreationOrchestrator.class);
        }

        @Bean
        JwtProperties jwtProperties() {
            String secret = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());
            return new JwtProperties("dynamicmart-identity-service", secret);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
