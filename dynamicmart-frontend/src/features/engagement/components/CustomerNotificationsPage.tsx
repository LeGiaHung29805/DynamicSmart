"use client";

import {
  AlertCircle,
  Bell,
  CheckCheck,
  Inbox,
  Package,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { engagementApi } from "../api/engagement.api";
import type { NotificationItem } from "../types/engagement.types";
import { date, notificationTypeLabel } from "./EngagementShared";

export function CustomerNotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [filter, setFilter] = useState<"ALL" | "UNREAD">("ALL");
  const [markingId, setMarkingId] = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        setErrorMsg("");
        const res = await engagementApi.notifications.list(0, 50);
        if (!ignore) {
          setItems(res.content ?? []);
          setIsLive(true);
        }
      } catch {
        if (!ignore) {
          setItems([]);
          setIsLive(false);
          setErrorMsg("Không thể tải thông báo từ hệ thống.");
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

  const handleMarkAsRead = async (notificationId: string) => {
    setMarkingId(notificationId);
    try {
      await engagementApi.notifications.markRead(notificationId);
      setItems((prev) =>
        prev.map((n) =>
          n.id === notificationId
            ? { ...n, read: true, readAt: new Date().toISOString() }
            : n
        )
      );
    } catch {
      setErrorMsg("Không thể đánh dấu thông báo đã đọc.");
    } finally {
      setMarkingId(null);
    }
  };

  const handleMarkAllAsRead = async () => {
    const unreadItems = items.filter((n) => !n.read && !n.readAt);
    if (unreadItems.length === 0) return;

    try {
      await Promise.all(unreadItems.map((item) => engagementApi.notifications.markRead(item.id)));
      setItems((prev) => prev.map((n) => ({ ...n, read: true, readAt: new Date().toISOString() })));
    } catch {
      setErrorMsg("Không thể đánh dấu toàn bộ thông báo đã đọc.");
    }
  };

  const isItemRead = (item: NotificationItem) => Boolean(item.read || item.readAt);

  const displayedItems =
    filter === "UNREAD"
      ? items.filter((item) => !isItemRead(item))
      : items;

  const unreadCount = items.filter((item) => !isItemRead(item)).length;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Cập nhật nhanh về tình trạng đơn hàng, thanh toán và câu hỏi sản phẩm của bạn."
          eyebrow="Tài khoản cá nhân"
          title="Thông báo hệ thống"
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

      <SurfacePanel className="p-4 sm:p-6">
        {errorMsg ? <p className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{errorMsg}</p> : null}
        {/* Thanh công cụ lọc & Đánh dấu đọc tất cả */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
          <div className="flex gap-2">
            <button
              className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                filter === "ALL"
                  ? "bg-slate-900 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
              type="button"
              onClick={() => setFilter("ALL")}
            >
              Tất cả ({items.length})
            </button>
            <button
              className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                filter === "UNREAD"
                  ? "bg-rose-600 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
              type="button"
              onClick={() => setFilter("UNREAD")}
            >
              Chưa đọc ({unreadCount})
            </button>
          </div>

          {unreadCount > 0 ? (
            <Button size="sm" variant="ghost" onClick={handleMarkAllAsRead}>
              <CheckCheck className="size-4 text-emerald-700" />
              Đánh dấu tất cả đã đọc
            </Button>
          ) : null}
        </div>

        {/* Danh sách thông báo */}
        <div className="mt-4 space-y-3">
          {loading ? (
            <div className="py-12 text-center text-sm text-slate-500">
              <RefreshCw className="mx-auto size-6 animate-spin text-brand" />
              <p className="mt-3 font-semibold">Đang tải danh sách thông báo...</p>
            </div>
          ) : displayedItems.length === 0 ? (
            <div className="py-12 text-center text-slate-400">
              <Inbox className="mx-auto size-10 stroke-1" />
              <p className="mt-3 font-bold text-slate-700">Không có thông báo nào</p>
              <p className="mt-1 text-xs text-slate-500">
                {filter === "UNREAD"
                  ? "Bạn đã đọc hết tất cả thông báo hiện có."
                  : "Khi có cập nhật đơn hàng hoặc phản hồi, hệ thống sẽ gửi tại đây."}
              </p>
            </div>
          ) : (
            displayedItems.map((item) => {
              const read = isItemRead(item);
              const isMarking = markingId === item.id;
              const isOrder = item.type.includes("ORDER");
              const isPayment = item.type.includes("PAYMENT");

              return (
                <article
                  className={`rounded-xl border p-4 transition ${
                    read
                      ? "border-slate-200 bg-white hover:border-slate-300"
                      : "border-emerald-200 bg-emerald-50/50 shadow-sm"
                  }`}
                  key={item.id}
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="flex gap-3 min-w-0">
                      <span
                        className={`grid size-9 shrink-0 place-items-center rounded-xl text-sm ${
                          isOrder
                            ? "bg-blue-100 text-blue-700"
                            : isPayment
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-amber-100 text-amber-700"
                        }`}
                      >
                        {isOrder ? (
                          <Package className="size-4" />
                        ) : isPayment ? (
                          <Sparkles className="size-4" />
                        ) : (
                          <Bell className="size-4" />
                        )}
                      </span>

                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <p className="font-bold text-sm text-slate-950 truncate">
                            {item.title}
                          </p>
                          {!read ? (
                            <span className="size-2 rounded-full bg-rose-500" />
                          ) : null}
                        </div>
                        <p className="mt-1 text-sm leading-6 text-slate-600">
                          {item.content}
                        </p>
                        <p className="mt-1 text-xs text-slate-400">
                          {date(item.createdAt)} · <span className="text-[11px]">{notificationTypeLabel(item.type)}</span>
                        </p>
                      </div>
                    </div>

                    {!read ? (
                      <Button
                        disabled={isMarking}
                        size="sm"
                        variant="outline"
                        onClick={() => handleMarkAsRead(item.id)}
                      >
                        <CheckCheck className="size-3.5 text-emerald-600" />
                        Đã đọc
                      </Button>
                    ) : null}
                  </div>
                </article>
              );
            })
          )}
        </div>
      </SurfacePanel>
    </div>
  );
}
