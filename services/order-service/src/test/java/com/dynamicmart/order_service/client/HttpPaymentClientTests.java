package com.dynamicmart.order_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dynamicmart.order_service.client.PaymentClient.OrderPaymentContextRequest;
import com.dynamicmart.order_service.client.PaymentClient.QuoteValidationRequest;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpPaymentClientTests {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private MockRestServiceServer server;
    private HttpPaymentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpPaymentClient(builder
                .baseUrl("http://payment.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY)
                .build());
    }

    @Test
    void validatesQuoteWithInternalApiKeyAndMapsResponse() {
        server.expect(requestTo("http://payment.test/api/v1/shipping/quotes/" + QUOTE_ID + "/validate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.requestFingerprint").value("fingerprint"))
                .andRespond(withSuccess("""
                        {
                          "quoteId": "00000000-0000-0000-0000-000000000002",
                          "feeVnd": 30000,
                          "shippingDiscountVnd": 5000,
                          "payableFeeVnd": 25000,
                          "serviceId": 53320,
                          "serviceName": "GHN Standard",
                          "eta": "2-3 ngày",
                          "expiresAt": "2026-09-23T08:15:00Z",
                          "requestFingerprint": "fingerprint"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.validateAndConsumeQuote(
                QUOTE_ID, new QuoteValidationRequest(CUSTOMER_ID, "fingerprint"));

        assertEquals(25_000, response.payableFeeVnd());
        assertEquals("GHN Standard", response.serviceName());
        assertEquals("fingerprint", response.requestFingerprint());
        server.verify();
    }

    @Test
    void sendsOrderPaymentContextUsingSharedEnumNames() {
        server.expect(requestTo("http://payment.test/api/v1/payments/order-context"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(jsonPath("$.timing").value("PREPAID"))
                .andExpect(jsonPath("$.method").value("VNPAY"))
                .andExpect(jsonPath("$.amountVnd").value(120000))
                .andRespond(withSuccess("""
                        {
                          "id": "00000000-0000-0000-0000-000000000004",
                          "orderId": "00000000-0000-0000-0000-000000000003",
                          "amountVnd": 120000,
                          "timing": "PREPAID",
                          "method": "VNPAY",
                          "status": "PENDING",
                          "redirectUrl": null,
                          "expiresAt": "2026-09-23T08:15:00Z",
                          "paidAt": null
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.createPayment(new OrderPaymentContextRequest(
                ORDER_ID, CUSTOMER_ID, 120_000, PaymentTiming.PREPAID, PaymentMethod.VNPAY, UUID.randomUUID()));

        assertEquals(ORDER_ID, response.orderId());
        assertEquals("PENDING", response.status());
        server.verify();
    }

    @Test
    void mapsPaymentClientErrorToStableOrderError() {
        server.expect(requestTo("http://payment.test/api/v1/shipping/quotes/" + QUOTE_ID + "/validate"))
                .andRespond(withResourceNotFound());

        OrderException exception = assertThrows(OrderException.class, () -> client.validateAndConsumeQuote(
                QUOTE_ID, new QuoteValidationRequest(CUSTOMER_ID, "expired-fingerprint")));

        assertEquals("PAYMENT_REQUEST_REJECTED", exception.getCode());
        assertEquals(422, exception.getStatus().value());
        server.verify();
    }
}
