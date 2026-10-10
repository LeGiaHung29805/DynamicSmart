"use client";

import {
  AlertCircle,
  CheckCircle2,
  Headphones,
  HelpCircle,
  MessageSquarePlus,
  Package,
  RefreshCw,
  Send,
  Sparkles,
  User,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useAuthSession } from "@/lib/auth/session";
import { customerOrdersApi, type CustomerOrder } from "@/features/order/api/customer-orders.api";
import { engagementApi } from "../api/engagement.api";
import type { ChatConversationItem } from "../types/engagement.types";
import { Status, decodeHtml } from "./EngagementShared";

const SUPPORT_TOPICS = [
  { id: "ORDER", label: "Kiểm tra đơn hàng & Giao hàng", icon: "📦" },
  { id: "PRODUCT", label: "Tư vấn sản phẩm & Thông số", icon: "🏷️" },
  { id: "RETURN", label: "Đổi trả hàng & Hoàn tiền", icon: "🔄" },
  { id: "PAYMENT", label: "Thanh toán & Hóa đơn", icon: "💳" },
  { id: "PROMOTION", label: "Mã giảm giá & Khuyến mãi", icon: "🎁" },
  { id: "GENERAL", label: "Thắc mắc & Hỗ trợ khác", icon: "💬" },
];

const QUICK_PROMPTS = [
  "Khi nào đơn hàng của tôi được giao?",
  "Quy trình đổi trả hàng trong bao lâu?",
  "Tôi có thể thay đổi địa chỉ nhận hàng không?",
  "Cách kiểm tra bảo hành sản phẩm?",
];

