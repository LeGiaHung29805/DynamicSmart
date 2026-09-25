package com.dynamicmart.identity.controller;

import com.dynamicmart.identity.dto.request.UpdateProfileRequest;
import com.dynamicmart.identity.dto.response.ProfileResponse;
import com.dynamicmart.identity.service.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profiles;
    public ProfileController(ProfileService profiles) { this.profiles = profiles; }
    @GetMapping public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) { return profiles.get(UUID.fromString(jwt.getSubject())); }
    @PutMapping public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.update(UUID.fromString(jwt.getSubject()), request);
    }
}
