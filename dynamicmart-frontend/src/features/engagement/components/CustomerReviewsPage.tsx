"use client";

import {
  AlertCircle,
  CheckCircle2,
  Clock,
  ExternalLink,
  MessageSquare,
  Package,
  RefreshCw,
  ShoppingBag,
  Star,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { useAuthSession } from "@/lib/auth/session";
import { customerOrdersApi, type PurchasedOrderItem } from "@/features/order/api/customer-orders.api";
import { engagementApi } from "../api/engagement.api";
import type { ReviewItem } from "../types/engagement.types";
import { CreateReviewModal } from "./CreateReviewModal";
import { date, money } from "./EngagementShared";

function decodeEntities(text?: string | null): string {
  if (!text) return "";
  return text
    .replace(/&agrave;/gi, "à")
    .replace(/&aacute;/gi, "á")
    .replace(/&acirc;/gi, "â")
    .replace(/&atilde;/gi, "ã")
    .replace(/&egrave;/gi, "è")
    .replace(/&eacute;/gi, "é")
    .replace(/&ecirc;/gi, "ê")
    .replace(/&igrave;/gi, "ì")
    .replace(/&iacute;/gi, "í")
    .replace(/&ograve;/gi, "ò")
    .replace(/&oacute;/gi, "ó")
    .replace(/&ocirc;/gi, "ô")
    .replace(/&otilde;/gi, "õ")
    .replace(/&ugrave;/gi, "ù")
    .replace(/&uacute;/gi, "ú")
    .replace(/&yacute;/gi, "ý")
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/&amp;/gi, "&")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">");
}

