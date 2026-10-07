package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.request.HideReviewRequest;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.dto.response.ReviewResponse;
import com.dynamicmart.engagement_service.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {
    private final ReviewService reviews;

    public AdminReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(reviews.adminReviews(page, size));
    }

    @PatchMapping("/{reviewId}/hide")
    public ApiResponse<ReviewResponse> hide(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID reviewId,
                                             @Valid @RequestBody HideReviewRequest request) {
        return ApiResponse.of(reviews.hide(CurrentUser.id(jwt), reviewId, request));
    }
}
