package com.dynamicmart.reporting_service.repository;

import com.dynamicmart.reporting_service.dto.response.BestSellerResponse;
import com.dynamicmart.reporting_service.entity.DailyProductMetric;
import com.dynamicmart.reporting_service.entity.DailyProductMetricId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DailyProductMetricRepository extends JpaRepository<DailyProductMetric, DailyProductMetricId> {
    @Query("""
            select new com.dynamicmart.reporting_service.dto.response.BestSellerResponse(
                metric.id.productId,
                sum(metric.quantitySold),
                sum(metric.grossSalesVnd),
                sum(metric.netItemSalesVnd)
            )
            from DailyProductMetric metric
            where metric.id.metricDate between :from and :to
            group by metric.id.productId
            order by sum(metric.quantitySold) desc, sum(metric.netItemSalesVnd) desc
            """)
    List<BestSellerResponse> bestSellers(LocalDate from, LocalDate to, Pageable pageable);
}
