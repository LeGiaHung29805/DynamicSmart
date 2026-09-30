package com.dynamicmart.identity.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dynamicmart.identity.auth.domain.RefreshTokenRepository;
import com.dynamicmart.identity.auth.domain.UserAccount;
import com.dynamicmart.identity.auth.domain.UserAccountRepository;
import com.dynamicmart.identity.auth.domain.UserRole;
import com.dynamicmart.identity.auth.domain.UserStatus;
import com.dynamicmart.identity.dto.request.ManageUserRequest;
import com.dynamicmart.identity.exception.IdentityException;
import com.dynamicmart.identity.repository.UserManagementAuditRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminUserServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RefreshTokenRepository refresh = mock(RefreshTokenRepository.class);
    private final UserManagementAuditRepository audits = mock(UserManagementAuditRepository.class);
    private final AdminUserService service = new AdminUserService(users, refresh, audits);

    @Test
    void adminCannotManageOwnRoleOrStatus() {
        UUID actor = UUID.randomUUID();
        assertThatThrownBy(() -> service.manage(actor, actor, UUID.randomUUID(), new ManageUserRequest(UserStatus.LOCKED, null, "test")))
                .isInstanceOf(IdentityException.class).hasMessageContaining("chính mình");
        verifyNoInteractions(users, refresh, audits);
    }

    @Test
    void lastActiveAdminCannotBeDisabled() {
        UUID actor = UUID.randomUUID(), targetId = UUID.randomUUID(); Instant now = Instant.now();
        UserAccount target = new UserAccount(targetId, "admin@example.com", "admin@example.com", "hash", "Admin", now);
        target.changeRole(UserRole.ADMIN, now);
        when(users.findById(targetId)).thenReturn(Optional.of(target)); when(users.countActiveAdmins()).thenReturn(1L);
        when(audits.existsByActorAdminIdAndIdempotencyKey(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.manage(actor, targetId, UUID.randomUUID(), new ManageUserRequest(UserStatus.DISABLED, null, "Không còn làm việc")))
                .isInstanceOf(IdentityException.class).hasMessageContaining("cuối cùng");
        verify(refresh, never()).revokeAllByUserId(any(), any());
    }
}
