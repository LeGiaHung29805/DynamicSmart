package com.dynamicmart.catalog_service.repository;

import com.dynamicmart.catalog_service.entity.InventoryReservation;
import com.dynamicmart.catalog_service.entity.InventoryReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByCheckoutSessionId(UUID checkoutSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from InventoryReservation reservation where reservation.checkoutSessionId = :checkoutSessionId")
    Optional<InventoryReservation> findByCheckoutSessionIdForUpdate(
            @Param("checkoutSessionId") UUID checkoutSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from InventoryReservation reservation where reservation.id = :id")
    Optional<InventoryReservation> findByIdForUpdate(@Param("id") UUID id);

    List<InventoryReservation> findAllByStatusAndExpiresAtLessThanEqual(
            InventoryReservationStatus status, Instant expiresAt);

    @Query("select reservation.id from InventoryReservation reservation " +
            "where reservation.status = :status and reservation.expiresAt <= :expiresAt " +
            "order by reservation.expiresAt")
    List<UUID> findDueReservationIds(@Param("status") InventoryReservationStatus status,
                                     @Param("expiresAt") Instant expiresAt,
                                     Pageable pageable);
}
