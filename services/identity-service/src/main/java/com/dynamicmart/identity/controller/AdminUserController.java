package com.dynamicmart.identity.controller;

import com.dynamicmart.identity.auth.domain.UserRole;
import com.dynamicmart.identity.auth.domain.UserStatus;
import com.dynamicmart.identity.dto.request.ManageUserRequest;
import com.dynamicmart.identity.dto.response.AdminUserResponse;
import com.dynamicmart.identity.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService users;
    public AdminUserController(AdminUserService users) { this.users = users; }
    @GetMapping
    public Page<AdminUserResponse> search(@RequestParam(required = false) String query,
                                          @RequestParam(required = false) UserRole role,
                                          @RequestParam(required = false) UserStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return users.search(query, role, status, page, size);
    }
    @GetMapping("/{userId}") public AdminUserResponse get(@PathVariable UUID userId) { return users.get(userId); }
    @PatchMapping("/{userId}")
    public AdminUserResponse manage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                    @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                    @Valid @RequestBody ManageUserRequest request) {
        return users.manage(UUID.fromString(jwt.getSubject()), userId, idempotencyKey, request);
    }
}
