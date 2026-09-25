package com.dynamicmart.identity.controller;

import com.dynamicmart.identity.dto.request.AddressRequest;
import com.dynamicmart.identity.dto.response.AddressResponse;
import com.dynamicmart.identity.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final AddressService addresses;
    public AddressController(AddressService addresses) { this.addresses = addresses; }
    @GetMapping public List<AddressResponse> list(@AuthenticationPrincipal Jwt jwt) { return addresses.list(userId(jwt)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest request) { return addresses.create(userId(jwt), request); }
    @PutMapping("/{addressId}")
    public AddressResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID addressId, @Valid @RequestBody AddressRequest request) { return addresses.update(userId(jwt), addressId, request); }
    @PostMapping("/{addressId}/default")
    public AddressResponse makeDefault(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID addressId) { return addresses.makeDefault(userId(jwt), addressId); }
    @DeleteMapping("/{addressId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID addressId) { addresses.deactivate(userId(jwt), addressId); }
    private UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
