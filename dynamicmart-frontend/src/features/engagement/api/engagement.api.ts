import { apiClient } from "@/lib/api/client";
import type {
  AnswerProductQuestionPayload,
  BestSellerItem,
  ChatConversationItem,
  CreateProductQuestionPayload,
  CreateReviewPayload,
  DailySalesMetric,
  HideProductQuestionPayload,
  HideReviewPayload,
  NotificationItem,
  PageResponse,
  ProductQuestionItem,
  ReviewItem,
  ReviewSummary,
  WishlistResponse,
} from "../types/engagement.types";

export const engagementApi = {
  // P0 - Đánh giá sản phẩm (Reviews)
  reviews: {
    getByProduct: (productId: string, page = 0, size = 20) =>
      apiClient.get<PageResponse<ReviewItem>>(
        `/api/v1/reviews/products/${productId}?page=${page}&size=${size}`
      ),
    getSummary: (productId: string) =>
      apiClient.get<ReviewSummary>(`/api/v1/reviews/products/${productId}/summary`),
    create: (payload: CreateReviewPayload) =>
      apiClient.post<ReviewItem>("/api/v1/reviews", payload),
    getAdminReviews: (page = 0, size = 20) =>
      apiClient.get<PageResponse<ReviewItem>>(
        `/api/v1/admin/reviews?page=${page}&size=${size}`
      ),
    hideReview: (reviewId: string, payload: HideReviewPayload) =>
      apiClient.patch<ReviewItem>(`/api/v1/admin/reviews/${reviewId}/hide`, payload),
  },

  // P1 - Wishlist (Danh sách yêu thích)
  wishlist: {
    get: () => apiClient.get<WishlistResponse>("/api/v1/wishlists/me"),
    add: (productId: string) =>
      apiClient.post<WishlistResponse>("/api/v1/wishlists/me/items", { productId }),
    remove: (productId: string) =>
      apiClient.delete<void>(`/api/v1/wishlists/me/items/${productId}`),
  },

  // P1 - Hỏi đáp sản phẩm (Product Q&A)
  questions: {
    getByProduct: (productId: string, page = 0, size = 20) =>
      apiClient.get<PageResponse<ProductQuestionItem>>(
        `/api/v1/product-questions/products/${productId}?page=${page}&size=${size}`
      ),
    getMyQuestions: (page = 0, size = 20) =>
      apiClient.get<PageResponse<ProductQuestionItem>>(
        `/api/v1/product-questions/me?page=${page}&size=${size}`
      ),
    create: (payload: CreateProductQuestionPayload) =>
      apiClient.post<ProductQuestionItem>("/api/v1/product-questions", payload),
    getAdminQuestions: (page = 0, size = 20) =>
      apiClient.get<PageResponse<ProductQuestionItem>>(
        `/api/v1/admin/product-questions?page=${page}&size=${size}`
      ),
    answer: (questionId: string, payload: AnswerProductQuestionPayload) =>
      apiClient.post<ProductQuestionItem>(
        `/api/v1/admin/product-questions/${questionId}/answers`,
        payload
      ),
    hide: (questionId: string, payload: HideProductQuestionPayload) =>
      apiClient.patch<ProductQuestionItem>(
        `/api/v1/admin/product-questions/${questionId}/hide`,
        payload
      ),
  },

  // P1 - Thông báo (Notifications)
  notifications: {
    list: (page = 0, size = 20) =>
      apiClient.get<PageResponse<NotificationItem>>(
        `/api/v1/notifications?page=${page}&size=${size}`
      ),
    markRead: (notificationId: string) =>
      apiClient.patch<NotificationItem>(`/api/v1/notifications/${notificationId}/read`),
  },

  // P0 - Báo cáo doanh thu & Bán chạy (Reporting)
  reports: {
    sales: (from: string, to: string) =>
      apiClient.get<DailySalesMetric[]>(
        `/api/v1/reports/sales/daily?from=${from}&to=${to}`
      ),
    bestSellers: (from: string, to: string, limit = 10) =>
      apiClient.get<BestSellerItem[]>(
        `/api/v1/reports/products/best-sellers?from=${from}&to=${to}&limit=${limit}`
      ),
  },

  // P2 - Hỗ trợ khách hàng (Chat Support)
  support: {
    customerConversations: (page = 0, size = 20) =>
      apiClient.get<PageResponse<ChatConversationItem>>(
        `/api/v1/support/conversations?page=${page}&size=${size}`
      ),
    customerConversation: (conversationId: string) =>
      apiClient.get<ChatConversationItem>(
        `/api/v1/support/conversations/${conversationId}`
      ),
    createConversation: (content: string) =>
      apiClient.post<ChatConversationItem>("/api/v1/support/conversations", { content }),
    customerSend: (conversationId: string, content: string) =>
      apiClient.post<ChatConversationItem>(
        `/api/v1/support/conversations/${conversationId}/messages`,
        { content }
      ),
    adminConversations: (page = 0, size = 20) =>
      apiClient.get<PageResponse<ChatConversationItem>>(
        `/api/v1/admin/support/conversations?page=${page}&size=${size}`
      ),
    adminConversation: (conversationId: string) =>
      apiClient.get<ChatConversationItem>(
        `/api/v1/admin/support/conversations/${conversationId}`
      ),
    adminAssign: (conversationId: string) =>
      apiClient.patch<ChatConversationItem>(
        `/api/v1/admin/support/conversations/${conversationId}/assign`
      ),
    adminSend: (conversationId: string, content: string) =>
      apiClient.post<ChatConversationItem>(
        `/api/v1/admin/support/conversations/${conversationId}/messages`,
        { content }
      ),
    adminClose: (conversationId: string) =>
      apiClient.patch<ChatConversationItem>(
        `/api/v1/admin/support/conversations/${conversationId}/close`
      ),
  },
};
