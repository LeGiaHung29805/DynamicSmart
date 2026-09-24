package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.request.InventoryReservationItemRequest;
import com.dynamicmart.catalog_service.dto.response.PurchasableVariantResponse;
import com.dynamicmart.catalog_service.dto.response.VariantPriceResponse;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.Category;
import com.dynamicmart.catalog_service.entity.InventoryItem;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductImage;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.entity.VariantStatus;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.ProductImageRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchasableVariantService {
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryItemRepository inventoryRepository;
    private final ProductImageRepository imageRepository;
    private final PromotionPriceService promotionPriceService;

    public PurchasableVariantService(ProductVariantRepository variantRepository,
                                     ProductRepository productRepository,
                                     CategoryRepository categoryRepository,
                                     InventoryItemRepository inventoryRepository,
                                     ProductImageRepository imageRepository,
                                     PromotionPriceService promotionPriceService) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.imageRepository = imageRepository;
        this.promotionPriceService = promotionPriceService;
    }

    @Transactional(readOnly = true)
    public List<PurchasableVariantResponse> validate(List<InventoryReservationItemRequest> requests) {
        Set<UUID> unique = new HashSet<>();
        for (InventoryReservationItemRequest request : requests) {
            if (!unique.add(request.variantId())) {
                throw new CatalogException(HttpStatus.BAD_REQUEST, "VARIANT_DUPLICATED",
                        "Một Variant chỉ được kiểm tra một lần trong mỗi yêu cầu.");
            }
        }
        List<UUID> variantIds = requests.stream().map(InventoryReservationItemRequest::variantId).toList();
        Map<UUID, ProductVariant> variants = new HashMap<>();
        variantRepository.findAllByIdIn(variantIds)
                .forEach(variant -> variants.put(variant.getId(), variant));
        Map<UUID, Product> products = new HashMap<>();
        productRepository.findAllById(variants.values().stream()
                        .map(ProductVariant::getProductId).distinct().toList())
                .forEach(product -> products.put(product.getId(), product));
        Map<UUID, Category> categories = new HashMap<>();
        categoryRepository.findAllById(products.values().stream()
                        .map(Product::getCategoryId).distinct().toList())
                .forEach(category -> categories.put(category.getId(), category));
        Map<UUID, InventoryItem> inventories = new HashMap<>();
        inventoryRepository.findAllByVariantIdIn(variantIds)
                .forEach(inventory -> inventories.put(inventory.getVariantId(), inventory));
        Map<UUID, List<ProductImage>> imagesByProduct = new HashMap<>();
        List<UUID> productIds = products.keySet().stream().toList();
        if (!productIds.isEmpty()) {
            imageRepository.findAllByProductIdInOrderByProductIdAscSortOrderAsc(productIds)
                    .forEach(image -> imagesByProduct.computeIfAbsent(image.getProductId(),
                            ignored -> new java.util.ArrayList<>()).add(image));
        }
        Map<UUID, VariantPriceResponse> prices = promotionPriceService.resolve(
                variants.values().stream().toList());
        Instant now = Instant.now();
        return requests.stream().map(request -> response(request, variants, products, categories,
                        inventories, imagesByProduct, prices, now))
                .toList();
    }

    private PurchasableVariantResponse response(InventoryReservationItemRequest request,
                                                Map<UUID, ProductVariant> variants,
                                                Map<UUID, Product> products,
                                                Map<UUID, Category> categories,
                                                Map<UUID, InventoryItem> inventories,
                                                Map<UUID, List<ProductImage>> imagesByProduct,
                                                Map<UUID, VariantPriceResponse> prices,
                                                Instant now) {
        ProductVariant variant = variants.get(request.variantId());
        if (variant == null) return unavailable(request.variantId(), "VARIANT_NOT_FOUND");
        Product product = products.get(variant.getProductId());
        if (product == null) return unavailable(request.variantId(), "PRODUCT_NOT_FOUND");
        Category category = categories.get(product.getCategoryId());
        InventoryItem inventory = inventories.get(variant.getId());
        int available = inventory == null ? 0 : inventory.getAvailableQuantity();
        String reason = null;
        if (variant.getStatus() != VariantStatus.ACTIVE) reason = "VARIANT_INACTIVE";
        else if (!product.isPublicAt(now)) reason = "PRODUCT_INACTIVE";
        else if (category == null || category.getStatus() != CatalogStatus.ACTIVE) {
            reason = "CATEGORY_INACTIVE";
        } else if (inventory == null) reason = "INVENTORY_NOT_INITIALIZED";
        else if (available < request.quantity()) reason = "INSUFFICIENT_INVENTORY";

        Integer length = variant.getLengthCm() == null ? product.getDefaultLengthCm() : variant.getLengthCm();
        Integer width = variant.getWidthCm() == null ? product.getDefaultWidthCm() : variant.getWidthCm();
        Integer height = variant.getHeightCm() == null ? product.getDefaultHeightCm() : variant.getHeightCm();
        String imageUrl = primaryImage(imagesByProduct.getOrDefault(product.getId(), List.of()),
                variant.getId());
        return new PurchasableVariantResponse(product.getId(), variant.getId(), product.getName(),
                variant.getName(), variant.getSku(), prices.getOrDefault(variant.getId(),
                VariantPriceResponse.listPrice(variant.getPriceVnd())), available,
                variant.getWeightGrams(), length, width, height, imageUrl, reason == null, reason);
    }

    private PurchasableVariantResponse unavailable(UUID variantId, String reason) {
        return new PurchasableVariantResponse(null, variantId, null, null, null, null,
                0, 0, null, null, null, null, false, reason);
    }

    private String primaryImage(List<ProductImage> images, UUID variantId) {
        return images.stream().filter(image -> variantId.equals(image.getVariantId()) && image.isPrimary())
                .findFirst()
                .or(() -> images.stream().filter(image -> image.getVariantId() == null && image.isPrimary())
                        .findFirst())
                .or(() -> images.stream().filter(image -> image.getVariantId() == null).findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }
}
