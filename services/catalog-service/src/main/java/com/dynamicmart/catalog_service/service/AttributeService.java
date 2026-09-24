package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.request.CreateAttributeRequest;
import com.dynamicmart.catalog_service.dto.request.MapCategoryAttributeRequest;
import com.dynamicmart.catalog_service.dto.request.UpdateAttributeRequest;
import com.dynamicmart.catalog_service.dto.request.UpsertAttributeOptionRequest;
import com.dynamicmart.catalog_service.dto.response.AttributeOptionResponse;
import com.dynamicmart.catalog_service.dto.response.AttributeResponse;
import com.dynamicmart.catalog_service.dto.response.CategoryAttributeResponse;
import com.dynamicmart.catalog_service.dto.response.CatalogFilterDefinitionResponse;
import com.dynamicmart.catalog_service.dto.response.CatalogFilterOptionResponse;
import com.dynamicmart.catalog_service.entity.AttributeDataType;
import com.dynamicmart.catalog_service.entity.AttributeDefinition;
import com.dynamicmart.catalog_service.entity.AttributeOption;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.CategoryAttribute;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.mapper.AttributeMapper;
import com.dynamicmart.catalog_service.repository.AttributeDefinitionRepository;
import com.dynamicmart.catalog_service.repository.AttributeOptionRepository;
import com.dynamicmart.catalog_service.repository.CategoryAttributeRepository;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttributeService {
    private final AttributeDefinitionRepository attributeRepository;
    private final AttributeOptionRepository optionRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;

    public AttributeService(AttributeDefinitionRepository attributeRepository,
                            AttributeOptionRepository optionRepository,
                            CategoryAttributeRepository categoryAttributeRepository,
                            CategoryRepository categoryRepository) {
        this.attributeRepository = attributeRepository;
        this.optionRepository = optionRepository;
        this.categoryAttributeRepository = categoryAttributeRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public AttributeResponse create(CreateAttributeRequest request) {
        String code = normalizeCode(request.code());
        requireUniqueCode(code, null);
        AttributeDefinition attribute = new AttributeDefinition(UUID.randomUUID(), code,
                request.name().trim(), request.dataType(), request.validationConfig());
        return AttributeMapper.toResponse(attributeRepository.save(attribute), List.of());
    }

    @Transactional
    public AttributeResponse update(UUID attributeId, UpdateAttributeRequest request) {
        AttributeDefinition attribute = requireAttribute(attributeId);
        String code = normalizeCode(request.code());
        requireUniqueCode(code, attributeId);
        requireCompatibleTypeChange(attribute, request.dataType());
        attribute.update(code, request.name().trim(), request.dataType(), request.validationConfig());
        return AttributeMapper.toResponse(attribute,
                optionRepository.findAllByAttributeIdOrderBySortOrderAscLabelAsc(attributeId));
    }

    @Transactional
    public AttributeResponse setActive(UUID attributeId, boolean active) {
        AttributeDefinition attribute = requireAttribute(attributeId);
        if (active) attribute.activate();
        else attribute.deactivate();
        return AttributeMapper.toResponse(attribute,
                optionRepository.findAllByAttributeIdOrderBySortOrderAscLabelAsc(attributeId));
    }

    @Transactional(readOnly = true)
    public AttributeResponse get(UUID attributeId) {
        AttributeDefinition attribute = requireAttribute(attributeId);
        return AttributeMapper.toResponse(attribute,
                optionRepository.findAllByAttributeIdOrderBySortOrderAscLabelAsc(attributeId));
    }

    @Transactional(readOnly = true)
    public List<AttributeResponse> listForAdmin() {
        List<AttributeDefinition> attributes = attributeRepository.findAllByOrderByNameAsc();
        if (attributes.isEmpty()) return List.of();
        List<UUID> ids = attributes.stream().map(AttributeDefinition::getId).toList();
        Map<UUID, List<AttributeOption>> optionsByAttribute = new HashMap<>();
        optionRepository.findAllByAttributeIdInOrderByAttributeIdAscSortOrderAscLabelAsc(ids)
                .forEach(option -> optionsByAttribute
                        .computeIfAbsent(option.getAttributeId(), ignored -> new ArrayList<>())
                        .add(option));
        return attributes.stream()
                .map(attribute -> AttributeMapper.toResponse(attribute,
                        optionsByAttribute.getOrDefault(attribute.getId(), List.of())))
                .toList();
    }

    @Transactional
    public AttributeOptionResponse createOption(UUID attributeId, UpsertAttributeOptionRequest request) {
        AttributeDefinition attribute = requireAttribute(attributeId);
        requireSelectable(attribute);
        String code = normalizeCode(request.code());
        if (optionRepository.existsByAttributeIdAndCodeIgnoreCase(attributeId, code)) {
            throw new CatalogException(HttpStatus.CONFLICT, "ATTRIBUTE_OPTION_CODE_EXISTS",
                    "Mã lựa chọn thuộc tính đã tồn tại.");
        }
        AttributeOption option = new AttributeOption(UUID.randomUUID(), attributeId, code,
                request.label().trim(), request.sortOrder());
        return AttributeMapper.toOptionResponse(optionRepository.save(option));
    }

    @Transactional
    public AttributeOptionResponse updateOption(UUID attributeId, UUID optionId,
                                                UpsertAttributeOptionRequest request) {
        AttributeDefinition attribute = requireAttribute(attributeId);
        requireSelectable(attribute);
        AttributeOption option = requireOption(attributeId, optionId);
        String code = normalizeCode(request.code());
        if (optionRepository.existsByAttributeIdAndCodeIgnoreCaseAndIdNot(attributeId, code, optionId)) {
            throw new CatalogException(HttpStatus.CONFLICT, "ATTRIBUTE_OPTION_CODE_EXISTS",
                    "Mã lựa chọn thuộc tính đã tồn tại.");
        }
        option.update(code, request.label().trim(), request.sortOrder());
        return AttributeMapper.toOptionResponse(option);
    }

    @Transactional
    public AttributeOptionResponse setOptionActive(UUID attributeId, UUID optionId, boolean active) {
        requireAttribute(attributeId);
        AttributeOption option = requireOption(attributeId, optionId);
        if (active) option.activate();
        else option.deactivate();
        return AttributeMapper.toOptionResponse(option);
    }

    @Transactional
    public CategoryAttributeResponse mapToCategory(UUID categoryId,
                                                   MapCategoryAttributeRequest request) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                    "Không tìm thấy danh mục.");
        }
        AttributeDefinition attribute = requireAttribute(request.attributeId());
        if (attribute.getStatus() != CatalogStatus.ACTIVE) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "ATTRIBUTE_INACTIVE",
                    "Không thể gắn thuộc tính đang ngừng hoạt động.");
        }

        CategoryAttribute mapping = categoryAttributeRepository
                .findByCategoryIdAndAttributeIdAndAppliesTo(categoryId, request.attributeId(),
                        request.appliesTo())
                .map(existing -> {
                    existing.update(request.required(), request.filterable(), request.sortOrder());
                    return existing;
                })
                .orElseGet(() -> new CategoryAttribute(UUID.randomUUID(), categoryId,
                        request.attributeId(), request.appliesTo(), request.required(),
                        request.filterable(), request.sortOrder()));
        categoryAttributeRepository.save(mapping);
        return toCategoryAttributeResponse(mapping, attribute);
    }

    @Transactional(readOnly = true)
    public List<CategoryAttributeResponse> listCategoryMappings(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                    "Không tìm thấy danh mục.");
        }
        List<CategoryAttribute> mappings = categoryAttributeRepository
                .findAllByCategoryIdOrderBySortOrderAsc(categoryId);
        Map<UUID, AttributeDefinition> attributes = new HashMap<>();
        attributeRepository.findAllById(mappings.stream().map(CategoryAttribute::getAttributeId).toList())
                .forEach(attribute -> attributes.put(attribute.getId(), attribute));
        return mappings.stream()
                .map(mapping -> toCategoryAttributeResponse(mapping,
                        requireMappedAttribute(attributes, mapping.getAttributeId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogFilterDefinitionResponse> listPublicFilters(String categorySlug) {
        var category = categoryRepository.findBySlugIgnoreCase(categorySlug)
                .filter(value -> value.getStatus() == CatalogStatus.ACTIVE)
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                        "Không tìm thấy danh mục đang hoạt động."));
        List<CategoryAttribute> mappings = categoryAttributeRepository
                .findAllByCategoryIdOrderBySortOrderAsc(category.getId()).stream()
                .filter(CategoryAttribute::isFilterable)
                .toList();
        Map<UUID, AttributeDefinition> definitions = new HashMap<>();
        attributeRepository.findAllById(mappings.stream().map(CategoryAttribute::getAttributeId).toList())
                .forEach(attribute -> definitions.put(attribute.getId(), attribute));
        List<UUID> selectableIds = definitions.values().stream()
                .filter(attribute -> attribute.getStatus() == CatalogStatus.ACTIVE)
                .filter(attribute -> isSelectable(attribute.getDataType()))
                .map(AttributeDefinition::getId)
                .toList();
        Map<UUID, List<AttributeOption>> options = new HashMap<>();
        if (!selectableIds.isEmpty()) {
            optionRepository.findAllByAttributeIdInOrderByAttributeIdAscSortOrderAscLabelAsc(selectableIds)
                    .stream().filter(option -> option.getStatus() == CatalogStatus.ACTIVE)
                    .forEach(option -> options.computeIfAbsent(option.getAttributeId(),
                            ignored -> new ArrayList<>()).add(option));
        }
        return mappings.stream()
                .filter(mapping -> definitions.containsKey(mapping.getAttributeId()))
                .map(mapping -> Map.entry(mapping, definitions.get(mapping.getAttributeId())))
                .filter(entry -> entry.getValue().getStatus() == CatalogStatus.ACTIVE)
                .map(entry -> new CatalogFilterDefinitionResponse(entry.getValue().getId(),
                        entry.getValue().getCode(), entry.getValue().getName(),
                        entry.getValue().getDataType(), entry.getKey().getAppliesTo(),
                        options.getOrDefault(entry.getValue().getId(), List.of()).stream()
                                .map(option -> new CatalogFilterOptionResponse(option.getCode(),
                                        option.getLabel()))
                                .toList()))
                .toList();
    }

    @Transactional
    public void unmapFromCategory(UUID categoryId, UUID mappingId) {
        CategoryAttribute mapping = categoryAttributeRepository.findById(mappingId)
                .filter(value -> value.getCategoryId().equals(categoryId))
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND,
                        "CATEGORY_ATTRIBUTE_NOT_FOUND", "Không tìm thấy cấu hình thuộc tính của danh mục."));
        categoryAttributeRepository.delete(mapping);
    }

    private void requireUniqueCode(String code, UUID excludedId) {
        boolean duplicate = excludedId == null
                ? attributeRepository.existsByCodeIgnoreCase(code)
                : attributeRepository.existsByCodeIgnoreCaseAndIdNot(code, excludedId);
        if (duplicate) {
            throw new CatalogException(HttpStatus.CONFLICT, "ATTRIBUTE_CODE_EXISTS",
                    "Mã thuộc tính đã tồn tại.");
        }
    }

    private void requireCompatibleTypeChange(AttributeDefinition attribute, AttributeDataType newType) {
        if (isSelectable(attribute.getDataType()) && !isSelectable(newType)
                && !optionRepository.findAllByAttributeIdOrderBySortOrderAscLabelAsc(attribute.getId()).isEmpty()) {
            throw new CatalogException(HttpStatus.CONFLICT, "ATTRIBUTE_OPTIONS_EXIST",
                    "Hãy ngừng sử dụng các lựa chọn trước khi đổi sang kiểu thuộc tính khác.");
        }
    }

    private void requireSelectable(AttributeDefinition attribute) {
        if (!isSelectable(attribute.getDataType())) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "ATTRIBUTE_NOT_SELECTABLE",
                    "Chỉ thuộc tính SELECT hoặc MULTI_SELECT mới có lựa chọn.");
        }
    }

    private boolean isSelectable(AttributeDataType type) {
        return type == AttributeDataType.SELECT || type == AttributeDataType.MULTI_SELECT;
    }

    private AttributeDefinition requireAttribute(UUID attributeId) {
        return attributeRepository.findById(attributeId)
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND,
                        "ATTRIBUTE_NOT_FOUND", "Không tìm thấy thuộc tính."));
    }

    private AttributeOption requireOption(UUID attributeId, UUID optionId) {
        return optionRepository.findById(optionId)
                .filter(option -> option.getAttributeId().equals(attributeId))
                .orElseThrow(() -> new CatalogException(HttpStatus.NOT_FOUND,
                        "ATTRIBUTE_OPTION_NOT_FOUND", "Không tìm thấy lựa chọn thuộc tính."));
    }

    private AttributeDefinition requireMappedAttribute(Map<UUID, AttributeDefinition> attributes,
                                                       UUID attributeId) {
        AttributeDefinition attribute = attributes.get(attributeId);
        if (attribute == null) {
            throw new CatalogException(HttpStatus.CONFLICT, "CATEGORY_ATTRIBUTE_INVALID",
                    "Cấu hình danh mục đang tham chiếu thuộc tính không tồn tại.");
        }
        return attribute;
    }

    private CategoryAttributeResponse toCategoryAttributeResponse(CategoryAttribute mapping,
                                                                  AttributeDefinition attribute) {
        return new CategoryAttributeResponse(mapping.getId(), mapping.getCategoryId(),
                mapping.getAttributeId(), attribute.getCode(), attribute.getName(),
                mapping.getAppliesTo(), mapping.isRequired(), mapping.isFilterable(),
                mapping.getSortOrder());
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
