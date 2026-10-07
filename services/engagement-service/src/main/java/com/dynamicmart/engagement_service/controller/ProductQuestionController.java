package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.request.CreateProductQuestionRequest;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.dto.response.ProductQuestionResponse;
import com.dynamicmart.engagement_service.service.ProductQuestionService;
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
@RequestMapping("/api/v1/product-questions")
public class ProductQuestionController {
    private final ProductQuestionService questions;

    public ProductQuestionController(ProductQuestionService questions) {
        this.questions = questions;
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<PageResponse<ProductQuestionResponse>> productQuestions(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(questions.productQuestions(productId, page, size));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ProductQuestionResponse>> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(questions.myQuestions(CurrentUser.id(jwt), page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductQuestionResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody CreateProductQuestionRequest request) {
        return ApiResponse.of(questions.create(CurrentUser.id(jwt), request));
    }
}
