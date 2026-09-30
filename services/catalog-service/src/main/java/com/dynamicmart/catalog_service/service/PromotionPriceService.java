package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.response.VariantPriceResponse;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PromotionPriceService {
    public Map<UUID, VariantPriceResponse> resolve(List<ProductVariant> variants) {
        Map<UUID, VariantPriceResponse> prices = new LinkedHashMap<>();
        variants.forEach(variant -> prices.put(variant.getId(),
                VariantPriceResponse.listPrice(variant.getPriceVnd())));
        return prices;
    }
}
