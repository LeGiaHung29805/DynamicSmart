package com.dynamicmart.catalog_service.mapper;

import com.dynamicmart.catalog_service.dto.response.CategoryResponse;
import com.dynamicmart.catalog_service.entity.Category;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getCode(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getStatus(),
                category.getSortOrder());
    }
}
