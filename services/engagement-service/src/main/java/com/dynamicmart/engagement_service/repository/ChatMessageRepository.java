package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    Optional<ChatMessage> findByIdempotencyKey(UUID idempotencyKey);

    List<ChatMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<ChatMessage> findAllByConversationIdInOrderByCreatedAtAsc(List<UUID> conversationIds);
}
