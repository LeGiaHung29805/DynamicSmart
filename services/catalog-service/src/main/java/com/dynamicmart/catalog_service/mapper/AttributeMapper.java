package com.dynamicmart.catalog_service.mapper;

import com.dynamicmart.catalog_service.dto.response.AttributeOptionResponse;
import com.dynamicmart.catalog_service.dto.response.AttributeResponse;
import com.dynamicmart.catalog_service.entity.AttributeDefinition;
import com.dynamicmart.catalog_service.entity.AttributeOption;
import java.util.List;

public final class AttributeMapper {
    private AttributeMapper() {
    }

    public static AttributeOptionResponse toOptionResponse(AttributeOption option) {
        return new AttributeOptionResponse(
                option.getId(),
                option.getAttributeId(),
                option.getCode(),
                option.getLabel(),
                option.getSortOrder(),
                option.getStatus());
    }

    public static AttributeResponse toResponse(AttributeDefinition attribute,
                                               List<AttributeOption> options) {
        return new AttributeResponse(
                attribute.getId(),
                attribute.getCode(),
                attribute.getName(),
                attribute.getDataType(),
                attribute.getValidationConfig(),
                attribute.getStatus(),
                options.stream().map(AttributeMapper::toOptionResponse).toList());
    }
}
