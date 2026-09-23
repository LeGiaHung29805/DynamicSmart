package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.request.AttributeValueRequest;
import com.dynamicmart.catalog_service.entity.AttributeAppliesTo;
import com.dynamicmart.catalog_service.entity.AttributeDataType;
import com.dynamicmart.catalog_service.entity.AttributeDefinition;
import com.dynamicmart.catalog_service.entity.AttributeOption;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.CategoryAttribute;
import com.dynamicmart.catalog_service.entity.ProductAttributeValue;
import com.dynamicmart.catalog_service.entity.VariantAttributeValue;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.AttributeDefinitionRepository;
import com.dynamicmart.catalog_service.repository.AttributeOptionRepository;
import com.dynamicmart.catalog_service.repository.CategoryAttributeRepository;
import com.dynamicmart.catalog_service.repository.ProductAttributeValueRepository;
import com.dynamicmart.catalog_service.repository.VariantAttributeValueRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class AttributeValueService {
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final AttributeDefinitionRepository attributeRepository;
    private final AttributeOptionRepository optionRepository;
    private final ProductAttributeValueRepository productValueRepository;
    private final VariantAttributeValueRepository variantValueRepository;

    public AttributeValueService(CategoryAttributeRepository categoryAttributeRepository,
                                 AttributeDefinitionRepository attributeRepository,
                                 AttributeOptionRepository optionRepository,
                                 ProductAttributeValueRepository productValueRepository,
                                 VariantAttributeValueRepository variantValueRepository) {
        this.categoryAttributeRepository = categoryAttributeRepository;
        this.attributeRepository = attributeRepository;
        this.optionRepository = optionRepository;
        this.productValueRepository = productValueRepository;
        this.variantValueRepository = variantValueRepository;
    }

    public List<ProductAttributeValue> replaceProductValues(UUID productId, UUID categoryId,
                                                            List<AttributeValueRequest> requests) {
        validate(categoryId, AttributeAppliesTo.PRODUCT, requests);
        Map<UUID, ProductAttributeValue> existing = new HashMap<>();
        productValueRepository.findAllByProductId(productId)
                .forEach(value -> existing.put(value.getAttributeId(), value));
        List<ProductAttributeValue> result = new ArrayList<>();
        for (AttributeValueRequest request : requests) {
            ProductAttributeValue value = existing.remove(request.attributeId());
            if (value == null) {
                value = new ProductAttributeValue(UUID.randomUUID(), productId,
                        request.attributeId(), request.value());
            } else {
                value.changeValue(request.value());
            }
            result.add(value);
        }
        if (!existing.isEmpty()) productValueRepository.deleteAll(existing.values());
        return productValueRepository.saveAll(result);
    }

    public List<VariantAttributeValue> replaceVariantValues(UUID variantId, UUID categoryId,
                                                            List<AttributeValueRequest> requests) {
        validate(categoryId, AttributeAppliesTo.VARIANT, requests);
        Map<UUID, VariantAttributeValue> existing = new HashMap<>();
        variantValueRepository.findAllByVariantId(variantId)
                .forEach(value -> existing.put(value.getAttributeId(), value));
        List<VariantAttributeValue> result = new ArrayList<>();
        for (AttributeValueRequest request : requests) {
            VariantAttributeValue value = existing.remove(request.attributeId());
            if (value == null) {
                value = new VariantAttributeValue(UUID.randomUUID(), variantId,
                        request.attributeId(), request.value());
            } else {
                value.changeValue(request.value());
            }
            result.add(value);
        }
        if (!existing.isEmpty()) variantValueRepository.deleteAll(existing.values());
        return variantValueRepository.saveAll(result);
    }

    public void validate(UUID categoryId, AttributeAppliesTo appliesTo,
                         List<AttributeValueRequest> requests) {
        List<CategoryAttribute> mappings = categoryAttributeRepository
                .findAllByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .filter(mapping -> mapping.getAppliesTo() == appliesTo)
                .toList();
        Map<UUID, CategoryAttribute> mappingsByAttribute = new HashMap<>();
        mappings.forEach(mapping -> mappingsByAttribute.put(mapping.getAttributeId(), mapping));

        Set<UUID> supplied = new HashSet<>();
        for (AttributeValueRequest request : requests) {
            if (!supplied.add(request.attributeId())) {
                throw badRequest("ATTRIBUTE_VALUE_DUPLICATED",
                        "Một thuộc tính chỉ được nhập một giá trị.");
            }
            if (!mappingsByAttribute.containsKey(request.attributeId())) {
                throw badRequest("ATTRIBUTE_NOT_MAPPED_TO_CATEGORY",
                        "Thuộc tính không được cấu hình cho danh mục và cấp dữ liệu này.");
            }
        }

        for (CategoryAttribute mapping : mappings) {
            if (mapping.isRequired() && !supplied.contains(mapping.getAttributeId())) {
                throw badRequest("REQUIRED_ATTRIBUTE_MISSING",
                        "Thiếu thuộc tính bắt buộc của danh mục.");
            }
        }

        Map<UUID, AttributeDefinition> definitions = new HashMap<>();
        attributeRepository.findAllById(supplied)
                .forEach(attribute -> definitions.put(attribute.getId(), attribute));
        Map<UUID, List<AttributeOption>> options = loadOptions(definitions);
        for (AttributeValueRequest request : requests) {
            AttributeDefinition definition = definitions.get(request.attributeId());
            if (definition == null || definition.getStatus() != CatalogStatus.ACTIVE) {
                throw badRequest("ATTRIBUTE_NOT_AVAILABLE",
                        "Thuộc tính không tồn tại hoặc đang ngừng hoạt động.");
            }
            validateValue(definition, request.value(),
                    options.getOrDefault(definition.getId(), List.of()));
        }
    }

    private Map<UUID, List<AttributeOption>> loadOptions(
            Map<UUID, AttributeDefinition> definitions) {
        List<UUID> selectableIds = definitions.values().stream()
                .filter(attribute -> attribute.getDataType() == AttributeDataType.SELECT
                        || attribute.getDataType() == AttributeDataType.MULTI_SELECT)
                .map(AttributeDefinition::getId)
                .toList();
        Map<UUID, List<AttributeOption>> result = new HashMap<>();
        if (selectableIds.isEmpty()) return result;
        optionRepository.findAllByAttributeIdInOrderByAttributeIdAscSortOrderAscLabelAsc(selectableIds)
                .forEach(option -> result.computeIfAbsent(option.getAttributeId(),
                        ignored -> new ArrayList<>()).add(option));
        return result;
    }

    private void validateValue(AttributeDefinition definition, JsonNode value,
                               List<AttributeOption> options) {
        if (value == null || value.isNull()) {
            throw badRequest("ATTRIBUTE_VALUE_REQUIRED", "Giá trị thuộc tính không được để trống.");
        }
        switch (definition.getDataType()) {
            case TEXT -> {
                require(value.isTextual(), "ATTRIBUTE_VALUE_TYPE", "Thuộc tính phải là chuỗi.");
                validateTextConfig(definition, value.asText());
            }
            case NUMBER -> {
                require(value.isIntegralNumber(), "ATTRIBUTE_VALUE_TYPE", "Thuộc tính phải là số nguyên.");
                validateNumberConfig(definition, value.decimalValue());
            }
            case DECIMAL -> {
                require(value.isNumber(), "ATTRIBUTE_VALUE_TYPE", "Thuộc tính phải là số.");
                validateNumberConfig(definition, value.decimalValue());
            }
            case BOOLEAN -> require(value.isBoolean(), "ATTRIBUTE_VALUE_TYPE",
                    "Thuộc tính phải là true hoặc false.");
            case SELECT -> {
                require(value.isTextual(), "ATTRIBUTE_VALUE_TYPE",
                        "Thuộc tính lựa chọn phải là mã lựa chọn.");
                requireOption(value.asText(), options);
            }
            case MULTI_SELECT -> {
                require(value.isArray() && value.size() > 0, "ATTRIBUTE_VALUE_TYPE",
                        "Thuộc tính nhiều lựa chọn phải là mảng không rỗng.");
                Set<String> unique = new HashSet<>();
                for (JsonNode item : value) {
                    require(item.isTextual(), "ATTRIBUTE_VALUE_TYPE",
                            "Mỗi lựa chọn phải là mã dạng chuỗi.");
                    String normalized = item.asText().toUpperCase(Locale.ROOT);
                    require(unique.add(normalized), "ATTRIBUTE_VALUE_DUPLICATED",
                            "Danh sách thuộc tính chứa lựa chọn trùng.");
                    requireOption(item.asText(), options);
                }
            }
        }
    }

    private void validateTextConfig(AttributeDefinition definition, String value) {
        JsonNode config = definition.getValidationConfig();
        if (config == null || config.isNull()) return;
        JsonNode minLength = config.get("minLength");
        JsonNode maxLength = config.get("maxLength");
        if (minLength != null && minLength.canConvertToInt()) {
            require(value.length() >= minLength.asInt(), "ATTRIBUTE_VALUE_TOO_SHORT",
                    "Giá trị thuộc tính ngắn hơn giới hạn.");
        }
        if (maxLength != null && maxLength.canConvertToInt()) {
            require(value.length() <= maxLength.asInt(), "ATTRIBUTE_VALUE_TOO_LONG",
                    "Giá trị thuộc tính dài hơn giới hạn.");
        }
        JsonNode regex = config.get("pattern");
        if (regex != null && regex.isTextual()) {
            try {
                require(Pattern.matches(regex.asText(), value), "ATTRIBUTE_VALUE_PATTERN",
                        "Giá trị thuộc tính không đúng định dạng.");
            } catch (PatternSyntaxException exception) {
                throw new CatalogException(HttpStatus.CONFLICT, "ATTRIBUTE_CONFIG_INVALID",
                        "Cấu hình kiểm tra thuộc tính không hợp lệ.");
            }
        }
    }

    private void validateNumberConfig(AttributeDefinition definition, BigDecimal value) {
        JsonNode config = definition.getValidationConfig();
        if (config == null || config.isNull()) return;
        JsonNode minimum = config.get("minimum");
        JsonNode maximum = config.get("maximum");
        if (minimum != null && minimum.isNumber()) {
            require(value.compareTo(minimum.decimalValue()) >= 0, "ATTRIBUTE_VALUE_TOO_SMALL",
                    "Giá trị thuộc tính nhỏ hơn giới hạn.");
        }
        if (maximum != null && maximum.isNumber()) {
            require(value.compareTo(maximum.decimalValue()) <= 0, "ATTRIBUTE_VALUE_TOO_LARGE",
                    "Giá trị thuộc tính lớn hơn giới hạn.");
        }
    }

    private void requireOption(String code, List<AttributeOption> options) {
        boolean valid = options.stream().anyMatch(option -> option.getStatus() == CatalogStatus.ACTIVE
                && option.getCode().equalsIgnoreCase(code));
        require(valid, "ATTRIBUTE_OPTION_INVALID",
                "Lựa chọn thuộc tính không tồn tại hoặc đang ngừng hoạt động.");
    }

    private void require(boolean condition, String code, String message) {
        if (!condition) throw badRequest(code, message);
    }

    private CatalogException badRequest(String code, String message) {
        return new CatalogException(HttpStatus.BAD_REQUEST, code, message);
    }
}
