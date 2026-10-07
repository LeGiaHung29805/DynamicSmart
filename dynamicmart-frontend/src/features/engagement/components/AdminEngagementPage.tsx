"use client";

import {
  AlertCircle,
  EyeOff,
  MessageSquareText,
  RefreshCw,
  ShieldAlert,
  Star,
  UserCheck,
  XCircle,
} from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { engagementApi } from "../api/engagement.api";
import { mockConversations, mockQuestions, mockReviews } from "../mock-data";
import type {
  ChatConversationItem,
  ProductQuestionItem,
  ReviewItem,
} from "../types/engagement.types";
import { AdminHideReviewModal } from "./AdminHideReviewModal";
import { Status } from "./EngagementShared";

export function AdminEngagementPage() {
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // 1. Quản lý Đánh giá (P0)
  const [reviews, setReviews] = useState<ReviewItem[]>(() =>
    mockReviews.map((r) => ({
      id: r.id,
      orderItemId: "order-item-" + r.id,
      orderId: "order-" + r.id,
      customerId: "cust-" + r.id,
      customerName: r.customer,
      productId: "prod-" + r.id,
      rating: r.rating,
      content: r.content,
      status: r.status as "VISIBLE" | "HIDDEN",
      createdAt: r.createdAt,
    }))
  );
  const [selectedReviewForHide, setSelectedReviewForHide] = useState<ReviewItem | null>(null);
  const [isHideModalOpen, setIsHideModalOpen] = useState(false);

  // 2. Quản lý Hỏi đáp (P1)
  const [questions, setQuestions] = useState<
    { id: string; product: string; question: string; answer?: string; status: string }[]
  >(mockQuestions);
  const [answeringQuestionId, setAnsweringQuestionId] = useState<string | null>(null);
  const [answerInput, setAnswerInput] = useState("");

  // 3. Quản lý Hỗ trợ Chat (P2)
  const [conversations, setConversations] = useState<
    { id: string; customer: string; status: string; assignedAdmin?: string; messagesCount?: number }[]
  >(
    mockConversations.map((c) => ({
      id: c.id,
      customer: c.customer,
      status: c.status,
      assignedAdmin: c.assignedAdmin,
      messagesCount: c.messages.length,
    }))
  );

  useEffect(() => {
    let ignore = false;
    async function load() {
      let anyLive = false;

      // Tải danh sách Review
      try {
        const reviewRes = await engagementApi.reviews.getAdminReviews(0, 20);
        if (!ignore && reviewRes?.content && reviewRes.content.length > 0) {
          setReviews(reviewRes.content);
          anyLive = true;
        }
      } catch {
        // Fallback giữ nguyên
      }

      // Tải danh sách Hỏi đáp
      try {
        const questionRes = await engagementApi.questions.getAdminQuestions(0, 20);
        if (!ignore && questionRes?.content && questionRes.content.length > 0) {
          setQuestions(
            questionRes.content.map((q: ProductQuestionItem) => ({
              id: q.id,
              product: `Sản phẩm #${q.productId.slice(0, 8)}`,
              question: q.content,
              answer: q.answers?.[0]?.content,
              status: q.status,
            }))
          );
          anyLive = true;
        }
      } catch {
        // Fallback giữ nguyên
      }

      // Tải danh sách Hội thoại Chat
      try {
        const chatRes = await engagementApi.support.adminConversations(0, 20);
        if (!ignore && chatRes?.content && chatRes.content.length > 0) {
          setConversations(
            chatRes.content.map((c: ChatConversationItem) => ({
              id: c.id,
              customer: `Khách #${c.customerId.slice(0, 8)}`,
              status: c.status,
              assignedAdmin: c.assignedAdminId ? `Admin #${c.assignedAdminId.slice(0, 6)}` : "Chưa nhận",
              messagesCount: c.messages?.length || 0,
            }))
          );
          anyLive = true;
        }
      } catch {
        // Fallback giữ nguyên
      }

      if (!ignore) {
        setIsLive(anyLive);
        setLoading(false);
      }
    }

    void load();
    return () => {
      ignore = true;
    };
  }, [refreshTrigger]);

  // Hành động ẩn review
  const handleOpenHideModal = (review: ReviewItem) => {
    setSelectedReviewForHide(review);
    setIsHideModalOpen(true);
  };

  const handleHideSuccess = (reviewId: string, reason: string) => {
    setReviews((prev) =>
      prev.map((r) =>
        r.id === reviewId ? { ...r, status: "HIDDEN", hiddenReason: reason } : r
      )
    );
  };

  // Hành động trả lời câu hỏi
  const handleAnswerSubmit = async (questionId: string) => {
    const trimmed = answerInput.trim();
    if (!trimmed) return;

    try {
      if (isLive) {
        await engagementApi.questions.answer(questionId, { content: trimmed });
      }
      setQuestions((prev) =>
        prev.map((q) =>
          q.id === questionId ? { ...q, answer: trimmed, status: "ANSWERED" } : q
        )
      );
      setAnsweringQuestionId(null);
      setAnswerInput("");
    } catch {
      setQuestions((prev) =>
        prev.map((q) =>
          q.id === questionId ? { ...q, answer: trimmed, status: "ANSWERED" } : q
        )
      );
      setAnsweringQuestionId(null);
      setAnswerInput("");
    }
  };

  // Hành động nhận hội thoại / đóng hội thoại
  const handleAssignConversation = async (conversationId: string) => {
    try {
      if (isLive) {
        await engagementApi.support.adminAssign(conversationId);
      }
      setConversations((prev) =>
        prev.map((c) =>
          c.id === conversationId ? { ...c, assignedAdmin: "Bạn (Tôi)" } : c
        )
      );
    } catch {
      setConversations((prev) =>
        prev.map((c) =>
          c.id === conversationId ? { ...c, assignedAdmin: "Bạn (Tôi)" } : c
        )
      );
    }
  };

  const handleCloseConversation = async (conversationId: string) => {
    try {
      if (isLive) {
        await engagementApi.support.adminClose(conversationId);
      }
      setConversations((prev) =>
        prev.map((c) =>
          c.id === conversationId ? { ...c, status: "CLOSED" } : c
        )
      );
    } catch {
      setConversations((prev) =>
        prev.map((c) =>
          c.id === conversationId ? { ...c, status: "CLOSED" } : c
        )
      );
    }
  };

  return (
    <div className="space-y-7">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Duyệt đánh giá, ẩn vi phạm có lưu lý do audit, trả lời hỏi đáp và xử lý hội thoại hỗ trợ khách hàng."
          eyebrow="Quản trị tương tác"
          title="Tương tác khách hàng"
        />

        <div className="flex items-center gap-2">
          {isLive ? (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-800 shadow-sm">
              <span className="size-2 rounded-full bg-emerald-500" />
              API Trực tiếp
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800 shadow-sm">
              <AlertCircle className="size-3.5" />
              Dữ liệu mô phỏng
            </span>
          )}

          <Button
            disabled={loading}
            size="sm"
            variant="outline"
            onClick={() => {
              setLoading(true);
              setRefreshTrigger((prev) => prev + 1);
            }}
          >
            <RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />
            Làm mới
          </Button>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-3">
        {/* Cột 1: Đánh giá & Kiểm duyệt (P0 - UC-15) */}
        <SurfacePanel>
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2.5">
              <Star className="size-5 text-amber-500 fill-amber-400" />
              <h2 className="font-black text-slate-950">Đánh giá sản phẩm</h2>
            </div>
            <span className="text-xs font-semibold text-slate-500">
              {reviews.length} đánh giá
            </span>
          </div>

          <div className="mt-4 space-y-3">
            {reviews.map((review) => {
              const isHidden = review.status === "HIDDEN";
              return (
                <div
                  className={`rounded-xl border p-4 transition ${
                    isHidden
                      ? "border-rose-200 bg-rose-50/40"
                      : "border-slate-200 bg-white"
                  }`}
                  key={review.id}
                >
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-bold text-sm text-slate-950">
                      {review.customerName || `Khách #${review.customerId.slice(0, 8)}`}
                    </p>
                    <Status value={review.status} />
                  </div>

                  <p className="mt-2 text-sm text-slate-600">
                    {review.content || "(Không có nhận xét chữ)"}
                  </p>

                  {review.hiddenReason ? (
                    <div className="mt-2 rounded-lg bg-rose-100/70 p-2 text-xs text-rose-800">
                      <strong>Lý do ẩn:</strong> {review.hiddenReason}
                    </div>
                  ) : null}

                  <div className="mt-3 flex items-center justify-between text-sm text-slate-500">
                    <span className="font-bold text-amber-600 flex items-center gap-1 text-xs">
                      <Star className="size-3.5 fill-current" />
                      {review.rating}/5
                    </span>
                    {!isHidden ? (
                      <Button
                        className="text-rose-600 hover:border-rose-300 hover:bg-rose-50"
                        size="sm"
                        variant="outline"
                        onClick={() => handleOpenHideModal(review)}
                      >
                        <EyeOff className="size-3.5" />
                        Ẩn vi phạm
                      </Button>
                    ) : (
                      <span className="text-xs font-bold text-rose-600">Đã ẩn (Audit)</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </SurfacePanel>

        {/* Cột 2: Hỏi đáp (P1) */}
        <SurfacePanel>
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2.5">
              <ShieldAlert className="size-5 text-brand" />
              <h2 className="font-black text-slate-950">Hỏi đáp sản phẩm</h2>
            </div>
            <span className="text-xs font-semibold text-slate-500">
              {questions.length} câu hỏi
            </span>
          </div>

          <div className="mt-4 space-y-3">
            {questions.map((question) => {
              const isAnswering = answeringQuestionId === question.id;
              return (
                <div className="rounded-xl border border-slate-200 bg-white p-4" key={question.id}>
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-bold text-sm text-slate-950 truncate">{question.product}</p>
                    <Status value={question.status} />
                  </div>
                  <p className="mt-2 text-sm text-slate-600">{question.question}</p>

                  {question.answer ? (
                    <div className="mt-2.5 rounded-lg bg-emerald-50/70 p-2.5 text-xs text-emerald-900 border border-emerald-100">
                      <strong className="text-emerald-950">Phản hồi:</strong> {question.answer}
                    </div>
                  ) : (
                    <div className="mt-3">
                      {isAnswering ? (
                        <div className="space-y-2">
                          <textarea
                            className="w-full rounded-lg border border-slate-200 p-2 text-xs outline-none focus:border-brand focus:ring-1 focus:ring-brand"
                            placeholder="Nhập câu trả lời của quản trị viên..."
                            rows={2}
                            value={answerInput}
                            onChange={(e) => setAnswerInput(e.target.value)}
                          />
                          <div className="flex justify-end gap-2">
                            <Button size="xs" variant="ghost" onClick={() => setAnsweringQuestionId(null)}>
                              Hủy
                            </Button>
                            <Button size="xs" onClick={() => handleAnswerSubmit(question.id)}>
                              Gửi trả lời
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            setAnsweringQuestionId(question.id);
                            setAnswerInput("");
                          }}
                        >
                          Trả lời câu hỏi
                        </Button>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </SurfacePanel>

        {/* Cột 3: Inbox hỗ trợ (P2) */}
        <SurfacePanel>
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2.5">
              <MessageSquareText className="size-5 text-brand" />
              <h2 className="font-black text-slate-950">Hỗ trợ khách hàng</h2>
            </div>
            <span className="text-xs font-semibold text-slate-500">
              {conversations.length} hội thoại
            </span>
          </div>

          <div className="mt-4 space-y-3">
            {conversations.map((conversation) => {
              const isClosed = conversation.status === "CLOSED";
              return (
                <div className="rounded-xl border border-slate-200 bg-white p-4" key={conversation.id}>
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-bold text-sm text-slate-950">{conversation.customer}</p>
                    <Status value={conversation.status} />
                  </div>
                  <p className="mt-2 text-xs text-slate-500">
                    Phụ trách: <span className="font-semibold text-slate-700">{conversation.assignedAdmin || "Chưa có"}</span>
                  </p>

                  <div className="mt-3 flex gap-2">
                    {!isClosed && conversation.assignedAdmin !== "Bạn (Tôi)" ? (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleAssignConversation(conversation.id)}
                      >
                        <UserCheck className="size-3.5" />
                        Tiếp nhận
                      </Button>
                    ) : null}

                    {!isClosed ? (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => handleCloseConversation(conversation.id)}
                      >
                        <XCircle className="size-3.5 text-slate-400" />
                        Đóng
                      </Button>
                    ) : (
                      <span className="text-xs text-slate-400 italic">Đã đóng trao đổi</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </SurfacePanel>
      </div>

      {/* Popup ẩn review có lưu audit */}
      <AdminHideReviewModal
        isOpen={isHideModalOpen}
        onClose={() => {
          setIsHideModalOpen(false);
          setSelectedReviewForHide(null);
        }}
        onSuccess={handleHideSuccess}
        review={selectedReviewForHide}
      />
    </div>
  );
}
