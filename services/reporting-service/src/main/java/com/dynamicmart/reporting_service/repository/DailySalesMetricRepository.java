package com.dynamicmart.reporting_service.repository;

import com.dynamicmart.reporting_service.entity.DailySalesMetric;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySalesMetricRepository extends JpaRepository<DailySalesMetric, LocalDate> {
    List<DailySalesMetric> findAllByMetricDateBetweenOrderByMetricDateAsc(LocalDate from, LocalDate to);
}
