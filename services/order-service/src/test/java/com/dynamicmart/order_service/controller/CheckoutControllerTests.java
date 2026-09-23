package com.dynamicmart.order_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
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
import com.dynamicmart.order_service.dto.request.CreateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.request.CheckoutPreviewRequest;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse.MoneyBreakdown;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse.ShippingQuoteResponse;
import com.dynamicmart.order_service.dto.response.CheckoutSessionResponse;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.exception.GlobalExceptionHandler;
import com.dynamicmart.order_service.service.CheckoutPreviewService;
import com.dynamicmart.order_service.service.CheckoutSessionService;
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

@WebMvcTest(controllers = CheckoutController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorWriter.class,
        CurrentCustomer.class,
        GlobalExceptionHandler.class,
        CheckoutControllerTests.TestBeans.class
})
class CheckoutControllerTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired private MockMvc mockMvc;
    @Autowired private CheckoutSessionService checkoutSessions;
    @Autowired private CheckoutPreviewService checkoutPreviews;

    @Test
    void checkoutEndpointRequiresCustomerAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/sessions")
                        .contentType("application/json")
                        .content("{\"source\":\"CART\",\"cartId\":\"" + CART_ID + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void validationFailureUsesSharedErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/sessions")
                        .with(customerJwt())
                        .contentType("application/json")
                        .content("{\"source\":\"BUY_NOW\",\"variantId\":\"" + CART_ID + "\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    }

    @Test
    void customerCanCreateSessionAndSubjectIsPassedAsCustomerId() throws Exception {
        when(checkoutSessions.create(eq(CUSTOMER_ID), isA(CreateCheckoutSessionRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/checkout/sessions")
                        .with(customerJwt())
                        .contentType("application/json")
                        .content("{\"source\":\"CART\",\"cartId\":\"" + CART_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(checkoutSessions).create(eq(CUSTOMER_ID), isA(CreateCheckoutSessionRequest.class));
    }

    @Test
    void customerCanRequestServerAuthoritativePreview() throws Exception {
        Instant now = Instant.parse("2026-09-23T09:00:00Z");
        when(checkoutPreviews.preview(eq(CUSTOMER_ID), eq(SESSION_ID), isA(CheckoutPreviewRequest.class)))
                .thenReturn(new CheckoutPreviewResponse(
                        SESSION_ID, CART_ID, List.of(),
                        new ShippingQuoteResponse(UUID.randomUUID(), 30_000, 0, 30_000, 53320,
                                "GHN Standard", "2-3 ngày", now.plusSeconds(900)),
                        new MoneyBreakdown(100_000, 10_000, 90_000, 0, 0, 30_000, 0, 120_000),
                        null, null));

        mockMvc.perform(post("/api/v1/checkout/sessions/{sessionId}/preview", SESSION_ID)
                        .with(customerJwt())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutSessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.money.finalTotalVnd").value(120000))
                .andExpect(jsonPath("$.shipping.feeVnd").value(30000));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        return jwt()
                .jwt(jwt -> jwt.subject(CUSTOMER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private CheckoutSessionResponse response() {
        Instant now = Instant.parse("2026-09-23T09:00:00Z");
        return new CheckoutSessionResponse(SESSION_ID, CheckoutSource.CART, CART_ID, null, CheckoutStatus.ACTIVE,
                null, null, now.plusSeconds(900), now, now, List.of());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        CheckoutSessionService checkoutSessionService() {
            return Mockito.mock(CheckoutSessionService.class);
        }

        @Bean
        CheckoutPreviewService checkoutPreviewService() {
            return Mockito.mock(CheckoutPreviewService.class);
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
