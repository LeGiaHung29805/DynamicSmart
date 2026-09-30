package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.dto.request.UpsertAttributeOptionRequest;
import com.dynamicmart.catalog_service.entity.AttributeDataType;
import com.dynamicmart.catalog_service.entity.AttributeDefinition;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.AttributeDefinitionRepository;
import com.dynamicmart.catalog_service.repository.AttributeOptionRepository;
import com.dynamicmart.catalog_service.repository.CategoryAttributeRepository;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttributeServiceTest {
    @Mock private AttributeDefinitionRepository attributeRepository;
    @Mock private AttributeOptionRepository optionRepository;
    @Mock private CategoryAttributeRepository categoryAttributeRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private AttributeService attributeService;

    @Test
    void rejectsOptionsForNonSelectableAttribute() {
        UUID attributeId = UUID.randomUUID();
        AttributeDefinition attribute = new AttributeDefinition(attributeId, "MATERIAL", "Material",
                AttributeDataType.TEXT, null);
        when(attributeRepository.findById(attributeId)).thenReturn(Optional.of(attribute));

        CatalogException exception = assertThrows(CatalogException.class,
                () -> attributeService.createOption(attributeId,
                        new UpsertAttributeOptionRequest("STEEL", "Steel", 0)));

        assertEquals("ATTRIBUTE_NOT_SELECTABLE", exception.getCode());
    }
}
