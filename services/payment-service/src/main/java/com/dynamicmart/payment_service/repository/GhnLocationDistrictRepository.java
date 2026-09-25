package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.GhnLocationDistrict;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GhnLocationDistrictRepository extends JpaRepository<GhnLocationDistrict, Integer> {
    List<GhnLocationDistrict> findByProvinceIdAndActiveTrueOrderByNameAsc(int provinceId);
}
