package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.request.CreateCategoryRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateCategoryRequest;
import com.dynamicmart.catalog_service.dto.response.CategoryResponse;
import com.dynamicmart.catalog_service.dto.response.CategoryTreeResponse;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.Category;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.mapper.CategoryMapper;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String code = normalizeCode(request.code());
        String slug = normalizeSlug(request.slug());
        requireUnique(code, slug, null);
        validateParent(request.parentId(), null);

        Category category = new Category(UUID.randomUUID(), request.parentId(), code,
                request.name().trim(), slug, trimToNull(request.description()), request.sortOrder());
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID categoryId, UpdateCategoryRequest request) {
        Category category = requireCategory(categoryId);
        String code = normalizeCode(request.code());
        String slug = normalizeSlug(request.slug());
        requireUnique(code, slug, categoryId);
        validateParent(request.parentId(), categoryId);

        category.update(request.parentId(), code, request.name().trim(), slug,
                trimToNull(request.description()), request.sortOrder());
        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse setActive(UUID categoryId, boolean active) {
        Category category = requireCategory(categoryId);
        if (active) category.activate();
        else category.deactivate();
        return CategoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID categoryId) {
        return CategoryMapper.toResponse(requireCategory(categoryId));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlugIgnoreCase(normalizeSlug(slug))
                .filter(value -> value.getStatus() == CatalogStatus.ACTIVE)
                .orElseThrow(() -> notFound("Không tìm thấy danh mục đang hoạt động."));
        return CategoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listForAdmin() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> listActiveTree() {
        List<Category> categories = categoryRepository
                .findAllByStatusOrderBySortOrderAscNameAsc(CatalogStatus.ACTIVE);
        Map<UUID, Category> byId = new HashMap<>();
        Map<UUID, List<Category>> byParent = new HashMap<>();
        for (Category category : categories) {
            byId.put(category.getId(), category);
            if (category.getParentId() != null) {
                byParent.computeIfAbsent(category.getParentId(), ignored -> new java.util.ArrayList<>())
                        .add(category);
            }
        }

        return categories.stream()
                .filter(category -> category.getParentId() == null)
                .map(category -> toTree(category, byParent, new HashSet<>()))
                .toList();
    }

    private CategoryTreeResponse toTree(Category category, Map<UUID, List<Category>> byParent,
                                        Set<UUID> ancestors) {
        Set<UUID> path = new HashSet<>(ancestors);
        if (!path.add(category.getId())) {
            throw new CatalogException(HttpStatus.CONFLICT, "CATEGORY_CYCLE_DETECTED",
                    "Dữ liệu danh mục đang chứa vòng lặp.");
        }
        List<CategoryTreeResponse> children = byParent.getOrDefault(category.getId(), List.of()).stream()
                .map(child -> toTree(child, byParent, path))
                .toList();
        return new CategoryTreeResponse(category.getId(), category.getCode(), category.getName(),
                category.getSlug(), category.getDescription(), category.getSortOrder(), children);
    }

    private void requireUnique(String code, String slug, UUID excludedId) {
        boolean duplicateCode = excludedId == null
                ? categoryRepository.existsByCodeIgnoreCase(code)
                : categoryRepository.existsByCodeIgnoreCaseAndIdNot(code, excludedId);
        if (duplicateCode) {
            throw new CatalogException(HttpStatus.CONFLICT, "CATEGORY_CODE_EXISTS",
                    "Mã danh mục đã tồn tại.");
        }
        boolean duplicateSlug = excludedId == null
                ? categoryRepository.existsBySlugIgnoreCase(slug)
                : categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, excludedId);
        if (duplicateSlug) {
            throw new CatalogException(HttpStatus.CONFLICT, "CATEGORY_SLUG_EXISTS",
                    "Đường dẫn danh mục đã tồn tại.");
        }
    }

    private void validateParent(UUID parentId, UUID currentId) {
        if (parentId == null) return;
        if (parentId.equals(currentId)) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "CATEGORY_PARENT_SELF",
                    "Danh mục không thể là cha của chính nó.");
        }

        Set<UUID> visited = new HashSet<>();
        UUID cursor = parentId;
        while (cursor != null) {
            if (!visited.add(cursor) || cursor.equals(currentId)) {
                throw new CatalogException(HttpStatus.BAD_REQUEST, "CATEGORY_PARENT_CYCLE",
                        "Quan hệ danh mục cha–con tạo thành vòng lặp.");
            }
            Category parent = categoryRepository.findById(cursor)
                    .orElseThrow(() -> new CatalogException(HttpStatus.BAD_REQUEST,
                            "CATEGORY_PARENT_NOT_FOUND", "Không tìm thấy danh mục cha."));
            cursor = parent.getParentId();
        }
    }

    private Category requireCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> notFound("Không tìm thấy danh mục."));
    }

    private CatalogException notFound(String message) {
        return new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", message);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSlug(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
