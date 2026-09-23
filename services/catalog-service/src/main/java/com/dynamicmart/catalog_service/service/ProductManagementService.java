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
import com.dynamicmart.catalog_service.dto.response.ProductVariantResponse;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
        requireCategory(request.categoryId());
        String slug = normalizeSlug(request.slug());
        requireUniqueSlug(slug, null);
        Product product = new Product(UUID.randomUUID(), request.categoryId(), request.name().trim(),
                slug, trimToNull(request.shortDescription()), trimToNull(request.description()),
                request.defaultWeightGrams(), request.defaultLengthCm(), request.defaultWidthCm(),
                request.defaultHeightCm());
        product.markFeatured(request.featured());
        productRepository.save(product);
        attributeValueService.replaceProductValues(product.getId(), product.getCategoryId(),
                request.attributes());
        return responseAssembler.adminProduct(product);
    }

    @Transactional
    public AdminProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = requireProduct(productId);
        requireCategory(request.categoryId());
        String slug = normalizeSlug(request.slug());
        requireUniqueSlug(slug, productId);
        if (!product.getCategoryId().equals(request.categoryId())) {
            validateVariantsForCategory(productId, request.categoryId());
        }
        product.update(request.categoryId(), request.name().trim(), slug,
                trimToNull(request.shortDescription()), trimToNull(request.description()),
                request.defaultWeightGrams(), request.defaultLengthCm(), request.defaultWidthCm(),
                request.defaultHeightCm());
        product.markFeatured(request.featured());
        attributeValueService.replaceProductValues(productId, request.categoryId(), request.attributes());
        if (product.getStatus() == ProductStatus.ACTIVE) validatePublishable(product);
        return responseAssembler.adminProduct(product);
    }

    @Transactional
    public AdminProductResponse setProductStatus(UUID productId, ProductStatusRequest request) {
        Product product = requireProduct(productId);
        switch (request.status()) {
            case DRAFT -> product.moveToDraft();
            case ACTIVE -> {
                validatePublishable(product);
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
        Product product = requireProduct(productId);
        String sku = normalizeSku(request.sku());
        requireUniqueSku(sku, null);
        ProductVariant variant = new ProductVariant(UUID.randomUUID(), productId, sku,
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
        Product product = requireProduct(productId);
        ProductVariant variant = requireVariant(productId, variantId);
        String sku = normalizeSku(request.sku());
        requireUniqueSku(sku, variantId);
        variant.update(sku, trimToNull(request.name()), request.priceVnd(), request.weightGrams(),
                request.lengthCm(), request.widthCm(), request.heightCm(), request.sortOrder());
        attributeValueService.replaceVariantValues(variantId, product.getCategoryId(), request.attributes());
        if (variant.getStatus() == VariantStatus.ACTIVE) requireShippingDimensions(product, variant);
        return responseAssembler.productVariant(product, variant);
    }

    @Transactional
    public ProductVariantResponse setVariantStatus(UUID productId, UUID variantId,
                                                   VariantStatus status) {
        Product product = requireProduct(productId);
        ProductVariant variant = requireVariant(productId, variantId);
        switch (status) {
            case ACTIVE -> {
                requireShippingDimensions(product, variant);
                validateCurrentVariantAttributes(product, variant);
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
        ProductImage image = new ProductImage(UUID.randomUUID(), productId, request.variantId(),
                request.imageUrl().trim(), trimToNull(request.altText()), request.sortOrder(),
                request.primary());
        if (request.primary()) demoteCurrentPrimary(productId, request.variantId(), null);
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
        if (request.primary()) demoteCurrentPrimary(productId, request.variantId(), imageId);
        image.update(request.imageUrl().trim(), trimToNull(request.altText()), request.sortOrder(),
                request.primary());
        return imageResponse(image);
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        requireProduct(productId);
        imageRepository.delete(requireImage(productId, imageId));
    }

    private void validatePublishable(Product product) {
        Category category = requireCategory(product.getCategoryId());
        if (category.getStatus() != CatalogStatus.ACTIVE) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRODUCT_CATEGORY_INACTIVE",
                    "Không thể công khai sản phẩm trong danh mục ngừng hoạt động.");
        }
        attributeValueService.validate(product.getCategoryId(),
                com.dynamicmart.catalog_service.entity.AttributeAppliesTo.PRODUCT,
                productValueRepository.findAllByProductId(product.getId()).stream()
                        .map(value -> new AttributeValueRequest(value.getAttributeId(), value.getValue()))
                        .toList());
        List<ProductVariant> activeVariants = variantRepository
                .findAllByProductIdAndStatusOrderBySortOrderAsc(product.getId(), VariantStatus.ACTIVE);
        if (activeVariants.isEmpty()) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "PRODUCT_HAS_NO_ACTIVE_VARIANT",
                    "Sản phẩm phải có ít nhất một Variant đang bán trước khi công khai.");
        }
        activeVariants.forEach(variant -> {
            requireShippingDimensions(product, variant);
            validateCurrentVariantAttributes(product, variant);
        });
    }

    private void validateCurrentVariantAttributes(Product product, ProductVariant variant) {
        attributeValueService.validate(product.getCategoryId(),
                com.dynamicmart.catalog_service.entity.AttributeAppliesTo.VARIANT,
                variantValueRepository.findAllByVariantId(variant.getId()).stream()
                        .map(value -> new AttributeValueRequest(value.getAttributeId(), value.getValue()))
                        .toList());
    }

    private void validateVariantsForCategory(UUID productId, UUID newCategoryId) {
        for (ProductVariant variant : variantRepository.findAllByProductIdOrderBySortOrderAsc(productId)) {
            List<AttributeValueRequest> values = variantValueRepository.findAllByVariantId(variant.getId()).stream()
                    .map(value -> new AttributeValueRequest(value.getAttributeId(), value.getValue()))
                    .toList();
            attributeValueService.validate(newCategoryId,
                    com.dynamicmart.catalog_service.entity.AttributeAppliesTo.VARIANT, values);
        }
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
        String type = request.contentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(type)) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "IMAGE_TYPE_NOT_ALLOWED",
                    "Ảnh chỉ hỗ trợ JPEG, PNG, WEBP hoặc AVIF.");
        }
        if (request.sizeBytes() > MAX_IMAGE_SIZE_BYTES) {
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

    private void requireUniqueSku(String sku, UUID excludedId) {
        boolean duplicate = excludedId == null ? variantRepository.existsBySkuIgnoreCase(sku)
                : variantRepository.existsBySkuIgnoreCaseAndIdNot(sku, excludedId);
        if (duplicate) throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_SKU_EXISTS",
                "Mã hàng đã tồn tại.");
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
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

    private String normalizeSlug(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSku(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
