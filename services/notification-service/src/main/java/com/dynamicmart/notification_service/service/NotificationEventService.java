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
        if (!markProcessed(event)) return false;
        OrderCompletedPayload payload = event.payload();
        if (payload == null || payload.customerId() == null) {
            throw new IllegalArgumentException("Payload OrderCompleted không hợp lệ.");
        }
        notifications.create(payload.customerId(), "ORDER_COMPLETED", "Đơn hàng đã hoàn tất",
                "Đơn hàng của bạn đã hoàn tất. Bạn có thể đánh giá sản phẩm đã mua.", Instant.now());
        return true;
    }

    @Transactional
    public boolean processPaymentSucceeded(EventEnvelope<PaymentSucceededPayload> event) {
        requireEvent(event, "PaymentSucceeded");
        if (!markProcessed(event)) return false;
        PaymentSucceededPayload payload = event.payload();
        if (payload == null || payload.customerId() == null) {
            throw new IllegalArgumentException("Payload PaymentSucceeded không hợp lệ.");
        }
        notifications.create(payload.customerId(), "PAYMENT_SUCCEEDED", "Thanh toán thành công",
                "Hệ thống đã ghi nhận thanh toán cho đơn hàng của bạn.", Instant.now());
        return true;
    }

    @Transactional
    public boolean processOrderCancelled(EventEnvelope<OrderCancelledPayload> event) {
        requireEvent(event, "OrderCancelled");
        if (!markProcessed(event)) return false;
        OrderCancelledPayload payload = event.payload();
        if (payload == null || payload.customerId() == null) {
            throw new IllegalArgumentException("Payload OrderCancelled không hợp lệ.");
        }
        String orderRef = payload.orderNumber() != null ? payload.orderNumber()
                : (payload.orderId() == null ? "" : payload.orderId().toString().substring(0, 8));
        String reason = payload.cancelReason() == null || payload.cancelReason().isBlank()
                ? "" : " Lý do: " + payload.cancelReason().trim();
        notifications.create(payload.customerId(), "ORDER_CANCELLED", "Đơn hàng đã bị hủy",
                "Đơn hàng #" + orderRef + " của bạn đã bị hủy." + reason, Instant.now());
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