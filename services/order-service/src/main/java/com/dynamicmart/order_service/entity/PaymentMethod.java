package com.dynamicmart.order_service.entity;

public enum PaymentMethod {
    VNPAY,
    ZALOPAY,
    PAYOS,
    BANK_QR,
    COD,
    FREE;

    public boolean isOnline() {
        return this == VNPAY || this == ZALOPAY || this == PAYOS || this == BANK_QR;
    }
}
