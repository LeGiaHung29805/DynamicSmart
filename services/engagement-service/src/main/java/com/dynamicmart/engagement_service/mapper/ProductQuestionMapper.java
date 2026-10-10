package com.dynamicmart.engagement_service.mapper;

import com.dynamicmart.engagement_service.dto.response.ProductAnswerResponse;
import com.dynamicmart.engagement_service.dto.response.ProductQuestionResponse;
import com.dynamicmart.engagement_service.entity.ProductAnswer;
import com.dynamicmart.engagement_service.entity.ProductQuestion;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class ProductQuestionMapper {
    public ProductQuestionResponse toResponse(ProductQuestion question, List<ProductAnswer> answers) {
        return new ProductQuestionResponse(question.getId(), question.getProductId(), question.getCustomerId(),
                question.getContent(), question.getStatus(), question.getHiddenReason(),
                answers.stream().map(this::toAnswer).toList(), question.getCreatedAt(), question.getUpdatedAt());
    }

    public Map<UUID, List<ProductAnswer>> groupAnswers(List<ProductAnswer> answers) {
        return answers.stream().collect(Collectors.groupingBy(ProductAnswer::getQuestionId));
    }

    private ProductAnswerResponse toAnswer(ProductAnswer answer) {
        return new ProductAnswerResponse(answer.getId(), answer.getQuestionId(), answer.getAdminId(),
                answer.getContent(), answer.getCreatedAt());
    }
}
