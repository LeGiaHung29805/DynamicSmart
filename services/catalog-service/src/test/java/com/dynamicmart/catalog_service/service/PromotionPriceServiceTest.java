package com.dynamicmart.catalog_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.client.PromotionPriceClient;
import com.dynamicmart.catalog_service.config.CatalogPromotionProperties;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromotionPriceServiceTest {
    private final PromotionPriceClient client = mock(PromotionPriceClient.class);
    private final CatalogPromotionProperties httpProperties = properties(CatalogPromotionProperties.Mode.HTTP);
    private final PromotionPriceService service = new PromotionPriceService(client, httpProperties);

    @Test
    void overlaysValidDirectSalePriceAndKeepsListPriceForOtherVariants() {
        ProductVariant saleVariant = variant(100_000);
        ProductVariant regularVariant = variant(75_000);
        UUID promotionId = UUID.randomUUID();
        when(client.resolve(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                new PromotionPriceClient.PriceResponse(saleVariant.getId(), 100_000, 20_000, 80_000,
                        20, promotionId, "Sale", Instant.now().plusSeconds(600))));

        var prices = service.resolve(List.of(saleVariant, regularVariant));

        assertThat(prices.get(saleVariant.getId()).salePriceVnd()).isEqualTo(80_000);
        assertThat(prices.get(saleVariant.getId()).directSalePromotionId()).isEqualTo(promotionId);
        assertThat(prices.get(regularVariant.getId()).salePriceVnd()).isNull();
    }

    @Test
    void ignoresResponseWhoseListPriceDoesNotMatchCatalog() {
        ProductVariant variant = variant(100_000);
        when(client.resolve(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                new PromotionPriceClient.PriceResponse(variant.getId(), 120_000, 20_000, 100_000,
                        17, UUID.randomUUID(), "Stale", Instant.now().plusSeconds(600))));

        assertThat(service.resolve(List.of(variant)).get(variant.getId()).salePriceVnd()).isNull();
    }

    @Test
    void calculatesDeterministicStandaloneDemoDiscountWithoutCallingCart() {
        PromotionPriceService demoService = new PromotionPriceService(client,
                properties(CatalogPromotionProperties.Mode.DEMO));
        ProductVariant variant = variant(1_000_000);

        var price = demoService.resolve(List.of(variant)).get(variant.getId());

        assertThat(price.listPriceVnd()).isEqualTo(1_000_000);
        assertThat(price.salePriceVnd()).isEqualTo(850_000);
        assertThat(price.directSaleDiscountVnd()).isEqualTo(150_000);
        assertThat(price.directSalePercent()).isEqualTo(15);
    }

    @Test
    void disabledPromotionKeepsListPrice() {
        PromotionPriceService disabledService = new PromotionPriceService(client,
                properties(CatalogPromotionProperties.Mode.DISABLED));
        ProductVariant variant = variant(500_000);

        assertThat(disabledService.resolve(List.of(variant)).get(variant.getId()).salePriceVnd()).isNull();
    }

    private ProductVariant variant(long price) {
        return new ProductVariant(UUID.randomUUID(), UUID.randomUUID(), "SKU-" + UUID.randomUUID(), null,
                price, 500, null, null, null, 0);
    }

    private CatalogPromotionProperties properties(CatalogPromotionProperties.Mode mode) {
        return new CatalogPromotionProperties(mode, null, 1000, 1500, 15, 120);
    }
}
