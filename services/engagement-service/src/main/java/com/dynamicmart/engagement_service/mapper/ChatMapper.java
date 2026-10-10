package com.dynamicmart.engagement_service.mapper;

import com.dynamicmart.engagement_service.dto.response.ChatConversationResponse;
import com.dynamicmart.engagement_service.dto.response.ChatMessageResponse;
import com.dynamicmart.engagement_service.entity.ChatConversation;
import com.dynamicmart.engagement_service.entity.ChatMessage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class ChatMapper {
    public ChatConversationResponse toResponse(ChatConversation conversation, List<ChatMessage> messages) {
        return new ChatConversationResponse(conversation.getId(), conversation.getCustomerId(),
                conversation.getAssignedAdminId(), conversation.getStatus(), conversation.getLastMessageAt(),
                conversation.getCreatedAt(), conversation.getUpdatedAt(),
                messages.stream().map(this::toMessage).toList());
    }

    public Map<UUID, List<ChatMessage>> groupMessages(List<ChatMessage> messages) {
        return messages.stream().collect(Collectors.groupingBy(ChatMessage::getConversationId));
    }

    private ChatMessageResponse toMessage(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getSenderRole(), message.getContent(),
                message.getIdempotencyKey(), message.getReadAt(), message.getCreatedAt());
    }
}
