package com.dynamicmart.order_service.standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dynamicmart.order_service.client.CheckoutSelectionGateway;
import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.VoucherPricingGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.config.JwtProperties;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import tools.jackson.databind.ObjectMapper;

class StandaloneGatewayConfigurationTests {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private final StandaloneGatewayConfiguration configuration = new StandaloneGatewayConfiguration();

    @Test
    void exposesTrustedCartAndRejectsCrossCustomerCart() {
        CheckoutSelectionGateway gateway = configuration.standaloneCheckoutSelectionGateway();

        var selection = gateway.loadSelectedCartItems(
                StandaloneGatewayConfiguration.CUSTOMER_ID, StandaloneGatewayConfiguration.CART_ID);

        assertEquals(2, selection.items().size());
        assertEquals("DPP-BLK-128", selection.items().get(0).sku());
        assertThrows(OrderException.class, () -> gateway.loadSelectedCartItems(
                StandaloneGatewayConfiguration.OTHER_CUSTOMER_ID, StandaloneGatewayConfiguration.CART_ID));
    }

    @Test
    void allocatesVoucherDiscountsExactly() {
        VoucherPricingGateway gateway = configuration.standaloneVoucherGateway();
        var request = new VoucherPricingGateway.VoucherPreviewRequest(
                StandaloneGatewayConfiguration.CUSTOMER_ID,
                StandaloneGatewayConfiguration.MERCHANDISE_VOUCHER_ID,
                StandaloneGatewayConfiguration.SHIPPING_VOUCHER_ID,
                40_000,
                List.of(
                        new VoucherPricingGateway.VoucherItem(
                                StandaloneGatewayConfiguration.PHONE_PRODUCT_ID,
                                StandaloneGatewayConfiguration.PHONE_VARIANT_ID, 1, 15_490_000),
                        new VoucherPricingGateway.VoucherItem(
                                StandaloneGatewayConfiguration.LAPTOP_PRODUCT_ID,
                                StandaloneGatewayConfiguration.LAPTOP_VARIANT_ID, 1, 24_990_000)));

        var preview = gateway.preview(request);

        long voucherDiscount = preview.vouchers().stream().mapToLong(value -> value.discountAmountVnd()).sum();
        long lineDiscount = preview.lineDiscounts().stream().mapToLong(value -> value.orderDiscountVnd()).sum();
        assertEquals(voucherDiscount, lineDiscount);
        assertEquals(40_000, preview.vouchers().get(1).shippingDiscountVnd());
    }

    @Test
    void reservationMocksAreIdempotent() {
        var inventory = configuration.standaloneInventoryGateway();
        UUID operationKey = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        var request = new InventoryReservationGateway.ReserveInventoryRequest(
                operationKey, sagaId, UUID.randomUUID(), StandaloneGatewayConfiguration.CUSTOMER_ID,
                UUID.randomUUID(), NOW.plusSeconds(900), List.of(new InventoryReservationGateway.InventoryLine(
                        StandaloneGatewayConfiguration.PHONE_PRODUCT_ID,
                        StandaloneGatewayConfiguration.PHONE_VARIANT_ID, 1)));

        UUID first = inventory.reserve(request).reservationId();
        UUID replay = inventory.reserve(request).reservationId();
        inventory.commit(new InventoryReservationGateway.CommitInventoryRequest(
                UUID.randomUUID(), sagaId, UUID.randomUUID(), UUID.randomUUID(), first));

        assertEquals(first, replay);

        VoucherReservationGateway vouchers = configuration.standaloneVoucherGateway();
        var voucherRequest = new VoucherReservationGateway.ReserveVoucherRequest(
                operationKey, sagaId, UUID.randomUUID(), StandaloneGatewayConfiguration.CUSTOMER_ID,
                UUID.randomUUID(), 30_000, NOW.plusSeconds(900), List.of(), List.of());
        assertEquals(vouchers.reserve(voucherRequest).reservationId(), vouchers.reserve(voucherRequest).reservationId());
    }

    @Test
    void paymentMockKeepsQuoteAndPaymentContracts() {
        PaymentClient gateway = configuration.standalonePaymentClient(Clock.fixed(NOW, ZoneOffset.UTC));
        var quote = gateway.createShippingQuote(new PaymentClient.ShippingQuoteRequest(
                StandaloneGatewayConfiguration.CUSTOMER_ID, UUID.randomUUID(), 201, 11007,
                List.of(new PaymentClient.ShippingItemRequest(
                        StandaloneGatewayConfiguration.PHONE_VARIANT_ID, 1, 420, 18, 10, 6)),
                30_000, "GHN_STANDARD"));
        var consumed = gateway.validateAndConsumeQuote(
                quote.quoteId(), new PaymentClient.QuoteValidationRequest(
                        StandaloneGatewayConfiguration.CUSTOMER_ID, quote.requestFingerprint()));
        UUID orderId = UUID.randomUUID();
        var payment = gateway.createPayment(new PaymentClient.OrderPaymentContextRequest(
                orderId, StandaloneGatewayConfiguration.CUSTOMER_ID, 15_000_000,
                PaymentTiming.POSTPAID, PaymentMethod.COD, UUID.randomUUID()));
        assertEquals(payment, gateway.getPayment(payment.id()));
        var paid = gateway.collectCodForReceivedOrder(orderId);

        assertEquals(quote, consumed);
        assertEquals(paid, gateway.getPayment(payment.id()));
        assertEquals(payment.id(), paid.id());
        assertEquals("SUCCEEDED", paid.status());
        assertEquals(64, quote.requestFingerprint().length());
    }

    @Test
    void issuedStandaloneTokenPassesRealJwtDecoder() {
        String issuer = "dynamicmart-identity-service";
        String secretBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
        var controller = new StandaloneTokenController(
                new JwtProperties(issuer, secretBase64), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        var response = controller.token("customer");
        byte[] secret = Base64.getDecoder().decode(secretBase64);
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator, new JwtIssuerValidator(issuer)));
        var jwt = decoder.decode(response.accessToken());

        assertEquals(StandaloneGatewayConfiguration.CUSTOMER_ID.toString(), jwt.getSubject());
        assertEquals("CUSTOMER", jwt.getClaimAsString("role"));
        assertTrue(jwt.getExpiresAt().isAfter(NOW));
    }
}
