package com.dynamicmart.order_service.client;

import java.util.UUID;

public interface AddressGateway {
    AddressSnapshot loadOwnedAddress(UUID customerId, UUID addressId);

    record AddressSnapshot(
            UUID addressId,
            String recipientName,
            String phone,
            String addressLine,
            int provinceId,
            int wardId,
            String provinceName,
            String wardName) {
    }
}
