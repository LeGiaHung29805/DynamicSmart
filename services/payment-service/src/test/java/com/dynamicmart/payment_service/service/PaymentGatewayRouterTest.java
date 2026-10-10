package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.client.PayOsGateway;
import com.dynamicmart.payment_service.client.SePayQrGateway;
import com.dynamicmart.payment_service.client.VnPayGateway;
import com.dynamicmart.payment_service.client.ZaloPayGateway;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentGatewayRouterTest {
    @Test
    void exposesOnlyConfiguredOnlineMethodsAndAlwaysIncludesCod() {
        VnPayGateway vnPay = mock(VnPayGateway.class);
        ZaloPayGateway zaloPay = mock(ZaloPayGateway.class);
        PayOsGateway payOs = mock(PayOsGateway.class);
        SePayQrGateway sePay = mock(SePayQrGateway.class);
        when(vnPay.isConfigured()).thenReturn(true);
        when(zaloPay.isConfigured()).thenReturn(false);
        when(payOs.isConfigured()).thenReturn(true);
        when(sePay.isConfigured()).thenReturn(false);
        PaymentGatewayRouter router = new PaymentGatewayRouter(vnPay, zaloPay, payOs, sePay);

        assertEquals(List.of("COD", "VNPAY", "PAYOS"), router.availableMethods());
        assertTrue(router.isAvailable("COD"));
        assertFalse(router.isAvailable("ZALOPAY"));
        assertFalse(router.isAvailable("UNKNOWN"));
    }
}
