package com.dynamicmart.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.engagement_service.dto.request.SendChatMessageRequest;
import com.dynamicmart.engagement_service.entity.ChatConversation;
import com.dynamicmart.engagement_service.entity.ChatMessage;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.ChatMapper;
import com.dynamicmart.engagement_service.repository.ChatConversationRepository;
import com.dynamicmart.engagement_service.repository.ChatMessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatServiceTest {
    private final ChatConversationRepository conversations = org.mockito.Mockito.mock(ChatConversationRepository.class);
    private final ChatMessageRepository messages = org.mockito.Mockito.mock(ChatMessageRepository.class);
    private final ChatService service = new ChatService(conversations, messages, new ChatMapper());

    @Test
    void customerCannotSendToAnotherCustomersConversation() {
        UUID customerId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(conversations.findByIdAndCustomerId(conversationId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.customerSend(customerId, conversationId, new SendChatMessageRequest("Xin hỗ trợ")))
                .isInstanceOf(EngagementException.class)
                .hasMessageContaining("Không tìm thấy hội thoại");
    }

    @Test
    void adminSendAssignsConversationAndStoresAdminMessage() {
        UUID adminId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation(UUID.randomUUID(), Instant.now());
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId())).thenReturn(List.of());

        var response = service.adminSend(adminId, conversationId, new SendChatMessageRequest("Mình đang kiểm tra."));

        assertThat(response.assignedAdminId()).isEqualTo(adminId);
        ArgumentCaptor<ChatMessage> message = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages).save(message.capture());
        assertThat(message.getValue().getSenderRole()).isEqualTo("ADMIN");
        assertThat(message.getValue().getSenderId()).isEqualTo(adminId);
    }
}
