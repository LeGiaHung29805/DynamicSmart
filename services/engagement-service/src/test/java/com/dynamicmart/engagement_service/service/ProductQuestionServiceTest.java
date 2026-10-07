package com.dynamicmart.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.dynamicmart.engagement_service.dto.request.AnswerProductQuestionRequest;
import com.dynamicmart.engagement_service.entity.ProductAnswer;
import com.dynamicmart.engagement_service.entity.ProductQuestion;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.ProductQuestionMapper;
import com.dynamicmart.engagement_service.repository.ProductAnswerRepository;
import com.dynamicmart.engagement_service.repository.ProductQuestionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductQuestionServiceTest {
    private final ProductQuestionRepository questions = org.mockito.Mockito.mock(ProductQuestionRepository.class);
    private final ProductAnswerRepository answers = org.mockito.Mockito.mock(ProductAnswerRepository.class);
    private final ProductQuestionService service = new ProductQuestionService(questions, answers, new ProductQuestionMapper());

    @Test
    void adminAnswerMarksQuestionAnswered() {
        UUID questionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ProductQuestion question = new ProductQuestion(UUID.randomUUID(), UUID.randomUUID(), "Còn hàng không?", Instant.now());
        ProductAnswer saved = new ProductAnswer(questionId, adminId, "Còn hàng.", Instant.now());
        when(questions.findById(questionId)).thenReturn(Optional.of(question));
        when(answers.save(any(ProductAnswer.class))).thenReturn(saved);
        when(answers.findAllByQuestionIdOrderByCreatedAtAsc(questionId)).thenReturn(List.of(saved));

        var response = service.answer(adminId, questionId, new AnswerProductQuestionRequest("Còn hàng."));

        assertThat(response.status()).isEqualTo("ANSWERED");
        assertThat(response.answers()).hasSize(1);
        assertThat(response.answers().get(0).adminId()).isEqualTo(adminId);
    }

    @Test
    void hiddenQuestionCannotBeAnswered() {
        UUID questionId = UUID.randomUUID();
        ProductQuestion question = new ProductQuestion(UUID.randomUUID(), UUID.randomUUID(), "Nội dung vi phạm", Instant.now());
        question.hide("Spam", Instant.now());
        when(questions.findById(questionId)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.answer(UUID.randomUUID(), questionId, new AnswerProductQuestionRequest("Không trả lời.")))
                .isInstanceOf(EngagementException.class)
                .hasMessageContaining("Không thể trả lời");
    }
}
