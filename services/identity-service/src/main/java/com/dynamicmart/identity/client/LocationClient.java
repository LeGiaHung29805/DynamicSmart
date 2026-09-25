package com.dynamicmart.identity.client;

import com.dynamicmart.identity.exception.IdentityException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LocationClient {
    private final RestClient client;
    private final String apiKey;

    public LocationClient(RestClient.Builder builder,
                          @Value("${app.clients.payment-service-url:http://localhost:8085}") String baseUrl,
                          @Value("${app.clients.internal-api-key:local-internal-key}") String apiKey) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public ResolvedLocation validateAndResolve(int provinceId, int wardId) {
        try {
            Validation validation = client.get().uri(uri -> uri.path("/api/v1/locations/validate")
                    .queryParam("provinceId", provinceId).queryParam("wardId", wardId).build())
                    .header("X-Internal-Api-Key", apiKey).retrieve().body(Validation.class);
            if (validation == null || !validation.valid()) throw invalidLocation();
            Location[] provinces = client.get().uri("/api/v1/locations/provinces").retrieve().body(Location[].class);
            Location[] wards = client.get().uri(uri -> uri.path("/api/v1/locations/wards").queryParam("provinceId", provinceId).build())
                    .retrieve().body(Location[].class);
            String provinceName = findName(provinces, provinceId);
            String wardName = findName(wards, wardId);
            return new ResolvedLocation(provinceName, wardName);
        } catch (RestClientException exception) {
            throw new IdentityException(HttpStatus.UNPROCESSABLE_ENTITY, "ADDRESS_LOCATION_INVALID",
                    "Tỉnh/Thành phố hoặc Phường/Xã không hợp lệ.");
        }
    }

    private String findName(Location[] values, int id) {
        if (values == null) throw invalidLocation();
        return Arrays.stream(values).filter(value -> value.id() == id).map(Location::name).findFirst().orElseThrow(this::invalidLocation);
    }
    private IdentityException invalidLocation() {
        return new IdentityException(HttpStatus.UNPROCESSABLE_ENTITY, "ADDRESS_LOCATION_INVALID", "Địa giới đã chọn không hợp lệ.");
    }
    private record Validation(boolean valid, int provinceId, int wardId) { }
    private record Location(int id, String name) { }
    public record ResolvedLocation(String provinceName, String wardName) { }
}
