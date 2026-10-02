package com.dynamicmart.order_service.controller;

import static org.mockito.Mockito.never;
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
import com.dynamicmart.order_service.dto.response.OrderCommandResponse;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.exception.GlobalExceptionHandler;
import com.dynamicmart.order_service.service.OrderLifecycleCommandService;
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

@WebMvcTest(controllers = OrderLifecycleController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorWriter.class,
        CurrentCustomer.class,
        GlobalExceptionHandler.class,
        OrderLifecycleControllerTests.TestBeans.class
})
class OrderLifecycleControllerTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID OPERATION_KEY = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-02T02:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private OrderLifecycleCommandService commands;

    @Test
    void customerReceiptUsesJwtOwnerAndIdempotencyKey() throws Exception {
        when(commands.customerConfirmReceived(CUSTOMER_ID, ORDER_ID, OPERATION_KEY)).thenReturn(
                new OrderCommandResponse(
                        ORDER_ID, OrderStatus.DELIVERED, List.of(OrderStatus.DELIVERED), UPDATED_AT, false));

        mockMvc.perform(post("/api/v1/orders/{orderId}/received", ORDER_ID)
                        .with(jwtFor(CUSTOMER_ID, "ROLE_CUSTOMER"))
                        .header("Idempotency-Key", OPERATION_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.appliedTransitions[0]").value("DELIVERED"));

        verify(commands).customerConfirmReceived(CUSTOMER_ID, ORDER_ID, OPERATION_KEY);
    }

    @Test
    void adminPackUsesAdminIdentity() throws Exception {
        when(commands.adminPack(ADMIN_ID, ORDER_ID, OPERATION_KEY)).thenReturn(
                new OrderCommandResponse(
                        ORDER_ID, OrderStatus.PACKING, List.of(OrderStatus.PACKING), UPDATED_AT, false));

        mockMvc.perform(post("/api/v1/orders/admin/{orderId}/pack", ORDER_ID)
                        .with(jwtFor(ADMIN_ID, "ROLE_ADMIN"))
                        .header("Idempotency-Key", OPERATION_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKING"));

        verify(commands).adminPack(ADMIN_ID, ORDER_ID, OPERATION_KEY);
    }

    @Test
    void customerCannotCallAdminLifecycleCommand() throws Exception {
        mockMvc.perform(post("/api/v1/orders/admin/{orderId}/ship", ORDER_ID)
                        .with(jwtFor(CUSTOMER_ID, "ROLE_CUSTOMER"))
                        .header("Idempotency-Key", OPERATION_KEY))
                .andExpect(status().isForbidden());

        verify(commands, never()).adminShip(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void adminCannotConfirmReceiptForCustomer() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/received", ORDER_ID)
                        .with(jwtFor(ADMIN_ID, "ROLE_ADMIN"))
                        .header("Idempotency-Key", OPERATION_KEY))
                .andExpect(status().isForbidden());

        verify(commands, never()).customerConfirmReceived(Mockito.any(), Mockito.any(), Mockito.any());
    }

    private RequestPostProcessor jwtFor(UUID subject, String role) {
        return jwt()
                .jwt(jwt -> jwt.subject(subject.toString()))
                .authorities(new SimpleGrantedAuthority(role));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        OrderLifecycleCommandService orderLifecycleCommandService() {
            return Mockito.mock(OrderLifecycleCommandService.class);
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
