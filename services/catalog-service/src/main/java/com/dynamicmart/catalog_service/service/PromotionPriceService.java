package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.client.PromotionPriceClient;
import com.dynamicmart.catalog_service.dto.response.VariantPriceResponse;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PromotionPriceService {
    private final PromotionPriceClient client;

    public PromotionPriceService(PromotionPriceClient client) {
        this.client = client;
    }

    public Map<UUID, VariantPriceResponse> resolve(List<ProductVariant> variants) {
        Map<UUID, VariantPriceResponse> prices = new LinkedHashMap<>();
        variants.forEach(variant -> prices.put(variant.getId(),
                VariantPriceResponse.listPrice(variant.getPriceVnd())));
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
}
