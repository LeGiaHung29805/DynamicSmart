package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.entity.AttributeAppliesTo;
import com.dynamicmart.catalog_service.entity.CategoryAttribute;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.AttributeDefinitionRepository;
import com.dynamicmart.catalog_service.repository.AttributeOptionRepository;
import com.dynamicmart.catalog_service.repository.CategoryAttributeRepository;
import com.dynamicmart.catalog_service.repository.ProductAttributeValueRepository;
import com.dynamicmart.catalog_service.repository.VariantAttributeValueRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttributeValueServiceTest {
    @Mock private CategoryAttributeRepository categoryAttributeRepository;
    @Mock private AttributeDefinitionRepository attributeRepository;
    @Mock private AttributeOptionRepository optionRepository;
    @Mock private ProductAttributeValueRepository productValueRepository;
    @Mock private VariantAttributeValueRepository variantValueRepository;

    @InjectMocks
    private AttributeValueService attributeValueService;

    @Test
    void rejectsMissingRequiredCategoryAttribute() {
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        CategoryAttribute mapping = new CategoryAttribute(UUID.randomUUID(), categoryId,
                attributeId, AttributeAppliesTo.PRODUCT, true, true, 0);
        when(categoryAttributeRepository.findAllByCategoryIdOrderBySortOrderAsc(categoryId))
                .thenReturn(List.of(mapping));

        CatalogException exception = assertThrows(CatalogException.class,
                () -> attributeValueService.validate(categoryId, AttributeAppliesTo.PRODUCT, List.of()));

        assertEquals("REQUIRED_ATTRIBUTE_MISSING", exception.getCode());
    }
}
