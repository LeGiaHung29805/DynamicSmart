"use client";

import { AlertCircle, EyeOff, Headphones, MessageSquareText, RefreshCw, Search, Send, ShieldAlert, Star, UserCheck, XCircle } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { apiClient } from "@/lib/api/client";
import { useAuthSession } from "@/lib/auth/session";
import type { ProductDetail } from "@/features/catalog/types";
import { engagementApi } from "../api/engagement.api";
import type { ChatConversationItem, ProductQuestionItem, ReviewItem } from "../types/engagement.types";
import { AdminHideReviewModal } from "./AdminHideReviewModal";
import { Status, date, decodeHtml } from "./EngagementShared";

const FETCH_SIZE = 100;
const PAGE_SIZE = 6;
type AdminQuestion = ProductQuestionItem & { productName: string };

function matches(query: string, ...values: Array<string | undefined | null>) {
  const needle = query.trim().toLocaleLowerCase("vi-VN");
  return !needle || values.some((value) => value?.toLocaleLowerCase("vi-VN").includes(needle));
}

function Pager({ page, items, onChange }: Readonly<{ page: number; items: number; onChange: (value: number) => void }>) {
  const total = Math.max(1, Math.ceil(items / PAGE_SIZE));
  if (total === 1) return null;
  return <div className="flex items-center justify-between gap-3 border-t border-slate-100 pt-4 text-sm"><span className="text-slate-500">Trang {page + 1} / {total}</span><div className="flex gap-2"><Button disabled={page === 0} size="sm" variant="outline" onClick={() => onChange(page - 1)}>Trang trước</Button><Button disabled={page + 1 >= total} size="sm" variant="outline" onClick={() => onChange(page + 1)}>Trang sau</Button></div></div>;
}

function Filters({ query, status, statuses, placeholder, onQuery, onStatus }: Readonly<{ query: string; status: string; statuses: Array<[string, string]>; placeholder: string; onQuery: (value: string) => void; onStatus: (value: string) => void }>) {
  return <div className="grid gap-3 border-b border-slate-100 pb-4 sm:grid-cols-[minmax(0,1fr)_190px]"><label className="relative"><Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" /><input aria-label="Tìm kiếm" className="min-h-10 w-full rounded-xl border border-slate-200 bg-white py-2 pr-3 pl-9 text-sm outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100" placeholder={placeholder} value={query} onChange={(event) => onQuery(event.target.value)} /></label><select aria-label="Lọc trạng thái" className="min-h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-emerald-600" value={status} onChange={(event) => onStatus(event.target.value)}><option value="">Tất cả trạng thái</option>{statuses.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>;
}

function Section({ icon, title, count, children }: Readonly<{ icon: React.ReactNode; title: string; count: string; children: React.ReactNode }>) {
  const pathname = usePathname();
  const routeSection = pathname.split("/").at(-1);
  const expected = title === "Đánh giá sản phẩm" ? "reviews" : title === "Hỏi đáp sản phẩm" ? "questions" : "support";
  if (pathname.startsWith("/admin/engagement/") && routeSection !== expected) return null;
  return <SurfacePanel><div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4"><div className="flex items-center gap-2.5">{icon}<h2 className="font-black text-slate-950">{title}</h2></div><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">{count}</span></div><div className="pt-4">{children}</div></SurfacePanel>;
}

