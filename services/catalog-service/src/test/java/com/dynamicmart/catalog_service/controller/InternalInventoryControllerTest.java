package com.dynamicmart.catalog_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dynamicmart.catalog_service.config.CatalogJwtProperties;
import com.dynamicmart.catalog_service.config.InternalApiProperties;
import com.dynamicmart.catalog_service.config.InternalApiVerifier;
import com.dynamicmart.catalog_service.config.SecurityConfig;
import com.dynamicmart.catalog_service.dto.response.InventoryReservationItemResponse;
import com.dynamicmart.catalog_service.dto.response.InventoryReservationResponse;
import com.dynamicmart.catalog_service.entity.InventoryReservationStatus;
import com.dynamicmart.catalog_service.exception.GlobalExceptionHandler;
import com.dynamicmart.catalog_service.service.InventoryReservationService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InternalInventoryController.class)
@Import({SecurityConfig.class, InternalApiVerifier.class, GlobalExceptionHandler.class,
        InternalInventoryControllerTest.TestBeans.class})
class InternalInventoryControllerTest {
    private static final UUID OPERATION_ID = UUID.randomUUID();
    private static final UUID CHECKOUT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @Autowired private InventoryReservationService reservationService;

    @Test
    void missingInternalKeyIsRejectedWithStandardErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/internal/inventory/reservations")
                        .header("Idempotency-Key", OPERATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_PARAMETER_INVALID"));
    }

    @Test
    void wrongInternalKeyIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/internal/inventory/reservations")
                        .header("X-Internal-Api-Key", "wrong-key")
                        .header("Idempotency-Key", OPERATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INTERNAL_API_FORBIDDEN"));
    }

    @Test
    void correctInternalKeyCanReserveInventory() throws Exception {
        InventoryReservationResponse response = new InventoryReservationResponse(
                RESERVATION_ID, CHECKOUT_ID, null, InventoryReservationStatus.RESERVED,
                Instant.parse("2099-01-01T00:15:00Z"), null, null, null,
                List.of(new InventoryReservationItemResponse(VARIANT_ID, 1, 0)));
        when(reservationService.reserve(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/catalog/internal/inventory/reservations")
                        .header("X-Internal-Api-Key", "test-internal-key")
                        .header("Idempotency-Key", OPERATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reservationId").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("RESERVED"));
    }

    private String requestBody() {
        return """
                {
                  "checkoutSessionId":"%s",
                  "expiresAt":"2099-01-01T00:15:00Z",
                  "items":[{"variantId":"%s","quantity":1}]
                }
                """.formatted(CHECKOUT_ID, VARIANT_ID);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        CatalogJwtProperties catalogJwtProperties() {
            String secret = Base64.getEncoder().encodeToString(
                    "01234567890123456789012345678901".getBytes());
            return new CatalogJwtProperties("dynamicmart-identity-service", secret);
        }

        @Bean
        InternalApiProperties internalApiProperties() {
            return new InternalApiProperties("test-internal-key");
        }

        @Bean
        InventoryReservationService inventoryReservationService() {
            return Mockito.mock(InventoryReservationService.class);
        }
    }
}
