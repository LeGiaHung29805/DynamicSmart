package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.DirectPricePromotion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectPricePromotionRepository extends JpaRepository<DirectPricePromotion, UUID> {
    @Query("select distinct p from DirectPricePromotion p join p.variantIds v where v = :variantId and p.status = 'ACTIVE' and p.startsAt <= :now and p.endsAt > :now order by p.startsAt desc")
    List<DirectPricePromotion> findActiveForVariant(@Param("variantId") UUID variantId, @Param("now") Instant now);

    @Query("select distinct p from DirectPricePromotion p join p.variantIds v where v in :variantIds and p.status = 'ACTIVE' and p.id <> :excludedId and p.startsAt < :endsAt and p.endsAt > :startsAt")
    List<DirectPricePromotion> findOverlapping(@Param("variantIds") Set<UUID> variantIds,
                                               @Param("excludedId") UUID excludedId,
                                               @Param("startsAt") Instant startsAt,
                                               @Param("endsAt") Instant endsAt);

    Optional<DirectPricePromotion> findByIdAndCreatedBy(UUID id, UUID createdBy);
}
