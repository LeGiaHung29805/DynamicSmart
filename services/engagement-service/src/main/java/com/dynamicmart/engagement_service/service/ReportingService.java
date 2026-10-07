package com.dynamicmart.engagement_service.service;

import com.dynamicmart.engagement_service.dto.event.EventEnvelope;
import com.dynamicmart.engagement_service.dto.event.OrderCancelledPayload;
import com.dynamicmart.engagement_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.engagement_service.dto.event.PaymentSucceededPayload;
import com.dynamicmart.engagement_service.dto.response.BestSellerResponse;
import com.dynamicmart.engagement_service.dto.response.DailySalesMetricResponse;
import com.dynamicmart.engagement_service.entity.DailyProductMetric;
import com.dynamicmart.engagement_service.entity.DailyProductMetricId;
import com.dynamicmart.engagement_service.entity.DailySalesMetric;
import com.dynamicmart.engagement_service.entity.ProcessedEvent;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.ReportMapper;
import com.dynamicmart.engagement_service.repository.DailyProductMetricRepository;
import com.dynamicmart.engagement_service.repository.DailySalesMetricRepository;
import com.dynamicmart.engagement_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {
    private final DailySalesMetricRepository dailySales;
    private final DailyProductMetricRepository dailyProducts;
    private final ProcessedEventRepository processedEvents;
    private final NotificationService notifications;
    private final ReportMapper mapper;

    public ReportingService(DailySalesMetricRepository dailySales, DailyProductMetricRepository dailyProducts,
                            ProcessedEventRepository processedEvents, NotificationService notifications,
                            ReportMapper mapper) {
        this.dailySales = dailySales;
        this.dailyProducts = dailyProducts;
        this.processedEvents = processedEvents;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @Transactional
    public boolean processOrderCompleted(EventEnvelope<OrderCompletedPayload> event) {
        requireEvent(event, "OrderCompleted");
        if (!markProcessed(event)) {
            return false;
        }
        OrderCompletedPayload payload = event.payload();
        if (payload == null || payload.orderId() == null || payload.customerId() == null) {
            throw new EngagementException(HttpStatus.BAD_REQUEST, "ORDER_COMPLETED_PAYLOAD_INVALID",
                    "Payload OrderCompleted không hợp lệ.");
        }
        Instant now = Instant.now();
        LocalDate date = event.occurredAt() == null
                ? LocalDate.now(ZoneOffset.UTC)
                : event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();
        DailySalesMetric salesMetric = dailySales.findById(date)
                .orElseGet(() -> new DailySalesMetric(date, now));
        salesMetric.addCompletedOrder(value(payload.grossItemSalesVnd()), value(payload.discountValueVnd()),
                value(payload.shippingFeeVnd()), value(payload.finalTotalVnd()), now);
        dailySales.save(salesMetric);

        if (payload.items() != null) {
            for (OrderCompletedPayload.OrderCompletedItemPayload item : payload.items()) {
                if (item.productId() == null || item.variantId() == null || item.quantity() == null) {
                    continue;
                }
                DailyProductMetricId id = new DailyProductMetricId(date, item.productId(), item.variantId());
                DailyProductMetric productMetric = dailyProducts.findById(id)
                        .orElseGet(() -> new DailyProductMetric(id, now));
                productMetric.addSoldQuantity(item.quantity(), value(item.grossSalesVnd()),
                        value(item.netItemSalesVnd()), now);
                dailyProducts.save(productMetric);
            }
        }
        notifications.create(payload.customerId(), "ORDER_COMPLETED", "Đơn hàng đã hoàn tất",
                "Đơn hàng của bạn đã hoàn tất. Bạn có thể đánh giá sản phẩm đã mua.", now);
        return true;
    }

    @Transactional
    public boolean processPaymentSucceeded(EventEnvelope<PaymentSucceededPayload> event) {
        requireEvent(event, "PaymentSucceeded");
        if (!markProcessed(event)) {
            return false;
        }
        PaymentSucceededPayload payload = event.payload();
        if (payload != null && payload.customerId() != null) {
            notifications.create(payload.customerId(), "PAYMENT_SUCCEEDED", "Thanh toán thành công",
                    "Hệ thống đã ghi nhận thanh toán cho đơn hàng của bạn.", Instant.now());
        }
        return true;
    }

    @Transactional
    public boolean processOrderCancelled(EventEnvelope<OrderCancelledPayload> event) {
        requireEvent(event, "OrderCancelled");
        if (!markProcessed(event)) {
            return false;
        }
        OrderCancelledPayload payload = event.payload();
        if (payload != null && payload.customerId() != null) {
            String orderRef = payload.orderNumber() != null ? payload.orderNumber()
                    : (payload.orderId() != null ? payload.orderId().toString().substring(0, 8) : "");
            String reasonText = payload.cancelReason() != null && !payload.cancelReason().isBlank()
                    ? " Lý do: " + payload.cancelReason().trim()
                    : "";
            notifications.create(payload.customerId(), "ORDER_CANCELLED", "Đơn hàng đã bị hủy",
                    "Đơn hàng #" + orderRef + " của bạn đã bị hủy." + reasonText, Instant.now());
        }
        return true;
    }

    @Transactional(readOnly = true)
    public List<DailySalesMetricResponse> sales(LocalDate from, LocalDate to) {
        return dailySales.findAllByMetricDateBetweenOrderByMetricDateAsc(from, to)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BestSellerResponse> bestSellers(LocalDate from, LocalDate to, int limit) {
        return dailyProducts.bestSellers(from, to, PageRequest.of(0, limit));
    }

    private boolean markProcessed(EventEnvelope<?> event) {
        if (processedEvents.existsById(event.eventId())) {
            return false;
        }
        try {
            processedEvents.saveAndFlush(new ProcessedEvent(event.eventId(), event.eventType(),
                    event.producer(), event.correlationId(), Instant.now()));
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private void requireEvent(EventEnvelope<?> event, String type) {
        if (event == null || event.eventId() == null || event.eventType() == null || event.producer() == null) {
            throw new EngagementException(HttpStatus.BAD_REQUEST, "EVENT_ENVELOPE_INVALID",
                    "Event envelope không hợp lệ.");
        }
        if (!type.equals(event.eventType())) {
            throw new EngagementException(HttpStatus.BAD_REQUEST, "EVENT_TYPE_INVALID",
                    "Event type không đúng endpoint xử lý.");
        }
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
