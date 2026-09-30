package com.dynamicmart.identity.service;

import com.dynamicmart.identity.auth.domain.UserAccount;
import com.dynamicmart.identity.auth.domain.UserAccountRepository;
import com.dynamicmart.identity.dto.request.UpdateProfileRequest;
import com.dynamicmart.identity.dto.response.ProfileResponse;
import com.dynamicmart.identity.exception.IdentityException;
import com.dynamicmart.identity.mapper.IdentityMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserAccountRepository users;
    public ProfileService(UserAccountRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) { return IdentityMapper.profile(requireUser(userId)); }

    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
        UserAccount user = requireUser(userId);
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        user.updateProfile(request.fullName().trim(), phone, Instant.now());
        return IdentityMapper.profile(user);
    }

    private UserAccount requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new IdentityException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản."));
    }
}
