export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type ReviewStatus = "VISIBLE" | "HIDDEN";

export interface ReviewItem {
  id: string;
  orderItemId: string;
  orderId: string;
  customerId: string;
  customerName?: string;
  productId: string;
  variantId?: string;
  rating: number; // 1 to 5
  content?: string;
  status: ReviewStatus;
  hiddenReason?: string;
  imageUrls?: string[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreateReviewPayload {
  orderItemId: string;
  rating: number;
  content?: string;
  imageUrls?: string[];
}

export interface HideReviewPayload {
  reason: string;
}

export interface ReviewSummary {
  productId: string;
  averageRating: number;
  totalReviews: number;
  ratingBreakdown: Record<number, number>;
}

export interface WishlistItem {
  id: string;
  productId: string;
  createdAt: string;
}

export interface WishlistResponse {
  id: string;
  customerId: string;
  items: WishlistItem[];
  updatedAt: string;
}

export type QuestionStatus = "OPEN" | "ANSWERED" | "HIDDEN";

export interface ProductAnswerItem {
  id: string;
  questionId: string;
  adminId: string;
  content: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ProductQuestionItem {
  id: string;
  productId: string;
  customerId: string;
  content: string;
  status: QuestionStatus;
  hiddenReason?: string;
  answers: ProductAnswerItem[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreateProductQuestionPayload {
  productId: string;
  content: string;
}

export interface AnswerProductQuestionPayload {
  content: string;
}

export interface HideProductQuestionPayload {
  reason: string;
}

export interface NotificationItem {
  id: string;
  customerId?: string;
  type: string;
  title: string;
  content: string;
  read?: boolean;
  readAt?: string | null;
  createdAt: string;
}

export interface DailySalesMetric {
  metricDate: string;
  grossItemSalesVnd: number;
  discountValueVnd: number;
  shippingFeeVnd: number;
  netRevenueVnd: number;
  orderCount: number;
  completedOrderCount: number;
  updatedAt: string;
}

export interface BestSellerItem {
  productId: string;
  variantId?: string;
  quantitySold: number;
  grossSalesVnd: number;
  netItemSalesVnd: number;
}

export type ChatRole = "CUSTOMER" | "ADMIN";
export type ChatStatus = "OPEN" | "CLOSED";

export interface ChatMessageItem {
  id: string;
  conversationId: string;
  senderId: string;
  senderRole: ChatRole;
  content: string;
  readAt?: string | null;
  createdAt: string;
}

export interface ChatConversationItem {
  id: string;
  customerId: string;
  assignedAdminId?: string | null;
  status: ChatStatus;
  messages: ChatMessageItem[];
  lastMessageAt?: string | null;
  createdAt: string;
  updatedAt?: string;
}
