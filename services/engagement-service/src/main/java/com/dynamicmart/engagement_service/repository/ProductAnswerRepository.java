package com.dynamicmart.engagement_service.repository;

import com.dynamicmart.engagement_service.entity.ProductAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAnswerRepository extends JpaRepository<ProductAnswer, UUID> {
    List<ProductAnswer> findAllByQuestionIdOrderByCreatedAtAsc(UUID questionId);

    List<ProductAnswer> findAllByQuestionIdInOrderByCreatedAtAsc(List<UUID> questionIds);
}
