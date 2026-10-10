package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.request.CreateReviewRequest;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.dto.response.ReviewResponse;
import com.dynamicmart.engagement_service.dto.response.ReviewSummaryResponse;
import com.dynamicmart.engagement_service.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<PageResponse<ReviewResponse>> productReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(reviews.productReviews(productId, page, size));
    }

    @GetMapping("/products/{productId}/summary")
    public ApiResponse<ReviewSummaryResponse> productSummary(@PathVariable UUID productId) {
        return ApiResponse.of(reviews.getSummaryByProduct(productId));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ReviewResponse>> myReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(reviews.customerReviews(CurrentUser.id(jwt), page, size));
    }

    @GetMapping("/me/product/{productId}")
    public ApiResponse<java.util.List<ReviewResponse>> myProductReviews(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId) {
        return ApiResponse.of(reviews.customerProductReviews(CurrentUser.id(jwt), productId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.of(reviews.create(CurrentUser.id(jwt), request));
    }
}
