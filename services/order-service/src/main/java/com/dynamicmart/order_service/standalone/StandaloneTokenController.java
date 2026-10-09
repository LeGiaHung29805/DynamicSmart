package com.dynamicmart.order_service.standalone;

import com.dynamicmart.order_service.config.JwtProperties;
import com.dynamicmart.order_service.exception.OrderException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/** Local-only token and fixture discovery API. The controller does not exist outside the standalone profile. */
@RestController
@Profile("standalone")
@RequestMapping("/api/v1/standalone")
public class StandaloneTokenController {
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StandaloneTokenController(JwtProperties jwtProperties, ObjectMapper objectMapper, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @PostMapping("/tokens/{principal}")
    TokenResponse token(@PathVariable String principal) {
        Principal fixture = switch (principal) {
            case "customer" -> new Principal(StandaloneGatewayConfiguration.CUSTOMER_ID, "CUSTOMER");
            case "other-customer" -> new Principal(StandaloneGatewayConfiguration.OTHER_CUSTOMER_ID, "CUSTOMER");
            case "admin" -> new Principal(StandaloneGatewayConfiguration.ADMIN_ID, "ADMIN");
            default -> throw new OrderException(
                    HttpStatus.NOT_FOUND, "STANDALONE_PRINCIPAL_NOT_FOUND",
                    "Principal hợp lệ: customer, other-customer hoặc admin.");
        };
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(2, ChronoUnit.HOURS);
        return new TokenResponse(sign(fixture, issuedAt, expiresAt), "Bearer", expiresAt, fixture.id(), fixture.role());
    }

    @GetMapping("/data")
    Map<String, Object> data() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerId", StandaloneGatewayConfiguration.CUSTOMER_ID);
        result.put("otherCustomerId", StandaloneGatewayConfiguration.OTHER_CUSTOMER_ID);
        result.put("adminId", StandaloneGatewayConfiguration.ADMIN_ID);
        result.put("addressId", StandaloneGatewayConfiguration.ADDRESS_ID);
        result.put("otherAddressId", StandaloneGatewayConfiguration.OTHER_ADDRESS_ID);
        result.put("cartId", StandaloneGatewayConfiguration.CART_ID);
        result.put("otherCartId", StandaloneGatewayConfiguration.OTHER_CART_ID);
        result.put("phoneProductId", StandaloneGatewayConfiguration.PHONE_PRODUCT_ID);
        result.put("phoneVariantId", StandaloneGatewayConfiguration.PHONE_VARIANT_ID);
        result.put("laptopProductId", StandaloneGatewayConfiguration.LAPTOP_PRODUCT_ID);
        result.put("laptopVariantId", StandaloneGatewayConfiguration.LAPTOP_VARIANT_ID);
        result.put("merchandiseVoucherId", StandaloneGatewayConfiguration.MERCHANDISE_VOUCHER_ID);
        result.put("shippingVoucherId", StandaloneGatewayConfiguration.SHIPPING_VOUCHER_ID);
        return result;
    }

    private String sign(Principal principal, Instant issuedAt, Instant expiresAt) {
        try {
            byte[] secret = Base64.getDecoder().decode(jwtProperties.hmacSecretBase64());
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            String header = encoder.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encoder.encodeToString(objectMapper.writeValueAsBytes(Map.of(
                    "iss", jwtProperties.issuer(),
                    "sub", principal.id().toString(),
                    "iat", issuedAt.getEpochSecond(),
                    "exp", expiresAt.getEpochSecond(),
                    "role", principal.role())));
            String unsigned = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return unsigned + "." + encoder.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo JWT standalone.", exception);
        }
    }

    private record Principal(UUID id, String role) {
    }

    record TokenResponse(String accessToken, String tokenType, Instant expiresAt, UUID subject, String role) {
    }
}
