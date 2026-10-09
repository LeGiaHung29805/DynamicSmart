package com.dynamicmart.order_service.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dynamicmart.order_service.config.JwtProperties;
import com.dynamicmart.order_service.config.SecurityConfig;
import com.dynamicmart.order_service.config.SecurityErrorWriter;
import com.dynamicmart.order_service.exception.GlobalExceptionHandler;
import com.dynamicmart.order_service.service.InternalApiGuard;
import com.dynamicmart.order_service.service.OrderQueryService;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderInternalController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorWriter.class,
        GlobalExceptionHandler.class,
        OrderInternalControllerTests.TestBeans.class
})
class OrderInternalControllerTests {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final UUID CUSTOMER_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID VARIANT_ID = UUID.fromString("40000000-0000-0000-0000-000000000005");

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderQueryService queries;

    @Test
    void rejectsMissingInternalKeyBeforeReadingOrderData() throws Exception {
        mockMvc.perform(post("/api/v1/orders/internal/review-eligibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INTERNAL_API_KEY_INVALID"));

        verify(queries, never()).checkReviewEligibility(CUSTOMER_ID, ORDER_ITEM_ID);
    }

    @Test
    void acceptsSharedInternalKeyUsedByEngagementService() throws Exception {
        when(queries.checkReviewEligibility(CUSTOMER_ID, ORDER_ITEM_ID)).thenReturn(
                new OrderInternalController.ReviewEligibility(
                        true, ORDER_ID, ORDER_ITEM_ID, CUSTOMER_ID, PRODUCT_ID, VARIANT_ID, null));

        mockMvc.perform(post("/api/v1/orders/internal/review-eligibility")
                        .header(InternalApiGuard.HEADER_NAME, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()));

        verify(queries).checkReviewEligibility(CUSTOMER_ID, ORDER_ITEM_ID);
    }

    private String requestBody() {
        return """
                {
                  "customerId": "%s",
                  "orderItemId": "%s"
                }
                """.formatted(CUSTOMER_ID, ORDER_ITEM_ID);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        OrderQueryService orderQueryService() {
            return Mockito.mock(OrderQueryService.class);
        }

        @Bean
        InternalApiGuard internalApiGuard() {
            return new InternalApiGuard(INTERNAL_KEY);
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
