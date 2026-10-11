package com.dynamicmart.notification_service.service;

import com.dynamicmart.notification_service.dto.event.EventEnvelope;
import com.dynamicmart.notification_service.dto.event.OrderCancelledPayload;
import com.dynamicmart.notification_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.notification_service.dto.event.PaymentSucceededPayload;
import com.dynamicmart.notification_service.repository.ProcessedEventRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventService {
    private final ProcessedEventRepository processedEvents;
    private final NotificationService notifications;

    public NotificationEventService(ProcessedEventRepository processedEvents,
                                    NotificationService notifications) {
        this.processedEvents = processedEvents;
        this.notifications = notifications;
    }

    @Transactional
    public boolean processOrderCompleted(EventEnvelope<OrderCompletedPayload> event) {
        requireEvent(event, "OrderCompleted");
        OrderCompletedPayload payload = event.payload();
        if (payload == null || payload.customerId() == null) {
            throw new IllegalArgumentException("Payload OrderCompleted không hợp lệ.");
        }
        if (!markProcessed(event)) return false;
        String orderRef = payload.orderNumber() != null && !payload.orderNumber().isBlank()
                ? payload.orderNumber().trim()
                : (payload.orderId() == null ? "" : payload.orderId().toString().substring(0, 8));
        String orderInfo = orderRef.isBlank()
                ? "Đơn hàng của bạn đã hoàn tất."
                : "Đơn hàng #" + orderRef + " của bạn đã hoàn tất.";
        notifications.create(payload.customerId(), "ORDER_COMPLETED", "Đơn hàng đã hoàn tất",
                orderInfo + " Bạn có thể đánh giá sản phẩm đã mua.", Instant.now());
        return true;
    }

    @Transactional
    public boolean processPaymentSucceeded(EventEnvelope<PaymentSucceededPayload> event) {
        requireEvent(event, "PaymentSucceeded");
        PaymentSucceededPayload payload = event.payload();
        if (payload == null || payload.customerId() == null) {
            throw new IllegalArgumentException("Payload PaymentSucceeded không hợp lệ.");
        }
        if (!markProcessed(event)) return false;
        String amountInfo = payload.amountVnd() != null && payload.amountVnd() > 0
                ? String.format(java.util.Locale.US, "%,d đ", payload.amountVnd())
                : null;
        String content = amountInfo != null
                ? "Hệ thống đã ghi nhận thanh toán " + amountInfo + " cho đơn hàng của bạn."
                : "Hệ thống đã ghi nhận thanh toán cho đơn hàng của bạn.";
        notifications.create(payload.customerId(), "PAYMENT_SUCCEEDED", "Thanh toán thành công",
                content, Instant.now());
        return true;
    }

    @Transactional
    public boolean processOrderCancelled(EventEnvelope<OrderCancelledPayload> event) {
        requireEvent(event, "OrderCancelled");
        OrderCancelledPayload payload = event.payload();
        if (payload == null || payload.customerId() == null) {
            throw new IllegalArgumentException("Payload OrderCancelled không hợp lệ.");
        }
        if (!markProcessed(event)) return false;
        String orderRef = payload.orderNumber() != null && !payload.orderNumber().isBlank()
                ? payload.orderNumber().trim()
                : (payload.orderId() == null ? "" : payload.orderId().toString().substring(0, 8));
        String reason = payload.cancelReason() == null || payload.cancelReason().isBlank()
                ? "" : " Lý do: " + payload.cancelReason().trim();
        String prefix = orderRef.isBlank() ? "Đơn hàng của bạn" : "Đơn hàng #" + orderRef;
        notifications.create(payload.customerId(), "ORDER_CANCELLED", "Đơn hàng đã bị hủy",
                prefix + " đã bị hủy." + reason, Instant.now());
        return true;
    }

    private boolean markProcessed(EventEnvelope<?> event) {
        return processedEvents.insertIfAbsent(
                event.eventId(), event.eventType(), event.producer(),
                Instant.now(), event.correlationId()) == 1;
    }

    private void requireEvent(EventEnvelope<?> event, String expectedType) {
        if (event == null || event.eventId() == null || event.producer() == null
                || !expectedType.equals(event.eventType())) {
            throw new IllegalArgumentException("Event envelope không hợp lệ.");
        }
    }
}