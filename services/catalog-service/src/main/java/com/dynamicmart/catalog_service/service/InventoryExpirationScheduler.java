package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.entity.InventoryReservationStatus;
import com.dynamicmart.catalog_service.repository.InventoryReservationRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryExpirationScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryExpirationScheduler.class);
    private final InventoryReservationRepository reservationRepository;
    private final InventoryReservationService reservationService;

    public InventoryExpirationScheduler(InventoryReservationRepository reservationRepository,
                                        InventoryReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${app.inventory.expiration-scan-ms:30000}")
    public void expireDueReservations() {
        var reservationIds = reservationRepository.findDueReservationIds(
                InventoryReservationStatus.RESERVED, Instant.now(), PageRequest.of(0, 100));
        for (UUID reservationId : reservationIds) {
            try {
                reservationService.expire(reservationId);
            } catch (RuntimeException exception) {
                LOGGER.error("Cannot expire inventory reservation {}", reservationId, exception);
            }
        }
    }
}
