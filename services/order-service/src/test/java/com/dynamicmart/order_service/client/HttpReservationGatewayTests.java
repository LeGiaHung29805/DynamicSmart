package com.dynamicmart.order_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dynamicmart.order_service.client.InventoryReservationGateway.CommitInventoryRequest;
import com.dynamicmart.order_service.client.InventoryReservationGateway.InventoryLine;
import com.dynamicmart.order_service.client.InventoryReservationGateway.ReleaseInventoryRequest;
import com.dynamicmart.order_service.client.InventoryReservationGateway.ReserveInventoryRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ConsumeVoucherRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ReleaseVoucherRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ReserveVoucherRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway.VoucherBenefit;
import com.dynamicmart.order_service.client.VoucherReservationGateway.VoucherLine;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherItem;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherPreviewRequest;
import com.dynamicmart.order_service.exception.OrderException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpReservationGatewayTests {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final UUID OPERATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID CHECKOUT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID VARIANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID VOUCHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Instant RESERVED_UNTIL = Instant.parse("2099-10-02T02:15:00Z");

    @Test
    void reservesInventoryUsingCatalogEnvelopeAndIdempotencyHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpInventoryReservationGateway gateway = new HttpInventoryReservationGateway(builder
                .baseUrl("http://catalog.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
        server.expect(requestTo("http://catalog.test/api/v1/catalog/internal/inventory/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(header("Idempotency-Key", OPERATION_ID.toString()))
                .andExpect(jsonPath("$.checkoutSessionId").value(CHECKOUT_ID.toString()))
                .andExpect(jsonPath("$.expiresAt").value(RESERVED_UNTIL.toString()))
                .andExpect(jsonPath("$.items[0].variantId").value(VARIANT_ID.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andRespond(withSuccess("{\"data\":{\"reservationId\":\"" + RESERVATION_ID + "\",\"status\":\"RESERVED\"}}",
                        MediaType.APPLICATION_JSON));

        var result = gateway.reserve(new ReserveInventoryRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, CUSTOMER_ID, CHECKOUT_ID, RESERVED_UNTIL,
                List.of(new InventoryLine(PRODUCT_ID, VARIANT_ID, 2))));

        assertEquals(RESERVATION_ID, result.reservationId());
        server.verify();
    }

    @Test
    void commitsInventoryUsingCatalogContractAndStableOperationHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpInventoryReservationGateway gateway = new HttpInventoryReservationGateway(builder
                .baseUrl("http://catalog.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
        server.expect(requestTo("http://catalog.test/api/v1/catalog/internal/inventory/reservations/"
                        + RESERVATION_ID + "/commit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(header("Idempotency-Key", OPERATION_ID.toString()))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andRespond(withSuccess("{\"data\":{\"reservationId\":\"" + RESERVATION_ID
                        + "\",\"status\":\"COMMITTED\"}}", MediaType.APPLICATION_JSON));

        gateway.commit(new CommitInventoryRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, ORDER_ID, RESERVATION_ID));

        server.verify();
    }

    @Test
    void releasesInventoryUsingStableOperationHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpInventoryReservationGateway gateway = new HttpInventoryReservationGateway(builder
                .baseUrl("http://catalog.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
        server.expect(requestTo("http://catalog.test/api/v1/catalog/internal/inventory/reservations/"
                        + RESERVATION_ID + "/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", OPERATION_ID.toString()))
                .andExpect(jsonPath("$.reason").value("PAYMENT_FAILED"))
                .andRespond(withSuccess("{\"data\":{\"reservationId\":\"" + RESERVATION_ID
                        + "\",\"status\":\"RELEASED\"}}", MediaType.APPLICATION_JSON));

        gateway.release(new ReleaseInventoryRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, RESERVATION_ID, "PAYMENT_FAILED"));

        server.verify();
    }

    @Test
    void rejectsMismatchedInventoryCommitResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpInventoryReservationGateway gateway = new HttpInventoryReservationGateway(builder
                .baseUrl("http://catalog.test").build());
        UUID wrongReservationId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        server.expect(requestTo("http://catalog.test/api/v1/catalog/internal/inventory/reservations/"
                        + RESERVATION_ID + "/commit"))
                .andRespond(withSuccess("{\"data\":{\"reservationId\":\"" + wrongReservationId
                        + "\",\"status\":\"COMMITTED\"}}", MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class, () -> gateway.commit(
                new CommitInventoryRequest(OPERATION_ID, SAGA_ID, CORRELATION_ID, ORDER_ID, RESERVATION_ID)));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("CATALOG_RESERVATION_CONTRACT_INVALID", exception.getCode());
        server.verify();
    }

    @Test
    void rejectsNullVoucherPreviewElementAsContractError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpVoucherGateway gateway = new HttpVoucherGateway(builder.baseUrl("http://cart.test").build());
        server.expect(requestTo("http://cart.test/api/v1/cart/internal/voucher-pricing/preview"))
                .andRespond(withSuccess("{\"vouchers\":[null],\"lineDiscounts\":[]}", MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class, () -> gateway.preview(new VoucherPreviewRequest(
                CUSTOMER_ID, VOUCHER_ID, null, 30_000,
                List.of(new VoucherItem(PRODUCT_ID, VARIANT_ID, 2, 150_000)))));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("CART_VOUCHER_CONTRACT_INVALID", exception.getCode());
        server.verify();
    }

    @Test
    void previewsVoucherBenefitsUsingCartContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpVoucherGateway gateway = new HttpVoucherGateway(builder
                .baseUrl("http://cart.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
        server.expect(requestTo("http://cart.test/api/v1/cart/internal/voucher-pricing/preview"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.merchandiseVoucherId").value(VOUCHER_ID.toString()))
                .andExpect(jsonPath("$.items[0].variantId").value(VARIANT_ID.toString()))
                .andExpect(jsonPath("$.items[0].unitPriceVnd").value(150000))
                .andRespond(withSuccess("""
                        {"vouchers":[{"voucherId":"%s","voucherCode":"SAVE10","scope":"ORDER",
                         "discountMethod":"PERCENT","discountValue":10,"eligibleSubtotalVnd":300000,
                         "discountAmountVnd":30000,"shippingDiscountVnd":0}],
                         "lineDiscounts":[{"variantId":"%s","productDiscountVnd":0,"orderDiscountVnd":30000}]}
                        """.formatted(VOUCHER_ID, VARIANT_ID), MediaType.APPLICATION_JSON));

        var result = gateway.preview(new VoucherPreviewRequest(
                CUSTOMER_ID, VOUCHER_ID, null, 30_000,
                List.of(new VoucherItem(PRODUCT_ID, VARIANT_ID, 2, 150_000))));

        assertEquals(1, result.vouchers().size());
        assertEquals(30_000, result.vouchers().get(0).discountAmountVnd());
        assertEquals(30_000, result.lineDiscounts().get(0).orderDiscountVnd());
        server.verify();
    }

    @Test
    void reservesVoucherGroupWithSagaOperationIdentity() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpVoucherGateway gateway = new HttpVoucherGateway(builder
                .baseUrl("http://cart.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
        server.expect(requestTo("http://cart.test/api/v1/cart/internal/voucher-reservation-groups"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.operationKey").value(OPERATION_ID.toString()))
                .andExpect(jsonPath("$.sagaId").value(SAGA_ID.toString()))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID.toString()))
                .andExpect(jsonPath("$.checkoutSessionId").value(CHECKOUT_ID.toString()))
                .andExpect(jsonPath("$.vouchers[0].voucherId").value(VOUCHER_ID.toString()))
                .andExpect(jsonPath("$.lines[0].variantId").value(VARIANT_ID.toString()))
                .andRespond(withSuccess("{\"reservationId\":\"" + RESERVATION_ID + "\"}",
                        MediaType.APPLICATION_JSON));

        var result = gateway.reserve(new ReserveVoucherRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, CUSTOMER_ID, CHECKOUT_ID, 30_000, RESERVED_UNTIL,
                List.of(new VoucherBenefit(VOUCHER_ID, "SAVE10", "ORDER", "PERCENT", 10L,
                        300_000, 30_000, 0)),
                List.of(new VoucherLine(PRODUCT_ID, VARIANT_ID, 2, 150_000, 0, 30_000))));

        assertEquals(RESERVATION_ID, result.reservationId());
        server.verify();
    }

    @Test
    void rejectsVoucherReservationResponseWithoutIdentity() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpVoucherGateway gateway = new HttpVoucherGateway(builder.baseUrl("http://cart.test").build());
        server.expect(requestTo("http://cart.test/api/v1/cart/internal/voucher-reservation-groups"))
                .andRespond(withSuccess("{\"reservationId\":null}", MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class, () -> gateway.reserve(new ReserveVoucherRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, CUSTOMER_ID, CHECKOUT_ID, 30_000, RESERVED_UNTIL,
                List.of(new VoucherBenefit(VOUCHER_ID, "SAVE10", "ORDER", "PERCENT", 10L,
                        300_000, 30_000, 0)),
                List.of(new VoucherLine(PRODUCT_ID, VARIANT_ID, 2, 150_000, 0, 30_000)))));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("CART_VOUCHER_CONTRACT_INVALID", exception.getCode());
        server.verify();
    }

    @Test
    void rejectsIncompleteVoucherPreviewContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpVoucherGateway gateway = new HttpVoucherGateway(builder.baseUrl("http://cart.test").build());
        server.expect(requestTo("http://cart.test/api/v1/cart/internal/voucher-pricing/preview"))
                .andRespond(withSuccess("{\"vouchers\":null,\"lineDiscounts\":[]}", MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class, () -> gateway.preview(new VoucherPreviewRequest(
                CUSTOMER_ID, VOUCHER_ID, null, 30_000,
                List.of(new VoucherItem(PRODUCT_ID, VARIANT_ID, 2, 150_000)))));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("CART_VOUCHER_CONTRACT_INVALID", exception.getCode());
        server.verify();
    }

    @Test
    void consumesVoucherGroupUsingCartContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpVoucherGateway gateway = new HttpVoucherGateway(builder
                .baseUrl("http://cart.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
        server.expect(requestTo("http://cart.test/api/v1/cart/internal/voucher-reservation-groups/"
                        + RESERVATION_ID + "/consume"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andRespond(withSuccess("{\"reservationId\":\"" + RESERVATION_ID + "\"}",
                        MediaType.APPLICATION_JSON));

        gateway.consume(new ConsumeVoucherRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, ORDER_ID, RESERVATION_ID));

        server.verify();
    }

    @Test
    void releasesVoucherGroupUsingCartContract() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpVoucherGateway gateway = new HttpVoucherGateway(builder
                .baseUrl("http://cart.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
        server.expect(requestTo("http://cart.test/api/v1/cart/internal/voucher-reservation-groups/"
                        + RESERVATION_ID + "/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.reason").value("PAYMENT_FAILED"))
                .andRespond(withSuccess("{\"reservationId\":\"" + RESERVATION_ID + "\"}",
                        MediaType.APPLICATION_JSON));

        gateway.release(new ReleaseVoucherRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, RESERVATION_ID, "PAYMENT_FAILED"));

        server.verify();
    }
}
