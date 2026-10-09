package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("!standalone")
public class HttpInventoryReservationGateway implements InventoryReservationGateway {
    private final RestClient client;
    public HttpInventoryReservationGateway(@Qualifier("catalogRestClient") RestClient client) { this.client = client; }
    @Override public Reservation reserve(ReserveInventoryRequest request) {
        ReservationData data = post("/api/v1/catalog/internal/inventory/reservations", request.operationKey(),
                new CatalogReserve(request.checkoutSessionId(), request.reservedUntil(),
                        request.lines().stream().map(value -> new CatalogLine(value.variantId(), value.quantity())).toList()));
        requireStatus(data, data.reservationId(), "RESERVED");
        return new Reservation(data.reservationId());
    }
    @Override public void commit(CommitInventoryRequest request) {
        ReservationData data = post("/api/v1/catalog/internal/inventory/reservations/{id}/commit",
                request.operationKey(), new CommitBody(request.orderId()), request.reservationId());
        requireStatus(data, request.reservationId(), "COMMITTED");
    }
    @Override public void release(ReleaseInventoryRequest request) {
        ReservationData data = post("/api/v1/catalog/internal/inventory/reservations/{id}/release",
                request.operationKey(), new ReleaseBody(request.reason()), request.reservationId());
        requireStatus(data, request.reservationId(), "RELEASED");
    }
    private ReservationData post(String uri, UUID operationKey, Object body, Object... variables) {
        try {
            Envelope response = client.post().uri(uri, variables)
                    .header("Idempotency-Key", operationKey.toString())
                    .body(body).retrieve().body(Envelope.class);
            if (response == null || response.data() == null || response.data().reservationId() == null
                    || response.data().status() == null || response.data().status().isBlank()) {
                throw contractInvalid();
            }
            return response.data();
        } catch (OrderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }
    private void requireStatus(ReservationData data, UUID expectedReservationId, String expectedStatus) {
        if (!expectedReservationId.equals(data.reservationId()) || !expectedStatus.equals(data.status())) {
            throw contractInvalid();
        }
    }
    private OrderException unavailable() { return new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_RESERVATION_UNAVAILABLE", "Không thể giữ hoặc trả tồn kho Catalog."); }
    private OrderException contractInvalid() { return new OrderException(HttpStatus.BAD_GATEWAY, "CATALOG_RESERVATION_CONTRACT_INVALID", "Catalog Service trả dữ liệu reservation không hợp lệ."); }
    private record CatalogReserve(UUID checkoutSessionId, java.time.Instant expiresAt, List<CatalogLine> items) { }
    private record CatalogLine(UUID variantId, int quantity) { }
    private record Envelope(ReservationData data) { }
    private record ReservationData(UUID reservationId, String status) { }
    private record CommitBody(UUID orderId) { }
    private record ReleaseBody(String reason) { }
}
