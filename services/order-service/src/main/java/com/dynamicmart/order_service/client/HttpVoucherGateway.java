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
public class HttpVoucherGateway implements VoucherPricingGateway, VoucherReservationGateway {
    private final RestClient client;
    public HttpVoucherGateway(@Qualifier("cartRestClient") RestClient client) { this.client = client; }
    @Override public VoucherPreview preview(VoucherPreviewRequest request) {
        PricingResponse response = post("/api/v1/cart/internal/voucher-pricing/preview",
                new PricingRequest(request.customerId(), request.merchandiseVoucherId(), request.shippingVoucherId(),
                        request.shippingFeeVnd(), request.items()), PricingResponse.class);
        if (response.vouchers() == null || response.lineDiscounts() == null) throw contractInvalid();
        if (response.vouchers().stream().anyMatch(java.util.Objects::isNull)
                || response.lineDiscounts().stream().anyMatch(java.util.Objects::isNull)) {
            throw contractInvalid();
        }
        return new VoucherPreview(response.vouchers().stream().map(value -> new AppliedVoucher(value.voucherId(), value.voucherCode(),
                value.scope(), value.discountMethod(), value.discountValue(), value.eligibleSubtotalVnd(), value.discountAmountVnd(), value.shippingDiscountVnd())).toList(),
                response.lineDiscounts().stream().map(value -> new LineDiscount(value.variantId(), value.productDiscountVnd(), value.orderDiscountVnd())).toList());
    }
    @Override public Reservation reserve(ReserveVoucherRequest request) {
        GroupResponse response = post("/api/v1/cart/internal/voucher-reservation-groups", request, GroupResponse.class);
        if (response.reservationId() == null) throw contractInvalid();
        return new Reservation(response.reservationId());
    }
    @Override public void consume(ConsumeVoucherRequest request) {
        GroupResponse response = post("/api/v1/cart/internal/voucher-reservation-groups/{id}/consume",
                new ConsumeBody(request.orderId()), GroupResponse.class, request.reservationId());
        if (!request.reservationId().equals(response.reservationId())) throw contractInvalid();
    }
    @Override public void release(ReleaseVoucherRequest request) {
        GroupResponse response = post("/api/v1/cart/internal/voucher-reservation-groups/{id}/release",
                new ReleaseBody(request.reason()), GroupResponse.class, request.reservationId());
        if (!request.reservationId().equals(response.reservationId())) throw contractInvalid();
    }
    private <T> T post(String uri, Object body, Class<T> type, Object... variables) {
        try { T value = client.post().uri(uri, variables).body(body).retrieve().body(type); if (value == null) throw contractInvalid(); return value; }
        catch (OrderException exception) { throw exception; }
        catch (RestClientException exception) { throw unavailable(); }
    }
    private OrderException unavailable() { return new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "CART_VOUCHER_UNAVAILABLE", "Không thể kết nối nghiệp vụ Voucher của Cart Service."); }
    private OrderException contractInvalid() { return new OrderException(HttpStatus.BAD_GATEWAY, "CART_VOUCHER_CONTRACT_INVALID", "Cart Service trả dữ liệu Voucher không hợp lệ."); }
    private record PricingRequest(UUID customerId, UUID merchandiseVoucherId, UUID shippingVoucherId,
                                  long shippingFeeVnd, List<VoucherItem> items) { }
    private record PricingResponse(List<PricingVoucher> vouchers, List<PricingLine> lineDiscounts) { }
    private record PricingVoucher(UUID voucherId, String voucherCode, String scope, String discountMethod,
                                  Long discountValue, long eligibleSubtotalVnd, long discountAmountVnd,
                                  long shippingDiscountVnd) { }
    private record PricingLine(UUID variantId, long productDiscountVnd, long orderDiscountVnd) { }
    private record GroupResponse(UUID reservationId) { }
    private record ConsumeBody(UUID orderId) { }
    private record ReleaseBody(String reason) { }
}
