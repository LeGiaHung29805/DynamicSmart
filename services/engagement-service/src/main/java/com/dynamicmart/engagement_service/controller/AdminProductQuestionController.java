package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.request.AnswerProductQuestionRequest;
import com.dynamicmart.engagement_service.dto.request.HideProductQuestionRequest;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.dto.response.ProductQuestionResponse;
import com.dynamicmart.engagement_service.service.ProductQuestionService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/product-questions")
public class AdminProductQuestionController {
    private final ProductQuestionService questions;

    public AdminProductQuestionController(ProductQuestionService questions) {
        this.questions = questions;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductQuestionResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(questions.adminQuestions(page, size));
    }

    @PostMapping("/{questionId}/answers")
    public ApiResponse<ProductQuestionResponse> answer(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable UUID questionId,
                                                        @Valid @RequestBody AnswerProductQuestionRequest request) {
        return ApiResponse.of(questions.answer(CurrentUser.id(jwt), questionId, request));
    }

    @PatchMapping("/{questionId}/hide")
    public ApiResponse<ProductQuestionResponse> hide(@PathVariable UUID questionId,
                                                      @Valid @RequestBody HideProductQuestionRequest request) {
        return ApiResponse.of(questions.hide(questionId, request));
    }
}
