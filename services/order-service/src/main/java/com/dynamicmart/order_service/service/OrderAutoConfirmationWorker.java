package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Completes delivery confirmation only after the customer confirmation window expires. */
@Component
public class OrderAutoConfirmationWorker {
    private static final Logger log = LoggerFactory.getLogger(OrderAutoConfirmationWorker.class);

    private final CustomerOrderRepository orders;
    private final OrderLifecycleCommandService lifecycle;
    private final Clock clock;
    private final Duration confirmationWindow;
    private final int batchSize;

    public OrderAutoConfirmationWorker(
            CustomerOrderRepository orders,
            OrderLifecycleCommandService lifecycle,
            Clock clock,
            @Value("${app.delivery-confirmation.auto-confirm-after:7d}") Duration confirmationWindow,
            @Value("${app.delivery-confirmation.batch-size:50}") int batchSize) {
        this.orders = orders;
        this.lifecycle = lifecycle;
        this.clock = clock;
        this.confirmationWindow = confirmationWindow;
        this.batchSize = Math.max(1, Math.min(batchSize, 200));
    }

    @Scheduled(
            fixedDelayString = "${app.delivery-confirmation.fixed-delay:1h}",
            initialDelayString = "${app.delivery-confirmation.initial-delay:2m}")
    public void confirmExpiredDeliveries() {
        Instant deadline = Instant.now(clock).minus(confirmationWindow);
        var candidates = orders.findAwaitingCustomerConfirmationBefore(
                deadline, PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "updatedAt")));
        for (var order : candidates) {
            try {
                lifecycle.autoConfirmReceived(order.getId());
            } catch (RuntimeException exception) {
                log.warn("Không thể tự xác nhận đơn {}. Worker sẽ thử lại ở lượt sau.", order.getId(), exception);
            }
        }
    }
}
