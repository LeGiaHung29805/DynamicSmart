package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.dto.response.LocationResponse;
import com.dynamicmart.payment_service.dto.response.LocationValidationResponse;
import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.mapper.LocationMapper;
import com.dynamicmart.payment_service.repository.GhnLocationProvinceRepository;
import com.dynamicmart.payment_service.repository.GhnLocationDistrictRepository;
import com.dynamicmart.payment_service.repository.GhnLocationWardRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private final GhnLocationProvinceRepository provinces;
    private final GhnLocationDistrictRepository districts;
    private final GhnLocationWardRepository wards;

    public LocationService(GhnLocationProvinceRepository provinces, GhnLocationDistrictRepository districts, GhnLocationWardRepository wards) {
        this.provinces = provinces; this.districts = districts;
        this.wards = wards;
    }

    public List<LocationResponse> provinces() {
        return provinces.findByActiveTrueOrderByNameAsc().stream().map(LocationMapper::toResponse).toList();
    }

    public List<LocationResponse> districts(int provinceId) {
        requireProvince(provinceId);
        return districts.findByProvinceIdAndActiveTrueOrderByNameAsc(provinceId).stream().map(LocationMapper::toResponse).toList();
    }

    public List<LocationResponse> wards(int provinceId) {
        requireProvince(provinceId);
        return wards.findByProvinceIdAndActiveTrueOrderByNameAsc(provinceId).stream().map(LocationMapper::toResponse).toList();
    }

    public LocationValidationResponse validate(int provinceId, int wardId) {
        requireProvince(provinceId);
        var ward = wards.findById(wardId).filter(value -> value.isActive() && value.getProvinceId() == provinceId)
                .orElseThrow(() -> new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "GHN_LOCATION_MISMATCH",
                        "Phường/Xã không thuộc Tỉnh/Thành phố đã chọn hoặc đã ngừng hỗ trợ."));
        return new LocationValidationResponse(true, provinceId, ward.getId());
    }

    public ValidatedLocation validateForQuote(int provinceId, int wardId) {
        requireProvince(provinceId);
        var ward = wards.findById(wardId).filter(w -> w.isActive() && w.getProvinceId() == provinceId
                        && w.getDistrictId() != null && w.getGhnWardCode() != null && !w.getGhnWardCode().isBlank())
                .orElseThrow(() -> new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "GHN_LOCATION_MISMATCH", "Phường/Xã không thuộc Tỉnh/Thành phố đã chọn hoặc đã ngừng hỗ trợ."));
        var district = districts.findById(ward.getDistrictId()).filter(d -> d.isActive() && d.getProvinceId() == provinceId)
                .orElseThrow(() -> new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "GHN_LOCATION_MISMATCH", "Địa giới GHN không còn hợp lệ hoặc đã ngừng hỗ trợ."));
        return new ValidatedLocation(provinceId, district.getId(), ward.getId(), ward.getGhnWardCode());
    }

    private void requireProvince(int id) {
        if (provinces.findById(id).filter(p -> p.isActive()).isEmpty()) {
            throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "PROVINCE_NOT_SUPPORTED", "Tỉnh/Thành phố không hợp lệ hoặc đã ngừng hỗ trợ.");
        }
    }
    private void requireDistrict(int id) {
        if (districts.findById(id).filter(d -> d.isActive()).isEmpty()) throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "DISTRICT_NOT_SUPPORTED", "Quận/Huyện không hợp lệ hoặc đã ngừng hỗ trợ.");
    }

    public record ValidatedLocation(int provinceId, int districtId, int wardId, String wardCode) { }
}
