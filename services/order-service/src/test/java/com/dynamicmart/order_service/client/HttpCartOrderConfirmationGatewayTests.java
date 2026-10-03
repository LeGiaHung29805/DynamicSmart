package com.dynamicmart.order_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.OrderConfirmedCommand;
import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.PurchasedCartItem;
import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpCartOrderConfirmationGatewayTests {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CART_ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void sendsVersionedCartItemIdentityUsingCartContract() {
        Fixture fixture = fixture();
        fixture.server().expect(requestTo("http://cart.test/api/v1/cart/internal/order-confirmed"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.source").value("CART"))
                .andExpect(jsonPath("$.items[0].id").value(CART_ITEM_ID.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].version").value(7))
                .andRespond(withSuccess("{\"removedCount\":1,\"duplicate\":false}", MediaType.APPLICATION_JSON));

        fixture.gateway().confirm(command());

        fixture.server().verify();
    }

    @Test
    void rejectsEmptySuccessResponseSoOutboxCanRetry() {
        Fixture fixture = fixture();
        fixture.server().expect(requestTo("http://cart.test/api/v1/cart/internal/order-confirmed"))
                .andRespond(withNoContent());

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.gateway().confirm(command()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("CART_CLEANUP_UNAVAILABLE", exception.getCode());
        fixture.server().verify();
    }

    private OrderConfirmedCommand command() {
        return new OrderConfirmedCommand(EVENT_ID, CORRELATION_ID, CUSTOMER_ID, "CART",
                List.of(new PurchasedCartItem(CART_ITEM_ID, 2, 7)));
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://cart.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new HttpCartOrderConfirmationGateway(builder.build()), server);
    }

    private record Fixture(HttpCartOrderConfirmationGateway gateway, MockRestServiceServer server) {
    }
}
