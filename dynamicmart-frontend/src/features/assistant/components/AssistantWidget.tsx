"use client";

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import { Bot, MessageCircle, PackageCheck, Send, X } from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { assistantApi, type AssistantHistoryTurn, type AssistantProduct } from "../api/assistant.api";

type Message = {
  id: string;
  role: "assistant" | "customer";
  content: string;
  products?: AssistantProduct[];
};

const suggestions = [
  "DynamicMart hỗ trợ phương thức thanh toán nào?",
  "Phí vận chuyển được tính như thế nào?",
  "Mua ngay có làm thay đổi giỏ hàng không?",
];

export function AssistantWidget() {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "welcome",
      role: "assistant",
      content: "Xin chào! Tôi trả lời câu hỏi mua sắm dựa trên tài liệu đã được DynamicMart kiểm duyệt.",
    },
  ]);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const messageListRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messageListRef.current?.scrollTo({ top: messageListRef.current.scrollHeight });
  }, [messages, busy]);

  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  async function submit(event?: FormEvent, suggestedMessage?: string) {
    event?.preventDefault();
    const question = (suggestedMessage ?? message).trim();
    if (question.length < 2 || busy) return;

    const history: AssistantHistoryTurn[] = messages
      .filter((item) => item.id !== "welcome" && item.content.trim())
      .slice(-10)
      .map((item) => {
        const productNames = item.products?.map((product) => product.name).filter(Boolean) ?? [];
        const productContext = productNames.length > 0
          ? ` Sản phẩm đã hiển thị: ${productNames.join(", ")}.`
          : "";
        return {
          role: item.role === "customer" ? "user" : "assistant",
          content: `${item.content}${productContext}`.slice(0, 800),
        };
      });

    setMessages((current) => [
      ...current,
      { id: crypto.randomUUID(), role: "customer", content: question },
    ]);
    setMessage("");
    setError("");
    setBusy(true);
    const answerId = crypto.randomUUID();
    setMessages((current) => [
      ...current,
      { id: answerId, role: "assistant", content: "" },
    ]);

    try {
      const response = await assistantApi.askStream(question, history, (content, replace) => {
        setMessages((current) => current.map((item) => item.id === answerId
          ? { ...item, content: replace ? content : item.content + content }
          : item));
      });
      setMessages((current) => current.map((item) => item.id === answerId
        ? {
            ...item,
            content: response.reply,
            products: response.products,
          }
        : item));
    } catch {
      const text = "Trợ lý đang tạm thời không phản hồi. Vui lòng thử lại sau.";
      setError(text);
      setMessages((current) => current.map((item) => item.id === answerId
        ? { ...item, content: text }
        : item));
    } finally {
      setBusy(false);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void submit();
    }
  }

  return (
    <div className="fixed right-4 bottom-5 z-[70] sm:right-6 sm:bottom-6">
      {open && (
        <section
          id="dynamicmart-assistant-panel"
          aria-label="Trợ lý mua sắm DynamicMart"
          className="mb-3 flex h-[min(36rem,calc(100vh-7rem))] w-[calc(100vw-2rem)] max-w-md flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-2xl"
        >
          <header className="flex items-center justify-between bg-emerald-950 px-4 py-3 text-white">
            <div className="flex items-center gap-3">
              <span className="grid size-10 place-items-center rounded-2xl bg-emerald-400 text-emerald-950">
                <Bot className="size-5" />
              </span>
              <div>
                <h2 className="font-black tracking-tight">Trợ lý DynamicMart</h2>
                <p className="text-xs text-emerald-100">RAG · trả lời có nguồn</p>
              </div>
            </div>
            <button
              aria-label="Đóng trợ lý"
              className="grid size-9 place-items-center rounded-xl text-emerald-100 transition hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
              onClick={() => setOpen(false)}
              type="button"
            >
              <X className="size-5" />
            </button>
          </header>

          <div ref={messageListRef} className="flex-1 space-y-3 overflow-y-auto bg-slate-50 p-4" aria-live="polite">
            {messages.map((item) => (
              <div key={item.id} className={`flex ${item.role === "customer" ? "justify-end" : "justify-start"}`}>
                <div className={`max-w-[88%] rounded-2xl px-4 py-3 text-sm leading-6 shadow-sm ${item.role === "customer" ? "rounded-br-md bg-slate-950 text-white" : "rounded-bl-md bg-white text-slate-700"}`}>
                  <p className="whitespace-pre-line break-words">{item.content || "Đang chuẩn bị câu trả lời…"}</p>
                  {item.products && item.products.length > 0 && (
                    <div className="mt-3 space-y-2 border-t border-slate-200 pt-3">
                      {item.products.slice(0, 6).map((product) => (
                        <a
                          key={product.id ?? product.url}
                          className="flex gap-3 rounded-xl border border-slate-200 bg-white p-2.5 transition hover:border-emerald-300 hover:bg-emerald-50/50"
                          href={product.url}
                        >
                          {product.image_url ? (
                            // Ảnh do Catalog Service quản lý; dùng img để hỗ trợ object-storage host động.
                            // eslint-disable-next-line @next/next/no-img-element
                            <img className="size-16 shrink-0 rounded-lg bg-slate-100 object-cover" src={product.image_url} alt="" />
                          ) : (
                            <span className="grid size-16 shrink-0 place-items-center rounded-lg bg-slate-100 text-[10px] font-bold text-slate-400">DynamicMart</span>
                          )}
                          <span className="min-w-0 flex-1">
                            <strong className="line-clamp-2 block leading-5 text-slate-900">{product.name}</strong>
                            {product.detail ? <span className="line-clamp-1 block text-xs text-slate-500">{product.detail}</span> : null}
                            <span className="mt-1 flex flex-wrap items-center gap-2">
                              {product.price !== null && product.price !== undefined ? <b className="text-emerald-800">{formatVnd(product.price)}</b> : null}
                              {product.compare_at_price !== null && product.compare_at_price !== undefined ? <del className="text-xs text-slate-400">{formatVnd(product.compare_at_price)}</del> : null}
                            </span>
                            <span className="mt-1 inline-flex items-center gap-1 text-xs font-semibold text-emerald-700"><PackageCheck className="size-3.5" /> Còn hàng</span>
                          </span>
                        </a>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}

            {messages.length === 1 && (
              <div className="flex flex-wrap gap-2">
                {suggestions.map((suggestion) => (
                  <button
                    key={suggestion}
                    className="rounded-full border border-emerald-200 bg-white px-3 py-1.5 text-left text-xs font-semibold text-emerald-800 transition hover:border-emerald-400 hover:bg-emerald-50"
                    onClick={() => void submit(undefined, suggestion)}
                    type="button"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            )}

            {busy && <p className="text-xs font-medium text-slate-500">Đang tìm trong tài liệu và danh mục sản phẩm…</p>}
          </div>

          {error && <p role="alert" className="border-t border-red-100 bg-red-50 px-4 py-2 text-xs text-red-700">{error}</p>}

          <form onSubmit={submit} className="flex items-end gap-2 border-t border-slate-200 bg-white p-3">
            <label className="sr-only" htmlFor="dynamicmart-assistant-message">Nội dung cần hỏi</label>
            <textarea
              ref={inputRef}
              id="dynamicmart-assistant-message"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={handleKeyDown}
              maxLength={500}
              rows={1}
              disabled={busy}
              placeholder="Hỏi về mua hàng, thanh toán…"
              className="max-h-28 min-h-11 flex-1 resize-none rounded-xl border border-slate-300 px-3 py-2.5 text-sm outline-none transition focus:border-emerald-600 focus:ring-4 focus:ring-emerald-100 disabled:bg-slate-100"
            />
            <button
              aria-label="Gửi câu hỏi"
              className="grid size-11 place-items-center rounded-xl bg-emerald-700 text-white transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
              disabled={busy || message.trim().length < 2}
              type="submit"
            >
              <Send className="size-4" />
            </button>
          </form>
        </section>
      )}

      <button
        aria-controls="dynamicmart-assistant-panel"
        aria-expanded={open}
        aria-label={open ? "Đóng trợ lý mua sắm" : "Mở trợ lý mua sắm"}
        className="ml-auto grid size-14 place-items-center rounded-2xl bg-emerald-700 text-white shadow-xl shadow-emerald-950/20 transition hover:-translate-y-0.5 hover:bg-emerald-800 focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-emerald-200"
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        {open ? <X className="size-5" /> : <MessageCircle className="size-6" />}
      </button>
    </div>
  );
}