export function CustomerSupportPage() {
  const session = useAuthSession();
  const [conversations, setConversations] = useState<ChatConversationItem[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [messageInput, setMessageInput] = useState("");
  const [sending, setSending] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [errorMsg, setErrorMsg] = useState("");

  // Modal tạo hội thoại mới
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedTopic, setSelectedTopic] = useState(SUPPORT_TOPICS[0].id);
  const [relatedOrderNumber, setRelatedOrderNumber] = useState("");
  const [initialContent, setInitialContent] = useState("");
  const [recentOrders, setRecentOrders] = useState<CustomerOrder[]>([]);
  const [creating, setCreating] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Tải danh sách đơn hàng gần đây để người dùng chọn khi cần hỗ trợ
  useEffect(() => {
    if (session.status !== "authenticated") return;
    customerOrdersApi
      .list({ size: 10 })
      .then((res) => {
        // Lấy danh sách orderId rồi fetch chi tiết hoặc dùng orderNumber nếu có
        const summaries = res.content ?? [];
        setRecentOrders(
          summaries.map((s) => ({
            orderId: s.orderId,
            orderNumber: s.orderNumber,
            status: s.status,
            paymentTiming: s.paymentTiming,
            paymentMethod: s.paymentMethod,
            money: { finalTotalVnd: s.finalTotalVnd, currency: s.currency },
            availableActions: [],
            createdAt: s.createdAt,
          }))
        );
      })
      .catch(() => {});
  }, [session.status]);

  // Tải danh sách các cuộc hội thoại
  useEffect(() => {
    if (session.status !== "authenticated") {
      setLoading(false);
      return;
    }
    let ignore = false;
    async function load() {
      try {
        setErrorMsg("");
        const res = await engagementApi.support.customerConversations(0, 50);
        if (!ignore) {
          const content = res.content ?? [];
          setConversations(content);
          setSelectedId((prev) => prev || content[0]?.id || "");
          setIsLive(true);
        }
      } catch {
        if (!ignore) {
          setIsLive(false);
          setConversations([]);
          setErrorMsg("Không thể kết nối dịch vụ hỗ trợ khách hàng.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      ignore = true;
    };
  }, [session.status, refreshTrigger]);

  // Tự động polling tin nhắn mới mỗi 4 giây cho hội thoại đang mở
  useEffect(() => {
    if (!selectedId) return;

    const interval = setInterval(async () => {
      try {
        const detail = await engagementApi.support.customerConversation(selectedId);
        if (detail) {
          setConversations((prev) =>
            prev.map((c) => (c.id === selectedId ? detail : c))
          );
        }
      } catch {
        // Bỏ qua lỗi polling im lặng
      }
    }, 4000);

    return () => clearInterval(interval);
  }, [selectedId]);

  // Tự động cuộn xuống dưới khi có tin nhắn mới
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [conversations, selectedId]);

  const selected =
    conversations.find((item) => item.id === selectedId) ?? conversations[0];

  const handleCreateConversationSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!initialContent.trim()) return;

    setCreating(true);
    setErrorMsg("");

    const topicItem = SUPPORT_TOPICS.find((t) => t.id === selectedTopic);
    const headerPrefix = `[${topicItem?.label || "Hỗ trợ"}]${
      relatedOrderNumber ? ` [Đơn hàng: #${relatedOrderNumber}]` : ""
    }\n`;
    const fullMessage = `${headerPrefix}${initialContent.trim()}`;

    try {
      const newConv = await engagementApi.support.createConversation(fullMessage);
      setConversations((prev) => [newConv, ...prev]);
      setSelectedId(newConv.id);
      setIsLive(true);
      setIsCreateModalOpen(false);
      setInitialContent("");
      setRelatedOrderNumber("");
    } catch {
      setErrorMsg("Không thể tạo hội thoại hỗ trợ. Vui lòng thử lại sau.");
    } finally {
      setCreating(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    const content = messageInput.trim();
    if (!content || !selected) return;

    setSending(true);
    try {
      setErrorMsg("");
      const updatedConv = await engagementApi.support.customerSend(selected.id, content);
      setConversations((prev) =>
        prev.map((c) => (c.id === selected.id ? updatedConv : c))
      );
      setMessageInput("");
    } catch {
      setErrorMsg("Không thể gửi tin nhắn. Vui lòng kiểm tra kết nối mạng.");
    } finally {
      setSending(false);
    }
  };

  const handleQuickPromptClick = (prompt: string) => {
    setMessageInput(prompt);
  };

  // Trích xuất tiêu đề hội thoại ngắn gọn từ tin nhắn đầu tiên
  const getConversationTitle = (c: ChatConversationItem) => {
    const raw = c.messages?.[0]?.content || "";
    const firstMsg = decodeHtml(raw);
    if (firstMsg.startsWith("[")) {
      const closingIdx = firstMsg.indexOf("]");
      if (closingIdx !== -1) {
        return decodeHtml(firstMsg.slice(1, closingIdx));
      }
    }
    return `Hội thoại hỗ trợ #${c.id.slice(0, 6)}`;
  };

  if (session.status === "anonymous") {
    return (
      <div className="space-y-6">
        <PageHeader
          description="Đội ngũ hỗ trợ và CSKH của DynamicMart luôn sẵn sàng giải đáp thắc mắc của bạn."
          eyebrow="Trung tâm trợ giúp"
          title="Chat trực tiếp với nhân viên hỗ trợ"
        />

        <SurfacePanel className="p-8 text-center sm:p-12">
          <div className="mx-auto flex size-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-800">
            <Headphones className="size-7" />
          </div>
          <h2 className="mt-4 text-xl font-bold text-slate-900">
            Đăng nhập để kết nối với CSKH
          </h2>
          <p className="mx-auto mt-2 max-w-md text-sm text-slate-600 leading-relaxed">
            Vui lòng đăng nhập vào tài khoản của bạn để gửi yêu cầu hỗ trợ, chat trực tiếp với nhân viên và theo dõi lịch sử tư vấn.
          </p>
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <Link href="/login?returnTo=/support">
              <Button className="bg-brand text-white hover:bg-brand/90 font-bold">
                Đăng nhập ngay
              </Button>
            </Link>
            <Link href="/help">
              <Button variant="secondary">
                Xem câu hỏi thường gặp
              </Button>
            </Link>
          </div>

          <div className="mt-10 grid gap-4 border-t border-slate-100 pt-8 sm:grid-cols-3 text-left">
            <div className="rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
              <div className="text-xs font-bold uppercase tracking-wider text-slate-500">Tổng đài miễn phí</div>
              <div className="mt-1 text-base font-black text-slate-900">1800 6868</div>
              <div className="text-xs text-slate-500">8:00 - 22:00 tất cả các ngày</div>
            </div>
            <div className="rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
              <div className="text-xs font-bold uppercase tracking-wider text-slate-500">Email hỗ trợ</div>
              <div className="mt-1 text-base font-black text-slate-900">support@dynamicmart.local</div>
              <div className="text-xs text-slate-500">Phản hồi trong vòng 24 giờ</div>
            </div>
            <div className="rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
              <div className="text-xs font-bold uppercase tracking-wider text-slate-500">Kênh trợ giúp khác</div>
              <div className="mt-1 text-base font-black text-slate-900">FAQ & Chính sách</div>
              <div className="text-xs text-slate-500">Tra cứu chính sách bảo hành & đổi trả</div>
            </div>
          </div>
        </SurfacePanel>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Đội ngũ hỗ trợ và CSKH của DynamicMart luôn sẵn sàng giải đáp thắc mắc của bạn."
          eyebrow="Trung tâm trợ giúp"
          title="Chat trực tiếp với nhân viên hỗ trợ"
        />

        <div className="flex items-center gap-2">
          {isLive ? (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-800 shadow-xs">
              <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
              CSKH Đang trực tuyến
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800 shadow-xs">
              <AlertCircle className="size-3.5" />
              Đang kết nối
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

      <div className="grid gap-5 lg:grid-cols-[320px_1fr]">
        {errorMsg ? (
          <p className="lg:col-span-2 rounded-xl bg-rose-50 p-3.5 text-sm text-rose-700 flex items-center gap-2">
            <AlertCircle className="size-4 shrink-0 text-rose-600" />
            {errorMsg}
          </p>
        ) : null}

        {/* Cột trái: Danh sách các cuộc hội thoại */}
        <SurfacePanel className="p-4 sm:p-4 h-fit">
          <Button
            className="mb-4 w-full gap-2 bg-emerald-900 hover:bg-emerald-800 text-white font-bold"
            onClick={() => setIsCreateModalOpen(true)}
          >
            <MessageSquarePlus className="size-4" />
            Tạo yêu cầu hỗ trợ mới
          </Button>

          <div className="space-y-2 max-h-[520px] overflow-y-auto pr-1">
            {conversations.length === 0 ? (
              <div className="text-center text-xs text-slate-500 py-10 px-4">
                <HelpCircle className="mx-auto size-8 text-slate-300 stroke-1" />
                <p className="mt-2 font-bold text-slate-700">Chưa có hội thoại nào</p>
                <p className="mt-1">Nhấn nút bên trên để gửi tin nhắn đến đội ngũ CSKH.</p>
              </div>
            ) : (
              conversations.map((conversation) => {
                const isSelected = selected?.id === conversation.id;
                const title = getConversationTitle(conversation);
                const lastMsg =
                  conversation.messages && conversation.messages.length > 0
                    ? conversation.messages[conversation.messages.length - 1].content
                    : "Chưa có tin nhắn";

                return (
                  <button
                    className={`w-full rounded-2xl border p-3.5 text-left transition ${
                      isSelected
                        ? "border-emerald-600 bg-emerald-50/80 shadow-xs"
                        : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50"
                    }`}
                    key={conversation.id}
                    onClick={() => setSelectedId(conversation.id)}
                    type="button"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-bold text-slate-950 text-sm truncate">
                        {decodeHtml(title)}
                      </span>
                      <span
                        className={`text-[10px] font-black px-2 py-0.5 rounded-full shrink-0 ${
                          conversation.status === "OPEN"
                            ? "bg-emerald-100 text-emerald-800"
                            : "bg-slate-100 text-slate-600"
                        }`}
                      >
                        {conversation.status === "OPEN" ? "ĐANG XỬ LÝ" : "ĐÃ ĐÓNG"}
                      </span>
                    </div>
                    <p className="mt-1.5 text-xs text-slate-600 line-clamp-2 leading-relaxed">
                      {decodeHtml(lastMsg)}
                    </p>
                  </button>
                );
              })
            )}
          </div>
        </SurfacePanel>

        {/* Cột phải: Khung chat chi tiết */}
        {selected ? (
          <SurfacePanel className="flex flex-col h-[620px] p-0 sm:p-0 overflow-hidden">
            {/* Header khung chat */}
            <div className="flex items-center justify-between gap-3 border-b border-slate-100 bg-slate-50/70 p-4 sm:px-6">
              <div className="flex items-center gap-3">
                <div className="grid size-10 place-items-center rounded-full bg-emerald-700 text-white font-bold shadow-xs">
                  <Headphones className="size-5" />
                </div>
                <div>
                  <h2 className="font-black text-slate-950 text-sm sm:text-base">
                    {decodeHtml(getConversationTitle(selected))}
                  </h2>
                  <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                    <span className="size-2 rounded-full bg-emerald-500 inline-block animate-pulse" />
                    Chuyên viên tư vấn DynamicMart · Sẵn sàng hỗ trợ
                  </p>
                </div>
              </div>
              <Status value={selected.status} />
            </div>

            {/* Danh sách tin nhắn */}
            <div className="flex-1 p-4 sm:p-6 space-y-4 overflow-y-auto">
              {!selected.messages || selected.messages.length === 0 ? (
                <div className="text-center py-16 text-slate-400 text-sm">
                  Chưa có tin nhắn trong cuộc trò chuyện này. Hãy gửi tin nhắn đầu tiên!
                </div>
              ) : (
                selected.messages.map((message, index) => {
                  const isCust = message.senderRole === "CUSTOMER";
                  return (
                    <div
                      className={`flex flex-col max-w-[85%] sm:max-w-[75%] ${
                        isCust ? "ml-auto items-end" : "mr-auto items-start"
                      }`}
                      key={`${message.id || index}`}
                    >
                      <div className="flex items-center gap-1.5 mb-1 text-[11px] font-semibold text-slate-400">
                        {isCust ? (
                          <>
                            <span>Bạn</span>
                            <User className="size-3" />
                          </>
                        ) : (
                          <>
                            <Headphones className="size-3 text-emerald-700" />
                            <span className="text-emerald-900 font-bold">
                              CSKH DynamicMart
                            </span>
                          </>
                        )}
                        <span>· {new Date(message.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</span>
                      </div>

                      <div
                        className={`rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-2xs whitespace-pre-wrap ${
                          isCust
                            ? "bg-emerald-950 text-white rounded-tr-xs"
                            : "bg-slate-100 text-slate-900 rounded-tl-xs border border-slate-200/60"
                        }`}
                      >
                        {decodeHtml(message.content)}
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Gợi ý câu hỏi nhanh */}
            {selected.status === "OPEN" ? (
              <div className="border-t border-slate-100 bg-slate-50/50 px-4 py-2.5 overflow-x-auto flex gap-2">
                <span className="inline-flex items-center gap-1 text-[11px] font-bold text-slate-400 shrink-0">
                  <Sparkles className="size-3 text-amber-500" /> Gợi ý:
                </span>
                {QUICK_PROMPTS.map((prompt) => (
                  <button
                    className="shrink-0 rounded-full border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-700 hover:border-emerald-600 hover:text-emerald-800 transition"
                    key={prompt}
                    onClick={() => handleQuickPromptClick(prompt)}
                    type="button"
                  >
                    {prompt}
                  </button>
                ))}
              </div>
            ) : null}

            {/* Input gửi tin nhắn */}
            <form
              className="p-3 sm:p-4 border-t border-slate-100 bg-white flex gap-2"
              onSubmit={handleSendMessage}
            >
              <Input
                aria-label="Tin nhắn"
                className="flex-1 text-sm h-11"
                disabled={selected.status === "CLOSED" || sending}
                placeholder={
                  selected.status === "CLOSED"
                    ? "Cuộc hội thoại này đã kết thúc..."
                    : "Nhập nội dung cần nhân viên hỗ trợ..."
                }
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
              />
              <Button
                className="h-11 px-5 gap-1.5 bg-emerald-950 hover:bg-emerald-900 text-white font-bold"
                disabled={!messageInput.trim() || selected.status === "CLOSED" || sending}
                type="submit"
              >
                <Send className="size-4" />
                <span className="hidden sm:inline">Gửi</span>
              </Button>
            </form>
          </SurfacePanel>
        ) : (
          <SurfacePanel className="flex items-center justify-center min-h-[520px]">
            <p className="text-slate-400 text-sm">Vui lòng chọn hoặc tạo cuộc hội thoại mới</p>
          </SurfacePanel>
        )}
      </div>

      {/* Modal Tạo Cuộc Hội Thoại Mới Đầy Đủ */}
      {isCreateModalOpen ? (
        <div
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm"
          role="dialog"
        >
          <div className="relative w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-6 sm:p-7 shadow-2xl">
            <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-4">
              <div>
                <p className="text-xs font-bold tracking-wider text-brand uppercase">
                  Chăm sóc khách hàng
                </p>
                <h3 className="mt-1 text-xl font-black text-slate-950">
                  Tạo yêu cầu hỗ trợ mới
                </h3>
              </div>
              <button
                aria-label="Đóng"
                className="rounded-xl p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                onClick={() => setIsCreateModalOpen(false)}
                type="button"
              >
                <X className="size-5" />
              </button>
            </div>

            <form className="mt-5 space-y-4" onSubmit={handleCreateConversationSubmit}>
              {/* Chọn chủ đề */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">
                  Chủ đề cần hỗ trợ <span className="text-rose-500">*</span>
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {SUPPORT_TOPICS.map((topic) => (
                    <button
                      className={`flex items-center gap-2 rounded-xl border p-2.5 text-left text-xs font-bold transition ${
                        selectedTopic === topic.id
                          ? "border-emerald-700 bg-emerald-50 text-emerald-950 ring-1 ring-emerald-700"
                          : "border-slate-200 bg-white text-slate-700 hover:border-slate-300"
                      }`}
                      key={topic.id}
                      onClick={() => setSelectedTopic(topic.id)}
                      type="button"
                    >
                      <span className="text-base">{topic.icon}</span>
                      <span className="truncate">{topic.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Chọn đơn hàng liên quan nếu có */}
              {recentOrders.length > 0 ? (
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                    Đơn hàng liên quan (tùy chọn)
                  </label>
                  <select
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-xs font-semibold text-slate-800 outline-none focus:border-brand"
                    value={relatedOrderNumber}
                    onChange={(e) => setRelatedOrderNumber(e.target.value)}
                  >
                    <option value="">-- Không gắn đơn hàng cụ thể --</option>
                    {recentOrders.map((o) => (
                      <option key={o.orderId} value={o.orderNumber}>
                        Đơn #{o.orderNumber} ({o.status}) · {new Date(o.createdAt).toLocaleDateString("vi-VN")}
                      </option>
                    ))}
                  </select>
                </div>
              ) : null}

              {/* Nội dung chi tiết */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">
                  Nội dung thắc mắc / yêu cầu <span className="text-rose-500">*</span>
                </label>
                <textarea
                  className="w-full min-h-[120px] rounded-xl border border-slate-200 p-3 text-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand/20 transition"
                  placeholder="Mô tả cụ thể sự cố, thông tin sản phẩm hoặc câu hỏi cần hỗ trợ..."
                  required
                  rows={4}
                  value={initialContent}
                  onChange={(e) => setInitialContent(e.target.value)}
                />
              </div>

              <div className="flex justify-end gap-2.5 border-t border-slate-100 pt-4">
                <Button
                  disabled={creating}
                  onClick={() => setIsCreateModalOpen(false)}
                  type="button"
                  variant="outline"
                >
                  Hủy
                </Button>
                <Button
                  className="bg-emerald-950 hover:bg-emerald-900 text-white font-bold px-5"
                  disabled={creating || !initialContent.trim()}
                  type="submit"
                >
                  {creating ? "Đang gửi..." : "Gửi yêu cầu hỗ trợ"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
}
