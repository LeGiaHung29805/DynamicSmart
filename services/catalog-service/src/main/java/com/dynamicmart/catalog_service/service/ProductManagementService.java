package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.request.AttributeValueRequest;
import com.dynamicmart.catalog_service.dto.request.CreateProductRequest;
import com.dynamicmart.catalog_service.dto.request.CreateVariantRequest;
import com.dynamicmart.catalog_service.dto.request.ProductStatusRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateProductRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateVariantRequest;
import com.dynamicmart.catalog_service.dto.request.UpsertProductImageRequest;
import com.dynamicmart.catalog_service.dto.response.AdminProductResponse;
import com.dynamicmart.catalog_service.dto.response.PageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductImageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductPublishCheckResponse;
import com.dynamicmart.catalog_service.dto.response.ProductPublishReadinessResponse;
import com.dynamicmart.catalog_service.dto.response.ProductVariantResponse;
import com.dynamicmart.catalog_service.entity.AttributeAppliesTo;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.Category;
import com.dynamicmart.catalog_service.entity.InventoryItem;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductImage;
import com.dynamicmart.catalog_service.entity.ProductStatus;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.entity.VariantAttributeValue;
import com.dynamicmart.catalog_service.entity.VariantStatus;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.ProductAttributeValueRepository;
import com.dynamicmart.catalog_service.repository.ProductImageRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import com.dynamicmart.catalog_service.repository.VariantAttributeValueRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductManagementService {
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_SLUG_LENGTH = 330;
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");
    private static final Pattern TRAILING_HYPHENS = Pattern.compile("-+$");
    private static final java.util.Set<String> ALLOWED_IMAGE_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/webp", "image/avif");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryItemRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final ProductAttributeValueRepository productValueRepository;
    private final VariantAttributeValueRepository variantValueRepository;
    private final AttributeValueService attributeValueService;
    private final CatalogResponseAssembler responseAssembler;

    public ProductManagementService(ProductRepository productRepository,
                                    ProductVariantRepository variantRepository,
                                    ProductImageRepository imageRepository,
                                    InventoryItemRepository inventoryRepository,
                                    CategoryRepository categoryRepository,
                                    ProductAttributeValueRepository productValueRepository,
                                    VariantAttributeValueRepository variantValueRepository,
                                    AttributeValueService attributeValueService,
                                    CatalogResponseAssembler responseAssembler) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.productValueRepository = productValueRepository;
        this.variantValueRepository = variantValueRepository;
        this.attributeValueService = attributeValueService;
        this.responseAssembler = responseAssembler;
    }

    @Transactional
    public AdminProductResponse createProduct(CreateProductRequest request) {
        requireAssignableCategory(request.categoryId());
        String slug = resolveCreateSlug(request.name(), request.slug());
        Product product = new Product(UUID.randomUUID(), request.categoryId(), request.name().trim(),
                slug, trimToNull(request.shortDescription()), trimToNull(request.description()),
                request.defaultWeightGrams(), request.defaultLengthCm(), request.defaultWidthCm(),
                request.defaultHeightCm());
        product.markFeatured(request.featured());
        productRepository.save(product);
        attributeValueService.replaceDraftProductValues(product.getId(), product.getCategoryId(),
                request.attributes());
        return responseAssembler.adminProduct(product);
    }

    @Transactional
    public AdminProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = requireProduct(productId);
        requireAssignableCategory(request.categoryId());
        String slug = normalizeSlug(request.slug());
        requireUniqueSlug(slug, productId);
        if (!product.getCategoryId().equals(request.categoryId())
                && variantRepository.existsByProductId(productId)) {
            throw new CatalogException(HttpStatus.CONFLICT, "PRODUCT_CATEGORY_IMMUTABLE",
                    "Không thể đổi danh mục sau khi sản phẩm đã có biến thể.");
        }
        product.update(request.categoryId(), request.name().trim(), slug,
                trimToNull(request.shortDescription()), trimToNull(request.description()),
                request.defaultWeightGrams(), request.defaultLengthCm(), request.defaultWidthCm(),
                request.defaultHeightCm());
        product.markFeatured(request.featured());
        if (product.getStatus() == ProductStatus.ACTIVE) {
            attributeValueService.replaceProductValues(productId, request.categoryId(), request.attributes());
        } else {
            attributeValueService.replaceDraftProductValues(productId, request.categoryId(), request.attributes());
        }
        if (product.getStatus() == ProductStatus.ACTIVE) validateActiveConfiguration(product);
        return responseAssembler.adminProduct(product);
    }

    @Transactional
    public AdminProductResponse setProductStatus(UUID productId, ProductStatusRequest request) {
        Product product = requireProduct(productId);
        switch (request.status()) {
            case DRAFT -> product.moveToDraft();
            case ACTIVE -> {
                requirePublishable(product);
                product.publish(request.publishedAt());
            }
            case INACTIVE -> product.deactivate();
            case ARCHIVED -> product.archive();
        }
        return responseAssembler.adminProduct(product);
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getProduct(UUID productId) {
        return responseAssembler.adminProduct(requireProduct(productId));
    }

    @Transactional(readOnly = true)
    public ProductPublishReadinessResponse getPublishReadiness(UUID productId) {
        return publishReadiness(requireProduct(productId));
    }

    @Transactional
    public void updateRating(UUID productId, double averageRating, int reviewCount) {
        Product product = requireProduct(productId);
        product.updateRating(averageRating, reviewCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> listProducts(String keyword, ProductStatus status,
                                                           int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Specification<Product> specification = (root, query, builder) -> builder.conjunction();
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("slug")), pattern)));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        Page<Product> products = productRepository.findAll(specification,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt")));
        List<AdminProductResponse> content = responseAssembler.adminProducts(products.getContent());
        return new PageResponse<>(content, products.getNumber(), products.getSize(),
                products.getTotalElements(), products.getTotalPages(), products.isFirst(), products.isLast());
    }

    @Transactional
    public ProductVariantResponse createVariant(UUID productId, CreateVariantRequest request) {
        Product product = requireProductForUpdate(productId);
        return createVariant(product, request);
    }

    @Transactional
    public List<ProductVariantResponse> createVariants(UUID productId,
                                                       List<CreateVariantRequest> requests) {
        Product product = requireProductForUpdate(productId);
        validateBatchUniqueness(requests);
        List<ProductVariantResponse> created = new ArrayList<>();
        for (CreateVariantRequest request : requests) {
            created.add(createVariant(product, request));
        }
        return List.copyOf(created);
    }

    private ProductVariantResponse createVariant(Product product, CreateVariantRequest request) {
        String sku = normalizeSku(request.sku());
        requireUniqueSku(sku, null);
        attributeValueService.validate(product.getCategoryId(), AttributeAppliesTo.VARIANT,
                request.attributes());
        requireUniqueVariantCombination(product.getId(), request.attributes(), null);
        ProductVariant variant = new ProductVariant(UUID.randomUUID(), product.getId(), sku,
                trimToNull(request.name()), request.priceVnd(), request.weightGrams(), request.lengthCm(),
                request.widthCm(), request.heightCm(), request.sortOrder());
        variantRepository.save(variant);
        inventoryRepository.save(new InventoryItem(variant.getId(), request.initialOnHandQuantity()));
        attributeValueService.replaceVariantValues(variant.getId(), product.getCategoryId(),
                request.attributes());
        return responseAssembler.productVariant(product, variant);
    }

    @Transactional
    public ProductVariantResponse updateVariant(UUID productId, UUID variantId,
                                                UpdateVariantRequest request) {
        Product product = requireProductForUpdate(productId);
        ProductVariant variant = requireVariant(productId, variantId);
        String sku = normalizeSku(request.sku());
        requireUniqueSku(sku, variantId);
        attributeValueService.validate(product.getCategoryId(), AttributeAppliesTo.VARIANT,
                request.attributes());
        requireUniqueVariantCombination(productId, request.attributes(), variantId);
        variant.update(sku, trimToNull(request.name()), request.priceVnd(), request.weightGrams(),
                request.lengthCm(), request.widthCm(), request.heightCm(), request.sortOrder());
        attributeValueService.replaceVariantValues(variantId, product.getCategoryId(), request.attributes());
        if (variant.getStatus() == VariantStatus.ACTIVE) requireShippingDimensions(product, variant);
        return responseAssembler.productVariant(product, variant);
    }

    @Transactional
    public ProductVariantResponse setVariantStatus(UUID productId, UUID variantId,
                                                   VariantStatus status) {
        Product product = requireProductForUpdate(productId);
        ProductVariant variant = requireVariant(productId, variantId);
        switch (status) {
            case ACTIVE -> {
                requireShippingDimensions(product, variant);
                validateCurrentVariantAttributes(product, variant);
                requireUniqueVariantCombination(productId,
                        variantValueRepository.findAllByVariantId(variantId).stream()
                                .map(value -> new AttributeValueRequest(
                                        value.getAttributeId(), value.getValue()))
                                .toList(),
                        variantId);
                variant.activate();
            }
            case INACTIVE -> variant.deactivate();
            case ARCHIVED -> variant.archive();
        }
        return responseAssembler.productVariant(product, variant);
    }

    @Transactional
    public ProductImageResponse createImage(UUID productId, UpsertProductImageRequest request) {
        requireProduct(productId);
        validateImage(request);
        validateImageVariant(productId, request.variantId());
        boolean primary = request.primary() || scopeImages(productId, request.variantId()).isEmpty();
        ProductImage image = new ProductImage(UUID.randomUUID(), productId, request.variantId(),
                request.imageUrl().trim(), trimToNull(request.altText()), request.sortOrder(),
                primary);
        if (primary) demoteCurrentPrimary(productId, request.variantId(), null);
        imageRepository.save(image);
        return imageResponse(image);
    }

    @Transactional
    public ProductImageResponse updateImage(UUID productId, UUID imageId,
                                            UpsertProductImageRequest request) {
        requireProduct(productId);
        validateImage(request);
        ProductImage image = requireImage(productId, imageId);
        if (!java.util.Objects.equals(image.getVariantId(), request.variantId())) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "IMAGE_OWNER_IMMUTABLE",
                    "Không thể chuyển ảnh sang Product hoặc Variant khác; hãy tạo ảnh mới.");
        }
        boolean primary = request.primary();
        if (!primary && image.isPrimary()) {
            List<ProductImage> alternatives = scopeImages(productId, request.variantId()).stream()
                    .filter(candidate -> !candidate.getId().equals(imageId))
                    .toList();
            if (alternatives.isEmpty()) {
                primary = true;
            } else {
                image.markPrimary(false);
                imageRepository.flush();
                alternatives.get(0).markPrimary(true);
            }
        }
        if (primary) demoteCurrentPrimary(productId, request.variantId(), imageId);
        image.update(request.imageUrl().trim(), trimToNull(request.altText()), request.sortOrder(),
                primary);
        return imageResponse(image);
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        Product product = requireProduct(productId);
        ProductImage image = requireImage(productId, imageId);
        ProductImage replacement = image.isPrimary()
                ? scopeImages(productId, image.getVariantId()).stream()
                        .filter(candidate -> !candidate.getId().equals(imageId))
                        .findFirst().orElse(null)
                : null;
        if (product.getStatus() == ProductStatus.ACTIVE && image.getVariantId() == null
                && image.isPrimary() && replacement == null) {
            throw new CatalogException(HttpStatus.CONFLICT, "ACTIVE_PRODUCT_PRIMARY_IMAGE_REQUIRED",
                    "Không thể xóa ảnh chính duy nhất khi sản phẩm đang công khai.");
        }
        if (image.isPrimary()) image.markPrimary(false);
        imageRepository.flush();
        imageRepository.delete(image);
        imageRepository.flush();
        if (replacement != null) replacement.markPrimary(true);
    }

    private void requirePublishable(Product product) {
        ProductPublishCheckResponse blocker = publishReadiness(product).checks().stream()
                .filter(check -> !check.complete())
                .findFirst()
                .orElse(null);
        if (blocker != null) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, blocker.code(), blocker.message());
        }
    }

    private void validateActiveConfiguration(Product product) {
        Category category = requireCategory(product.getCategoryId());
        if (category.getStatus() != CatalogStatus.ACTIVE) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRODUCT_CATEGORY_INACTIVE",
                    "Không thể duy trì sản phẩm công khai trong danh mục ngừng hoạt động.");
        }
        requireLeafCategory(category);
        attributeValueService.validate(product.getCategoryId(),
                com.dynamicmart.catalog_service.entity.AttributeAppliesTo.PRODUCT,
                productValueRepository.findAllByProductId(product.getId()).stream()
                        .map(value -> new AttributeValueRequest(value.getAttributeId(), value.getValue()))
                        .toList());
        List<ProductVariant> activeVariants = variantRepository
                .findAllByProductIdAndStatusOrderBySortOrderAsc(product.getId(), VariantStatus.ACTIVE);
        if (activeVariants.isEmpty()) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRODUCT_HAS_NO_ACTIVE_VARIANT",
                    "Sản phẩm công khai phải có ít nhất một biến thể đang hoạt động.");
        }
        activeVariants.forEach(variant -> {
            requireShippingDimensions(product, variant);
            validateCurrentVariantAttributes(product, variant);
        });
        validateUniqueVariantCombinations(activeVariants);
    }

    private ProductPublishReadinessResponse publishReadiness(Product product) {
        List<ProductPublishCheckResponse> checks = new ArrayList<>();
        Category category = requireCategory(product.getCategoryId());
        boolean categoryActive = category.getStatus() == CatalogStatus.ACTIVE;
        checks.add(check("PRODUCT_CATEGORY_INACTIVE", "Danh mục đang hoạt động", categoryActive,
                "Không thể công khai sản phẩm trong danh mục ngừng hoạt động."));

        boolean categoryAssignable = !categoryRepository.existsByParentId(category.getId());
        checks.add(check("PRODUCT_CATEGORY_MUST_BE_LEAF", "Đã chọn danh mục cuối cùng",
                categoryAssignable,
                "Sản phẩm phải thuộc danh mục cuối cùng, không thể gắn trực tiếp vào danh mục cha."));

        try {
            attributeValueService.validate(product.getCategoryId(),
                    com.dynamicmart.catalog_service.entity.AttributeAppliesTo.PRODUCT,
                    productValueRepository.findAllByProductId(product.getId()).stream()
                            .map(value -> new AttributeValueRequest(value.getAttributeId(), value.getValue()))
                            .toList());
            checks.add(check("PRODUCT_ATTRIBUTES_INVALID", "Thuộc tính sản phẩm hợp lệ", true,
                    "Thuộc tính sản phẩm đã hợp lệ."));
        } catch (CatalogException exception) {
            checks.add(check("PRODUCT_ATTRIBUTES_INVALID", "Thuộc tính sản phẩm hợp lệ", false,
                    exception.getMessage()));
        }

        List<ProductVariant> activeVariants = variantRepository
                .findAllByProductIdAndStatusOrderBySortOrderAsc(product.getId(), VariantStatus.ACTIVE);
        boolean hasActiveVariant = !activeVariants.isEmpty();
        checks.add(check("PRODUCT_HAS_NO_ACTIVE_VARIANT", "Có biến thể đang hoạt động",
                hasActiveVariant,
                "Sản phẩm phải có ít nhất một biến thể đang hoạt động trước khi công khai."));

        boolean variantsValid = hasActiveVariant;
        String variantMessage = "Các biến thể đang hoạt động có đủ thuộc tính và thông tin vận chuyển.";
        for (ProductVariant variant : activeVariants) {
            try {
                requireShippingDimensions(product, variant);
                validateCurrentVariantAttributes(product, variant);
            } catch (CatalogException exception) {
                variantsValid = false;
                variantMessage = exception.getMessage();
                break;
            }
        }
        checks.add(check("VARIANT_CONFIGURATION_INVALID", "Biến thể đủ thông tin",
                variantsValid, variantMessage));

        boolean combinationsUnique = hasActiveVariant;
        String combinationMessage = "Mỗi tổ hợp thuộc tính chỉ thuộc một biến thể đang hoạt động.";
        if (hasActiveVariant) {
            try {
                validateUniqueVariantCombinations(activeVariants);
            } catch (CatalogException exception) {
                combinationsUnique = false;
                combinationMessage = exception.getMessage();
            }
        }
        checks.add(check("VARIANT_COMBINATION_DUPLICATED", "Không trùng tổ hợp biến thể",
                combinationsUnique, combinationMessage));

        List<UUID> activeVariantIds = activeVariants.stream().map(ProductVariant::getId).toList();
        boolean hasAvailableInventory = !activeVariantIds.isEmpty()
                && inventoryRepository.findAllByVariantIdIn(activeVariantIds).stream()
                        .anyMatch(inventory -> inventory.getAvailableQuantity() > 0);
        checks.add(check("PRODUCT_HAS_NO_AVAILABLE_INVENTORY", "Có tồn kho khả dụng",
                hasAvailableInventory,
                "Cần ít nhất một biến thể đang hoạt động còn tồn kho khả dụng."));

        boolean hasPrimaryImage = imageRepository
                .findByProductIdAndVariantIdIsNullAndPrimaryTrue(product.getId()).isPresent();
        checks.add(check("PRODUCT_PRIMARY_IMAGE_REQUIRED", "Có ảnh chính của sản phẩm",
                hasPrimaryImage,
                "Cần đặt một ảnh cấp sản phẩm làm ảnh chính trước khi công khai."));

        return new ProductPublishReadinessResponse(
                checks.stream().allMatch(ProductPublishCheckResponse::complete), checks);
    }

    private void validateCurrentVariantAttributes(Product product, ProductVariant variant) {
        attributeValueService.validate(product.getCategoryId(),
                com.dynamicmart.catalog_service.entity.AttributeAppliesTo.VARIANT,
                variantValueRepository.findAllByVariantId(variant.getId()).stream()
                        .map(value -> new AttributeValueRequest(value.getAttributeId(), value.getValue()))
                        .toList());
    }

    private void requireShippingDimensions(Product product, ProductVariant variant) {
        Integer length = variant.getLengthCm() == null ? product.getDefaultLengthCm() : variant.getLengthCm();
        Integer width = variant.getWidthCm() == null ? product.getDefaultWidthCm() : variant.getWidthCm();
        Integer height = variant.getHeightCm() == null ? product.getDefaultHeightCm() : variant.getHeightCm();
        if (variant.getWeightGrams() <= 0 || length == null || width == null || height == null
                || length <= 0 || width <= 0 || height <= 0) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "VARIANT_SHIPPING_DIMENSIONS_REQUIRED",
                    "Variant đang bán phải có cân nặng và đủ kích thước đóng gói hợp lệ.");
        }
    }

    private void validateImage(UpsertProductImageRequest request) {
        if (request.contentType() != null && !request.contentType().isBlank()) {
            String type = request.contentType().trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_IMAGE_TYPES.contains(type)) {
                throw new CatalogException(HttpStatus.BAD_REQUEST, "IMAGE_TYPE_NOT_ALLOWED",
                        "Ảnh chỉ hỗ trợ JPEG, PNG, WEBP hoặc AVIF.");
            }
        }
        if (request.sizeBytes() != null && request.sizeBytes() > MAX_IMAGE_SIZE_BYTES) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "IMAGE_TOO_LARGE",
                    "Dung lượng ảnh không được vượt quá 5 MB.");
        }
        String url = request.imageUrl().trim().toLowerCase(Locale.ROOT);
        if (!(url.startsWith("https://") || url.startsWith("http://") || url.startsWith("/"))) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "IMAGE_URL_INVALID",
                    "Đường dẫn ảnh không hợp lệ.");
        }
    }

    private void validateImageVariant(UUID productId, UUID variantId) {
        if (variantId != null) requireVariant(productId, variantId);
    }

    private void demoteCurrentPrimary(UUID productId, UUID variantId, UUID excludedImageId) {
        java.util.Optional<ProductImage> current = variantId == null
                ? imageRepository.findByProductIdAndVariantIdIsNullAndPrimaryTrue(productId)
                : imageRepository.findByVariantIdAndPrimaryTrue(variantId);
        current.filter(image -> !image.getId().equals(excludedImageId))
                .ifPresent(image -> image.markPrimary(false));
    }

    private List<ProductImage> scopeImages(UUID productId, UUID variantId) {
        if (variantId != null) {
            return imageRepository.findAllByVariantIdOrderBySortOrderAsc(variantId);
        }
        return imageRepository.findAllByProductIdOrderBySortOrderAsc(productId).stream()
                .filter(image -> image.getVariantId() == null)
                .toList();
    }

    private ProductPublishCheckResponse check(String code, String label, boolean complete,
                                               String message) {
        return new ProductPublishCheckResponse(code, label, complete, message);
    }

    private ProductImage requireImage(UUID productId, UUID imageId) {
        return imageRepository.findById(imageId)
                .filter(image -> image.getProductId().equals(productId))
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_IMAGE_NOT_FOUND",
                        "Không tìm thấy ảnh sản phẩm."));
    }

    private ProductImageResponse imageResponse(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getProductId(), image.getVariantId(),
                image.getImageUrl(), image.getAltText(), image.getSortOrder(), image.isPrimary());
    }

    private void requireUniqueSlug(String slug, UUID excludedId) {
        boolean duplicate = excludedId == null ? productRepository.existsBySlugIgnoreCase(slug)
                : productRepository.existsBySlugIgnoreCaseAndIdNot(slug, excludedId);
        if (duplicate) throw new CatalogException(HttpStatus.CONFLICT, "PRODUCT_SLUG_EXISTS",
                "Đường dẫn sản phẩm đã tồn tại.");
    }

    private String resolveCreateSlug(String productName, String requestedSlug) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String customSlug = normalizeSlug(requestedSlug);
            requireUniqueSlug(customSlug, null);
            return customSlug;
        }

        String baseSlug = normalizeSlug(productName);
        if (!productRepository.existsBySlugIgnoreCase(baseSlug)) return baseSlug;

        for (int suffixNumber = 2; suffixNumber < Integer.MAX_VALUE; suffixNumber++) {
            String suffix = "-" + suffixNumber;
            String candidate = truncateSlug(baseSlug, MAX_SLUG_LENGTH - suffix.length()) + suffix;
            if (!productRepository.existsBySlugIgnoreCase(candidate)) return candidate;
        }
        throw new CatalogException(HttpStatus.CONFLICT, "PRODUCT_SLUG_EXHAUSTED",
                "Không thể tạo đường dẫn duy nhất cho sản phẩm.");
    }

    private void requireUniqueSku(String sku, UUID excludedId) {
        boolean duplicate = excludedId == null ? variantRepository.existsBySkuIgnoreCase(sku)
                : variantRepository.existsBySkuIgnoreCaseAndIdNot(sku, excludedId);
        if (duplicate) throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_SKU_EXISTS",
                "Mã hàng đã tồn tại.");
    }

    private void validateBatchUniqueness(List<CreateVariantRequest> requests) {
        Set<String> skus = new HashSet<>();
        Set<String> combinations = new HashSet<>();
        for (CreateVariantRequest request : requests) {
            if (!skus.add(normalizeSku(request.sku()))) {
                throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_SKU_EXISTS",
                        "Danh sách tạo nhanh chứa mã hàng bị trùng.");
            }
            if (!combinations.add(variantCombinationKey(request.attributes()))) {
                throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_COMBINATION_EXISTS",
                        "Danh sách tạo nhanh chứa tổ hợp thuộc tính bị trùng.");
            }
        }
    }

    private void requireUniqueVariantCombination(UUID productId,
                                                  List<AttributeValueRequest> requestedValues,
                                                  UUID excludedVariantId) {
        String requestedKey = variantCombinationKey(requestedValues);
        List<ProductVariant> variants = variantRepository.findAllByProductIdOrderBySortOrderAsc(productId)
                .stream()
                .filter(variant -> !variant.getId().equals(excludedVariantId))
                .toList();
        Map<UUID, List<VariantAttributeValue>> valuesByVariant = variantValuesByVariant(variants);
        boolean duplicate = variants.stream().anyMatch(variant -> requestedKey.equals(
                variantCombinationKey(valuesByVariant.getOrDefault(variant.getId(), List.of()))));
        if (duplicate) {
            throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_COMBINATION_EXISTS",
                    "Tổ hợp thuộc tính này đã thuộc một biến thể khác của sản phẩm.");
        }
    }

    private void validateUniqueVariantCombinations(List<ProductVariant> variants) {
        Map<UUID, List<VariantAttributeValue>> valuesByVariant = variantValuesByVariant(variants);
        Set<String> combinations = new HashSet<>();
        for (ProductVariant variant : variants) {
            String key = variantCombinationKey(
                    valuesByVariant.getOrDefault(variant.getId(), List.of()));
            if (!combinations.add(key)) {
                throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_COMBINATION_EXISTS",
                        "Có nhiều biến thể đang dùng cùng một tổ hợp thuộc tính.");
            }
        }
    }

    private Map<UUID, List<VariantAttributeValue>> variantValuesByVariant(
            List<ProductVariant> variants) {
        if (variants.isEmpty()) return Map.of();
        Map<UUID, List<VariantAttributeValue>> result = new HashMap<>();
        variantValueRepository.findAllByVariantIdIn(
                        variants.stream().map(ProductVariant::getId).toList())
                .forEach(value -> result.computeIfAbsent(value.getVariantId(),
                        ignored -> new ArrayList<>()).add(value));
        return result;
    }

    private String variantCombinationKey(List<AttributeValueRequest> values) {
        if (values.isEmpty()) return "__DEFAULT__";
        return values.stream()
                .map(value -> combinationEntry(value.attributeId(), value.value()))
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private String variantCombinationKey(java.util.Collection<VariantAttributeValue> values) {
        if (values.isEmpty()) return "__DEFAULT__";
        return values.stream()
                .map(value -> combinationEntry(value.getAttributeId(), value.getValue()))
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private String combinationEntry(UUID attributeId, JsonNode value) {
        return attributeId + "=" + canonicalAttributeValue(value);
    }

    private String canonicalAttributeValue(JsonNode value) {
        if (value == null || value.isNull()) return "null";
        if (value.isTextual()) return value.asString().trim().toUpperCase(Locale.ROOT);
        if (value.isNumber()) return value.decimalValue().stripTrailingZeros().toPlainString();
        if (value.isBoolean()) return Boolean.toString(value.asBoolean());
        if (value.isArray()) {
            List<String> items = new ArrayList<>();
            value.forEach(item -> items.add(canonicalAttributeValue(item)));
            items.sort(String::compareTo);
            return "[" + String.join(",", items) + "]";
        }
        return value.toString();
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm."));
    }

    private Product requireProductForUpdate(UUID productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm."));
    }

    private ProductVariant requireVariant(UUID productId, UUID variantId) {
        return variantRepository.findById(variantId)
                .filter(variant -> variant.getProductId().equals(productId))
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                        "Không tìm thấy Variant của sản phẩm."));
    }

    private Category requireCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                        "Không tìm thấy danh mục."));
    }

    private Category requireAssignableCategory(UUID categoryId) {
        Category category = requireCategory(categoryId);
        requireLeafCategory(category);
        return category;
    }

    private void requireLeafCategory(Category category) {
        if (categoryRepository.existsByParentId(category.getId())) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRODUCT_CATEGORY_MUST_BE_LEAF",
                    "Sản phẩm phải thuộc danh mục cuối cùng, không thể gắn trực tiếp vào danh mục cha.");
        }
    }

    private String normalizeSlug(String value) {
        String source = value == null ? "" : value.trim()
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String withoutAccents = COMBINING_MARKS.matcher(
                Normalizer.normalize(source, Normalizer.Form.NFD)).replaceAll("");
        String slug = EDGE_HYPHENS.matcher(NON_SLUG_CHARACTERS.matcher(
                withoutAccents.toLowerCase(Locale.ROOT)).replaceAll("-")).replaceAll("");
        slug = truncateSlug(slug, MAX_SLUG_LENGTH);
        if (slug.isBlank()) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRODUCT_SLUG_INVALID",
                    "Không thể tạo đường dẫn hợp lệ cho sản phẩm.");
        }
        return slug;
    }

    private String truncateSlug(String slug, int maximumLength) {
        if (slug.length() <= maximumLength) return slug;
        return TRAILING_HYPHENS.matcher(slug.substring(0, maximumLength)).replaceAll("");
    }

    private String normalizeSku(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
