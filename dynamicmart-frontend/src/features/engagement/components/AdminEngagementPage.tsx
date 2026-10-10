"use client";

import {
  AlertCircle,
  EyeOff,
  Headphones,
  MessageSquareText,
  RefreshCw,
  Send,
  ShieldAlert,
  Star,
  User,
  UserCheck,
  X,
  XCircle,
} from "lucide-react";
import { useEffect, useState } from "react";
import Link from "next/link";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { useAuthSession } from "@/lib/auth/session";
import { engagementApi } from "../api/engagement.api";
import { apiClient } from "@/lib/api/client";
import type { ProductDetail } from "@/features/catalog/types";
import type {
  ChatConversationItem,
  ProductQuestionItem,
  ReviewItem,
} from "../types/engagement.types";
import { AdminHideReviewModal } from "./AdminHideReviewModal";
import { Status, decodeHtml } from "./EngagementShared";

export function AdminEngagementPage() {
  const session = useAuthSession();
  const isAdmin = session.status === "authenticated" && session.user.role === "ADMIN";

  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // 1. Quản lý Đánh giá (P0)
  const [reviews, setReviews] = useState<ReviewItem[]>([]);
  const [selectedReviewForHide, setSelectedReviewForHide] = useState<ReviewItem | null>(null);
  const [isHideModalOpen, setIsHideModalOpen] = useState(false);

  // 2. Quản lý Hỏi đáp (P1)
  const [questions, setQuestions] = useState<
    { id: string; product: string; question: string; answer?: string; status: string }[]
  >([]);
  const [answeringQuestionId, setAnsweringQuestionId] = useState<string | null>(null);
  const [answerInput, setAnswerInput] = useState("");

  // 3. Quản lý Hỗ trợ Chat (P2)
  const [conversations, setConversations] = useState<
    { id: string; customer: string; status: string; assignedAdmin?: string; messagesCount?: number }[]
  >([]);
  const [activeChatDetail, setActiveChatDetail] = useState<ChatConversationItem | null>(null);
  const [chatReplyInput, setChatReplyInput] = useState("");
  const [sendingReply, setSendingReply] = useState(false);
  const [loadingChatDetail, setLoadingChatDetail] = useState(false);

  useEffect(() => {
    if (!isAdmin) {
      if (session.status !== "loading") {
        setLoading(false);
      }
      return;
    }
    let ignore = false;
    async function load() {
      let anyLive = false;
      let hasError = false;
      setErrorMsg("");

      // Tải danh sách Review
      try {
        const reviewRes = await engagementApi.reviews.getAdminReviews(0, 20);
        if (!ignore) {
          setReviews(reviewRes.content ?? []);
          anyLive = true;
        }
      } catch {
        hasError = true;
      }

      // Tải danh sách Hỏi đáp
      try {
        const questionRes = await engagementApi.questions.getAdminQuestions(0, 20);
        if (!ignore) {
          const rawQ = questionRes.content ?? [];
          const enrichedQ = await Promise.all(
            rawQ.map(async (q: ProductQuestionItem) => {
              let prodName = `Sản phẩm #${q.productId.slice(0, 8)}`;
              try {
                const prod = await apiClient.get<ProductDetail>(`/api/v1/catalog/products/id/${q.productId}`);
                if (prod?.name) prodName = prod.name;
              } catch {}
              return {
                id: q.id,
                product: prodName,
                question: q.content,
                answer: q.answers?.[0]?.content,
                status: q.status,
              };
            })
          );
          if (!ignore) {
            setQuestions(enrichedQ);
            anyLive = true;
          }
        }
      } catch {
        hasError = true;
      }

      // Tải danh sách Hội thoại Chat
      try {
        const chatRes = await engagementApi.support.adminConversations(0, 20);
        if (!ignore) {
          setConversations(
            (chatRes.content ?? []).map((c: ChatConversationItem) => ({
              id: c.id,
              customer: "Khách hàng DynamicMart",
              status: c.status,
              assignedAdmin: c.assignedAdminId ? `Admin #${c.assignedAdminId.slice(0, 6)}` : "Chưa nhận",
              messagesCount: c.messages?.length || 0,
            }))
          );
          anyLive = true;
        }
      } catch {
        hasError = true;
      }

      if (!ignore) {
        setIsLive(anyLive);
        if (hasError) setErrorMsg("Một số dữ liệu quản trị không tải được từ Engagement API.");
        setLoading(false);
      }
    }

    void load();
    return () => {
      ignore = true;
    };
  }, [isAdmin, session.status, refreshTrigger]);

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
      setErrorMsg("");
      await engagementApi.questions.answer(questionId, { content: trimmed });
      setQuestions((prev) =>
        prev.map((q) =>
          q.id === questionId ? { ...q, answer: trimmed, status: "ANSWERED" } : q
        )
      );
      setAnsweringQuestionId(null);
      setAnswerInput("");
    } catch {
      setErrorMsg("Không thể lưu câu trả lời.");
    }
  };

  // Hành động nhận hội thoại / đóng hội thoại
  const handleAssignConversation = async (conversationId: string) => {
    try {
      setErrorMsg("");
      await engagementApi.support.adminAssign(conversationId);
      setConversations((prev) =>
        prev.map((c) =>
          c.id === conversationId ? { ...c, assignedAdmin: "Bạn (Tôi)" } : c
        )
      );
    } catch {
      setErrorMsg("Không thể nhận hội thoại.");
    }
  };

  const handleCloseConversation = async (conversationId: string) => {
    try {
      setErrorMsg("");
      await engagementApi.support.adminClose(conversationId);
      setConversations((prev) =>
        prev.map((c) =>
          c.id === conversationId ? { ...c, status: "CLOSED" } : c
        )
      );
    } catch {
      setErrorMsg("Không thể đóng hội thoại.");
    }
  };

  const handleOpenChatDetail = async (conversationId: string) => {
    setLoadingChatDetail(true);
    try {
      const detail = await engagementApi.support.adminConversation(conversationId);
      setActiveChatDetail(detail);
    } catch {
      setErrorMsg("Không thể tải chi tiết hội thoại.");
    } finally {
      setLoadingChatDetail(false);
    }
  };

  const handleAdminSendReply = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeChatDetail || !chatReplyInput.trim()) return;

    setSendingReply(true);
    try {
      const updated = await engagementApi.support.adminSend(
        activeChatDetail.id,
        chatReplyInput.trim()
      );
      setActiveChatDetail(updated);
      setChatReplyInput("");
      setRefreshTrigger((prev) => prev + 1);
    } catch {
      setErrorMsg("Không thể gửi phản hồi từ admin.");
    } finally {
      setSendingReply(false);
    }
  };

  if (session.status === "loading") {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <RefreshCw className="size-8 animate-spin text-emerald-600" />
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="space-y-7">
        <PageHeader
          description="Duyệt đánh giá, ẩn vi phạm có lưu lý do audit, trả lời hỏi đáp và xử lý hội thoại hỗ trợ khách hàng."
          eyebrow="Quản trị tương tác"
          title="Tương tác khách hàng"
        />
        <div className="rounded-3xl border border-amber-200 bg-amber-50/70 p-8 text-center shadow-sm">
          <div className="mx-auto grid size-14 place-items-center rounded-2xl bg-amber-100 text-amber-700">
            <ShieldAlert className="size-7" />
          </div>
          <h2 className="mt-4 text-lg font-bold text-slate-900">Yêu cầu quyền Quản trị viên (ADMIN)</h2>
          <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-600">
            Trang này dành riêng cho tài khoản Quản trị viên để kiểm duyệt đánh giá, giải đáp thắc mắc và hỗ trợ khách hàng.
          </p>
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/login?returnTo=/admin/engagement"
              className="inline-flex items-center gap-2 rounded-xl bg-slate-950 px-5 py-2.5 text-xs font-bold text-white shadow-sm transition hover:bg-emerald-600"
            >
              Đăng nhập tài khoản Quản trị
            </Link>
          </div>
          <p className="mt-4 text-xs text-slate-500">
            Tài khoản demo: <code className="font-mono font-semibold text-slate-700">admin@dynamicmart.local</code> / Mật khẩu: <code className="font-mono font-semibold text-slate-700">Password@123</code>
          </p>
        </div>
      </div>
    );
  }

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
              Mất kết nối API
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

      {errorMsg ? <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{errorMsg}</p> : null}

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
                      {review.customerName || "Khách hàng DynamicMart"}
                    </p>
                    <Status value={review.status} />
                  </div>

                  <p className="mt-2 text-sm text-slate-600">
                    {decodeHtml(review.content) || "(Không có nhận xét chữ)"}
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
                  <p className="mt-2 text-sm text-slate-600">{decodeHtml(question.question)}</p>

                  {question.answer ? (
                    <div className="mt-2.5 rounded-lg bg-emerald-50/70 p-2.5 text-xs text-emerald-900 border border-emerald-100">
                      <strong className="text-emerald-950">Phản hồi:</strong> {decodeHtml(question.answer)}
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

                  <div className="mt-3 flex flex-wrap gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={loadingChatDetail}
                      onClick={() => handleOpenChatDetail(conversation.id)}
                    >
                      <MessageSquareText className="size-3.5" />
                      Xem & Trả lời
                    </Button>

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
                      <span className="text-xs text-slate-400 italic self-center">Đã đóng trao đổi</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </SurfacePanel>
      </div>

      {/* Modal Admin Chat Trực tiếp */}
      {activeChatDetail ? (
        <div
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm"
          role="dialog"
        >
          <div className="relative flex flex-col w-full max-w-2xl h-[600px] rounded-3xl border border-slate-200 bg-white shadow-2xl overflow-hidden">
            <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 p-4 sm:px-6">
              <div className="flex items-center gap-3">
                <span className="grid size-10 place-items-center rounded-full bg-emerald-700 text-white font-bold">
                  <Headphones className="size-5" />
                </span>
                <div>
                  <h3 className="font-bold text-slate-950">
                    Hội thoại hỗ trợ khách hàng
                  </h3>
                  <p className="text-xs text-slate-500">
                    Mã hội thoại: #{activeChatDetail.id.slice(0, 12)} · Trạng thái: {activeChatDetail.status}
                  </p>
                </div>
              </div>
              <button
                aria-label="Đóng"
                className="rounded-xl p-1.5 text-slate-400 hover:bg-slate-200 hover:text-slate-700 transition"
                onClick={() => setActiveChatDetail(null)}
                type="button"
              >
                <X className="size-5" />
              </button>
            </div>

            {/* Danh sách tin nhắn */}
            <div className="flex-1 p-4 sm:p-6 space-y-3 overflow-y-auto bg-slate-50/30">
              {!activeChatDetail.messages || activeChatDetail.messages.length === 0 ? (
                <div className="text-center py-16 text-slate-400 text-sm">
                  Chưa có tin nhắn trong cuộc trò chuyện này.
                </div>
              ) : (
                activeChatDetail.messages.map((message, index) => {
                  const isCust = message.senderRole === "CUSTOMER";
                  return (
                    <div
                      className={`flex flex-col max-w-[80%] ${
                        isCust ? "mr-auto items-start" : "ml-auto items-end"
                      }`}
                      key={`${message.id || index}`}
                    >
                      <span className="text-[11px] text-slate-400 mb-1 flex items-center gap-1">
                        {isCust ? <User className="size-3" /> : <Headphones className="size-3 text-emerald-600" />}
                        {isCust ? "Khách hàng" : "Admin (Bạn)"} · {new Date(message.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                      </span>
                      <div
                        className={`rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap ${
                          isCust
                            ? "bg-white text-slate-900 border border-slate-200 rounded-tl-xs"
                            : "bg-emerald-950 text-white rounded-tr-xs"
                        }`}
                      >
                        {decodeHtml(message.content)}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Form gửi phản hồi admin */}
            <form
              className="p-3 sm:p-4 border-t border-slate-100 bg-white flex gap-2"
              onSubmit={handleAdminSendReply}
            >
              <input
                aria-label="Nội dung phản hồi"
                className="flex-1 rounded-xl border border-slate-200 px-4 py-2 text-sm outline-none focus:border-brand"
                disabled={activeChatDetail.status === "CLOSED" || sendingReply}
                placeholder={
                  activeChatDetail.status === "CLOSED"
                    ? "Cuộc hội thoại này đã đóng..."
                    : "Nhập nội dung phản hồi cho khách hàng..."
                }
                value={chatReplyInput}
                onChange={(e) => setChatReplyInput(e.target.value)}
              />
              <Button
                className="bg-emerald-950 hover:bg-emerald-900 text-white font-bold px-4"
                disabled={!chatReplyInput.trim() || activeChatDetail.status === "CLOSED" || sendingReply}
                type="submit"
              >
                <Send className="size-4" />
                <span>Gửi</span>
              </Button>
            </form>
          </div>
        </div>
      ) : null}

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
