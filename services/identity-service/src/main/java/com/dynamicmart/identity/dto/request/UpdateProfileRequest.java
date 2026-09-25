package com.dynamicmart.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "^$|^[0-9+() .-]{8,20}$", message = "Số điện thoại không hợp lệ") String phone) { }
