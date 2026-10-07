package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.ChatConversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {
    Page<ChatConversation> findAllByCustomerIdOrderByLastMessageAtDesc(UUID customerId, Pageable pageable);

    Page<ChatConversation> findAllByOrderByLastMessageAtDesc(Pageable pageable);

    Optional<ChatConversation> findByIdAndCustomerId(UUID id, UUID customerId);
}
