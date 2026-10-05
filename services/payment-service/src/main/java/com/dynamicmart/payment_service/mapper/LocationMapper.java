package com.dynamicmart.payment_service.mapper;

import com.dynamicmart.payment_service.dto.response.LocationResponse;
import com.dynamicmart.payment_service.entity.GhnLocationDistrict;
import com.dynamicmart.payment_service.entity.GhnLocationProvince;
import com.dynamicmart.payment_service.entity.GhnLocationWard;

public final class LocationMapper {
    private LocationMapper() { }

    public static LocationResponse toResponse(GhnLocationProvince province) {
        return new LocationResponse(province.getId(), province.getName());
    }

    public static LocationResponse toResponse(GhnLocationDistrict district) {
        return new LocationResponse(district.getId(), district.getName());
    }

    public static LocationResponse toResponse(GhnLocationWard ward) {
        return new LocationResponse(ward.getId(), ward.getName());
    }
}
