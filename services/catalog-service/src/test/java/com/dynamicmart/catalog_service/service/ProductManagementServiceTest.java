package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.dto.request.CreateProductRequest;
import com.dynamicmart.catalog_service.dto.request.ProductStatusRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateProductRequest;
import com.dynamicmart.catalog_service.dto.request.UpsertProductImageRequest;
import com.dynamicmart.catalog_service.dto.response.ProductImageResponse;
import com.dynamicmart.catalog_service.dto.response.ProductPublishReadinessResponse;
import com.dynamicmart.catalog_service.entity.Category;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductStatus;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.ProductAttributeValueRepository;
import com.dynamicmart.catalog_service.repository.ProductImageRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import com.dynamicmart.catalog_service.repository.VariantAttributeValueRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductManagementServiceTest {
    private static final UUID CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ORIGINAL_CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductImageRepository imageRepository;
    @Mock private InventoryItemRepository inventoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductAttributeValueRepository productValueRepository;
    @Mock private VariantAttributeValueRepository variantValueRepository;
    @Mock private AttributeValueService attributeValueService;
    @Mock private CatalogResponseAssembler responseAssembler;

    @InjectMocks
    private ProductManagementService productService;

    @BeforeEach
    void configureCategory() {
        Category category = new Category(CATEGORY_ID, null, "FASHION", "Thời trang",
                "thoi-trang", null, 0);
        lenient().when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        lenient().when(categoryRepository.existsByParentId(CATEGORY_ID)).thenReturn(false);
    }

    @Test
    void rejectsParentCategoryWhenCreatingProduct() {
        when(categoryRepository.existsByParentId(CATEGORY_ID)).thenReturn(true);

        CatalogException exception = assertThrows(CatalogException.class,
                () -> productService.createProduct(request("Áo Thun Nam", null)));

        assertEquals("PRODUCT_CATEGORY_MUST_BE_LEAF", exception.getCode());
    }

    @Test
    void generatesVietnameseSlugAndUsesNextAvailableSuffix() {
        when(productRepository.existsBySlugIgnoreCase("ao-thun-nam")).thenReturn(true);
        when(productRepository.existsBySlugIgnoreCase("ao-thun-nam-2")).thenReturn(true);
        when(productRepository.existsBySlugIgnoreCase("ao-thun-nam-3")).thenReturn(false);

        productService.createProduct(request("Áo Thun Nam", null));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertEquals("ao-thun-nam-3", productCaptor.getValue().getSlug());
    }

    @Test
    void normalizesCustomSlugWithoutReplacingItFromProductName() {
        when(productRepository.existsBySlugIgnoreCase("ao-thun-dac-biet")).thenReturn(false);

        productService.createProduct(request("Tên sản phẩm khác", "ÁO THUN Đặc Biệt"));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertEquals("ao-thun-dac-biet", productCaptor.getValue().getSlug());
    }

    @Test
    void rejectsDuplicateCustomSlugInsteadOfChangingItSilently() {
        when(productRepository.existsBySlugIgnoreCase("ao-thun-nam")).thenReturn(true);

        CatalogException exception = assertThrows(CatalogException.class,
                () -> productService.createProduct(request("Tên sản phẩm khác", "ao-thun-nam")));

        assertEquals("PRODUCT_SLUG_EXISTS", exception.getCode());
    }

    @Test
    void rejectsCategoryChangeAfterVariantsExist() {
        Product product = product(ORIGINAL_CATEGORY_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugIgnoreCaseAndIdNot("san-pham", PRODUCT_ID)).thenReturn(false);
        when(variantRepository.existsByProductId(PRODUCT_ID)).thenReturn(true);

        UpdateProductRequest update = new UpdateProductRequest(CATEGORY_ID, "Sản phẩm", "san-pham",
                null, null, null, null, null, null, false, List.of());

        CatalogException exception = assertThrows(CatalogException.class,
                () -> productService.updateProduct(PRODUCT_ID, update));

        assertEquals("PRODUCT_CATEGORY_IMMUTABLE", exception.getCode());
    }

    @Test
    void reportsMissingPublishRequirementsBeforeActivation() {
        Product product = product(CATEGORY_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productValueRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of());
        when(variantRepository.findAllByProductIdAndStatusOrderBySortOrderAsc(
                PRODUCT_ID, com.dynamicmart.catalog_service.entity.VariantStatus.ACTIVE))
                .thenReturn(List.of());

        ProductPublishReadinessResponse readiness = productService.getPublishReadiness(PRODUCT_ID);

        assertFalse(readiness.ready());
        assertTrue(readiness.checks().stream().anyMatch(check ->
                check.code().equals("PRODUCT_HAS_NO_ACTIVE_VARIANT") && !check.complete()));
        CatalogException exception = assertThrows(CatalogException.class,
                () -> productService.setProductStatus(PRODUCT_ID,
                        new ProductStatusRequest(ProductStatus.ACTIVE, null)));
        assertEquals("PRODUCT_HAS_NO_ACTIVE_VARIANT", exception.getCode());
    }

    @Test
    void makesFirstImageInScopePrimaryAutomatically() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(CATEGORY_ID)));
        when(imageRepository.findAllByProductIdOrderBySortOrderAsc(PRODUCT_ID)).thenReturn(List.of());

        ProductImageResponse image = productService.createImage(PRODUCT_ID,
                new UpsertProductImageRequest(null, "https://example.com/product.webp",
                        null, null, "Sản phẩm", 0, false));

        assertTrue(image.primary());
    }

    private CreateProductRequest request(String name, String slug) {
        return new CreateProductRequest(CATEGORY_ID, name, slug, null, null,
                null, null, null, null, false, List.of());
    }

    private Product product(UUID categoryId) {
        return new Product(PRODUCT_ID, categoryId, "Sản phẩm", "san-pham", null, null,
                null, null, null, null);
    }
}
