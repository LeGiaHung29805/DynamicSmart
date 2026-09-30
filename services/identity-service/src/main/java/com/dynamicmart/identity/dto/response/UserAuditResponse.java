package com.dynamicmart.identity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserAuditResponse(UUID id, String action, String oldRole, String newRole,
                                String oldStatus, String newStatus, String reason, Instant createdAt) { }
