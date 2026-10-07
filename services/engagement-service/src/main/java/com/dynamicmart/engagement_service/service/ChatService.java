package com.dynamicmart.engagement_service.service;

import com.dynamicmart.engagement_service.dto.request.CreateConversationRequest;
import com.dynamicmart.engagement_service.dto.request.SendChatMessageRequest;
import com.dynamicmart.engagement_service.dto.response.ChatConversationResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.entity.ChatConversation;
import com.dynamicmart.engagement_service.entity.ChatMessage;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.ChatMapper;
import com.dynamicmart.engagement_service.repository.ChatConversationRepository;
import com.dynamicmart.engagement_service.repository.ChatMessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final ChatMapper mapper;

    public ChatService(ChatConversationRepository conversations, ChatMessageRepository messages, ChatMapper mapper) {
        this.conversations = conversations;
        this.messages = messages;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatConversationResponse> customerConversations(UUID customerId, int page, int size) {
        var source = conversations.findAllByCustomerIdOrderByLastMessageAtDesc(customerId, PageRequest.of(page, size));
        return withMessages(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse customerConversation(UUID customerId, UUID conversationId) {
        ChatConversation conversation = conversations.findByIdAndCustomerId(conversationId, customerId)
                .orElseThrow(() -> new EngagementException(HttpStatus.NOT_FOUND,
                        "CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại."));
        return response(conversation);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatConversationResponse> adminConversations(int page, int size) {
        var source = conversations.findAllByOrderByLastMessageAtDesc(PageRequest.of(page, size));
        return withMessages(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse adminConversation(UUID conversationId) {
        return response(requireConversation(conversationId));
    }

    @Transactional
    public ChatConversationResponse create(UUID customerId, CreateConversationRequest request) {
        return create(customerId, request, null);
    }

    @Transactional
    public ChatConversationResponse create(UUID customerId, CreateConversationRequest request, UUID idempotencyKey) {
        UUID messageKey = messageKey(idempotencyKey);
        ChatMessage existing = existingMessage(messageKey, customerId, "CUSTOMER", null);
        if (existing != null) {
            ChatConversation conversation = conversations.findByIdAndCustomerId(existing.getConversationId(), customerId)
                    .orElseThrow(() -> idempotencyConflict());
            return response(conversation);
        }
        Instant now = Instant.now();
        ChatConversation conversation = conversations.save(new ChatConversation(customerId, now));
        ChatMessage firstMessage = messages.save(new ChatMessage(conversation.getId(), customerId,
                "CUSTOMER", request.content().trim(), messageKey, now));
        conversation.touchMessage(now);
        return mapper.toResponse(conversation, List.of(firstMessage));
    }

    @Transactional
    public ChatConversationResponse customerSend(UUID customerId, UUID conversationId, SendChatMessageRequest request) {
        return customerSend(customerId, conversationId, request, null);
    }

    @Transactional
    public ChatConversationResponse customerSend(UUID customerId, UUID conversationId, SendChatMessageRequest request,
                                                 UUID idempotencyKey) {
        ChatConversation conversation = conversations.findByIdAndCustomerId(conversationId, customerId)
                .orElseThrow(() -> new EngagementException(HttpStatus.NOT_FOUND,
                        "CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại."));
        return send(conversation, customerId, "CUSTOMER", request.content(), idempotencyKey);
    }

    @Transactional
    public ChatConversationResponse adminSend(UUID adminId, UUID conversationId, SendChatMessageRequest request) {
        return adminSend(adminId, conversationId, request, null);
    }

    @Transactional
    public ChatConversationResponse adminSend(UUID adminId, UUID conversationId, SendChatMessageRequest request,
                                              UUID idempotencyKey) {
        ChatConversation conversation = requireConversation(conversationId);
        if (conversation.getAssignedAdminId() == null) {
            conversation.assign(adminId, Instant.now());
        }
        return send(conversation, adminId, "ADMIN", request.content(), idempotencyKey);
    }

    @Transactional
    public ChatConversationResponse assignToSelf(UUID adminId, UUID conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        conversation.assign(adminId, Instant.now());
        return response(conversation);
    }

    @Transactional
    public ChatConversationResponse close(UUID conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        conversation.close(Instant.now());
        return response(conversation);
    }

    private ChatConversationResponse send(ChatConversation conversation, UUID senderId, String senderRole,
                                          String content, UUID idempotencyKey) {
        if ("CLOSED".equals(conversation.getStatus())) {
            throw new EngagementException(HttpStatus.CONFLICT, "CONVERSATION_CLOSED",
                    "Hội thoại đã đóng.");
        }
        UUID messageKey = messageKey(idempotencyKey);
        ChatMessage existing = existingMessage(messageKey, senderId, senderRole, conversation.getId());
        if (existing != null) {
            return response(conversation);
        }
        Instant now = Instant.now();
        messages.save(new ChatMessage(conversation.getId(), senderId, senderRole, content.trim(), messageKey, now));
        conversation.touchMessage(now);
        return response(conversation);
    }

    private UUID messageKey(UUID idempotencyKey) {
        return idempotencyKey == null ? UUID.randomUUID() : idempotencyKey;
    }

    private ChatMessage existingMessage(UUID idempotencyKey, UUID senderId, String senderRole, UUID conversationId) {
        ChatMessage existing = messages.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing == null) {
            return null;
        }
        boolean sameMessageIntent = Objects.equals(existing.getSenderId(), senderId)
                && Objects.equals(existing.getSenderRole(), senderRole)
                && (conversationId == null || Objects.equals(existing.getConversationId(), conversationId));
        if (!sameMessageIntent) {
            throw idempotencyConflict();
        }
        return existing;
    }

    private EngagementException idempotencyConflict() {
        return new EngagementException(HttpStatus.CONFLICT, "CHAT_IDEMPOTENCY_KEY_REUSED",
                "Idempotency-Key đã được dùng cho một tin nhắn khác.");
    }

    private ChatConversation requireConversation(UUID conversationId) {
        return conversations.findById(conversationId).orElseThrow(() ->
                new EngagementException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại."));
    }

    private ChatConversationResponse response(ChatConversation conversation) {
        return mapper.toResponse(conversation,
                messages.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId()));
    }

    private PageResponse<ChatConversationResponse> withMessages(List<ChatConversation> conversationList,
                                                                Pageable pageable,
                                                                long total) {
        List<UUID> conversationIds = conversationList.stream().map(ChatConversation::getId).toList();
        Map<UUID, List<ChatMessage>> grouped = conversationIds.isEmpty()
                ? Map.of()
                : mapper.groupMessages(messages.findAllByConversationIdInOrderByCreatedAtAsc(conversationIds));
        List<ChatConversationResponse> content = conversationList.stream()
                .map(conversation -> mapper.toResponse(conversation,
                        grouped.getOrDefault(conversation.getId(), List.of())))
                .toList();
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total,
                totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }
}
