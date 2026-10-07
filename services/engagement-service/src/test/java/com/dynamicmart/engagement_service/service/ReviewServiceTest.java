package com.dynamicmart.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.engagement_service.client.CatalogRatingClient;
import com.dynamicmart.engagement_service.client.OrderReviewEligibilityClient;
import com.dynamicmart.engagement_service.client.OrderReviewEligibilityClient.ReviewEligibility;
import com.dynamicmart.engagement_service.dto.request.CreateReviewRequest;
import com.dynamicmart.engagement_service.dto.request.HideReviewRequest;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.dto.response.ReviewResponse;
import com.dynamicmart.engagement_service.entity.Review;
import com.dynamicmart.engagement_service.entity.ReviewImage;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.ReviewMapper;
import com.dynamicmart.engagement_service.messaging.producer.ReviewEventPublisher;
import com.dynamicmart.engagement_service.repository.ReviewImageRepository;
import com.dynamicmart.engagement_service.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.HtmlUtils;

class ReviewServiceTest {

    private final ReviewRepository reviews = org.mockito.Mockito.mock(ReviewRepository.class);
    private final ReviewImageRepository images = org.mockito.Mockito.mock(ReviewImageRepository.class);
    private final OrderReviewEligibilityClient eligibilityClient = org.mockito.Mockito.mock(OrderReviewEligibilityClient.class);
    private final CatalogRatingClient catalogRatingClient = org.mockito.Mockito.mock(CatalogRatingClient.class);
    private final ReviewEventPublisher reviewEvents = org.mockito.Mockito.mock(ReviewEventPublisher.class);
    private final ReviewMapper mapper = new ReviewMapper();

    private final ReviewService service = new ReviewService(reviews, images, eligibilityClient, catalogRatingClient, reviewEvents, mapper);

    @BeforeEach
    void setUp() {
        when(reviews.save(any(Review.class))).then(returnsFirstArg());
        when(images.saveAll(anyList())).then(returnsFirstArg());
    }

    @Test
    @DisplayName("Tạo đánh giá thành công khi khách hàng và dòng đơn hàng đủ điều kiện")
    void createReviewSucceedsWhenEligible() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        when(reviews.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(eligibilityClient.requireEligible(customerId, orderItemId)).thenReturn(
                new ReviewEligibility(true, orderId, orderItemId, customerId, productId, variantId, null)
        );

        CreateReviewRequest request = new CreateReviewRequest(
                orderItemId,
                (short) 5,
                "Sản phẩm rất đẹp và chất lượng!",
                List.of("https://cdn.example.com/img1.jpg", "https://cdn.example.com/img2.jpg")
        );

        ReviewResponse response = service.create(customerId, request);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo((short) 5);
        assertThat(response.content()).isEqualTo(HtmlUtils.htmlEscape("Sản phẩm rất đẹp và chất lượng!"));
        assertThat(response.status()).isEqualTo("VISIBLE");
        assertThat(response.orderItemId()).isEqualTo(orderItemId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.variantId()).isEqualTo(variantId);
        assertThat(response.imageUrls()).containsExactly(
                "https://cdn.example.com/img1.jpg",
                "https://cdn.example.com/img2.jpg"
        );

        verify(reviews).save(any(Review.class));
        verify(images).saveAll(anyList());
    }

