package com.dynamicmart.identity.controller;

import com.dynamicmart.identity.dto.response.AddressResponse;
import com.dynamicmart.identity.exception.IdentityException;
import com.dynamicmart.identity.service.AddressService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/internal/users/{userId}/addresses")
public class InternalAddressController {
    private final AddressService addresses;
    private final byte[] internalKey;
    public InternalAddressController(AddressService addresses, @Value("${app.clients.internal-api-key}") String internalKey) {
        this.addresses = addresses; this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }
    @GetMapping("/{addressId}")
    public AddressResponse get(@RequestHeader("X-Internal-Api-Key") String supplied,
                               @PathVariable UUID userId, @PathVariable UUID addressId) {
        if (!MessageDigest.isEqual(internalKey, supplied.getBytes(StandardCharsets.UTF_8)))
            throw new IdentityException(HttpStatus.FORBIDDEN, "INTERNAL_API_FORBIDDEN", "Internal API key không hợp lệ.");
        return addresses.getActive(userId, addressId);
    }
}
