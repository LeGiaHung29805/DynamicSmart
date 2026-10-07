package com.dynamicmart.engagement_service.mapper;

import com.dynamicmart.engagement_service.dto.response.ReviewResponse;
import com.dynamicmart.engagement_service.entity.Review;
import com.dynamicmart.engagement_service.entity.ReviewImage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class ReviewMapper {
    public ReviewResponse toResponse(Review review, List<ReviewImage> images) {
        return new ReviewResponse(review.getId(), review.getOrderItemId(), review.getOrderId(),
                review.getCustomerId(), review.getProductId(), review.getVariantId(), review.getRating(),
                review.getContent() == null ? null : HtmlUtils.htmlEscape(review.getContent()),
                review.getStatus(), review.getHiddenReason(),
                images.stream().map(ReviewImage::getImageUrl).toList(),
                review.getCreatedAt(), review.getUpdatedAt());
    }

    public Map<UUID, List<ReviewImage>> groupImages(List<ReviewImage> images) {
        return images.stream().collect(Collectors.groupingBy(ReviewImage::getReviewId));
    }
}
