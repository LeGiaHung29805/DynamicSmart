package com.dynamicmart.identity.service;

import com.dynamicmart.identity.client.LocationClient;
import com.dynamicmart.identity.dto.request.AddressRequest;
import com.dynamicmart.identity.dto.response.AddressResponse;
import com.dynamicmart.identity.entity.Address;
import com.dynamicmart.identity.entity.AddressStatus;
import com.dynamicmart.identity.exception.IdentityException;
import com.dynamicmart.identity.mapper.IdentityMapper;
import com.dynamicmart.identity.repository.AddressRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {
    private final AddressRepository addresses;
    private final LocationClient locations;
    public AddressService(AddressRepository addresses, LocationClient locations) { this.addresses = addresses; this.locations = locations; }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addresses.findAllByUserIdAndStatusOrderByDefaultAddressDescUpdatedAtDesc(userId, AddressStatus.ACTIVE)
                .stream().map(IdentityMapper::address).toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        var location = locations.validateAndResolve(request.provinceId(), request.wardId());
        Instant now = Instant.now();
        boolean makeDefault = request.defaultAddress() || list(userId).isEmpty();
        if (makeDefault) clearDefault(userId, null, now);
        Address address = new Address(UUID.randomUUID(), userId, request.recipientName().trim(), request.phone().trim(),
                request.addressLine().trim(), request.provinceId(), request.wardId(), location.provinceName(),
                location.wardName(), makeDefault, now);
        return IdentityMapper.address(addresses.save(address));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        Address address = requireOwned(userId, addressId);
        if (address.getStatus() != AddressStatus.ACTIVE) throw inactive();
        var location = locations.validateAndResolve(request.provinceId(), request.wardId());
        Instant now = Instant.now();
        if (request.defaultAddress()) clearDefault(userId, addressId, now);
        address.update(request.recipientName().trim(), request.phone().trim(), request.addressLine().trim(),
                request.provinceId(), request.wardId(), location.provinceName(), location.wardName(), request.defaultAddress(), now);
        return IdentityMapper.address(address);
    }

    @Transactional
    public AddressResponse makeDefault(UUID userId, UUID addressId) {
        Address address = requireOwned(userId, addressId);
        if (address.getStatus() != AddressStatus.ACTIVE) throw inactive();
        Instant now = Instant.now(); clearDefault(userId, addressId, now); address.setDefaultAddress(true, now);
        return IdentityMapper.address(address);
    }

    @Transactional
    public void deactivate(UUID userId, UUID addressId) { requireOwned(userId, addressId).deactivate(Instant.now()); }

    private void clearDefault(UUID userId, UUID except, Instant now) {
        addresses.findAllByUserIdAndStatusOrderByDefaultAddressDescUpdatedAtDesc(userId, AddressStatus.ACTIVE).stream()
                .filter(Address::isDefaultAddress).filter(value -> except == null || !value.getId().equals(except))
                .forEach(value -> value.setDefaultAddress(false, now));
    }
    private Address requireOwned(UUID userId, UUID addressId) {
        return addresses.findByIdAndUserId(addressId, userId).orElseThrow(() ->
                new IdentityException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ."));
    }
    private IdentityException inactive() { return new IdentityException(HttpStatus.CONFLICT, "ADDRESS_INACTIVE", "Địa chỉ đã ngừng sử dụng."); }
}
