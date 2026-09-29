package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpAddressGateway implements AddressGateway {
    private final RestClient client;
    public HttpAddressGateway(@Qualifier("identityRestClient") RestClient client) { this.client = client; }
    @Override public AddressSnapshot loadOwnedAddress(UUID customerId, UUID addressId) {
        try {
            IdentityAddress value = client.get().uri("/api/v1/auth/internal/users/{customerId}/addresses/{addressId}", customerId, addressId)
                    .retrieve().body(IdentityAddress.class);
            if (value == null || !"ACTIVE".equals(value.status())) throw invalid();
            return new AddressSnapshot(value.id(), value.recipientName(), value.phone(), value.addressLine(),
                    value.provinceId(), value.wardId(), value.provinceName(), value.wardName());
        } catch (RestClientException exception) { throw invalid(); }
    }
    private OrderException invalid() { return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "CHECKOUT_ADDRESS_INVALID", "Địa chỉ không tồn tại, đã ngừng dùng hoặc không thuộc khách hàng."); }
    private record IdentityAddress(UUID id, String recipientName, String phone, String addressLine, int provinceId,
                                   int wardId, String provinceName, String wardName, String status) { }
}
