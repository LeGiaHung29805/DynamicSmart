"use client";

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import { Bot, ChevronDown, MessageCircle, Send, X } from "lucide-react";
import { assistantApi, type AssistantCitation } from "../api/assistant.api";

type Message = {
  id: string;
  role: "assistant" | "customer";
  content: string;
  citations?: AssistantCitation[];
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

    setMessages((current) => [
      ...current,
      { id: crypto.randomUUID(), role: "customer", content: question },
    ]);
    setMessage("");
    setError("");
    setBusy(true);

    try {
      const response = await assistantApi.ask(question);
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: "assistant",
          content: response.reply,
          citations: response.citations,
        },
      ]);
    } catch {
      const text = "Trợ lý đang tạm thời không phản hồi. Vui lòng thử lại sau.";
      setError(text);
      setMessages((current) => [
        ...current,
        { id: crypto.randomUUID(), role: "assistant", content: text },
      ]);
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
                  <p>{item.content}</p>
                  {item.citations && item.citations.length > 0 && (
                    <details className="mt-3 border-t border-slate-200 pt-2 text-xs text-slate-500">
                      <summary className="flex cursor-pointer list-none items-center gap-1 font-bold text-emerald-800">
                        <ChevronDown className="size-3.5" /> Nguồn tham khảo
                      </summary>
                      <ul className="mt-2 space-y-2">
                        {item.citations.slice(0, 4).map((citation) => (
                          <li key={`${citation.source}-${citation.title}`}>
                            <strong className="block text-slate-700">{citation.title}</strong>
                            <span>{citation.excerpt}</span>
                          </li>
                        ))}
                      </ul>
                    </details>
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

            {busy && <p className="text-xs font-medium text-slate-500">Đang truy xuất tài liệu…</p>}
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

