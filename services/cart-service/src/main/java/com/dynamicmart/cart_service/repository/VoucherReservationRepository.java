package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.VoucherReservation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherReservationRepository extends JpaRepository<VoucherReservation, UUID> {
    Optional<VoucherReservation> findByCheckoutSessionIdAndVoucherId(UUID checkoutSessionId, UUID voucherId);
    List<VoucherReservation> findAllByStatusAndReservedUntilLessThanEqual(String status, Instant now);
    List<VoucherReservation> findAllByOrderByCreatedAtDesc();
    long countByVoucherIdAndCustomerIdAndStatusIn(UUID voucherId, UUID customerId, List<String> statuses);
    @Query("select count(r) from VoucherReservation r where r.voucherId = :voucherId and r.status in ('RESERVED','CONSUMED')")
    long countAllocated(@Param("voucherId") UUID voucherId);
    @Query("select count(r) from VoucherReservation r, Voucher v where r.voucherId = v.id and r.checkoutSessionId = :checkoutSessionId and r.status in ('RESERVED','CONSUMED') and ((:shipping = true and v.scope = 'SHIPPING_DISCOUNT') or (:shipping = false and v.scope <> 'SHIPPING_DISCOUNT'))")
    long countSlot(@Param("checkoutSessionId") UUID checkoutSessionId, @Param("shipping") boolean shipping);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from VoucherReservation r where r.id = :id")
    Optional<VoucherReservation> findByIdForUpdate(@Param("id") UUID id);
}
