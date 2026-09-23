package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CheckoutPricingService {
    public PricingBreakdown calculate(PricingRequest request) {
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw invalid("Checkout phải có ít nhất một dòng hàng.");
        }
        requireNonNegative(request.shippingFeeVnd(), "Phí giao hàng");
        requireNonNegative(request.shippingDiscountVnd(), "Giảm phí giao hàng");
        if (request.shippingDiscountVnd() > request.shippingFeeVnd()) {
            throw invalid("Giảm phí giao hàng không được vượt phí giao hàng.");
        }

        try {
            long listSubtotal = 0;
            long directSaleDiscount = 0;
            long itemsSubtotal = 0;
            long productDiscount = 0;
            long orderDiscount = 0;
            long merchandiseTotal = 0;

            for (LinePricing line : request.lines()) {
                validateLine(line);
                long lineListSubtotal = Math.multiplyExact(line.listPriceVnd(), line.quantity());
                long lineDirectDiscount = Math.multiplyExact(line.directSaleDiscountVnd(), line.quantity());
                long lineItemsSubtotal = Math.subtractExact(lineListSubtotal, lineDirectDiscount);
                long allocatedDiscount = Math.addExact(line.productDiscountVnd(), line.orderDiscountVnd());
                if (allocatedDiscount > lineItemsSubtotal) {
                    throw invalid("Tổng giảm giá của một dòng không được vượt tiền hàng sau direct sale.");
                }
                long lineTotal = Math.subtractExact(lineItemsSubtotal, allocatedDiscount);

                listSubtotal = Math.addExact(listSubtotal, lineListSubtotal);
                directSaleDiscount = Math.addExact(directSaleDiscount, lineDirectDiscount);
                itemsSubtotal = Math.addExact(itemsSubtotal, lineItemsSubtotal);
                productDiscount = Math.addExact(productDiscount, line.productDiscountVnd());
                orderDiscount = Math.addExact(orderDiscount, line.orderDiscountVnd());
                merchandiseTotal = Math.addExact(merchandiseTotal, lineTotal);
            }

            long expectedItemsSubtotal = Math.subtractExact(listSubtotal, directSaleDiscount);
            if (itemsSubtotal != expectedItemsSubtotal) {
                throw invalid("Tổng tiền hàng sau direct sale không khớp với các dòng.");
            }
            long expectedMerchandiseTotal = Math.subtractExact(
                    Math.subtractExact(itemsSubtotal, productDiscount), orderDiscount);
            if (merchandiseTotal != expectedMerchandiseTotal) {
                throw invalid("Phân bổ giảm giá xuống các dòng không khớp tổng Checkout.");
            }
            long payableShipping = Math.subtractExact(request.shippingFeeVnd(), request.shippingDiscountVnd());
            long finalTotal = Math.addExact(merchandiseTotal, payableShipping);

            return new PricingBreakdown(
                    listSubtotal,
                    directSaleDiscount,
                    itemsSubtotal,
                    productDiscount,
                    orderDiscount,
                    request.shippingFeeVnd(),
                    request.shippingDiscountVnd(),
                    finalTotal);
        } catch (ArithmeticException exception) {
            throw new OrderException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "MONEY_OVERFLOW",
                    "Giá trị tiền hoặc số lượng vượt giới hạn hỗ trợ.");
        }
    }

    private void validateLine(LinePricing line) {
        if (line == null || line.variantId() == null) {
            throw invalid("Dòng hàng phải có variantId.");
        }
        if (line.quantity() <= 0) {
            throw invalid("Số lượng phải lớn hơn 0.");
        }
        requireNonNegative(line.listPriceVnd(), "Giá niêm yết");
        requireNonNegative(line.directSaleDiscountVnd(), "Direct sale discount");
        requireNonNegative(line.productDiscountVnd(), "Giảm theo sản phẩm");
        requireNonNegative(line.orderDiscountVnd(), "Giảm toàn đơn phân bổ");
        if (line.directSaleDiscountVnd() > line.listPriceVnd()) {
            throw invalid("Direct sale discount không được vượt giá niêm yết.");
        }
    }

    private void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw invalid(field + " không được âm.");
        }
    }

    private OrderException invalid(String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRICE_BREAKDOWN", message);
    }

    public record PricingRequest(
            List<LinePricing> lines,
            long shippingFeeVnd,
            long shippingDiscountVnd) {
        public PricingRequest {
            lines = lines == null ? null : List.copyOf(lines);
        }
    }

    public record LinePricing(
            UUID variantId,
            long listPriceVnd,
            long directSaleDiscountVnd,
            int quantity,
            long productDiscountVnd,
            long orderDiscountVnd) {
    }

    public record PricingBreakdown(
            long itemsListSubtotalVnd,
            long directSaleDiscountVnd,
            long itemsSubtotalVnd,
            long productDiscountVnd,
            long orderDiscountVnd,
            long shippingFeeVnd,
            long shippingDiscountVnd,
            long finalTotalVnd) {
    }
}
