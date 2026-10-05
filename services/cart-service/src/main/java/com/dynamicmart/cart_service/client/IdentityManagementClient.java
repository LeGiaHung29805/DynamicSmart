package com.dynamicmart.cart_service.client;

import com.dynamicmart.cart_service.exception.CartException;
import tools.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IdentityManagementClient {
    private final RestClient client;

    public IdentityManagementClient(RestClient.Builder builder,
                                    @Value("${app.clients.identity-service-url:http://localhost:8081}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public JsonNode profile(String token) { return get("/api/v1/profile", token); }
    public JsonNode updateProfile(String token, JsonNode body) { return put("/api/v1/profile", token, body); }
    public JsonNode addresses(String token) { return get("/api/v1/addresses", token); }
    public JsonNode createAddress(String token, JsonNode body) { return post("/api/v1/addresses", token, body); }
    public JsonNode updateAddress(String token, UUID id, JsonNode body) { return put("/api/v1/addresses/" + id, token, body); }
    public JsonNode makeDefault(String token, UUID id) { return post("/api/v1/addresses/" + id + "/default", token, null); }
    public void deactivateAddress(String token, UUID id) {
        execute(() -> { client.delete().uri("/api/v1/addresses/{id}", id).header("Authorization", bearer(token)).retrieve().toBodilessEntity(); return null; });
    }
    public JsonNode users(String token, String query, String status, String role, int page, int size) {
        return execute(() -> client.get().uri(builder -> builder.path("/api/v1/admin/users")
                .queryParamIfPresent("query", optional(query)).queryParamIfPresent("status", optional(status))
                .queryParamIfPresent("role", optional(role)).queryParam("page", page).queryParam("size", size).build())
                .header("Authorization", bearer(token)).retrieve().body(JsonNode.class));
    }
    public JsonNode user(String token, UUID id) { return get("/api/v1/admin/users/" + id, token); }
    public JsonNode manageUser(String token, UUID id, UUID key, JsonNode body) {
        return execute(() -> client.patch().uri("/api/v1/admin/users/{id}", id).header("Authorization", bearer(token))
                .header("Idempotency-Key", key.toString()).body(body).retrieve().body(JsonNode.class));
    }

    private JsonNode get(String uri, String token) { return execute(() -> client.get().uri(uri).header("Authorization", bearer(token)).retrieve().body(JsonNode.class)); }
    private JsonNode post(String uri, String token, JsonNode body) {
        return execute(() -> {
            var request = client.post().uri(uri).header("Authorization", bearer(token));
            return (body == null ? request : request.body(body)).retrieve().body(JsonNode.class);
        });
    }
    private JsonNode put(String uri, String token, JsonNode body) { return execute(() -> client.put().uri(uri).header("Authorization", bearer(token)).body(body).retrieve().body(JsonNode.class)); }
    private String bearer(String token) { return "Bearer " + token; }
    private java.util.Optional<String> optional(String value) { return value == null || value.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(value); }
    private <T> T execute(java.util.function.Supplier<T> call) {
        try { return call.get(); }
        catch (RestClientResponseException exception) {
            throw new CartException(HttpStatus.valueOf(exception.getStatusCode().value()), "IDENTITY_REQUEST_FAILED",
                    "Không thể xử lý dữ liệu tài khoản lúc này.");
        } catch (RestClientException exception) {
            throw new CartException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_UNAVAILABLE", "Dịch vụ đăng nhập tạm thời không phản hồi.");
        }
    }
}
