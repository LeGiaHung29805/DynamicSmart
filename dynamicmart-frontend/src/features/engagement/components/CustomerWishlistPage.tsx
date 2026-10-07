"use client";

import {
  AlertCircle,
  Heart,
  Inbox,
  RefreshCw,
  ShoppingBag,
  Trash2,
} from "lucide-react";
import { useEffect, useState } from "react";
import Link from "next/link";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { engagementApi } from "../api/engagement.api";
import { date, money } from "./EngagementShared";

interface DisplayWishlistItem {
  id: string;
  productId: string;
  name: string;
  price?: number;
  savedAt: string;
}

export function CustomerWishlistPage() {
  const [items, setItems] = useState<DisplayWishlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        setErrorMsg("");
        const res = await engagementApi.wishlist.get();
        if (!ignore) {
          setItems(
            (res.items ?? []).map((item) => ({
                id: item.id,
                productId: item.productId,
                name: `Sản phẩm #${item.productId.slice(0, 8)}`,
                savedAt: item.createdAt,
              }))
          );
          setIsLive(true);
        }
      } catch {
        if (!ignore) {
          setItems([]);
          setIsLive(false);
          setErrorMsg("Không thể tải danh sách yêu thích.");
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

  const handleRemove = async (productId: string, itemId: string) => {
    setDeletingId(itemId);
    try {
      await engagementApi.wishlist.remove(productId);
      setItems((current) => current.filter((value) => value.id !== itemId));
    } catch {
      setErrorMsg("Không thể bỏ sản phẩm khỏi danh sách yêu thích.");
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Những sản phẩm bạn đã lưu để theo dõi và mua sắm sau."
          eyebrow="Tài khoản cá nhân"
          title="Danh sách yêu thích"
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

      <SurfacePanel className="p-4 sm:p-6">
        {errorMsg ? <p className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{errorMsg}</p> : null}
        {loading ? (
          <div className="py-12 text-center text-sm text-slate-500">
            <RefreshCw className="mx-auto size-6 animate-spin text-brand" />
            <p className="mt-3 font-semibold">Đang tải danh sách yêu thích...</p>
          </div>
        ) : items.length === 0 ? (
          <div className="py-12 text-center text-slate-400">
            <Inbox className="mx-auto size-10 stroke-1" />
            <p className="mt-3 font-bold text-slate-700">Danh sách yêu thích trống</p>
            <p className="mt-1 text-xs text-slate-500">
              Hãy bấm vào biểu tượng trái tim ở các sản phẩm bạn quan tâm để lưu lại tại đây.
            </p>
            <Link
              className="mt-4 inline-flex items-center gap-2 rounded-lg bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-slate-800"
              href="/products"
            >
              Khám phá sản phẩm
            </Link>
          </div>
        ) : (
          <div className="grid gap-3">
            {items.map((item) => (
              <div
                className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 p-4 transition hover:border-slate-300 hover:shadow-xs"
                key={item.id}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-rose-50 text-rose-600">
                    <Heart className="size-5 fill-current" />
                  </span>
                  <div className="min-w-0">
                    <p className="font-bold text-slate-950 truncate">{item.name}</p>
                    <p className="text-xs text-slate-500">
                      Đã lưu {date(item.savedAt)} {item.price ? `· ${money(item.price)}` : ""}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <Link
                    className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-bold text-slate-700 transition hover:bg-slate-50"
                    href={item.productId.startsWith("wl-") ? "/products" : `/products/${item.productId}`}
                  >
                    <ShoppingBag className="size-3.5" />
                    Xem sản phẩm
                  </Link>

                  <Button
                    aria-label="Bỏ yêu thích"
                    disabled={deletingId === item.id}
                    size="icon"
                    variant="ghost"
                    onClick={() => handleRemove(item.productId, item.id)}
                  >
                    <Trash2 className="size-4 text-slate-400 hover:text-rose-600 transition" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </SurfacePanel>
    </div>
  );
}
