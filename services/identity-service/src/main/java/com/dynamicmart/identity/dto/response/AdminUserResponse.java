package com.dynamicmart.identity.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(UUID id, String email, String fullName, String phone, String role, String status,
                                Instant lastLoginAt, Instant createdAt, List<UserAuditResponse> audits) { }
