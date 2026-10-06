package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> { throw invalid(); })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> { throw unavailable(); })
                    .body(IdentityAddress.class);
            if (value == null || !addressId.equals(value.id()) || isBlank(value.recipientName())
                    || isBlank(value.phone()) || isBlank(value.addressLine()) || value.provinceId() < 1
                    || value.wardId() < 1 || isBlank(value.provinceName()) || isBlank(value.wardName())) {
                throw contractInvalid();
            }
            if (!"ACTIVE".equals(value.status())) throw invalid();
            return new AddressSnapshot(value.id(), value.recipientName(), value.phone(), value.addressLine(),
                    value.provinceId(), value.wardId(), value.provinceName(), value.wardName());
        } catch (OrderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }
    private OrderException invalid() { return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "CHECKOUT_ADDRESS_INVALID", "Địa chỉ không tồn tại, đã ngừng dùng hoặc không thuộc khách hàng."); }
    private OrderException unavailable() { return new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_ADDRESS_UNAVAILABLE", "Không thể kiểm tra địa chỉ với Identity Service."); }
    private OrderException contractInvalid() { return new OrderException(HttpStatus.BAD_GATEWAY, "IDENTITY_ADDRESS_CONTRACT_INVALID", "Identity Service trả dữ liệu địa chỉ không hợp lệ."); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private record IdentityAddress(UUID id, String recipientName, String phone, String addressLine, int provinceId,
                                   int wardId, String provinceName, String wardName, String status) { }
}
