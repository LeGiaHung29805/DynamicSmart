package com.dynamicmart.order_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
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

class HttpCheckoutSelectionGatewayTests {
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CART_ITEM_1 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CART_ITEM_2 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PRODUCT_1 = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PRODUCT_2 = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID VARIANT_1 = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID VARIANT_2 = UUID.fromString("00000000-0000-0000-0000-000000000008");

    @Test
    void loadsAuthoritativeCartAndPreservesCartOrderWhenCatalogOrderDiffers() {
        Fixture fixture = fixture();
        fixture.cartServer().expect(requestTo("http://cart.test/api/v1/cart/internal/checkout-selections/"
                        + CUSTOMER_ID + "/" + CART_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andRespond(withSuccess(cartSelection(CART_ID), MediaType.APPLICATION_JSON));
        fixture.catalogServer().expect(requestTo("http://catalog.test/api/v1/catalog/internal/variants/validate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY))
                .andExpect(jsonPath("$.items[0].variantId").value(VARIANT_1.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].variantId").value(VARIANT_2.toString()))
                .andRespond(withSuccess(catalogEnvelope(variant(PRODUCT_2, VARIANT_2, 220_000, 700, 22, 12, 8)
                                + "," + variant(PRODUCT_1, VARIANT_1, 150_000, 500, 20, 10, 6)),
                        MediaType.APPLICATION_JSON));

        var result = fixture.gateway().loadSelectedCartItems(CUSTOMER_ID, CART_ID);

        assertEquals(CART_ID, result.cartId());
        assertEquals(2, result.items().size());
        assertEquals(VARIANT_1, result.items().get(0).variantId());
        assertEquals(CART_ITEM_1, result.items().get(0).sourceCartItemId());
        assertEquals(7L, result.items().get(0).sourceCartItemVersion());
        assertEquals(VARIANT_2, result.items().get(1).variantId());
        fixture.verify();
    }

    @Test
    void rejectsCatalogDimensionsInsteadOfInventingShippingData() {
        Fixture fixture = fixture();
        fixture.catalogServer().expect(requestTo("http://catalog.test/api/v1/catalog/internal/variants/validate"))
                .andRespond(withSuccess(catalogEnvelope(variant(PRODUCT_1, VARIANT_1, 150_000, 0, null, 10, 6)),
                        MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.gateway().loadBuyNowItem(CUSTOMER_ID, VARIANT_1, 1));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("CHECKOUT_SOURCE_CONTRACT_INVALID", exception.getCode());
        fixture.verify();
    }

    @Test
    void rejectsMismatchedCartIdentityBeforeCallingCatalog() {
        Fixture fixture = fixture();
        UUID wrongCartId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        fixture.cartServer().expect(requestTo("http://cart.test/api/v1/cart/internal/checkout-selections/"
                        + CUSTOMER_ID + "/" + CART_ID))
                .andRespond(withSuccess(cartSelection(wrongCartId), MediaType.APPLICATION_JSON));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.gateway().loadSelectedCartItems(CUSTOMER_ID, CART_ID));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("CHECKOUT_SOURCE_CONTRACT_INVALID", exception.getCode());
        fixture.verify();
    }

    @Test
    void mapsCatalogTransportFailureToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.catalogServer().expect(requestTo("http://catalog.test/api/v1/catalog/internal/variants/validate"))
                .andRespond(withServerError());

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.gateway().loadBuyNowItem(CUSTOMER_ID, VARIANT_1, 1));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("CATALOG_UNAVAILABLE", exception.getCode());
        fixture.verify();
    }

    private Fixture fixture() {
        RestClient.Builder cartBuilder = RestClient.builder()
                .baseUrl("http://cart.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY);
        RestClient.Builder catalogBuilder = RestClient.builder()
                .baseUrl("http://catalog.test")
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, INTERNAL_KEY);
        MockRestServiceServer cartServer = MockRestServiceServer.bindTo(cartBuilder).build();
        MockRestServiceServer catalogServer = MockRestServiceServer.bindTo(catalogBuilder).build();
        return new Fixture(new HttpCheckoutSelectionGateway(cartBuilder.build(), catalogBuilder.build()),
                cartServer, catalogServer);
    }

    private String cartSelection(UUID cartId) {
        return """
                {
                  "cartId": "%s",
                  "items": [
                    {"cartItemId":"%s","cartItemVersion":7,"productId":"%s","variantId":"%s","quantity":2},
                    {"cartItemId":"%s","cartItemVersion":3,"productId":"%s","variantId":"%s","quantity":1}
                  ]
                }
                """.formatted(cartId, CART_ITEM_1, PRODUCT_1, VARIANT_1,
                CART_ITEM_2, PRODUCT_2, VARIANT_2);
    }

    private String catalogEnvelope(String variants) {
        return "{\"data\":[" + variants + "]}";
    }

    private String variant(UUID productId, UUID variantId, long price, int weight,
                           Integer length, Integer width, Integer height) {
        return """
                {
                  "productId":"%s","categoryId":"00000000-0000-0000-0000-000000000010",
                  "variantId":"%s","productName":"Product","variantName":"Variant","sku":"SKU",
                  "price":{"listPriceVnd":%d,"salePriceVnd":null,"directSaleDiscountVnd":0,
                           "directSalePercent":null,"directSaleEndsAt":null,"directSalePromotionId":null},
                  "availableQuantity":10,"weightGrams":%d,"lengthCm":%s,"widthCm":%s,"heightCm":%s,
                  "imageUrl":"https://example.test/image.jpg","purchasable":true,"unavailableReason":null
                }
                """.formatted(productId, variantId, price, weight, jsonNumber(length), jsonNumber(width), jsonNumber(height));
    }

    private String jsonNumber(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private record Fixture(
            HttpCheckoutSelectionGateway gateway,
            MockRestServiceServer cartServer,
            MockRestServiceServer catalogServer) {
        void verify() {
            cartServer.verify();
            catalogServer.verify();
        }
    }
}
