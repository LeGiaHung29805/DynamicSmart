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
public class HttpPaymentClient implements PaymentClient {
    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    public HttpPaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ShippingQuoteResponse createShippingQuote(ShippingQuoteRequest request) {
        return post("/api/v1/shipping/quotes", request, ShippingQuoteResponse.class);
    }

    @Override
    public ShippingQuoteResponse validateAndConsumeQuote(UUID quoteId, QuoteValidationRequest request) {
        return post("/api/v1/shipping/quotes/{quoteId}/validate", request, ShippingQuoteResponse.class, quoteId);
    }

    @Override
    public PaymentResponse createPayment(OrderPaymentContextRequest request) {
        return post("/api/v1/payments/order-context", request, PaymentResponse.class);
    }

    @Override
    public PaymentResponse createVnPayAttempt(UUID paymentId) {
        return post("/api/v1/payments/{paymentId}/vnpay-attempt", null, PaymentResponse.class, paymentId);
    }

    private <T> T post(String uri, Object body, Class<T> responseType, Object... uriVariables) {
        try {
            var request = restClient.post().uri(uri, uriVariables);
            var response = body == null ? request.retrieve() : request.body(body).retrieve();
            T result = response
                    .onStatus(HttpStatusCode::is4xxClientError, (ignoredRequest, ignoredResponse) -> {
                        throw new OrderException(
                                HttpStatus.UNPROCESSABLE_CONTENT,
                                "PAYMENT_REQUEST_REJECTED",
                                "Payment Service từ chối dữ liệu do Order Service gửi.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (ignoredRequest, ignoredResponse) -> {
                        throw unavailable();
                    })
                    .body(responseType);
            if (result == null) {
                throw unavailable();
            }
            return result;
        } catch (OrderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private OrderException unavailable() {
        return new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PAYMENT_SERVICE_UNAVAILABLE",
                "Không thể kết nối Payment Service. Vui lòng thử lại sau.");
    }
}
