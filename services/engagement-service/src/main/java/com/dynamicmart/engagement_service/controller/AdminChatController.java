package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.request.SendChatMessageRequest;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.ChatConversationResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.service.ChatService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/support/conversations")
public class AdminChatController {
    private final ChatService chat;

    public AdminChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    public ApiResponse<PageResponse<ChatConversationResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(chat.adminConversations(page, size));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ChatConversationResponse> detail(@PathVariable UUID conversationId) {
        return ApiResponse.of(chat.adminConversation(conversationId));
    }

    @PatchMapping("/{conversationId}/assign")
    public ApiResponse<ChatConversationResponse> assign(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable UUID conversationId) {
        return ApiResponse.of(chat.assignToSelf(CurrentUser.id(jwt), conversationId));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ChatConversationResponse> send(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID conversationId,
                                                       @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
                                                       @Valid @RequestBody SendChatMessageRequest request) {
        return ApiResponse.of(chat.adminSend(CurrentUser.id(jwt), conversationId, request, idempotencyKey));
    }

    @PatchMapping("/{conversationId}/close")
    public ApiResponse<ChatConversationResponse> close(@PathVariable UUID conversationId) {
        return ApiResponse.of(chat.close(conversationId));
    }
}
