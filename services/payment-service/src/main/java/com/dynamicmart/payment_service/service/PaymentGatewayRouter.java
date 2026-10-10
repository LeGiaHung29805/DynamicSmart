package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.client.PayOsGateway;
import com.dynamicmart.payment_service.client.SePayQrGateway;
import com.dynamicmart.payment_service.client.VnPayGateway;
import com.dynamicmart.payment_service.client.ZaloPayGateway;
import com.dynamicmart.payment_service.exception.PaymentException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayRouter {
    public record CreatedPayment(String reference, String redirectUrl) { }
    private final VnPayGateway vnPay;
    private final ZaloPayGateway zaloPay;
    private final PayOsGateway payOs;
    private final SePayQrGateway sePay;

    public PaymentGatewayRouter(VnPayGateway vnPay, ZaloPayGateway zaloPay, PayOsGateway payOs, SePayQrGateway sePay) {
        this.vnPay = vnPay; this.zaloPay = zaloPay; this.payOs = payOs; this.sePay = sePay;
    }

    public CreatedPayment create(String method, UUID paymentId, UUID orderId, UUID customerId, long amountVnd, int attemptNo, Instant expiresAt) {
        return switch (method) {
            case "VNPAY" -> {
                String reference = "DM" + paymentId.toString().replace("-", "").substring(0, 20).toUpperCase() + attemptNo;
                yield new CreatedPayment(reference, vnPay.createRedirectUrl(reference, amountVnd, "Thanh toan don " + orderId, expiresAt));
            }
            case "ZALOPAY" -> zaloPay.create(paymentId, customerId, amountVnd, attemptNo);
            case "PAYOS" -> payOs.create(paymentId, amountVnd, attemptNo, expiresAt);
            case "BANK_QR" -> sePay.create(paymentId, amountVnd, attemptNo);
            default -> throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_METHOD_NOT_ONLINE", "Phương thức không hỗ trợ tạo phiên thanh toán online.");
        };
    }

    public boolean isAvailable(String method) {
        return switch (method) {
            case "COD" -> true;
            case "VNPAY" -> vnPay.isConfigured();
            case "ZALOPAY" -> zaloPay.isConfigured();
            case "PAYOS" -> payOs.isConfigured();
            case "BANK_QR" -> sePay.isConfigured();
            default -> false;
        };
    }

    public List<String> availableMethods() {
        List<String> methods = new ArrayList<>();
        methods.add("COD");
        for (String method : List.of("VNPAY", "ZALOPAY", "PAYOS", "BANK_QR")) {
            if (isAvailable(method)) methods.add(method);
        }
        return List.copyOf(methods);
    }
}
