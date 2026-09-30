package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpInventoryReservationGateway implements InventoryReservationGateway {
    private final RestClient client;
    public HttpInventoryReservationGateway(@Qualifier("catalogRestClient") RestClient client) { this.client = client; }
    @Override public Reservation reserve(ReserveInventoryRequest request) {
        try {
            Envelope response = client.post().uri("/api/v1/catalog/internal/inventory/reservations")
                    .header("Idempotency-Key", request.operationKey().toString())
                    .body(new CatalogReserve(request.checkoutSessionId(), request.reservedUntil(),
                            request.lines().stream().map(value -> new CatalogLine(value.variantId(), value.quantity())).toList()))
                    .retrieve().body(Envelope.class);
            if (response == null || response.data() == null || response.data().reservationId() == null) throw unavailable();
            return new Reservation(response.data().reservationId());
        } catch (RestClientException exception) { throw unavailable(); }
    }
    @Override public void commit(CommitInventoryRequest request) {
        try { client.post().uri("/api/v1/catalog/internal/inventory/reservations/{id}/commit", request.reservationId())
                .header("Idempotency-Key", request.operationKey().toString())
                .body(new CommitBody(request.orderId())).retrieve().toBodilessEntity(); }
        catch (RestClientException exception) { throw unavailable(); }
    }
    @Override public void release(ReleaseInventoryRequest request) {
        try { client.post().uri("/api/v1/catalog/internal/inventory/reservations/{id}/release", request.reservationId())
                .header("Idempotency-Key", request.operationKey().toString()).body(new ReleaseBody(request.reason())).retrieve().toBodilessEntity(); }
        catch (RestClientException exception) { throw unavailable(); }
    }
    private OrderException unavailable() { return new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_RESERVATION_UNAVAILABLE", "Không thể giữ hoặc trả tồn kho Catalog."); }
    private record CatalogReserve(UUID checkoutSessionId, java.time.Instant expiresAt, List<CatalogLine> items) { }
    private record CatalogLine(UUID variantId, int quantity) { }
    private record Envelope(ReservationData data) { }
    private record ReservationData(UUID reservationId) { }
    private record CommitBody(UUID orderId) { }
    private record ReleaseBody(String reason) { }
}
