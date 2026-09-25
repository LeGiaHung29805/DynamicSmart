package com.dynamicmart.identity.service;

import com.dynamicmart.identity.auth.domain.RefreshTokenRepository;
import com.dynamicmart.identity.auth.domain.UserAccount;
import com.dynamicmart.identity.auth.domain.UserAccountRepository;
import com.dynamicmart.identity.auth.domain.UserRole;
import com.dynamicmart.identity.auth.domain.UserStatus;
import com.dynamicmart.identity.dto.request.ManageUserRequest;
import com.dynamicmart.identity.dto.response.AdminUserResponse;
import com.dynamicmart.identity.entity.UserManagementAudit;
import com.dynamicmart.identity.exception.IdentityException;
import com.dynamicmart.identity.mapper.IdentityMapper;
import com.dynamicmart.identity.repository.UserManagementAuditRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final UserManagementAuditRepository audits;
    public AdminUserService(UserAccountRepository users, RefreshTokenRepository refreshTokens, UserManagementAuditRepository audits) {
        this.users = users; this.refreshTokens = refreshTokens; this.audits = audits;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> search(String query, UserRole role, UserStatus status, int page, int size) {
        Specification<UserAccount> spec = (root, ignored, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String value = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, ignored, cb) -> cb.or(cb.like(cb.lower(root.get("email")), value), cb.like(cb.lower(root.get("fullName")), value)));
        }
        if (role != null) spec = spec.and((root, ignored, cb) -> cb.equal(root.get("role"), role));
        if (status != null) spec = spec.and((root, ignored, cb) -> cb.equal(root.get("status"), status));
        return users.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending()))
                .map(user -> IdentityMapper.adminUser(user, java.util.List.of()));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID userId) {
        UserAccount user = requireUser(userId);
        return IdentityMapper.adminUser(user, audits.findAllByTargetUserIdOrderByCreatedAtDesc(userId).stream().map(IdentityMapper::audit).toList());
    }

    @Transactional
    public AdminUserResponse manage(UUID actorId, UUID targetId, UUID idempotencyKey, ManageUserRequest request) {
        if (actorId.equals(targetId)) throw conflict("ADMIN_SELF_MANAGEMENT_FORBIDDEN", "Không thể tự đổi vai trò hoặc trạng thái của chính mình.");
        UserAccount target = requireUser(targetId);
        if (audits.existsByActorAdminIdAndIdempotencyKey(actorId, idempotencyKey)) return get(targetId);
        if (request.status() == null && request.role() == null) throw conflict("USER_CHANGE_EMPTY", "Cần chọn vai trò hoặc trạng thái mới.");
        if (target.getRole() == UserRole.ADMIN && target.getStatus() == UserStatus.ACTIVE &&
                ((request.status() != null && request.status() != UserStatus.ACTIVE) || (request.role() != null && request.role() != UserRole.ADMIN)) &&
                users.countActiveAdmins() <= 1) {
            throw conflict("LAST_ACTIVE_ADMIN", "Không thể vô hiệu hóa quản trị viên đang hoạt động cuối cùng.");
        }
        Instant now = Instant.now(); String oldRole = target.getRole().name(); String oldStatus = target.getStatus().name();
        if (request.role() != null) target.changeRole(request.role(), now);
        if (request.status() != null) target.changeStatus(request.status(), now);
        String action = request.role() != null && !oldRole.equals(target.getRole().name()) ? "CHANGE_ROLE" : switch (target.getStatus()) {
            case LOCKED -> "LOCK"; case DISABLED -> "DISABLE"; case ACTIVE -> "UNLOCK";
        };
        audits.save(new UserManagementAudit(targetId, actorId, action, oldRole, target.getRole().name(), oldStatus,
                target.getStatus().name(), request.reason().trim(), idempotencyKey, now));
        refreshTokens.revokeAllByUserId(targetId, now);
        return get(targetId);
    }

    private UserAccount requireUser(UUID id) { return users.findById(id).orElseThrow(() -> new IdentityException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản.")); }
    private IdentityException conflict(String code, String message) { return new IdentityException(HttpStatus.CONFLICT, code, message); }
}
