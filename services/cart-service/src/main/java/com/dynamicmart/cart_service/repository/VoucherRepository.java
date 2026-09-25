package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.Voucher;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    @Query("select v from Voucher v where v.distributionMode = 'DEFAULT_FOR_ELIGIBLE' and v.defaultVoucher = true and v.status = 'ACTIVE' and v.startsAt <= :now and v.endsAt > :now order by v.endsAt")
    List<Voucher> findVisibleDefaults(@Param("now") Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.id = :id")
    Optional<Voucher> findByIdForUpdate(@Param("id") UUID id);
}
