package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.OrderConfirmedCommand;
import com.dynamicmart.order_service.exception.OrderException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("!standalone")
public class HttpCartOrderConfirmationGateway implements CartOrderConfirmationGateway {
    private final RestClient client;

    public HttpCartOrderConfirmationGateway(@Qualifier("cartRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public void confirm(OrderConfirmedCommand command) {
        try {
            CartCleanupResponse response = client.post()
                    .uri("/api/v1/cart/internal/order-confirmed")
                    .body(command)
                    .retrieve()
                    .body(CartCleanupResponse.class);
            if (response == null) {
                throw unavailable();
            }
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private OrderException unavailable() {
        return new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CART_CLEANUP_UNAVAILABLE",
                "Không thể gửi OrderConfirmed tới Cart Service.");
    }

    private record CartCleanupResponse(int removedCount, boolean duplicate) {
    }
}
