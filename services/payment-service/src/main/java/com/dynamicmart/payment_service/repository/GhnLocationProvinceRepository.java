package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.GhnLocationProvince;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GhnLocationProvinceRepository extends JpaRepository<GhnLocationProvince, Integer> {
    List<GhnLocationProvince> findByActiveTrueOrderByNameAsc();
}
