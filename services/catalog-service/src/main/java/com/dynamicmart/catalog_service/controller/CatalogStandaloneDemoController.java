package com.dynamicmart.catalog_service.controller;

import com.dynamicmart.catalog_service.config.CatalogDemoProperties;
import com.dynamicmart.catalog_service.config.CatalogJwtProperties;
import com.dynamicmart.catalog_service.dto.request.InventoryReservationItemRequest;
import com.dynamicmart.catalog_service.dto.response.ApiResponse;
import com.dynamicmart.catalog_service.dto.response.PurchasableVariantResponse;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.service.PurchasableVariantService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("standalone-demo")
@RestController
@RequestMapping("/api/v1/catalog/demo")
public class CatalogStandaloneDemoController {
    private static final String REFRESH_COOKIE = "dm_catalog_demo_refresh";
    private static final String COOKIE_PATH = "/api/v1/catalog/demo/auth";

    private final PurchasableVariantService purchasableVariantService;
    private final CatalogDemoProperties demoProperties;
    private final CatalogJwtProperties jwtProperties;
    private final Map<String, DemoUser> refreshSessions = new ConcurrentHashMap<>();
    private final Map<String, byte[]> registeredPasswords = new ConcurrentHashMap<>();
    private final Map<UUID, DemoCheckoutSession> checkoutSessions = new ConcurrentHashMap<>();

    public CatalogStandaloneDemoController(PurchasableVariantService purchasableVariantService,
                                           CatalogDemoProperties demoProperties,
                                           CatalogJwtProperties jwtProperties) {
        this.purchasableVariantService = purchasableVariantService;
        this.demoProperties = demoProperties;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/auth/login")
    public ApiResponse<DemoAuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        requireDemoPassword(request.email(), request.password());
        return ApiResponse.of(authenticated(userFor(request.email(), null), response));
    }

