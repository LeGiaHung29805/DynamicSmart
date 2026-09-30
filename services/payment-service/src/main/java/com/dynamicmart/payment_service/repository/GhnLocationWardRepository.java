package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.GhnLocationWard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GhnLocationWardRepository extends JpaRepository<GhnLocationWard, Integer> {
    List<GhnLocationWard> findByProvinceIdAndActiveTrueOrderByNameAsc(int provinceId);
    List<GhnLocationWard> findByDistrictIdAndActiveTrueOrderByNameAsc(int districtId);
}