    @Test
    @DisplayName("Từ chối tạo đánh giá nếu dòng đơn hàng đã được đánh giá trước đó (trùng lặp)")
    void createReviewRejectsDuplicateOrderItem() {
        UUID customerId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        when(reviews.existsByOrderItemId(orderItemId)).thenReturn(true);

        CreateReviewRequest request = new CreateReviewRequest(
                orderItemId,
                (short) 4,
                "Đánh giá lại",
                List.of()
        );

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(EngagementException.class)
                .satisfies(ex -> {
                    EngagementException ee = (EngagementException) ex;
                    assertThat(ee.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ee.getCode()).isEqualTo("REVIEW_ALREADY_EXISTS");
                });

        verify(eligibilityClient, never()).requireEligible(any(), any());
        verify(reviews, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Từ chối tạo đánh giá nếu Order Service báo chưa đủ điều kiện (chưa hoàn tất hoặc không sở hữu)")
    void createReviewRejectsWhenOrderNotEligible() {
        UUID customerId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        when(reviews.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(eligibilityClient.requireEligible(customerId, orderItemId)).thenThrow(
                new EngagementException(HttpStatus.UNPROCESSABLE_ENTITY, "REVIEW_NOT_ELIGIBLE",
                        "Đơn hàng chưa hoàn tất hoặc bạn không sở hữu đơn hàng này.")
        );

        CreateReviewRequest request = new CreateReviewRequest(
                orderItemId,
                (short) 5,
                "Nhận xét",
                List.of()
        );

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(EngagementException.class)
                .satisfies(ex -> {
                    EngagementException ee = (EngagementException) ex;
                    assertThat(ee.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(ee.getCode()).isEqualTo("REVIEW_NOT_ELIGIBLE");
                });

        verify(reviews, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Chuẩn hóa nội dung nhận xét và giới hạn tối đa 5 hình ảnh hợp lệ")
    void createReviewNormalizesContentAndLimitsImagesToFive() {
        UUID customerId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        when(reviews.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(eligibilityClient.requireEligible(customerId, orderItemId)).thenReturn(
                new ReviewEligibility(true, UUID.randomUUID(), orderItemId, customerId,
                        UUID.randomUUID(), UUID.randomUUID(), null)
        );

        CreateReviewRequest request = new CreateReviewRequest(
                orderItemId,
                (short) 5,
                "   Nội dung có khoảng trắng thừa hai đầu   ",
                List.of(
                        "https://cdn.example.com/1.jpg",
                        "  https://cdn.example.com/2.jpg  ",
                        "",
                        "https://cdn.example.com/3.jpg",
                        "https://cdn.example.com/4.jpg",
                        "https://cdn.example.com/5.jpg",
                        "https://cdn.example.com/6.jpg" // ảnh thứ 6 vượt quá giới hạn 5
                )
        );

        ReviewResponse response = service.create(customerId, request);

        assertThat(response.content()).isEqualTo(HtmlUtils.htmlEscape("Nội dung có khoảng trắng thừa hai đầu"));
        assertThat(response.imageUrls()).hasSize(5);
        assertThat(response.imageUrls()).containsExactly(
                "https://cdn.example.com/1.jpg",
                "https://cdn.example.com/2.jpg",
                "https://cdn.example.com/3.jpg",
                "https://cdn.example.com/4.jpg",
                "https://cdn.example.com/5.jpg"
        );
    }

    @Test
    @DisplayName("Admin ẩn đánh giá thành công kèm lý do kiểm duyệt và lưu audit")
    void hideReviewSucceedsWithAdminAuditReason() {
        UUID adminId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        Instant now = Instant.now();

        Review existingReview = new Review(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), (short) 1,
                "Nội dung chứa từ ngữ thô tục", now
        );
        when(reviews.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(images.findAllByReviewIdOrderBySortOrderAsc(existingReview.getId())).thenReturn(List.of());

        HideReviewRequest hideRequest = new HideReviewRequest("  Nội dung vi phạm tiêu chuẩn cộng đồng  ");

        ReviewResponse response = service.hide(adminId, reviewId, hideRequest);

        assertThat(response.status()).isEqualTo("HIDDEN");
        assertThat(response.hiddenReason()).isEqualTo("Nội dung vi phạm tiêu chuẩn cộng đồng");
        assertThat(existingReview.getHiddenBy()).isEqualTo(adminId);
        assertThat(existingReview.getHiddenAt()).isNotNull();
    }

    @Test
    @DisplayName("Ẩn đánh giá thất bại khi không tìm thấy review")
    void hideReviewThrowsNotFoundWhenReviewDoesNotExist() {
        UUID adminId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        when(reviews.findById(reviewId)).thenReturn(Optional.empty());

        HideReviewRequest hideRequest = new HideReviewRequest("Lý do");

        assertThatThrownBy(() -> service.hide(adminId, reviewId, hideRequest))
                .isInstanceOf(EngagementException.class)
                .satisfies(ex -> {
                    EngagementException ee = (EngagementException) ex;
                    assertThat(ee.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ee.getCode()).isEqualTo("REVIEW_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("Lấy danh sách đánh giá của sản phẩm chỉ trả về các đánh giá VISIBLE có phân trang")
    void productReviewsReturnsOnlyVisibleReviewsPaginated() {
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        Review review1 = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                productId, UUID.randomUUID(), (short) 5, "Rất tốt", now);
        Review review2 = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                productId, UUID.randomUUID(), (short) 4, "Tốt", now);

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(reviews.findAllByProductIdAndStatusOrderByCreatedAtDesc(productId, "VISIBLE", pageRequest))
                .thenReturn(new PageImpl<>(List.of(review1, review2), pageRequest, 2));

        when(images.findAllByReviewIdInOrderBySortOrderAsc(anyList())).thenReturn(List.of());

        PageResponse<ReviewResponse> response = service.productReviews(productId, 0, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).allMatch(r -> "VISIBLE".equals(r.status()));

        verify(reviews).findAllByProductIdAndStatusOrderByCreatedAtDesc(productId, "VISIBLE", pageRequest);
    }

    @Test
    @DisplayName("Admin xem toàn bộ danh sách đánh giá bao gồm cả đánh giá bị ẩn")
    void adminReviewsReturnsAllReviewsPaginated() {
        Instant now = Instant.now();

        Review visibleReview = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), (short) 5, "Tốt", now);
        Review hiddenReview = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), (short) 1, "Spam", now);
        hiddenReview.hide(UUID.randomUUID(), "Spam", now);

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(reviews.findAllByOrderByCreatedAtDesc(pageRequest))
                .thenReturn(new PageImpl<>(List.of(visibleReview, hiddenReview), pageRequest, 2));

        when(images.findAllByReviewIdInOrderBySortOrderAsc(anyList())).thenReturn(List.of());

        PageResponse<ReviewResponse> response = service.adminReviews(0, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content().get(0).status()).isEqualTo("VISIBLE");
        assertThat(response.content().get(1).status()).isEqualTo("HIDDEN");

        verify(reviews).findAllByOrderByCreatedAtDesc(pageRequest);
    }

    @Test
    @DisplayName("Lấy tóm tắt đánh giá trả về 0 khi sản phẩm chưa có đánh giá nào")
    void getSummaryByProductReturnsZeroWhenNoReviews() {
        UUID productId = UUID.randomUUID();
        when(reviews.findAllByProductIdAndStatus(productId, "VISIBLE")).thenReturn(List.of());

        var summary = service.getSummaryByProduct(productId);

        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.totalReviews()).isEqualTo(0);
        assertThat(summary.averageRating()).isEqualTo(0.0);
        assertThat(summary.ratingBreakdown().get(5)).isEqualTo(0);
    }

    @Test
    @DisplayName("Lấy tóm tắt đánh giá tính đúng điểm trung bình và phân bố sao")
    void getSummaryByProductCalculatesAverageAndBreakdown() {
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        Review r1 = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                productId, UUID.randomUUID(), (short) 5, "Xuất sắc", now);
        Review r2 = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                productId, UUID.randomUUID(), (short) 4, "Rất tốt", now);

        when(reviews.findAllByProductIdAndStatus(productId, "VISIBLE")).thenReturn(List.of(r1, r2));

        var summary = service.getSummaryByProduct(productId);

        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.totalReviews()).isEqualTo(2);
        assertThat(summary.averageRating()).isEqualTo(4.5);
        assertThat(summary.ratingBreakdown().get(5)).isEqualTo(1);
        assertThat(summary.ratingBreakdown().get(4)).isEqualTo(1);
        assertThat(summary.ratingBreakdown().get(3)).isEqualTo(0);
    }
}