export function CustomerReviewsPage() {
  const session = useAuthSession();
  const [activeTab, setActiveTab] = useState<"pending" | "reviewed">("pending");
  const [loading, setLoading] = useState(true);
  const [purchasedItems, setPurchasedItems] = useState<PurchasedOrderItem[]>([]);
  const [myReviews, setMyReviews] = useState<ReviewItem[]>([]);
  const [errorMsg, setErrorMsg] = useState("");
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // Modal review state
  const [selectedItemForReview, setSelectedItemForReview] = useState<PurchasedOrderItem | null>(null);

  useEffect(() => {
    if (session.status !== "authenticated") {
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setErrorMsg("");

    Promise.all([
      customerOrdersApi.getPurchasedItems().catch(() => []),
      engagementApi.reviews.getMyReviews(0, 100).catch(() => ({ content: [] })),
    ])
      .then(([items, reviewsRes]) => {
        if (cancelled) return;
        setPurchasedItems(items);
        setMyReviews(reviewsRes.content ?? []);
      })
      .catch(() => {
        if (!cancelled) setErrorMsg("Không thể tải danh sách đánh giá của bạn. Vui lòng thử lại sau.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [session.status, refreshTrigger]);

  // Lọc sản phẩm chờ đánh giá (thuộc đơn COMPLETED nhưng chưa có review cho orderItemId đó)
  const pendingItems = purchasedItems.filter(
    (item) => !myReviews.some((r) => r.orderItemId === item.orderItemId)
  );

  const handleReviewSuccess = (newReview: ReviewItem) => {
    setMyReviews((prev) => [newReview, ...prev]);
    setSelectedItemForReview(null);
    setActiveTab("reviewed");
  };

  if (session.status === "anonymous") {
    return (
      <div className="space-y-6">
        <PageHeader
          description="Quản lý và chia sẻ đánh giá về các sản phẩm bạn đã mua."
          eyebrow="Tài khoản cá nhân"
          title="Đánh giá của tôi"
        />
        <SurfacePanel className="py-12 text-center">
          <Star className="mx-auto size-12 text-amber-500 fill-amber-100" />
          <h2 className="mt-4 text-lg font-black text-slate-950">Vui lòng đăng nhập</h2>
          <p className="mt-1 text-sm text-slate-500">
            Đăng nhập để xem danh sách sản phẩm chờ đánh giá và các nhận xét đã gửi.
          </p>
          <Link
            className="mt-5 inline-flex rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-bold text-white hover:bg-slate-800"
            href="/login?returnTo=%2Fcustomer%2Faccount%2Freviews"
          >
            Đăng nhập ngay
          </Link>
        </SurfacePanel>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Đánh giá sản phẩm đã mua để tích điểm và giúp đỡ người mua khác đưa ra lựa chọn đúng đắn."
          eyebrow="Tài khoản cá nhân"
          title="Đánh giá của tôi"
        />

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

      {errorMsg ? (
        <div className="flex items-center gap-2 rounded-xl bg-rose-50 p-3.5 text-sm text-rose-700">
          <AlertCircle className="size-4 shrink-0 text-rose-600" />
          <span>{errorMsg}</span>
        </div>
      ) : null}

      {/* Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          className={`flex items-center gap-2 border-b-2 px-5 py-3 text-sm font-bold transition ${
            activeTab === "pending"
              ? "border-amber-600 text-amber-900"
              : "border-transparent text-slate-500 hover:text-slate-900"
          }`}
          onClick={() => setActiveTab("pending")}
          type="button"
        >
          <Clock className="size-4" />
          Chưa đánh giá
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-black ${
              activeTab === "pending"
                ? "bg-amber-100 text-amber-900"
                : "bg-slate-100 text-slate-600"
            }`}
          >
            {pendingItems.length}
          </span>
        </button>

        <button
          className={`flex items-center gap-2 border-b-2 px-5 py-3 text-sm font-bold transition ${
            activeTab === "reviewed"
              ? "border-emerald-700 text-emerald-950"
              : "border-transparent text-slate-500 hover:text-slate-900"
          }`}
          onClick={() => setActiveTab("reviewed")}
          type="button"
        >
          <CheckCircle2 className="size-4" />
          Đã đánh giá
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-black ${
              activeTab === "reviewed"
                ? "bg-emerald-100 text-emerald-900"
                : "bg-slate-100 text-slate-600"
            }`}
          >
            {myReviews.length}
          </span>
        </button>
      </div>

      {loading ? (
        <SurfacePanel className="py-16 text-center text-sm text-slate-500">
          <RefreshCw className="mx-auto size-7 animate-spin text-brand" />
          <p className="mt-3 font-semibold text-slate-700">Đang tải lịch sử đánh giá...</p>
        </SurfacePanel>
      ) : activeTab === "pending" ? (
        /* Tab Chờ đánh giá */
        <SurfacePanel className="p-4 sm:p-6">
          {pendingItems.length === 0 ? (
            <div className="py-12 text-center text-slate-400">
              <CheckCircle2 className="mx-auto size-12 text-emerald-600/70" />
              <h3 className="mt-3 text-base font-bold text-slate-900">
                Tuyệt vời! Không có sản phẩm nào chờ đánh giá
              </h3>
              <p className="mt-1 text-xs text-slate-500 max-w-md mx-auto">
                Tất cả sản phẩm trong các đơn hàng hoàn tất của bạn đã được đánh giá hoặc bạn chưa có đơn hàng hoàn thành mới nào.
              </p>
              <Link
                className="mt-5 inline-flex items-center gap-2 rounded-xl bg-slate-950 px-4 py-2.5 text-xs font-bold text-white hover:bg-slate-800"
                href="/products"
              >
                <ShoppingBag className="size-3.5" />
                Khám phá sản phẩm mua sắm
              </Link>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {pendingItems.map((item) => (
                <div
                  className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center sm:justify-between first:pt-0 last:pb-0"
                  key={item.orderItemId}
                >
                  <div className="flex items-center gap-3.5 min-w-0">
                    {item.imageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        alt={item.productName}
                        className="size-16 rounded-xl border border-slate-200 object-cover shrink-0"
                        src={item.imageUrl}
                      />
                    ) : (
                      <div className="grid size-16 shrink-0 place-items-center rounded-xl bg-slate-100 text-xs font-bold text-slate-400">
                        SP
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="font-bold text-slate-950 text-sm truncate">
                        {item.productName}
                      </p>
                      {item.variantName ? (
                        <p className="text-xs text-slate-500">{item.variantName}</p>
                      ) : null}
                      <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                        <span>Đơn #{item.orderNumber}</span>
                        {item.completedAt ? (
                          <span>· Hoàn tất {date(item.completedAt)}</span>
                        ) : null}
                        <span>· {money(item.unitPriceVnd)}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <Link
                      className="inline-flex items-center gap-1 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50"
                      href={`/products/${item.productId}`}
                    >
                      <ExternalLink className="size-3.5" />
                      Xem sản phẩm
                    </Link>
                    <Button
                      className="gap-1.5 bg-amber-600 hover:bg-amber-700 text-white font-bold"
                      size="sm"
                      onClick={() => setSelectedItemForReview(item)}
                    >
                      <Star className="size-3.5 fill-amber-300 text-amber-100" />
                      Đánh giá ngay
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </SurfacePanel>
      ) : (
        /* Tab Đã đánh giá */
        <SurfacePanel className="p-4 sm:p-6">
          {myReviews.length === 0 ? (
            <div className="py-12 text-center text-slate-400">
              <MessageSquare className="mx-auto size-12 text-slate-300" />
              <h3 className="mt-3 text-base font-bold text-slate-900">
                Bạn chưa viết đánh giá nào
              </h3>
              <p className="mt-1 text-xs text-slate-500">
                Hãy chia sẻ cảm nhận về các sản phẩm bạn đã mua để giúp cộng đồng mua sắm tốt hơn.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {myReviews.map((review) => (
                <article
                  className="rounded-2xl border border-slate-200 bg-white p-4.5 transition hover:border-slate-300"
                  key={review.id}
                >
                  <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3">
                    <div className="flex items-center gap-2">
                      <div className="flex items-center gap-0.5 text-amber-500">
                        {[1, 2, 3, 4, 5].map((s) => (
                          <Star
                            className={`size-4 ${
                              s <= review.rating ? "fill-current" : "text-slate-200"
                            }`}
                            key={s}
                          />
                        ))}
                      </div>
                      <span className="text-xs font-bold text-amber-700">
                        {review.rating}/5 sao
                      </span>
                    </div>

                    <div className="flex items-center gap-2 text-xs text-slate-500">
                      <span>Đánh giá ngày {date(review.createdAt)}</span>
                      <span className="rounded-full bg-emerald-50 px-2 py-0.5 font-bold text-emerald-700">
                        {review.status === "VISIBLE" ? "Đã duyệt & Hiển thị" : "Đang kiểm duyệt"}
                      </span>
                    </div>
                  </div>

                  {review.content ? (
                    <p className="mt-3 text-sm leading-6 text-slate-800">
                      {decodeEntities(review.content)}
                    </p>
                  ) : (
                    <p className="mt-3 text-xs italic text-slate-400">
                      (Không có nhận xét chi tiết)
                    </p>
                  )}

                  {review.imageUrls && review.imageUrls.length > 0 ? (
                    <div className="mt-3 flex flex-wrap gap-2">
                      {review.imageUrls.map((url) => (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          alt="Ảnh feedback"
                          className="size-16 rounded-xl border border-slate-200 object-cover"
                          key={url}
                          src={url}
                        />
                      ))}
                    </div>
                  ) : null}

                  <div className="mt-3 flex items-center justify-end border-t border-slate-50 pt-2">
                    <Link
                      className="inline-flex items-center gap-1 text-xs font-bold text-brand hover:underline"
                      href={`/products/${review.productId}`}
                    >
                      Xem trang sản phẩm <ExternalLink className="size-3" />
                    </Link>
                  </div>
                </article>
              ))}
            </div>
          )}
        </SurfacePanel>
      )}

      {/* Modal Đánh giá khi nhấn "Đánh giá ngay" */}
      <CreateReviewModal
        isOpen={!!selectedItemForReview}
        orderItemId={selectedItemForReview?.orderItemId}
        orderNumber={selectedItemForReview?.orderNumber}
        productImage={selectedItemForReview?.imageUrl}
        productName={selectedItemForReview?.productName}
        onClose={() => setSelectedItemForReview(null)}
        onSuccess={handleReviewSuccess}
      />
    </div>
  );
}
