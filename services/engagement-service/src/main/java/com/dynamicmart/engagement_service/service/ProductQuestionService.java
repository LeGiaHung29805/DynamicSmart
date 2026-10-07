package com.dynamicmart.engagement_service.service;

import com.dynamicmart.engagement_service.dto.request.AnswerProductQuestionRequest;
import com.dynamicmart.engagement_service.dto.request.CreateProductQuestionRequest;
import com.dynamicmart.engagement_service.dto.request.HideProductQuestionRequest;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.dto.response.ProductQuestionResponse;
import com.dynamicmart.engagement_service.entity.ProductAnswer;
import com.dynamicmart.engagement_service.entity.ProductQuestion;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.ProductQuestionMapper;
import com.dynamicmart.engagement_service.repository.ProductAnswerRepository;
import com.dynamicmart.engagement_service.repository.ProductQuestionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQuestionService {
    private final ProductQuestionRepository questions;
    private final ProductAnswerRepository answers;
    private final ProductQuestionMapper mapper;

    public ProductQuestionService(ProductQuestionRepository questions, ProductAnswerRepository answers,
                                  ProductQuestionMapper mapper) {
        this.questions = questions;
        this.answers = answers;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductQuestionResponse> productQuestions(UUID productId, int page, int size) {
        var source = questions.findAllByProductIdAndStatusInOrderByCreatedAtDesc(productId,
                List.of("OPEN", "ANSWERED"), PageRequest.of(page, size));
        return withAnswers(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductQuestionResponse> myQuestions(UUID customerId, int page, int size) {
        var source = questions.findAllByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));
        return withAnswers(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductQuestionResponse> adminQuestions(int page, int size) {
        var source = questions.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return withAnswers(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional
    public ProductQuestionResponse create(UUID customerId, CreateProductQuestionRequest request) {
        Instant now = Instant.now();
        ProductQuestion question = questions.save(new ProductQuestion(request.productId(), customerId,
                request.content().trim(), now));
        return mapper.toResponse(question, List.of());
    }

    @Transactional
    public ProductQuestionResponse answer(UUID adminId, UUID questionId, AnswerProductQuestionRequest request) {
        ProductQuestion question = requireQuestion(questionId);
        if ("HIDDEN".equals(question.getStatus())) {
            throw new EngagementException(HttpStatus.CONFLICT, "QUESTION_HIDDEN",
                    "Không thể trả lời câu hỏi đã bị ẩn.");
        }
        Instant now = Instant.now();
        ProductAnswer answer = answers.save(new ProductAnswer(questionId, adminId, request.content().trim(), now));
        question.markAnswered(now);
        List<ProductAnswer> allAnswers = answers.findAllByQuestionIdOrderByCreatedAtAsc(questionId);
        if (allAnswers.stream().noneMatch(value -> value.getId().equals(answer.getId()))) {
            allAnswers = new java.util.ArrayList<>(allAnswers);
            allAnswers.add(answer);
        }
        return mapper.toResponse(question, allAnswers);
    }

    @Transactional
    public ProductQuestionResponse hide(UUID questionId, HideProductQuestionRequest request) {
        ProductQuestion question = requireQuestion(questionId);
        question.hide(request.reason().trim(), Instant.now());
        return mapper.toResponse(question, answers.findAllByQuestionIdOrderByCreatedAtAsc(questionId));
    }

    private ProductQuestion requireQuestion(UUID questionId) {
        return questions.findById(questionId).orElseThrow(() ->
                new EngagementException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Không tìm thấy câu hỏi."));
    }

    private PageResponse<ProductQuestionResponse> withAnswers(List<ProductQuestion> questionList,
                                                              Pageable pageable,
                                                              long total) {
        List<UUID> questionIds = questionList.stream().map(ProductQuestion::getId).toList();
        Map<UUID, List<ProductAnswer>> grouped = questionIds.isEmpty()
                ? Map.of()
                : mapper.groupAnswers(answers.findAllByQuestionIdInOrderByCreatedAtAsc(questionIds));
        List<ProductQuestionResponse> content = questionList.stream()
                .map(question -> mapper.toResponse(question, grouped.getOrDefault(question.getId(), List.of())))
                .toList();
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total,
                totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }
}
