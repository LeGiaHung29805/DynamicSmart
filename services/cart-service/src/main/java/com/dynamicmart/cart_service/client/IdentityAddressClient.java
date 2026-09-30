package com.dynamicmart.cart_service.client;

import com.dynamicmart.cart_service.exception.CartException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class IdentityAddressClient {
    private final RestClient client; private final String internalKey;
    public IdentityAddressClient(RestClient.Builder builder,
                                 @Value("${app.clients.identity-service-url:http://localhost:8081}") String baseUrl,
                                 @Value("${app.security.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(baseUrl).build(); this.internalKey = internalKey;
    }
    public AddressSnapshot requireActive(UUID customerId, UUID addressId) {
        try {
            AddressSnapshot result = client.get().uri("/api/v1/auth/internal/users/{userId}/addresses/{addressId}", customerId, addressId)
                    .header("X-Internal-Api-Key", internalKey).retrieve().body(AddressSnapshot.class);
            if (result == null || !"ACTIVE".equals(result.status())) throw invalid();
            return result;
        } catch (RestClientException exception) { throw invalid(); }
    }
    private CartException invalid() { return new CartException(HttpStatus.UNPROCESSABLE_ENTITY, "CHECKOUT_ADDRESS_INVALID", "Địa chỉ không tồn tại, đã ngừng dùng hoặc không thuộc khách hàng."); }
    public record AddressSnapshot(UUID id, String recipientName, String phone, String addressLine,
                                  int provinceId, int wardId, String provinceName, String wardName,
                                  boolean defaultAddress, String status, java.time.Instant updatedAt) { }
}
