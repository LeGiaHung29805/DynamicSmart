package com.dynamicmart.order_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpAddressGatewayTests {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADDRESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void loadsOwnedActiveAddressUsingIdentityContract() {
        Fixture fixture = fixture();
        fixture.server().expect(requestTo("http://identity.test/api/v1/auth/internal/users/"
                        + CUSTOMER_ID + "/addresses/" + ADDRESS_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andRespond(withSuccess(address(ADDRESS_ID, "ACTIVE"), MediaType.APPLICATION_JSON));

        AddressGateway.AddressSnapshot result = fixture.gateway().loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID);

        assertEquals(ADDRESS_ID, result.addressId());
        assertEquals("Nguyễn Văn A", result.recipientName());
        assertEquals(202, result.provinceId());
        assertEquals(1450, result.wardId());
        fixture.server().verify();
    }

    @Test
    void rejectsAddressThatIsInactive() {
        Fixture fixture = fixture();
        fixture.server().expect(requestTo("http://identity.test/api/v1/auth/internal/users/"
                        + CUSTOMER_ID + "/addresses/" + ADDRESS_ID))
                .andRespond(withSuccess(address(ADDRESS_ID, "INACTIVE"), MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.gateway().loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, exception.getStatus());
        assertEquals("CHECKOUT_ADDRESS_INVALID", exception.getCode());
        fixture.server().verify();
    }

    @Test
    void rejectsMismatchedAddressIdentityAsBadGatewayContract() {
        Fixture fixture = fixture();
        UUID wrongAddressId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        fixture.server().expect(requestTo("http://identity.test/api/v1/auth/internal/users/"
                        + CUSTOMER_ID + "/addresses/" + ADDRESS_ID))
                .andRespond(withSuccess(address(wrongAddressId, "ACTIVE"), MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.gateway().loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("IDENTITY_ADDRESS_CONTRACT_INVALID", exception.getCode());
        fixture.server().verify();
    }

    @Test
    void mapsIdentityServerFailureToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server().expect(requestTo("http://identity.test/api/v1/auth/internal/users/"
                        + CUSTOMER_ID + "/addresses/" + ADDRESS_ID))
                .andRespond(withServerError());

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.gateway().loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("IDENTITY_ADDRESS_UNAVAILABLE", exception.getCode());
        fixture.server().verify();
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://identity.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new HttpAddressGateway(builder.build()), server);
    }

    private String address(UUID addressId, String status) {
        return """
                {
                  "id":"%s","recipientName":"Nguyễn Văn A","phone":"0900000000",
                  "addressLine":"1 Đường A","provinceId":202,"wardId":1450,
                  "provinceName":"Hồ Chí Minh","wardName":"Phường 1",
                  "defaultAddress":true,"status":"%s","updatedAt":"2026-10-02T02:00:00Z"
                }
                """.formatted(addressId, status);
    }

    private record Fixture(HttpAddressGateway gateway, MockRestServiceServer server) {
    }
}
