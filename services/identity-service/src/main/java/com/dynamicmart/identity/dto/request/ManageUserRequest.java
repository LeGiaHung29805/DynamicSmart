package com.dynamicmart.identity.dto.request;

import com.dynamicmart.identity.auth.domain.UserRole;
import com.dynamicmart.identity.auth.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManageUserRequest(UserStatus status, UserRole role, @NotBlank @Size(max = 500) String reason) { }
