"use client";

import {
  AlertCircle,
  MessageSquarePlus,
  RefreshCw,
  Send,
  User,
  Headphones,
} from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { engagementApi } from "../api/engagement.api";
import type { ChatConversationItem } from "../types/engagement.types";
import { engagementStatusLabel, Status } from "./EngagementShared";

export function CustomerSupportPage() {
  const [conversations, setConversations] = useState<ChatConversationItem[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [messageInput, setMessageInput] = useState("");
  const [sending, setSending] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        setErrorMsg("");
        const res = await engagementApi.support.customerConversations(0, 20);
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
          setErrorMsg("Không thể tải hội thoại hỗ trợ.");
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
  }, [refreshTrigger]);

  const selected =
    conversations.find((item) => item.id === selectedId) ?? conversations[0];

  const handleCreateConversation = async () => {
    try {
      setErrorMsg("");
      const newConv = await engagementApi.support.createConversation("Hỗ trợ đơn hàng");
      setConversations((prev) => [newConv, ...prev]);
      setSelectedId(newConv.id);
      setIsLive(true);
    } catch {
      setErrorMsg("Không thể tạo hội thoại hỗ trợ.");
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
      setConversations((prev) => prev.map((c) => (c.id === selected.id ? updatedConv : c)));
      setMessageInput("");
    } catch {
      setErrorMsg("Không thể gửi tin nhắn. Nội dung chưa được lưu.");
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Theo dõi các trao đổi hỗ trợ khách hàng và giải đáp thắc mắc sau mua hàng."
          eyebrow="Hỗ trợ & Chăm sóc"
          title="Hội thoại với nhân viên hỗ trợ"
        />

        <div className="flex items-center gap-2">
          {isLive ? (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-800 shadow-sm">
              <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
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

      <div className="grid gap-5 lg:grid-cols-[290px_1fr]">
        {errorMsg ? <p className="lg:col-span-2 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{errorMsg}</p> : null}
        {/* Danh sách các cuộc hội thoại */}
        <SurfacePanel className="p-4 sm:p-4 h-fit">
          <Button
            className="mb-4 w-full"
            onClick={handleCreateConversation}
          >
            <MessageSquarePlus className="size-4" />
            Tạo hội thoại mới
          </Button>

          <div className="space-y-2">
            {conversations.length === 0 ? (
              <p className="text-center text-xs text-slate-500 py-6">
                Chưa có hội thoại nào
              </p>
            ) : (
              conversations.map((conversation) => (
                <button
                  className={`w-full rounded-xl border p-3 text-left transition ${
                    selected?.id === conversation.id
                      ? "border-emerald-600 bg-emerald-50/70 shadow-xs"
                      : "border-slate-200 bg-white hover:border-slate-300"
                  }`}
                  key={conversation.id}
                  onClick={() => setSelectedId(conversation.id)}
                  type="button"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-950 text-sm">
                      #{conversation.id.slice(0, 10)}
                    </span>
                    <span
                      className={`text-[10px] font-bold px-1.5 py-0.5 rounded-md ${
                        conversation.status === "OPEN"
                          ? "bg-emerald-100 text-emerald-800"
                          : "bg-slate-100 text-slate-600"
                      }`}
                    >
                      {engagementStatusLabel(conversation.status)}
                    </span>
                  </div>
                  <span className="mt-1 block text-xs text-slate-500 truncate">
                    {conversation.messages && conversation.messages.length > 0
                      ? conversation.messages[conversation.messages.length - 1].content
                      : "Chưa có tin nhắn"}
                  </span>
                </button>
              ))
            )}
          </div>
        </SurfacePanel>

        {/* Khung chat chi tiết */}
        {selected ? (
          <SurfacePanel className="flex flex-col min-h-[480px]">
            <div className="flex items-center justify-between gap-3 border-b border-slate-100 pb-3">
              <div>
                <h2 className="font-black text-slate-950">
                  Hội thoại #{selected.id.slice(0, 12)}
                </h2>
                <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                  <Headphones className="size-3.5 text-slate-400" />
                  Người trực:{" "}
                  <span className="font-medium text-slate-700">
                    {selected.assignedAdminId || "Đang chờ admin tiếp nhận"}
                  </span>
                </p>
              </div>
              <Status value={selected.status} />
            </div>

            {/* Danh sách tin nhắn */}
            <div className="flex-1 my-4 space-y-3 overflow-y-auto max-h-[360px] pr-1">
              {(!selected.messages || selected.messages.length === 0) ? (
                <div className="text-center py-12 text-slate-400 text-sm">
                  Chưa có tin nhắn trong cuộc trò chuyện này. Hãy gửi tin nhắn đầu tiên!
                </div>
              ) : (
                selected.messages.map((message, index) => {
                  const isCust = message.senderRole === "CUSTOMER";
                  return (
                    <div
                      className={`flex flex-col max-w-[80%] ${
                        isCust ? "ml-auto items-end" : "mr-auto items-start"
                      }`}
                      key={`${message.id || index}`}
                    >
                      <div
                        className={`rounded-2xl px-4 py-2.5 text-sm ${
                          isCust
                            ? "bg-emerald-950 text-white rounded-br-xs"
                            : "bg-slate-100 text-slate-800 rounded-bl-xs"
                        }`}
                      >
                        <p className="leading-relaxed">{message.content}</p>
                      </div>
                      <span className="mt-1 text-[11px] text-slate-400 flex items-center gap-1">
                        {isCust ? (
                          <User className="size-3" />
                        ) : (
                          <Headphones className="size-3" />
                        )}
                        {message.createdAt}
                      </span>
                    </div>
                  );
                })
              )}
            </div>

            {/* Input gửi tin nhắn */}
            <form
              className="mt-auto flex gap-2 border-t border-slate-100 pt-3"
              onSubmit={handleSendMessage}
            >
              <Input
                aria-label="Tin nhắn"
                className="flex-1 text-sm"
                disabled={selected.status === "CLOSED" || sending}
                placeholder={
                  selected.status === "CLOSED"
                    ? "Cuộc hội thoại này đã đóng..."
                    : "Nhập nội dung tin nhắn hỗ trợ..."
                }
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
              />
              <Button
                disabled={!messageInput.trim() || selected.status === "CLOSED" || sending}
                size="icon-lg"
                type="submit"
              >
                <Send className="size-4" />
              </Button>
            </form>
          </SurfacePanel>
        ) : (
          <SurfacePanel className="flex items-center justify-center min-h-[480px]">
            <p className="text-slate-400 text-sm">Vui lòng chọn hoặc tạo cuộc hội thoại</p>
          </SurfacePanel>
        )}
      </div>
    </div>
  );
}
