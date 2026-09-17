package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.LocationResponse;
import com.dynamicmart.payment_service.api.LocationValidationResponse;
import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.repository.GhnLocationProvinceRepository;
import com.dynamicmart.payment_service.repository.GhnLocationWardRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private final GhnLocationProvinceRepository provinces;
    private final GhnLocationWardRepository wards;

    public LocationService(GhnLocationProvinceRepository provinces, GhnLocationWardRepository wards) {
        this.provinces = provinces;
        this.wards = wards;
    }

    public List<LocationResponse> provinces() {
        return provinces.findByActiveTrueOrderByNameAsc().stream().map(p -> new LocationResponse(p.getId(), p.getName())).toList();
    }

    public List<LocationResponse> wards(int provinceId) {
        requireProvince(provinceId);
        return wards.findByProvinceIdAndActiveTrueOrderByNameAsc(provinceId).stream().map(w -> new LocationResponse(w.getId(), w.getName())).toList();
    }

    public LocationValidationResponse validate(int provinceId, int wardId) {
        requireProvince(provinceId);
        var ward = wards.findById(wardId).filter(w -> w.isActive() && w.getProvinceId() == provinceId)
                .orElseThrow(() -> new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "WARD_PROVINCE_MISMATCH", "Phường/Xã không thuộc Tỉnh/Thành phố đã chọn hoặc đã ngừng hỗ trợ."));
        return new LocationValidationResponse(true, provinceId, ward.getId());
    }

    private void requireProvince(int id) {
        if (provinces.findById(id).filter(p -> p.isActive()).isEmpty()) {
            throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "PROVINCE_NOT_SUPPORTED", "Tỉnh/Thành phố không hợp lệ hoặc đã ngừng hỗ trợ.");
        }
    }
}
