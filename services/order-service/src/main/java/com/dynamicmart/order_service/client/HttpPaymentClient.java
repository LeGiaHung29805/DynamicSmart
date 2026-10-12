package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
    public PaymentResponse getPayment(UUID paymentId) {
        return get("/api/v1/payments/{paymentId}", PaymentResponse.class, paymentId);
    }

    @Override
    public PaymentResponse createVnPayAttempt(UUID paymentId) {
        return post("/api/v1/payments/{paymentId}/vnpay-attempt", null, PaymentResponse.class, paymentId);
    }

    @Override
    public PaymentResponse collectCodForReceivedOrder(UUID orderId) {
        return post("/api/v1/payments/orders/{orderId}/cod-collected", null, PaymentResponse.class, orderId);
    }

    private <T> T post(String uri, Object body, Class<T> responseType, Object... uriVariables) {
        try {
            var request = restClient.post().uri(uri, uriVariables);
            T result = (body == null ? request.retrieve() : request.body(body).retrieve()).body(responseType);
            if (result == null) throw unavailable();
            return result;
        } catch (OrderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw translatePaymentRejection(exception);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private <T> T get(String uri, Class<T> responseType, Object... uriVariables) {
        try {
            T result = restClient.get().uri(uri, uriVariables).retrieve().body(responseType);
            if (result == null) throw unavailable();
            return result;
        } catch (OrderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw translatePaymentRejection(exception);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private OrderException translatePaymentRejection(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        PaymentError error = exception.getResponseBodyAs(PaymentError.class);
        boolean hasErrorBody = error != null && error.code() != null && !error.code().isBlank()
                && error.message() != null && !error.message().isBlank();
        if (status != null && status.is4xxClientError()) {
            return hasErrorBody
                    ? new OrderException(status, error.code(), error.message())
                    : new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "PAYMENT_REQUEST_REJECTED",
                            "Payment Service từ chối dữ liệu do Order Service gửi.");
        }
        if (status != null && (status == HttpStatus.BAD_GATEWAY || status == HttpStatus.SERVICE_UNAVAILABLE) && hasErrorBody) {
            return new OrderException(status, error.code(), error.message());
        }
        return unavailable();
    }

    private OrderException unavailable() {
        return new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PAYMENT_SERVICE_UNAVAILABLE",
                "Không thể kết nối Payment Service. Vui lòng thử lại sau.");
    }
    private record PaymentError(String code, String message) { }
}
