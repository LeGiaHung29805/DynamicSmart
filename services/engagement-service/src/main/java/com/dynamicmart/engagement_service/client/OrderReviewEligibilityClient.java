package com.dynamicmart.engagement_service.client;

import com.dynamicmart.engagement_service.exception.EngagementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OrderReviewEligibilityClient {
    private final RestClient restClient;

    public OrderReviewEligibilityClient(@Value("${app.clients.order-service-url}") String orderServiceUrl,
                                        @Value("${app.security.internal-api-key:}") String internalApiKey,
                                        RestClient.Builder builder) {
        RestClient.Builder clientBuilder = builder.baseUrl(orderServiceUrl);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            clientBuilder.defaultHeader("X-Internal-Api-Key", internalApiKey);
        }
        this.restClient = clientBuilder.build();
    }

    public ReviewEligibility requireEligible(UUID customerId, UUID orderItemId) {
        try {
            ReviewEligibility response = restClient.post()
                    .uri("/api/v1/orders/internal/review-eligibility")
                    .body(new ReviewEligibilityRequest(customerId, orderItemId))
                    .retrieve()
                    .body(ReviewEligibility.class);
            if (response == null || !response.eligible()) {
                String reason = response == null || response.reason() == null
                        ? "Dòng đơn hàng chưa đủ điều kiện đánh giá."
                        : response.reason();
                throw new EngagementException(HttpStatus.UNPROCESSABLE_ENTITY, "REVIEW_NOT_ELIGIBLE", reason);
            }
            return response;
        } catch (EngagementException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new EngagementException(HttpStatus.BAD_GATEWAY, "REVIEW_ELIGIBILITY_UNAVAILABLE",
                    "Chưa kiểm tra được điều kiện đánh giá từ Order Service.");
        }
    }

    public record ReviewEligibilityRequest(UUID customerId, UUID orderItemId) {
    }

    public record ReviewEligibility(
            boolean eligible,
            UUID orderId,
            UUID orderItemId,
            UUID customerId,
            UUID productId,
            UUID variantId,
            String reason
    ) {
    }
}
