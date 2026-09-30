package com.dynamicmart.catalog_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.dynamicmart.catalog_service.dto.request.UpdateCategoryRequest;
import com.dynamicmart.catalog_service.entity.Category;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void rejectsParentThatCreatesCycle() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Category root = new Category(rootId, null, "ROOT", "Root", "root", null, 0);
        Category child = new Category(childId, rootId, "CHILD", "Child", "child", null, 0);
        when(categoryRepository.findById(rootId)).thenReturn(Optional.of(root));
        when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));

        UpdateCategoryRequest request = new UpdateCategoryRequest(childId, "ROOT", "Root",
                "root", null, 0);

        CatalogException exception = assertThrows(CatalogException.class,
                () -> categoryService.update(rootId, request));
        assertEquals("CATEGORY_PARENT_CYCLE", exception.getCode());
    }
}
