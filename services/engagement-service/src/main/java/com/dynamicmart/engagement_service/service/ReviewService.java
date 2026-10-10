package com.dynamicmart.engagement_service.service;

import com.dynamicmart.engagement_service.client.CatalogRatingClient;
import com.dynamicmart.engagement_service.client.OrderReviewEligibilityClient;
import com.dynamicmart.engagement_service.dto.request.CreateReviewRequest;
import com.dynamicmart.engagement_service.dto.request.HideReviewRequest;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.dto.response.ReviewResponse;
import com.dynamicmart.engagement_service.dto.response.ReviewSummaryResponse;
import com.dynamicmart.engagement_service.entity.Review;
import com.dynamicmart.engagement_service.entity.ReviewImage;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.ReviewMapper;
import com.dynamicmart.engagement_service.messaging.producer.ReviewEventPublisher;
import com.dynamicmart.engagement_service.repository.ReviewImageRepository;
import com.dynamicmart.engagement_service.repository.ReviewRepository;
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
public class ReviewService {
    private final ReviewRepository reviews;
    private final ReviewImageRepository images;
    private final OrderReviewEligibilityClient eligibilityClient;
    private final CatalogRatingClient catalogRatingClient;
    private final ReviewEventPublisher reviewEvents;
    private final ReviewMapper mapper;

    public ReviewService(ReviewRepository reviews, ReviewImageRepository images,
                         OrderReviewEligibilityClient eligibilityClient, 
                         CatalogRatingClient catalogRatingClient,
                         ReviewEventPublisher reviewEvents,
                         ReviewMapper mapper) {
        this.reviews = reviews;
        this.images = images;
        this.eligibilityClient = eligibilityClient;
        this.catalogRatingClient = catalogRatingClient;
        this.reviewEvents = reviewEvents;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> productReviews(UUID productId, int page, int size) {
        var source = reviews.findAllByProductIdAndStatusOrderByCreatedAtDesc(productId, "VISIBLE",
                PageRequest.of(page, size));
        return withImages(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummaryByProduct(UUID productId) {
        List<Review> list = reviews.findAllByProductIdAndStatus(productId, "VISIBLE");
        long total = list.size();
        if (total == 0) {
            return new ReviewSummaryResponse(productId, 0.0, 0, Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
        }
        double avg = list.stream().mapToInt(Review::getRating).average().orElse(0.0);
        double roundedAvg = Math.round(avg * 10.0) / 10.0;
        Map<Integer, Long> breakdown = new java.util.HashMap<>();
        for (int i = 1; i <= 5; i++) {
            breakdown.put(i, 0L);
        }
        for (Review r : list) {
            int rating = r.getRating();
            breakdown.put(rating, breakdown.getOrDefault(rating, 0L) + 1);
        }
        return new ReviewSummaryResponse(productId, roundedAvg, total, breakdown);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> adminReviews(int page, int size) {
        var source = reviews.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return withImages(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> customerReviews(UUID customerId, int page, int size) {
        var source = reviews.findAllByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));
        return withImages(source.getContent(), source.getPageable(), source.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> customerProductReviews(UUID customerId, UUID productId) {
        List<Review> list = reviews.findAllByCustomerIdAndProductIdOrderByCreatedAtDesc(customerId, productId);
        if (list.isEmpty()) {
            return List.of();
        }
        List<UUID> reviewIds = list.stream().map(Review::getId).toList();
        Map<UUID, List<ReviewImage>> grouped = mapper.groupImages(images.findAllByReviewIdInOrderBySortOrderAsc(reviewIds));
        return list.stream()
                .map(review -> mapper.toResponse(review, grouped.getOrDefault(review.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ReviewResponse create(UUID customerId, CreateReviewRequest request) {
        if (reviews.existsByOrderItemId(request.orderItemId())) {
            throw new EngagementException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS",
                    "Dòng đơn hàng này đã được đánh giá.");
        }
        var eligibility = eligibilityClient.requireEligible(customerId, request.orderItemId());
        Instant now = Instant.now();
        Review review = reviews.save(new Review(request.orderItemId(), eligibility.orderId(), customerId,
                eligibility.productId(), eligibility.variantId(), request.rating(),
                normalizeContent(request.content()), now));
        List<ReviewImage> savedImages = saveImages(review.getId(), request.imageUrls(), now);
        
        try {
            ReviewSummaryResponse summary = getSummaryByProduct(eligibility.productId());
            catalogRatingClient.updateRating(eligibility.productId(), summary.averageRating(), (int) summary.totalReviews());
            reviewEvents.reviewCreated(review.getId(), eligibility.productId(), summary.averageRating(), summary.totalReviews());
        } catch (Exception ignored) { }
        
        return mapper.toResponse(review, savedImages);
    }

    @Transactional
    public ReviewResponse hide(UUID adminId, UUID reviewId, HideReviewRequest request) {
        Review review = reviews.findById(reviewId).orElseThrow(() ->
                new EngagementException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Không tìm thấy đánh giá."));
        review.hide(adminId, request.reason().trim(), Instant.now());
        
        try {
            ReviewSummaryResponse summary = getSummaryByProduct(review.getProductId());
            catalogRatingClient.updateRating(review.getProductId(), summary.averageRating(), (int) summary.totalReviews());
            reviewEvents.reviewHidden(review.getId(), review.getProductId(), summary.averageRating(), summary.totalReviews());
        } catch (Exception ignored) { }
        
        return mapper.toResponse(review, images.findAllByReviewIdOrderBySortOrderAsc(review.getId()));
    }

    private PageResponse<ReviewResponse> withImages(List<Review> reviewList, Pageable pageable, long total) {
        List<UUID> reviewIds = reviewList.stream().map(Review::getId).toList();
        Map<UUID, List<ReviewImage>> grouped = reviewIds.isEmpty()
                ? Map.of()
                : mapper.groupImages(images.findAllByReviewIdInOrderBySortOrderAsc(reviewIds));
        List<ReviewResponse> content = reviewList.stream()
                .map(review -> mapper.toResponse(review, grouped.getOrDefault(review.getId(), List.of())))
                .toList();
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), total,
                totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }

    private List<ReviewImage> saveImages(UUID reviewId, List<String> imageUrls, Instant now) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<String> cleanUrls = imageUrls.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .limit(5)
                .toList();
        List<ReviewImage> reviewImages = java.util.stream.IntStream.range(0, cleanUrls.size())
                .mapToObj(index -> new ReviewImage(reviewId, cleanUrls.get(index), index, now))
                .toList();
        return images.saveAll(reviewImages);
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.trim();
    }
}
