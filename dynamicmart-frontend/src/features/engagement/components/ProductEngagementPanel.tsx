"use client";

import { CheckCircle2, MessageSquarePlus, MessageSquareText, PenLine, Send, Star, X } from "lucide-react";
import { useEffect, useState } from "react";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { engagementApi } from "../api/engagement.api";
import type { ProductQuestionItem, ReviewItem } from "../types/engagement.types";
import { CreateReviewModal } from "./CreateReviewModal";

export interface ProductEngagementPanelProps {
  productId?: string;
  productName?: string;
}

export function ProductEngagementPanel({
  productId,
  productName,
}: Readonly<ProductEngagementPanelProps>) {
  const [reviews, setReviews] = useState<ReviewItem[]>([]);
  const [questions, setQuestions] = useState<ProductQuestionItem[]>([]);
  const [loadError, setLoadError] = useState("");

  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [isQuestionModalOpen, setIsQuestionModalOpen] = useState(false);
  const [questionContent, setQuestionContent] = useState("");
  const [submittingQuestion, setSubmittingQuestion] = useState(false);
  const [questionSuccess, setQuestionSuccess] = useState(false);
  const [questionError, setQuestionError] = useState("");

  // Lấy review thật và hỏi đáp thật theo productId nếu có
  useEffect(() => {
    if (!productId) return;
    Promise.all([
      engagementApi.reviews.getByProduct(productId, 0, 10),
      engagementApi.questions.getByProduct(productId, 0, 10),
    ]).then(([reviewResult, questionResult]) => {
      setLoadError("");
      setReviews(reviewResult.content ?? []);
      setQuestions(questionResult.content ?? []);
    }).catch(() => setLoadError("Không thể tải đánh giá và hỏi đáp. Vui lòng thử lại sau."));
  }, [productId]);

  const handleReviewSuccess = (newReview: ReviewItem) => {
    setReviews((prev) => [newReview, ...prev]);
  };

  const handleSubmitQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!questionContent.trim()) return;
    if (!productId) {
      setQuestionError("Không xác định được mã sản phẩm.");
      return;
    }

    setSubmittingQuestion(true);
    setQuestionError("");
    try {
      const created = await engagementApi.questions.create({
        productId,
        content: questionContent.trim(),
      });
      setQuestions((prev) => [created, ...prev]);
      setQuestionSuccess(true);
      setTimeout(() => {
        setQuestionSuccess(false);
        setIsQuestionModalOpen(false);
        setQuestionContent("");
      }, 1200);
    } catch {
      setQuestionError("Không thể gửi câu hỏi. Vui lòng đăng nhập và thử lại.");
    } finally {
      setSubmittingQuestion(false);
    }
  };

  // Tính điểm đánh giá trung bình
  const avgRating =
    reviews.length > 0
      ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)
      : "0.0";

  return (
    <SurfacePanel className="h-fit">
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div>
          <p className="text-xs font-bold tracking-wider text-brand uppercase">
            Đánh giá & Hỏi đáp
          </p>
          <div className="mt-1 flex items-center gap-2">
            <h2 className="text-lg font-black text-slate-950">Phản hồi từ khách hàng</h2>
            <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-xs font-bold text-amber-700">
              <Star className="size-3.5 fill-amber-400 text-amber-500" />
              {avgRating} ({reviews.length})
            </span>
          </div>
        </div>
        <MessageSquareText className="size-6 text-brand" />
      </div>

      <div className="mt-5 space-y-4">
        {loadError ? <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{loadError}</p> : null}
        {/* Nút hành động */}
        <div className="flex gap-2">
          <Button
            className="flex-1"
            size="sm"
            onClick={() => setIsReviewModalOpen(true)}
          >
            <PenLine className="size-4" />
            Viết đánh giá
          </Button>
          <Button
            className="flex-1"
            size="sm"
            variant="outline"
            onClick={() => setIsQuestionModalOpen(true)}
          >
            <MessageSquarePlus className="size-4" />
            Gửi câu hỏi
          </Button>
        </div>

        {/* Danh sách review */}
        <div className="space-y-3">
          <h3 className="text-xs font-bold tracking-wider text-slate-400 uppercase">
            Đánh giá gần đây
          </h3>
          {reviews.length === 0 ? (
            <p className="text-sm text-slate-500">Chưa có đánh giá nào cho sản phẩm này.</p>
          ) : (
            reviews.map((review) => (
              <article
                className="rounded-xl border border-slate-200 bg-white p-3.5"
                key={review.id}
              >
                <div className="flex items-center justify-between gap-3">
                  <p className="font-bold text-slate-950">
                    {review.customerName || `Khách #${review.customerId.slice(0, 6)}`}
                  </p>
                  <span className="inline-flex items-center gap-1 text-sm font-bold text-amber-600">
                    <Star className="size-4 fill-current" />
                    {review.rating}/5
                  </span>
                </div>
                {review.content ? (
                  <p className="mt-2 text-sm leading-6 text-slate-600">{review.content}</p>
                ) : null}
                {review.imageUrls && review.imageUrls.length > 0 ? (
                  <div className="mt-2 flex flex-wrap gap-2">
                    {review.imageUrls.map((url) => (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        alt="Ảnh feedback"
                        className="size-12 rounded-lg border border-slate-200 object-cover"
                        key={url}
                        src={url}
                      />
                    ))}
                  </div>
                ) : null}
              </article>
            ))
          )}
        </div>

        {/* Danh sách hỏi đáp */}
        <div className="space-y-3 border-t border-slate-100 pt-4">
          <h3 className="text-xs font-bold tracking-wider text-slate-400 uppercase">
            Hỏi đáp về sản phẩm ({questions.length})
          </h3>
          {questions.length === 0 ? (
            <p className="text-sm text-slate-500">Chưa có câu hỏi nào. Hãy là người đầu tiên đặt câu hỏi!</p>
          ) : (
            questions.map((question) => (
              <article className="rounded-xl border border-slate-200 bg-white p-3.5" key={question.id}>
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm font-bold text-slate-950">{question.content}</p>
                  {question.answers && question.answers.length > 0 ? (
                    <span className="shrink-0 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700">
                      Đã trả lời
                    </span>
                  ) : (
                    <span className="shrink-0 rounded-full bg-amber-50 px-2 py-0.5 text-[10px] font-bold text-amber-700">
                      Chờ trả lời
                    </span>
                  )}
                </div>

                {question.answers && question.answers.length > 0 ? (
                  <div className="mt-2.5 rounded-lg border-l-2 border-brand bg-slate-50 p-2.5 text-xs text-slate-700">
                    <p className="font-bold text-brand mb-0.5">Phản hồi từ DynamicMart:</p>
                    <p className="leading-relaxed">{question.answers[0].content}</p>
                  </div>
                ) : (
                  <p className="mt-1.5 text-xs font-medium text-slate-400">
                    Đang chờ quản trị viên DynamicMart phản hồi...
                  </p>
                )}
              </article>
            ))
          )}
        </div>
      </div>

      {/* Modal viết review */}
      <CreateReviewModal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        onSuccess={handleReviewSuccess}
        productName={productName}
      />

      {/* Modal gửi câu hỏi */}
      {isQuestionModalOpen ? (
        <div
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm"
          role="dialog"
        >
          <div className="relative w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
            <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-3">
              <div>
                <p className="text-xs font-bold tracking-wider text-brand uppercase">Hỏi đáp sản phẩm</p>
                <h3 className="mt-1 text-lg font-black text-slate-950">Đặt câu hỏi cho DynamicMart</h3>
              </div>
              <button
                aria-label="Đóng"
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                onClick={() => setIsQuestionModalOpen(false)}
                type="button"
              >
                <X className="size-5" />
              </button>
            </div>

            {questionSuccess ? (
              <div className="py-8 text-center">
                <CheckCircle2 className="mx-auto size-12 text-emerald-600" />
                <h4 className="mt-3 text-base font-bold text-slate-950">Gửi câu hỏi thành công!</h4>
                <p className="mt-1 text-xs text-slate-500">
                  Câu hỏi của bạn đã được gửi tới ban quản trị và sẽ sớm được giải đáp.
                </p>
              </div>
            ) : (
              <form className="mt-4 space-y-4" onSubmit={handleSubmitQuestion}>
                {questionError ? (
                  <p className="rounded-lg bg-rose-50 p-2.5 text-xs text-rose-700">{questionError}</p>
                ) : null}

                <div>
                  <label className="block text-xs font-bold text-slate-700" htmlFor="question-input">
                    Nội dung câu hỏi của bạn <span className="text-rose-500">*</span>
                  </label>
                  <textarea
                    className="mt-1.5 w-full rounded-xl border border-slate-200 p-3 text-sm outline-none transition focus:border-brand focus:ring-2 focus:ring-brand/20"
                    id="question-input"
                    maxLength={1000}
                    placeholder="Ví dụ: Sản phẩm có hỗ trợ đổi trả nếu không vừa kích cỡ không?..."
                    required
                    rows={4}
                    value={questionContent}
                    onChange={(e) => setQuestionContent(e.target.value)}
                  />
                  <div className="mt-1 text-right text-[11px] text-slate-400">
                    {questionContent.length}/1000 ký tự
                  </div>
                </div>

                <div className="flex justify-end gap-2.5 pt-2 border-t border-slate-100">
                  <Button
                    disabled={submittingQuestion}
                    size="sm"
                    variant="outline"
                    onClick={() => setIsQuestionModalOpen(false)}
                    type="button"
                  >
                    Hủy bỏ
                  </Button>
                  <Button disabled={submittingQuestion || !questionContent.trim()} size="sm" type="submit">
                    <Send className="size-3.5" />
                    {submittingQuestion ? "Đang gửi…" : "Gửi câu hỏi"}
                  </Button>
                </div>
              </form>
            )}
          </div>
        </div>
      ) : null}
    </SurfacePanel>
  );
}
