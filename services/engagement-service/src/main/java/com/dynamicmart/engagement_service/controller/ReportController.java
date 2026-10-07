package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.BestSellerResponse;
import com.dynamicmart.engagement_service.dto.response.DailySalesMetricResponse;
import com.dynamicmart.engagement_service.service.ReportingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportingService reporting;

    public ReportController(ReportingService reporting) {
        this.reporting = reporting;
    }

    @GetMapping("/sales/daily")
    public ApiResponse<List<DailySalesMetricResponse>> sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.of(reporting.sales(from, to));
    }

    @GetMapping("/products/best-sellers")
    public ApiResponse<List<BestSellerResponse>> bestSellers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ApiResponse.of(reporting.bestSellers(from, to, limit));
    }
}
