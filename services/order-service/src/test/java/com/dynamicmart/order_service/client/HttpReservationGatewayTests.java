package com.dynamicmart.order_service.client;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dynamicmart.order_service.client.InventoryReservationGateway.CommitInventoryRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ConsumeVoucherRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
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
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        gateway.commit(new CommitInventoryRequest(
                OPERATION_ID, SAGA_ID, CORRELATION_ID, ORDER_ID, RESERVATION_ID));

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
}
