package com.dynamicmart.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 150) String recipientName,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 500) String addressLine,
        @Positive int provinceId,
        @Positive int wardId,
        @NotBlank @Size(max = 150) String provinceName,
        @NotBlank @Size(max = 150) String wardName,
        boolean defaultAddress) { }
