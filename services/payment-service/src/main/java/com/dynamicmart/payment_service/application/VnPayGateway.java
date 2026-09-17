package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.config.VnPayProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class VnPayGateway {
    private final VnPayProperties properties;

    public VnPayGateway(VnPayProperties properties) { this.properties = properties; }

    public String createRedirectUrl(String reference, long amountVnd, String orderInfo) {
        requireConfigured();
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_Amount", Long.toString(Math.multiplyExact(amountVnd, 100)));
        fields.put("vnp_Command", "pay");
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_OrderInfo", orderInfo);
        fields.put("vnp_ReturnUrl", properties.returnUrl());
        fields.put("vnp_TmnCode", properties.tmnCode());
        fields.put("vnp_TxnRef", reference);
        fields.put("vnp_Version", "2.1.0");
        String query = canonical(fields);
        return properties.paymentUrl() + (properties.paymentUrl().contains("?") ? "&" : "?") + query + "&vnp_SecureHash=" + sign(query);
    }

    public boolean hasValidSignature(Map<String, String> request) {
        if (!configured()) return false;
        String received = request.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;
        Map<String, String> fields = new TreeMap<>(request);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        return constantTimeEquals(sign(canonical(fields)), received);
    }

    private String canonical(Map<String, String> fields) {
        return fields.entrySet().stream().filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(java.util.stream.Collectors.joining("&"));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot sign VNPay request", exception);
        }
    }

    private boolean configured() { return properties.tmnCode() != null && !properties.tmnCode().isBlank() && properties.paymentUrl() != null && !properties.paymentUrl().isBlank() && properties.returnUrl() != null && !properties.returnUrl().isBlank() && properties.hashSecret() != null && !properties.hashSecret().isBlank(); }
    private void requireConfigured() { if (!configured()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "VNPAY_NOT_CONFIGURED", "VNPay chưa được cấu hình cho môi trường này."); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private boolean constantTimeEquals(String left, String right) { return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.toLowerCase().getBytes(StandardCharsets.UTF_8)); }
}
