package com.dynamicmart.identity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(UUID id, String recipientName, String phone, String addressLine,
                              int provinceId, int wardId, String provinceName, String wardName,
                              boolean defaultAddress, String status, Instant updatedAt) { }