function EmptyDetail({ children }: Readonly<{ children: React.ReactNode }>) { return <aside className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-center text-sm leading-6 text-slate-500 lg:sticky lg:top-24">{children}</aside>; }

export function AdminEngagementPage() {
  const session = useAuthSession();
  const isAdmin = session.status === "authenticated" && session.user.role === "ADMIN";
  const [loading, setLoading] = useState(true); const [error, setError] = useState(""); const [reload, setReload] = useState(0);
  const [reviews, setReviews] = useState<ReviewItem[]>([]); const [questions, setQuestions] = useState<AdminQuestion[]>([]); const [chats, setChats] = useState<ChatConversationItem[]>([]);
  const [selectedReview, setSelectedReview] = useState<ReviewItem | null>(null); const [hideReview, setHideReview] = useState<ReviewItem | null>(null);
  const [selectedQuestion, setSelectedQuestion] = useState<AdminQuestion | null>(null); const [answer, setAnswer] = useState("");
  const [selectedChat, setSelectedChat] = useState<ChatConversationItem | null>(null); const [chatLoading, setChatLoading] = useState(false); const [reply, setReply] = useState(""); const [sending, setSending] = useState(false);
  const [reviewQuery, setReviewQuery] = useState(""); const [reviewStatus, setReviewStatus] = useState(""); const [reviewPage, setReviewPage] = useState(0);
  const [questionQuery, setQuestionQuery] = useState(""); const [questionStatus, setQuestionStatus] = useState(""); const [questionPage, setQuestionPage] = useState(0);
  const [chatQuery, setChatQuery] = useState(""); const [chatStatus, setChatStatus] = useState(""); const [chatPage, setChatPage] = useState(0);

  useEffect(() => {
    if (!isAdmin) return;
    let cancelled = false;
    async function load() {
      setLoading(true); setError("");
      const [reviewResult, questionResult, chatResult] = await Promise.allSettled([engagementApi.reviews.getAdminReviews(0, FETCH_SIZE), engagementApi.questions.getAdminQuestions(0, FETCH_SIZE), engagementApi.support.adminConversations(0, FETCH_SIZE)]);
      if (cancelled) return;
      if (reviewResult.status === "fulfilled") setReviews(reviewResult.value.content ?? []);
      if (chatResult.status === "fulfilled") setChats(chatResult.value.content ?? []);
      if (questionResult.status === "fulfilled") {
        const rows = await Promise.all((questionResult.value.content ?? []).map(async (question) => {
          let productName = `Sản phẩm #${question.productId.slice(0, 8)}`;
          try { const product = await apiClient.get<ProductDetail>(`/api/v1/catalog/products/id/${question.productId}`); productName = product.name || productName; } catch { /* Product may be archived. */ }
          return { ...question, productName };
        }));
        if (!cancelled) setQuestions(rows);
      }
      if (!cancelled) { if ([reviewResult, questionResult, chatResult].some((result) => result.status === "rejected")) setError("Một phần dữ liệu chưa tải được. Hãy thử làm mới."); setLoading(false); }
    }
    void load(); return () => { cancelled = true; };
  }, [isAdmin, reload]);

  const filteredReviews = useMemo(() => reviews.filter((row) => (!reviewStatus || row.status === reviewStatus) && matches(reviewQuery, row.customerName, row.content, row.orderId)), [reviews, reviewQuery, reviewStatus]);
  const filteredQuestions = useMemo(() => questions.filter((row) => (!questionStatus || row.status === questionStatus) && matches(questionQuery, row.productName, row.content, row.answers[0]?.content)), [questions, questionQuery, questionStatus]);
  const filteredChats = useMemo(() => chats.filter((row) => (!chatStatus || row.status === chatStatus) && matches(chatQuery, row.customerId, row.assignedAdminId, row.messages.at(-1)?.content)), [chats, chatQuery, chatStatus]);
  const reviewRows = filteredReviews.slice(reviewPage * PAGE_SIZE, reviewPage * PAGE_SIZE + PAGE_SIZE);
  const questionRows = filteredQuestions.slice(questionPage * PAGE_SIZE, questionPage * PAGE_SIZE + PAGE_SIZE);
  const chatRows = filteredChats.slice(chatPage * PAGE_SIZE, chatPage * PAGE_SIZE + PAGE_SIZE);
  const resetReview = (value: string, status = false) => { if (status) setReviewStatus(value); else setReviewQuery(value); setReviewPage(0); };
  const resetQuestion = (value: string, status = false) => { if (status) setQuestionStatus(value); else setQuestionQuery(value); setQuestionPage(0); };
  const resetChat = (value: string, status = false) => { if (status) setChatStatus(value); else setChatQuery(value); setChatPage(0); };

  async function openChat(item: ChatConversationItem) { setChatLoading(true); setSelectedChat(null); try { setSelectedChat(await engagementApi.support.adminConversation(item.id)); } catch { setError("Không thể tải chi tiết hội thoại."); } finally { setChatLoading(false); } }
  async function submitAnswer() { if (!selectedQuestion || !answer.trim()) return; try { const content = answer.trim(); await engagementApi.questions.answer(selectedQuestion.id, { content }); const updated = { ...selectedQuestion, status: "ANSWERED" as const, answers: [{ id: `pending-${selectedQuestion.id}`, questionId: selectedQuestion.id, adminId: "", content, createdAt: new Date().toISOString() }] }; setQuestions((rows) => rows.map((row) => row.id === updated.id ? updated : row)); setSelectedQuestion(updated); setAnswer(""); } catch { setError("Không thể lưu câu trả lời."); } }
  async function updateChat(action: "assign" | "close") { if (!selectedChat) return; try { const updated = action === "assign" ? await engagementApi.support.adminAssign(selectedChat.id) : await engagementApi.support.adminClose(selectedChat.id); setSelectedChat(updated); setChats((rows) => rows.map((row) => row.id === updated.id ? updated : row)); } catch { setError(action === "assign" ? "Không thể tiếp nhận hội thoại." : "Không thể đóng hội thoại."); } }
  async function submitReply(event: React.FormEvent) { event.preventDefault(); if (!selectedChat || !reply.trim()) return; setSending(true); try { const updated = await engagementApi.support.adminSend(selectedChat.id, reply.trim()); setSelectedChat(updated); setChats((rows) => rows.map((row) => row.id === updated.id ? updated : row)); setReply(""); } catch { setError("Không thể gửi phản hồi."); } finally { setSending(false); } }
  function hidden(id: string, reason: string) { const updated = reviews.map((row) => row.id === id ? { ...row, status: "HIDDEN" as const, hiddenReason: reason } : row); setReviews(updated); setSelectedReview(updated.find((row) => row.id === id) ?? null); setHideReview(null); }

  if (session.status === "loading") return <div className="flex min-h-[50vh] items-center justify-center"><RefreshCw className="size-8 animate-spin text-emerald-600" /></div>;
  if (!isAdmin) return <div className="space-y-7"><PageHeader eyebrow="Quản trị tương tác" title="Tương tác khách hàng" description="Duyệt đánh giá, trả lời hỏi đáp và xử lý yêu cầu hỗ trợ." /><SurfacePanel className="py-12 text-center"><ShieldAlert className="mx-auto size-10 text-amber-600" /><h2 className="mt-4 text-lg font-black">Cần quyền quản trị viên</h2><Link className="mt-5 inline-flex rounded-xl bg-emerald-950 px-4 py-2 text-sm font-bold text-white" href="/login?returnTo=/admin/engagement">Đăng nhập</Link></SurfacePanel></div>;

  return <div className="space-y-7"><div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><PageHeader eyebrow="Quản trị tương tác" title="Tương tác khách hàng" description="Ba nghiệp vụ tách riêng; chọn một dòng để xem và xử lý ngay bên cạnh." /><Button disabled={loading} variant="outline" onClick={() => setReload((value) => value + 1)}><RefreshCw className={loading ? "animate-spin" : ""} />Làm mới</Button></div>{error ? <p className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800"><AlertCircle className="mr-1 inline size-4" />{error}</p> : null}
    <Section count={`${filteredReviews.length} / ${reviews.length} đánh giá`} icon={<Star className="size-5 fill-amber-400 text-amber-500" />} title="Đánh giá sản phẩm"><Filters query={reviewQuery} status={reviewStatus} placeholder="Tìm khách hàng, nội dung hoặc đơn hàng..." statuses={[["VISIBLE", "Đang hiển thị"], ["HIDDEN", "Đã ẩn"]]} onQuery={resetReview} onStatus={(value) => resetReview(value, true)} /><div className="mt-4 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]"><div className="space-y-2">{reviewRows.length ? reviewRows.map((review) => <button className={`w-full rounded-xl border p-3 text-left transition ${selectedReview?.id === review.id ? "border-emerald-500 bg-emerald-50" : "border-slate-200 hover:border-emerald-200"}`} key={review.id} type="button" onClick={() => setSelectedReview(review)}><div className="flex justify-between gap-3"><strong className="text-sm">{review.customerName || "Khách hàng DynamicMart"}</strong><Status value={review.status} /></div><p className="mt-1 line-clamp-2 text-sm text-slate-600">{decodeHtml(review.content) || "Không có nhận xét chữ"}</p><span className="mt-2 inline-flex items-center gap-1 text-xs font-bold text-amber-700"><Star className="size-3 fill-current" />{review.rating}/5 · {date(review.createdAt)}</span></button>) : <EmptyDetail>Không có đánh giá phù hợp.</EmptyDetail>}<Pager items={filteredReviews.length} page={reviewPage} onChange={setReviewPage} /></div><ReviewPanel review={selectedReview} onHide={() => selectedReview && setHideReview(selectedReview)} /></div></Section>
    <Section count={`${filteredQuestions.length} / ${questions.length} câu hỏi`} icon={<MessageSquareText className="size-5 text-emerald-700" />} title="Hỏi đáp sản phẩm"><Filters query={questionQuery} status={questionStatus} placeholder="Tìm sản phẩm, câu hỏi hoặc phản hồi..." statuses={[["OPEN", "Chưa trả lời"], ["ANSWERED", "Đã trả lời"], ["HIDDEN", "Đã ẩn"]]} onQuery={resetQuestion} onStatus={(value) => resetQuestion(value, true)} /><div className="mt-4 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]"><div className="space-y-2">{questionRows.length ? questionRows.map((question) => <button className={`w-full rounded-xl border p-3 text-left transition ${selectedQuestion?.id === question.id ? "border-emerald-500 bg-emerald-50" : "border-slate-200 hover:border-emerald-200"}`} key={question.id} type="button" onClick={() => { setSelectedQuestion(question); setAnswer(""); }}><div className="flex justify-between gap-3"><strong className="truncate text-sm">{question.productName}</strong><Status value={question.status} /></div><p className="mt-1 line-clamp-2 text-sm text-slate-600">{decodeHtml(question.content)}</p></button>) : <EmptyDetail>Không có câu hỏi phù hợp.</EmptyDetail>}<Pager items={filteredQuestions.length} page={questionPage} onChange={setQuestionPage} /></div><QuestionPanel answer={answer} question={selectedQuestion} onAnswer={setAnswer} onSubmit={() => void submitAnswer()} /></div></Section>
    <Section count={`${filteredChats.length} / ${chats.length} hội thoại`} icon={<Headphones className="size-5 text-sky-700" />} title="Hỗ trợ khách hàng"><Filters query={chatQuery} status={chatStatus} placeholder="Tìm mã khách hàng hoặc tin nhắn gần nhất..." statuses={[["OPEN", "Đang mở"], ["CLOSED", "Đã đóng"]]} onQuery={resetChat} onStatus={(value) => resetChat(value, true)} /><div className="mt-4 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]"><div className="space-y-2">{chatRows.length ? chatRows.map((chat) => <button className={`w-full rounded-xl border p-3 text-left transition ${selectedChat?.id === chat.id ? "border-emerald-500 bg-emerald-50" : "border-slate-200 hover:border-emerald-200"}`} key={chat.id} type="button" onClick={() => void openChat(chat)}><div className="flex justify-between gap-3"><strong className="text-sm">Khách hàng #{chat.customerId.slice(0, 8)}</strong><Status value={chat.status} /></div><p className="mt-1 line-clamp-1 text-xs text-slate-500">{decodeHtml(chat.messages.at(-1)?.content) || "Chưa có tin nhắn"}</p><p className="mt-2 text-xs text-slate-400">{chat.assignedAdminId ? "Đã có nhân viên phụ trách" : "Chưa phân công"}</p></button>) : <EmptyDetail>Không có hội thoại phù hợp.</EmptyDetail>}<Pager items={filteredChats.length} page={chatPage} onChange={setChatPage} /></div><ChatPanel chat={selectedChat} loading={chatLoading} reply={reply} sending={sending} onAssign={() => void updateChat("assign")} onClose={() => void updateChat("close")} onReply={setReply} onSubmit={submitReply} /></div></Section>
    <AdminHideReviewModal isOpen={Boolean(hideReview)} review={hideReview} onClose={() => setHideReview(null)} onSuccess={hidden} />
  </div>;
}

function ReviewPanel({ review, onHide }: Readonly<{ review: ReviewItem | null; onHide: () => void }>) { if (!review) return <EmptyDetail>Chọn một đánh giá để xem đầy đủ nội dung và thao tác kiểm duyệt.</EmptyDetail>; return <aside className="rounded-2xl border border-slate-200 bg-slate-50 p-5 lg:sticky lg:top-24"><div className="flex justify-between gap-3"><h3 className="font-black">Chi tiết đánh giá</h3><Status value={review.status} /></div><p className="mt-4 text-sm font-bold">{review.customerName || "Khách hàng DynamicMart"}</p><p className="mt-1 text-xs text-slate-500">Đơn #{review.orderId.slice(0, 8)} · {date(review.createdAt)}</p><p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-slate-700">{decodeHtml(review.content) || "Khách hàng không để lại nhận xét chữ."}</p>{review.hiddenReason ? <p className="mt-4 rounded-xl bg-rose-100 p-3 text-xs text-rose-800"><strong>Lý do ẩn:</strong> {review.hiddenReason}</p> : <Button className="mt-5 text-rose-700" size="sm" variant="outline" onClick={onHide}><EyeOff />Ẩn vi phạm</Button>}</aside>; }
function QuestionPanel({ question, answer, onAnswer, onSubmit }: Readonly<{ question: AdminQuestion | null; answer: string; onAnswer: (value: string) => void; onSubmit: () => void }>) { if (!question) return <EmptyDetail>Chọn một câu hỏi để xem đầy đủ và trả lời ngay tại đây.</EmptyDetail>; const existing = question.answers[0]?.content; return <aside className="rounded-2xl border border-slate-200 bg-slate-50 p-5 lg:sticky lg:top-24"><div className="flex justify-between gap-3"><div><h3 className="font-black">Chi tiết câu hỏi</h3><p className="mt-1 text-xs text-slate-500">{question.productName}</p></div><Status value={question.status} /></div><p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-slate-700">{decodeHtml(question.content)}</p>{existing ? <div className="mt-4 rounded-xl border border-emerald-100 bg-emerald-50 p-3 text-sm text-emerald-950"><strong>Phản hồi:</strong><p className="mt-1 whitespace-pre-wrap">{decodeHtml(existing)}</p></div> : question.status === "HIDDEN" ? <p className="mt-4 text-sm text-slate-500">Câu hỏi đã ẩn, không thể trả lời thêm.</p> : <div className="mt-4 space-y-2"><textarea className="min-h-28 w-full rounded-xl border border-slate-200 bg-white p-3 text-sm outline-none focus:border-emerald-600" placeholder="Nhập phản hồi cho khách hàng..." value={answer} onChange={(event) => onAnswer(event.target.value)} /><Button disabled={!answer.trim()} size="sm" onClick={onSubmit}><Send />Gửi trả lời</Button></div>}</aside>; }
function ChatPanel({ chat, loading, reply, sending, onAssign, onClose, onReply, onSubmit }: Readonly<{ chat: ChatConversationItem | null; loading: boolean; reply: string; sending: boolean; onAssign: () => void; onClose: () => void; onReply: (value: string) => void; onSubmit: (event: React.FormEvent) => void }>) { if (loading) return <EmptyDetail>Đang tải hội thoại...</EmptyDetail>; if (!chat) return <EmptyDetail>Chọn một hội thoại để đọc và phản hồi ngay tại đây.</EmptyDetail>; const closed = chat.status === "CLOSED"; return <aside className="flex min-h-96 max-h-[calc(100vh-7rem)] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white lg:sticky lg:top-24"><div className="border-b border-slate-100 bg-slate-50 p-4"><div className="flex justify-between gap-3"><div><h3 className="font-black">Hội thoại hỗ trợ</h3><p className="mt-1 text-xs text-slate-500">Khách hàng #{chat.customerId.slice(0, 8)}</p></div><Status value={chat.status} /></div><div className="mt-3 flex gap-2">{!chat.assignedAdminId && !closed ? <Button size="sm" variant="outline" onClick={onAssign}><UserCheck />Tiếp nhận</Button> : null}{!closed ? <Button size="sm" variant="ghost" onClick={onClose}><XCircle />Đóng</Button> : null}</div></div><div className="flex-1 space-y-3 overflow-y-auto bg-slate-50/50 p-4">{chat.messages.length ? chat.messages.map((message) => { const customer = message.senderRole === "CUSTOMER"; return <div className={customer ? "mr-6" : "ml-6 text-right"} key={message.id}><span className="mb-1 block text-[11px] text-slate-400">{customer ? "Khách hàng" : "Quản trị viên"} · {new Date(message.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</span><p className={`inline-block rounded-2xl px-3 py-2 text-left text-sm leading-5 ${customer ? "border border-slate-200 bg-white text-slate-800" : "bg-emerald-900 text-white"}`}>{decodeHtml(message.content)}</p></div>; }) : <p className="py-8 text-center text-sm text-slate-400">Chưa có tin nhắn.</p>}</div><form className="flex gap-2 border-t border-slate-100 p-3" onSubmit={onSubmit}><input className="min-w-0 flex-1 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-emerald-600" disabled={closed || sending} placeholder={closed ? "Hội thoại đã đóng" : "Nhập phản hồi..."} value={reply} onChange={(event) => onReply(event.target.value)} /><Button disabled={closed || sending || !reply.trim()} size="sm" type="submit"><Send />Gửi</Button></form></aside>; }
