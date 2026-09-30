package com.dynamicmart.identity.auth.api;

import com.dynamicmart.identity.auth.domain.UserAccount;
import com.dynamicmart.identity.auth.domain.UserAccountRepository;
import com.dynamicmart.identity.auth.domain.UserStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/internal/session-validations")
public class SessionValidationController {
    private final UserAccountRepository users; private final byte[] internalKey;
    public SessionValidationController(UserAccountRepository users, @Value("${app.clients.internal-api-key}") String internalKey) {
        this.users = users; this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }
    @GetMapping
    public SessionValidationResponse validate(@RequestHeader("X-Internal-Api-Key") String supplied,
                                              @RequestParam UUID userId, @RequestParam int authVersion) {
        if (!MessageDigest.isEqual(internalKey, supplied.getBytes(StandardCharsets.UTF_8))) return new SessionValidationResponse(false);
        UserAccount user = users.findById(userId).orElse(null);
        return new SessionValidationResponse(user != null && user.getStatus() == UserStatus.ACTIVE && user.getAuthVersion() == authVersion);
    }
    public record SessionValidationResponse(boolean valid) { }
}
