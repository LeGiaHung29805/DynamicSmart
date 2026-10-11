package com.dynamicmart.reporting_service.service;

import com.dynamicmart.reporting_service.dto.event.EventEnvelope;
import com.dynamicmart.reporting_service.dto.event.OrderCompletedPayload;
import com.dynamicmart.reporting_service.dto.response.BestSellerResponse;
import com.dynamicmart.reporting_service.dto.response.DailySalesMetricResponse;
import com.dynamicmart.reporting_service.entity.DailyProductMetric;
import com.dynamicmart.reporting_service.entity.DailyProductMetricId;
import com.dynamicmart.reporting_service.entity.DailySalesMetric;
import com.dynamicmart.reporting_service.exception.ReportingException;
import com.dynamicmart.reporting_service.mapper.ReportMapper;
import com.dynamicmart.reporting_service.repository.DailyProductMetricRepository;
import com.dynamicmart.reporting_service.repository.DailySalesMetricRepository;
import com.dynamicmart.reporting_service.repository.ProcessedEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {
    private final DailySalesMetricRepository dailySales;
    private final DailyProductMetricRepository dailyProducts;
    private final ProcessedEventRepository processedEvents;
    private final ReportMapper mapper;

    public ReportingService(DailySalesMetricRepository dailySales,
                            DailyProductMetricRepository dailyProducts,
                            ProcessedEventRepository processedEvents,
                            ReportMapper mapper) {
        this.dailySales = dailySales;
        this.dailyProducts = dailyProducts;
        this.processedEvents = processedEvents;
        this.mapper = mapper;
    }

    @Transactional
    public boolean processOrderCompleted(EventEnvelope<OrderCompletedPayload> event) {
        requireEvent(event);
        if (!markProcessed(event)) return false;
        OrderCompletedPayload payload = event.payload();
        if (payload == null || payload.orderId() == null || payload.customerId() == null) {
            throw new ReportingException(HttpStatus.BAD_REQUEST,
                    "ORDER_COMPLETED_PAYLOAD_INVALID", "Payload OrderCompleted không hợp lệ.");
        }
        Instant now = Instant.now();
        LocalDate date = event.occurredAt() == null
                ? LocalDate.now(ZoneOffset.UTC)
                : event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();
        DailySalesMetric salesMetric = dailySales.findById(date)
                .orElseGet(() -> new DailySalesMetric(date, now));
        salesMetric.addCompletedOrder(value(payload.grossItemSalesVnd()),
                value(payload.discountValueVnd()), value(payload.shippingFeeVnd()),
                value(payload.finalTotalVnd()), now);
        dailySales.save(salesMetric);

        if (payload.items() != null) {
            for (OrderCompletedPayload.OrderCompletedItemPayload item : payload.items()) {
                if (item.productId() == null || item.variantId() == null || item.quantity() == null) continue;
                DailyProductMetricId id = new DailyProductMetricId(date, item.productId(), item.variantId());
                DailyProductMetric metric = dailyProducts.findById(id)
                        .orElseGet(() -> new DailyProductMetric(id, now));
                metric.addSoldQuantity(item.quantity(), value(item.grossSalesVnd()),
                        value(item.netItemSalesVnd()), now);
                dailyProducts.save(metric);
            }
        }
        return true;
    }

    @Transactional(readOnly = true)
    public List<DailySalesMetricResponse> sales(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return dailySales.findAllByMetricDateBetweenOrderByMetricDateAsc(from, to)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BestSellerResponse> bestSellers(LocalDate from, LocalDate to, int limit) {
        validateRange(from, to);
        return dailyProducts.bestSellers(from, to, PageRequest.of(0, limit));
    }

    private boolean markProcessed(EventEnvelope<?> event) {
        return processedEvents.insertIfAbsent(
                event.eventId(), event.eventType(), event.producer(),
                Instant.now(), event.correlationId()) == 1;
    }

    private void requireEvent(EventEnvelope<?> event) {
        if (event == null || event.eventId() == null || event.producer() == null
                || !"OrderCompleted".equals(event.eventType())) {
            throw new ReportingException(HttpStatus.BAD_REQUEST,
                    "EVENT_ENVELOPE_INVALID", "Event OrderCompleted không hợp lệ.");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ReportingException(HttpStatus.BAD_REQUEST,
                    "REPORT_DATE_RANGE_INVALID", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}