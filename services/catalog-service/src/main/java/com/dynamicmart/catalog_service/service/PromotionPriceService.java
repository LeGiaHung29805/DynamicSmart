package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.client.PromotionPriceClient;
import com.dynamicmart.catalog_service.config.CatalogPromotionProperties;
import com.dynamicmart.catalog_service.dto.response.VariantPriceResponse;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PromotionPriceService {
    private final PromotionPriceClient client;
    private final CatalogPromotionProperties properties;

    public PromotionPriceService(PromotionPriceClient client, CatalogPromotionProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public Map<UUID, VariantPriceResponse> resolve(List<ProductVariant> variants) {
        Map<UUID, VariantPriceResponse> prices = new LinkedHashMap<>();
        variants.forEach(variant -> prices.put(variant.getId(),
                VariantPriceResponse.listPrice(variant.getPriceVnd())));
        if (properties.mode() == CatalogPromotionProperties.Mode.DISABLED) return prices;
        if (properties.mode() == CatalogPromotionProperties.Mode.DEMO) {
            variants.forEach(variant -> prices.put(variant.getId(), demoPrice(variant)));
            return prices;
        }
        client.resolve(variants.stream()
                        .map(variant -> new PromotionPriceClient.PriceRequest(variant.getId(), variant.getPriceVnd()))
                .toList())
                .forEach(price -> {
                    VariantPriceResponse current = prices.get(price.variantId());
                    if (current == null || price.listPriceVnd() != current.listPriceVnd() || price.listPriceVnd() < 0 || price.salePriceVnd() < 0
                            || price.salePriceVnd() > price.listPriceVnd()) return;
                    prices.put(price.variantId(), new VariantPriceResponse(price.listPriceVnd(),
                            price.discountVnd() > 0 ? price.salePriceVnd() : null,
                            price.discountVnd(), price.discountPercent(), price.endsAt(), price.promotionId()));
                });
        return prices;
    }

    private VariantPriceResponse demoPrice(ProductVariant variant) {
        int percent = properties.demoDiscountPercent();
        long discount = variant.getPriceVnd() / 100 * percent
                + variant.getPriceVnd() % 100 * percent / 100;
        UUID promotionId = UUID.nameUUIDFromBytes(
                ("catalog-standalone-demo:" + variant.getId()).getBytes(StandardCharsets.UTF_8));
        return new VariantPriceResponse(variant.getPriceVnd(), variant.getPriceVnd() - discount,
                discount, percent, Instant.now().plusSeconds(properties.demoDurationMinutes() * 60L),
                promotionId);
    }

}
