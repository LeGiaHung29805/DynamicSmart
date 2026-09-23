package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderStateMachine {
    public OrderStatus initialStatus(PaymentTiming timing, PaymentMethod method, long finalTotalVnd) {
        validatePaymentChoice(timing, method, finalTotalVnd);
        return timing == PaymentTiming.PREPAID ? OrderStatus.PENDING_PAYMENT : OrderStatus.CONFIRMED;
    }

    public List<OrderStatus> onPaymentSucceeded(
            OrderStatus current,
            PaymentTiming timing,
            PaymentMethod method) {
        if (timing == PaymentTiming.NOT_REQUIRED || method == PaymentMethod.FREE) {
            throw invalid("Đơn 0 đồng không nhận sự kiện thanh toán ngoài hệ thống.");
        }

        if (timing == PaymentTiming.PREPAID) {
            if (current == OrderStatus.PENDING_PAYMENT) {
                return List.of(OrderStatus.CONFIRMED);
            }
            return List.of();
        }

        if (current == OrderStatus.DELIVERED) {
            return List.of(OrderStatus.COMPLETED);
        }
        if (current == OrderStatus.CANCELLED || current == OrderStatus.COMPLETED) {
            return List.of();
        }
        if (current == OrderStatus.CONFIRMED
                || current == OrderStatus.PACKING
                || current == OrderStatus.SHIPPING
                || current == OrderStatus.HANDOVER_PENDING) {
            return List.of();
        }
        throw invalid("Thanh toán thành công không hợp lệ với trạng thái hiện tại.");
    }

    public Optional<OrderStatus> onPaymentFailedOrExpired(OrderStatus current, PaymentTiming timing) {
        if (timing != PaymentTiming.PREPAID) {
            throw invalid("Chỉ đơn trả trước đang chờ thanh toán mới có thể bị hủy bởi Payment.");
        }
        if (current == OrderStatus.PENDING_PAYMENT) {
            return Optional.of(OrderStatus.CANCELLED);
        }
        return Optional.empty();
    }

    public OrderStatus adminPack(OrderStatus current) {
        require(current == OrderStatus.CONFIRMED, "Chỉ đơn đã xác nhận mới được chuyển sang đóng gói.");
        return OrderStatus.PACKING;
    }

    public OrderStatus adminShip(OrderStatus current) {
        require(current == OrderStatus.PACKING, "Chỉ đơn đang đóng gói mới được chuyển sang giao hàng.");
        return OrderStatus.SHIPPING;
    }

    public OrderStatus adminHandover(OrderStatus current, PaymentTiming timing) {
        require(timing == PaymentTiming.POSTPAID,
                "Chỉ đơn trả sau mới có bước chờ bàn giao và thu tiền.");
        require(current == OrderStatus.SHIPPING,
                "Chỉ đơn trả sau đang giao mới được chuyển sang chờ bàn giao.");
        return OrderStatus.HANDOVER_PENDING;
    }

    public List<OrderStatus> customerConfirmReceived(
            OrderStatus current,
            PaymentTiming timing,
            boolean paymentSucceeded) {
        if (current == OrderStatus.COMPLETED) {
            return List.of();
        }
        if (current == OrderStatus.DELIVERED) {
            return paymentSucceeded || timing == PaymentTiming.NOT_REQUIRED
                    ? List.of(OrderStatus.COMPLETED)
                    : List.of();
        }

        OrderStatus requiredStatus = timing == PaymentTiming.POSTPAID
                ? OrderStatus.HANDOVER_PENDING
                : OrderStatus.SHIPPING;
        require(current == requiredStatus,
                "Đơn chưa ở trạng thái cho phép khách hàng xác nhận đã nhận.");

        boolean canComplete = timing != PaymentTiming.POSTPAID || paymentSucceeded;
        return canComplete
                ? List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED)
                : List.of(OrderStatus.DELIVERED);
    }

    private void validatePaymentChoice(PaymentTiming timing, PaymentMethod method, long finalTotalVnd) {
        require(finalTotalVnd >= 0, "Tổng tiền cuối không được âm.");
        if (finalTotalVnd == 0) {
            require(timing == PaymentTiming.NOT_REQUIRED && method == PaymentMethod.FREE,
                    "Đơn 0 đồng bắt buộc dùng NOT_REQUIRED và FREE.");
            return;
        }
        require(timing != PaymentTiming.NOT_REQUIRED && method != PaymentMethod.FREE,
                "Đơn có số tiền phải thanh toán không được dùng FREE.");
        require(!(timing == PaymentTiming.PREPAID && method == PaymentMethod.COD),
                "COD không hỗ trợ thanh toán trả trước.");
        require(timing == PaymentTiming.PREPAID || timing == PaymentTiming.POSTPAID,
                "Thời điểm thanh toán không hợp lệ.");
        require(method == PaymentMethod.VNPAY || method == PaymentMethod.COD,
                "Phương thức thanh toán không hợp lệ.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw invalid(message);
        }
    }

    private OrderException invalid(String message) {
        return new OrderException(HttpStatus.CONFLICT, "INVALID_ORDER_TRANSITION", message);
    }
}
