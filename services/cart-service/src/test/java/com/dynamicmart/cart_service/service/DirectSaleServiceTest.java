package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.PromotionDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.cart_service.entity.DirectPricePromotion;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.DirectPricePromotionRepository;
import com.dynamicmart.cart_service.repository.PromotionAuditRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DirectSaleServiceTest {
    private final DirectPricePromotionRepository promotions = mock(DirectPricePromotionRepository.class);
    private final DirectSaleService service = new DirectSaleService(promotions, mock(PromotionAuditRepository.class));

    @Test
    void resolvesSaleAndListPricesInOneRepositoryQuery() {
        UUID saleVariant = UUID.randomUUID();
        UUID regularVariant = UUID.randomUUID();
        Instant now = Instant.now();
        DirectPricePromotion promotion = new DirectPricePromotion("Sale", null, "PERCENTAGE", null,
                2_000, null, now.minusSeconds(60), now.plusSeconds(600), UUID.randomUUID(), Set.of(saleVariant), now);
        promotion.changeStatus("ACTIVE", now);
        when(promotions.findActiveForVariants(anySet(), any())).thenReturn(List.of(promotion));

        var result = service.resolveBatch(new DirectSalePriceBatchRequest(List.of(
                new DirectSalePriceRequest(saleVariant, 100_000),
                new DirectSalePriceRequest(regularVariant, 75_000))));

        assertThat(result).extracting(DirectSalePriceResponse::salePriceVnd).containsExactly(80_000L, 75_000L);
        assertThat(result.get(0).discountPercent()).isEqualTo(20);
        assertThat(result.get(1).promotionId()).isNull();
        verify(promotions).findActiveForVariants(anySet(), any());
    }

    @Test
    void rejectsDuplicateVariantBeforeQueryingDatabase() {
        UUID variantId = UUID.randomUUID();
        var request = new DirectSalePriceBatchRequest(List.of(
                new DirectSalePriceRequest(variantId, 100_000),
                new DirectSalePriceRequest(variantId, 100_000)));

        assertThatThrownBy(() -> service.resolveBatch(request))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("một lần");
    }
}
