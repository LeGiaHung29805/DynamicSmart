package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.request.CreateConversationRequest;
import com.dynamicmart.engagement_service.dto.request.SendChatMessageRequest;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.ChatConversationResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.service.ChatService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/support/conversations")
public class ChatController {
    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    public ApiResponse<PageResponse<ChatConversationResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(chat.customerConversations(CurrentUser.id(jwt), page, size));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ChatConversationResponse> detail(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable UUID conversationId) {
        return ApiResponse.of(chat.customerConversation(CurrentUser.id(jwt), conversationId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatConversationResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
                                                         @Valid @RequestBody CreateConversationRequest request) {
        return ApiResponse.of(chat.create(CurrentUser.id(jwt), request, idempotencyKey));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ChatConversationResponse> send(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID conversationId,
                                                       @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
                                                       @Valid @RequestBody SendChatMessageRequest request) {
        return ApiResponse.of(chat.customerSend(CurrentUser.id(jwt), conversationId, request, idempotencyKey));
    }
}