    @PostMapping("/auth/register")
    public ApiResponse<DemoAuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        String email = normalizedEmail(request.email());
        registeredPasswords.put(email, passwordDigest(request.password()));
        return ApiResponse.of(authenticated(userFor(email, request.fullName()), response));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<DemoAuthResponse> refresh(HttpServletRequest request,
                                                HttpServletResponse response) {
        String refreshToken = cookie(request, REFRESH_COOKIE);
        DemoUser user = refreshToken == null ? null : refreshSessions.remove(refreshToken);
        if (user == null) {
            clearRefreshCookie(response);
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "DEMO_SESSION_EXPIRED",
                    "Phiên demo đã hết hạn, vui lòng đăng nhập lại.");
        }
        return ApiResponse.of(authenticated(user, response));
    }

    @PostMapping("/auth/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookie(request, REFRESH_COOKIE);
        if (refreshToken != null) refreshSessions.remove(refreshToken);
        clearRefreshCookie(response);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @PostMapping("/auth/password/forgot")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest ignored,
                               HttpServletResponse response) {
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @PostMapping("/auth/password/reset")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest ignored,
                              HttpServletResponse response) {
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @PostMapping("/cart/items")
    public void addToCart(@AuthenticationPrincipal Jwt principal,
                          @Valid @RequestBody AddToCartRequest request,
                          HttpServletResponse response) {
        PurchasableVariantResponse variant = requirePurchasable(request.variantId(), request.quantity());
        if (!request.productId().equals(variant.productId())) {
            throw new CatalogException(HttpStatus.CONFLICT, "PRODUCT_VARIANT_MISMATCH",
                    "Variant không thuộc sản phẩm được yêu cầu.");
        }
        requireCustomer(principal);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @PostMapping("/checkout/sessions")
    public ApiResponse<DemoCheckoutSession> createCheckout(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody CreateCheckoutRequest request) {
        if (!"BUY_NOW".equals(request.source())) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "CHECKOUT_SOURCE_INVALID",
                    "Catalog demo chỉ hỗ trợ luồng Mua ngay.");
        }
        UUID customerId = requireCustomer(principal);
        PurchasableVariantResponse variant = requirePurchasable(request.variantId(), request.quantity());
        long unitPrice = variant.price().salePriceVnd() == null
                ? variant.price().listPriceVnd() : variant.price().salePriceVnd();
        DemoCheckoutSession session = new DemoCheckoutSession(UUID.randomUUID(), customerId,
                variant.variantId(), variant.sku(), variant.productName(), variant.variantName(),
                request.quantity(), unitPrice, Math.multiplyExact(unitPrice, request.quantity()), Instant.now());
        checkoutSessions.put(session.id(), session);
        return ApiResponse.of(session);
    }

    @GetMapping("/checkout/sessions/{sessionId}")
    public ApiResponse<DemoCheckoutSession> checkout(@AuthenticationPrincipal Jwt principal,
                                                     @PathVariable UUID sessionId) {
        UUID customerId = requireCustomer(principal);
        DemoCheckoutSession session = checkoutSessions.get(sessionId);
        if (session == null || !customerId.equals(session.customerId())) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "CHECKOUT_SESSION_NOT_FOUND",
                    "Không tìm thấy phiên Mua ngay của bạn.");
        }
        return ApiResponse.of(session);
    }

    private PurchasableVariantResponse requirePurchasable(UUID variantId, int quantity) {
        PurchasableVariantResponse response = purchasableVariantService
                .validate(java.util.List.of(new InventoryReservationItemRequest(variantId, quantity))).get(0);
        if (!response.purchasable()) {
            throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_NOT_PURCHASABLE",
                    "Không thể mua Variant: " + response.unavailableReason());
        }
        return response;
    }

    private UUID requireCustomer(Jwt principal) {
        if (principal == null || principal.getSubject() == null) {
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Bạn cần đăng nhập để tiếp tục.");
        }
        return UUID.fromString(principal.getSubject());
    }

    private DemoAuthResponse authenticated(DemoUser user, HttpServletResponse response) {
        Instant expiresAt = Instant.now().plusSeconds(demoProperties.tokenTtlSeconds());
        String refreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshSessions.put(refreshToken, user);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true).secure(false).sameSite("Lax").path(COOKIE_PATH)
                .maxAge(demoProperties.tokenTtlSeconds() * 8).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return new DemoAuthResponse(signAccessToken(user, expiresAt), expiresAt, user);
    }

    private String signAccessToken(DemoUser user, Instant expiresAt) {
        try {
            byte[] secret = Base64.getDecoder().decode(jwtProperties.hmacSecretBase64());
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(jwtProperties.issuer())
                    .subject(user.id().toString())
                    .issueTime(java.util.Date.from(Instant.now()))
                    .expirationTime(java.util.Date.from(expiresAt))
                    .claim("role", user.role())
                    .claim("authVersion", 0)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (IllegalArgumentException | JOSEException exception) {
            throw new IllegalStateException("Không thể ký token demo; hãy kiểm tra JWT_HMAC_SECRET_BASE64.",
                    exception);
        }
    }

    private DemoUser userFor(String rawEmail, String requestedName) {
        String email = normalizedEmail(rawEmail);
        String role = email.startsWith("admin@") ? "ADMIN" : "CUSTOMER";
        String name = requestedName == null || requestedName.isBlank()
                ? ("ADMIN".equals(role) ? "Quản trị viên Catalog Demo" : "Khách hàng Catalog Demo")
                : requestedName.trim();
        return new DemoUser(UUID.nameUUIDFromBytes(("catalog-demo-user:" + email)
                .getBytes(StandardCharsets.UTF_8)), email, name, role);
    }

    private void requireDemoPassword(String rawEmail, String password) {
        byte[] expected = registeredPasswords.getOrDefault(normalizedEmail(rawEmail),
                passwordDigest(demoProperties.password()));
        if (!MessageDigest.isEqual(expected, passwordDigest(password))) {
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "DEMO_CREDENTIALS_INVALID",
                    "Mật khẩu demo không đúng.");
        }
    }

    private String normalizedEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private byte[] passwordDigest(String password) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK không hỗ trợ SHA-256.", exception);
        }
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies()).filter(value -> name.equals(value.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(false).sameSite("Lax").path(COOKIE_PATH).maxAge(0).build().toString());
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8) String password,
                                  @NotBlank @Size(max = 120) String fullName) { }
    public record ForgotPasswordRequest(@NotBlank @Email String email) { }
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8) String password) { }
    public record AddToCartRequest(@NotNull UUID productId,
                                   @NotNull UUID variantId,
                                   @Positive int quantity) { }
    public record CreateCheckoutRequest(@NotBlank String source,
                                        @NotNull UUID variantId,
                                        @Positive int quantity) { }
    public record DemoUser(UUID id, String email, String fullName, String role) { }
    public record DemoAuthResponse(String accessToken, Instant accessTokenExpiresAt, DemoUser user) { }
    public record DemoCheckoutSession(UUID id, UUID customerId, UUID variantId, String sku,
                                      String productName, String variantName, int quantity,
                                      long unitPriceVnd, long lineTotalVnd, Instant createdAt) { }
}
