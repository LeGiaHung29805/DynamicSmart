package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ShippingPackageCalculator {
    public PackageMetrics calculate(List<CheckoutSessionItem> items) {
        if (items == null || items.isEmpty()) {
            throw invalid("Checkout không có sản phẩm để báo giá giao hàng.");
        }
        try {
            int totalWeight = 0;
            int length = 0;
            int width = 0;
            int height = 0;
            for (CheckoutSessionItem item : items) {
                if (item.getQuantity() <= 0 || item.getWeightGrams() <= 0
                        || item.getLengthCm() == null || item.getLengthCm() <= 0
                        || item.getWidthCm() == null || item.getWidthCm() <= 0
                        || item.getHeightCm() == null || item.getHeightCm() <= 0) {
                    throw invalid("Snapshot kích thước hoặc trọng lượng sản phẩm không hợp lệ.");
                }
                totalWeight = Math.addExact(totalWeight, Math.multiplyExact(item.getWeightGrams(), item.getQuantity()));
                length = Math.max(length, item.getLengthCm());
                width = Math.max(width, item.getWidthCm());
                height = Math.addExact(height, Math.multiplyExact(item.getHeightCm(), item.getQuantity()));
            }
            return new PackageMetrics(totalWeight, length, width, height);
        } catch (ArithmeticException exception) {
            throw new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "SHIPPING_PACKAGE_OVERFLOW",
                    "Trọng lượng hoặc kích thước kiện hàng vượt giới hạn hỗ trợ.");
        }
    }

    private OrderException invalid(String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_SHIPPING_PACKAGE", message);
    }

    public record PackageMetrics(int totalWeightGrams, int lengthCm, int widthCm, int heightCm) {
    }
}
