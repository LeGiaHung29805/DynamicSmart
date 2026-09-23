package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.response.AdminProductResponse;
import com.dynamicmart.catalog_service.dto.response.AttributeValueResponse;
import com.dynamicmart.catalog_service.dto.response.CategoryBriefResponse;
import com.dynamicmart.catalog_service.dto.response.InventoryAvailabilityResponse;
import com.dynamicmart.catalog_service.dto.response.ProductDetailResponse;
import com.dynamicmart.catalog_service.dto.response.ProductImageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductSummaryResponse;
import com.dynamicmart.catalog_service.dto.response.ProductVariantResponse;
import com.dynamicmart.catalog_service.dto.response.VariantPriceResponse;
import com.dynamicmart.catalog_service.entity.AttributeDefinition;
import com.dynamicmart.catalog_service.entity.Category;
import com.dynamicmart.catalog_service.entity.InventoryItem;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductAttributeValue;
import com.dynamicmart.catalog_service.entity.ProductImage;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.entity.VariantAttributeValue;
import com.dynamicmart.catalog_service.entity.VariantStatus;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.AttributeDefinitionRepository;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.ProductAttributeValueRepository;
import com.dynamicmart.catalog_service.repository.ProductImageRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import com.dynamicmart.catalog_service.repository.VariantAttributeValueRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogResponseAssembler {
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductAttributeValueRepository productValueRepository;
    private final VariantAttributeValueRepository variantValueRepository;
    private final AttributeDefinitionRepository attributeRepository;
    private final InventoryItemRepository inventoryRepository;
    private final PromotionPriceService promotionPriceService;

    public CatalogResponseAssembler(CategoryRepository categoryRepository,
                                    ProductVariantRepository variantRepository,
                                    ProductImageRepository imageRepository,
                                    ProductAttributeValueRepository productValueRepository,
                                    VariantAttributeValueRepository variantValueRepository,
                                    AttributeDefinitionRepository attributeRepository,
                                    InventoryItemRepository inventoryRepository,
                                    PromotionPriceService promotionPriceService) {
        this.categoryRepository = categoryRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.productValueRepository = productValueRepository;
        this.variantValueRepository = variantValueRepository;
        this.attributeRepository = attributeRepository;
        this.inventoryRepository = inventoryRepository;
        this.promotionPriceService = promotionPriceService;
    }

    @Transactional(readOnly = true)
    public AdminProductResponse adminProduct(Product product) {
        return adminProducts(List.of(product)).get(0);
    }

    @Transactional(readOnly = true)
    public List<AdminProductResponse> adminProducts(List<Product> products) {
        if (products.isEmpty()) return List.of();
        LoadedCatalog loaded = load(products);
        return products.stream().map(product -> toAdmin(product, loaded)).toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse productDetail(Product product) {
        LoadedCatalog loaded = load(List.of(product));
        List<ProductVariantResponse> variants = loaded.variantsByProduct()
                .getOrDefault(product.getId(), List.of()).stream()
                .filter(variant -> variant.getStatus() == VariantStatus.ACTIVE)
                .map(variant -> toVariant(variant, loaded))
                .toList();
        if (variants.isEmpty()) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_AVAILABLE",
                    "Sản phẩm chưa có phiên bản đang bán.");
        }
        return new ProductDetailResponse(product.getId(), product.getName(), product.getSlug(),
                product.getShortDescription(), product.getDescription(),
                categoryBrief(requireCategory(product, loaded)), product.isFeatured(),
                product.getPublishedAt(), productAttributes(product.getId(), loaded),
                productImages(product.getId(), loaded), variants);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> productSummaries(List<Product> products,
                                                         Set<UUID> bestSellerProductIds,
                                                         Set<UUID> voucherEligibleProductIds) {
        if (products.isEmpty()) return List.of();
        LoadedCatalog loaded = load(products);
        List<ProductSummaryResponse> summaries = new ArrayList<>();
        for (Product product : products) {
            List<ProductVariant> purchasable = loaded.variantsByProduct()
                    .getOrDefault(product.getId(), List.of()).stream()
                    .filter(variant -> variant.getStatus() == VariantStatus.ACTIVE)
                    .filter(variant -> available(variant.getId(), loaded) > 0)
                    .sorted(Comparator.comparingInt(ProductVariant::getSortOrder)
                            .thenComparing(ProductVariant::getId))
                    .toList();
            if (purchasable.isEmpty()) continue;
            ProductVariant representative = purchasable.get(0);
            long minimum = purchasable.stream().mapToLong(ProductVariant::getPriceVnd).min().orElseThrow();
            long maximum = purchasable.stream().mapToLong(ProductVariant::getPriceVnd).max().orElseThrow();
            ProductImage primaryImage = primaryImage(product.getId(), representative.getId(), loaded);
            summaries.add(new ProductSummaryResponse(product.getId(), product.getName(),
                    product.getSlug(), product.getShortDescription(),
                    categoryBrief(requireCategory(product, loaded)),
                    primaryImage == null ? null : imageResponse(primaryImage),
                    representative.getId(), loaded.prices().get(representative.getId()),
                    minimum, maximum, true, product.isFeatured(),
                    bestSellerProductIds.contains(product.getId()),
                    voucherEligibleProductIds.contains(product.getId()), product.getPublishedAt()));
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse productVariant(Product product, ProductVariant variant) {
        LoadedCatalog loaded = load(List.of(product));
        return toVariant(variant, loaded);
    }

    private AdminProductResponse toAdmin(Product product, LoadedCatalog loaded) {
        List<ProductVariantResponse> variants = loaded.variantsByProduct()
                .getOrDefault(product.getId(), List.of()).stream()
                .map(variant -> toVariant(variant, loaded))
                .toList();
        return new AdminProductResponse(product.getId(),
                categoryBrief(requireCategory(product, loaded)), product.getName(), product.getSlug(),
                product.getShortDescription(), product.getDescription(), product.getStatus(),
                product.getPublishedAt(), product.isFeatured(), product.getDefaultWeightGrams(),
                product.getDefaultLengthCm(), product.getDefaultWidthCm(), product.getDefaultHeightCm(),
                productAttributes(product.getId(), loaded), productImages(product.getId(), loaded), variants);
    }

    private ProductVariantResponse toVariant(ProductVariant variant, LoadedCatalog loaded) {
        InventoryItem inventory = loaded.inventoryByVariant().get(variant.getId());
        InventoryAvailabilityResponse availability = inventory == null
                ? InventoryAvailabilityResponse.empty()
                : new InventoryAvailabilityResponse(inventory.getOnHandQuantity(),
                        inventory.getReservedQuantity(), inventory.getAvailableQuantity());
        boolean purchasable = variant.getStatus() == VariantStatus.ACTIVE
                && availability.availableQuantity() > 0;
        return new ProductVariantResponse(variant.getId(), variant.getProductId(), variant.getSku(),
                variant.getName(), loaded.prices().getOrDefault(variant.getId(),
                        VariantPriceResponse.listPrice(variant.getPriceVnd())), variant.getWeightGrams(),
                variant.getLengthCm(), variant.getWidthCm(), variant.getHeightCm(), variant.getStatus(),
                variant.getSortOrder(), availability, purchasable,
                variantAttributes(variant.getId(), loaded), variantImages(variant.getId(), loaded));
    }

    private LoadedCatalog load(List<Product> products) {
        List<UUID> productIds = products.stream().map(Product::getId).toList();
        Map<UUID, Category> categories = byId(categoryRepository.findAllById(
                products.stream().map(Product::getCategoryId).distinct().toList()), Category::getId);

        List<ProductVariant> variants = variantRepository
                .findAllByProductIdInOrderByProductIdAscSortOrderAsc(productIds);
        List<UUID> variantIds = variants.stream().map(ProductVariant::getId).toList();
        List<ProductImage> images = imageRepository
                .findAllByProductIdInOrderByProductIdAscSortOrderAsc(productIds);
        List<ProductAttributeValue> productValues = productValueRepository.findAllByProductIdIn(productIds);
        List<VariantAttributeValue> variantValues = variantIds.isEmpty()
                ? List.of() : variantValueRepository.findAllByVariantIdIn(variantIds);
        Set<UUID> attributeIds = new java.util.HashSet<>();
        productValues.forEach(value -> attributeIds.add(value.getAttributeId()));
        variantValues.forEach(value -> attributeIds.add(value.getAttributeId()));
        Map<UUID, AttributeDefinition> attributes = byId(attributeRepository.findAllById(attributeIds),
                AttributeDefinition::getId);
        Map<UUID, InventoryItem> inventory = variantIds.isEmpty()
                ? Map.of() : byId(inventoryRepository.findAllByVariantIdIn(variantIds),
                InventoryItem::getVariantId);

        return new LoadedCatalog(categories, groupBy(variants, ProductVariant::getProductId),
                groupBy(images, ProductImage::getProductId),
                groupBy(productValues, ProductAttributeValue::getProductId),
                groupBy(variantValues, VariantAttributeValue::getVariantId), attributes, inventory,
                promotionPriceService.resolve(variants));
    }

    private List<AttributeValueResponse> productAttributes(UUID productId, LoadedCatalog loaded) {
        return loaded.productValuesByProduct().getOrDefault(productId, List.of()).stream()
                .map(value -> attributeResponse(value.getId(), value.getAttributeId(), value.getValue(), loaded))
                .toList();
    }

    private List<AttributeValueResponse> variantAttributes(UUID variantId, LoadedCatalog loaded) {
        return loaded.variantValuesByVariant().getOrDefault(variantId, List.of()).stream()
                .map(value -> attributeResponse(value.getId(), value.getAttributeId(), value.getValue(), loaded))
                .toList();
    }

    private AttributeValueResponse attributeResponse(UUID valueId, UUID attributeId,
                                                     tools.jackson.databind.JsonNode value,
                                                     LoadedCatalog loaded) {
        AttributeDefinition attribute = loaded.attributes().get(attributeId);
        if (attribute == null) {
            throw new CatalogException(HttpStatus.CONFLICT, "ATTRIBUTE_REFERENCE_INVALID",
                    "Giá trị đang tham chiếu thuộc tính không tồn tại.");
        }
        return new AttributeValueResponse(valueId, attributeId, attribute.getCode(),
                attribute.getName(), attribute.getDataType(), value);
    }

    private List<ProductImageResponse> productImages(UUID productId, LoadedCatalog loaded) {
        return loaded.imagesByProduct().getOrDefault(productId, List.of()).stream()
                .filter(image -> image.getVariantId() == null)
                .map(this::imageResponse)
                .toList();
    }

    private List<ProductImageResponse> variantImages(UUID variantId, LoadedCatalog loaded) {
        return loaded.imagesByProduct().values().stream().flatMap(Collection::stream)
                .filter(image -> variantId.equals(image.getVariantId()))
                .map(this::imageResponse)
                .toList();
    }

    private ProductImage primaryImage(UUID productId, UUID representativeVariantId,
                                      LoadedCatalog loaded) {
        List<ProductImage> images = loaded.imagesByProduct().getOrDefault(productId, List.of());
        return images.stream().filter(image -> image.getVariantId() == null && image.isPrimary())
                .findFirst()
                .or(() -> images.stream().filter(image -> representativeVariantId.equals(image.getVariantId())
                        && image.isPrimary()).findFirst())
                .or(() -> images.stream().filter(image -> image.getVariantId() == null).findFirst())
                .orElse(null);
    }

    private ProductImageResponse imageResponse(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getProductId(), image.getVariantId(),
                image.getImageUrl(), image.getAltText(), image.getSortOrder(), image.isPrimary());
    }

    private int available(UUID variantId, LoadedCatalog loaded) {
        InventoryItem inventory = loaded.inventoryByVariant().get(variantId);
        return inventory == null ? 0 : inventory.getAvailableQuantity();
    }

    private Category requireCategory(Product product, LoadedCatalog loaded) {
        Category category = loaded.categories().get(product.getCategoryId());
        if (category == null) {
            throw new CatalogException(HttpStatus.CONFLICT, "PRODUCT_CATEGORY_INVALID",
                    "Sản phẩm đang tham chiếu danh mục không tồn tại.");
        }
        return category;
    }

    private CategoryBriefResponse categoryBrief(Category category) {
        return new CategoryBriefResponse(category.getId(), category.getCode(),
                category.getName(), category.getSlug());
    }

    private <T> Map<UUID, T> byId(Iterable<T> values, java.util.function.Function<T, UUID> id) {
        Map<UUID, T> result = new HashMap<>();
        values.forEach(value -> result.put(id.apply(value), value));
        return result;
    }

    private <T> Map<UUID, List<T>> groupBy(List<T> values,
                                            java.util.function.Function<T, UUID> group) {
        Map<UUID, List<T>> result = new LinkedHashMap<>();
        values.forEach(value -> result.computeIfAbsent(group.apply(value),
                ignored -> new ArrayList<>()).add(value));
        return result;
    }

    private record LoadedCatalog(
            Map<UUID, Category> categories,
            Map<UUID, List<ProductVariant>> variantsByProduct,
            Map<UUID, List<ProductImage>> imagesByProduct,
            Map<UUID, List<ProductAttributeValue>> productValuesByProduct,
            Map<UUID, List<VariantAttributeValue>> variantValuesByVariant,
            Map<UUID, AttributeDefinition> attributes,
            Map<UUID, InventoryItem> inventoryByVariant,
            Map<UUID, VariantPriceResponse> prices
    ) {
    }
}
