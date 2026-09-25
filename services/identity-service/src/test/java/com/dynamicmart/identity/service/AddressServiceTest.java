package com.dynamicmart.identity.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dynamicmart.identity.client.LocationClient;
import com.dynamicmart.identity.dto.request.AddressRequest;
import com.dynamicmart.identity.exception.IdentityException;
import com.dynamicmart.identity.repository.AddressRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AddressServiceTest {
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final LocationClient locations = mock(LocationClient.class);
    private final AddressService service = new AddressService(addresses, locations);

    @Test
    void customerCannotDeactivateAnotherCustomersAddress() {
        UUID user = UUID.randomUUID(), address = UUID.randomUUID();
        when(addresses.findByIdAndUserId(address, user)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivate(user, address)).isInstanceOf(IdentityException.class).hasMessageContaining("Không tìm thấy");
        verify(addresses, never()).delete(any());
    }

    @Test
    void invalidProvinceWardPairIsNeverPersisted() {
        UUID user = UUID.randomUUID();
        var request = new AddressRequest("Thảo", "0900000000", "12 Nguyễn Huệ", 1, 999, "tampered", "tampered", false);
        when(addresses.findAllByUserIdAndStatusOrderByDefaultAddressDescUpdatedAtDesc(any(), any())).thenReturn(List.of());
        when(locations.validateAndResolve(1, 999)).thenThrow(new IdentityException(HttpStatus.UNPROCESSABLE_ENTITY, "ADDRESS_LOCATION_INVALID", "Địa giới đã chọn không hợp lệ."));
        assertThatThrownBy(() -> service.create(user, request)).isInstanceOf(IdentityException.class).hasMessageContaining("Địa giới");
        verify(addresses, never()).save(any());
    }
}
