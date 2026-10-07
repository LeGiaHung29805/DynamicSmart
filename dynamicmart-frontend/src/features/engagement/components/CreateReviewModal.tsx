"use client";

import { AlertCircle, CheckCircle2, ImagePlus, Star, Trash2, X } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { engagementApi } from "../api/engagement.api";
import type { CreateReviewPayload, ReviewItem } from "../types/engagement.types";

export interface CreateReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  orderItemId?: string;
  productName?: string;
  onSuccess?: (review: ReviewItem) => void;
}

const RATING_LABELS: Record<number, string> = {
  1: "Rất không hài lòng",
  2: "Không hài lòng",
  3: "Bình thường",
  4: "Hài lòng",
  5: "Cực kỳ hài lòng",
};

export function CreateReviewModal({
  isOpen,
  onClose,
  orderItemId: defaultOrderItemId = "",
  productName,
  onSuccess,
}: Readonly<CreateReviewModalProps>) {
  const [orderItemId, setOrderItemId] = useState(defaultOrderItemId);
  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [content, setContent] = useState("");
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [newImageUrl, setNewImageUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSuccess, setIsSuccess] = useState(false);

  if (!isOpen) return null;

  const handleAddImageUrl = () => {
    const trimmed = newImageUrl.trim();
    if (!trimmed) return;
    if (imageUrls.length >= 5) {
      setErrorMessage("Chỉ được đính kèm tối đa 5 hình ảnh.");
      return;
    }
    if (imageUrls.includes(trimmed)) {
      setErrorMessage("Hình ảnh này đã được thêm.");
      return;
    }
    setImageUrls([...imageUrls, trimmed]);
    setNewImageUrl("");
    setErrorMessage("");
  };

  const handleRemoveImageUrl = (index: number) => {
    setImageUrls(imageUrls.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");

    const targetOrderItemId = (orderItemId || defaultOrderItemId).trim();
    if (!targetOrderItemId) {
      setErrorMessage("Vui lòng nhập mã dòng đơn hàng (OrderItem ID) đã mua.");
      return;
    }

    if (rating < 1 || rating > 5) {
      setErrorMessage("Vui lòng chọn số sao đánh giá từ 1 đến 5.");
      return;
    }

    setSubmitting(true);
    const payload: CreateReviewPayload = {
      orderItemId: targetOrderItemId,
      rating,
      content: content.trim() || undefined,
      imageUrls: imageUrls.length > 0 ? imageUrls : undefined,
    };

    try {
      const created = await engagementApi.reviews.create(payload);
      setIsSuccess(true);
      setTimeout(() => {
        onSuccess?.(created);
        onClose();
        setIsSuccess(false);
        setContent("");
        setImageUrls([]);
      }, 1200);
    } catch (err: unknown) {
      const errorObj = err as { message?: string; payload?: { message?: string } };
      const serverMsg =
        errorObj.payload?.message ||
        errorObj.message ||
        "Không thể gửi đánh giá. Hãy kiểm tra xem đơn hàng đã hoàn tất chưa.";
      setErrorMessage(serverMsg);
    } finally {
      setSubmitting(false);
    }
  };

  const activeRating = hoverRating || rating;

  return (
    <div
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm"
      role="dialog"
    >
      <div className="relative w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-7">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-4">
          <div>
            <p className="text-xs font-bold tracking-wider text-brand uppercase">Đánh giá sản phẩm</p>
            <h2 className="mt-1 text-xl font-black text-slate-950">
              {productName ? `Đánh giá "${productName}"` : "Chia sẻ cảm nhận về sản phẩm"}
            </h2>
          </div>
          <button
            aria-label="Đóng"
            className="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            onClick={onClose}
            type="button"
          >
            <X className="size-5" />
          </button>
        </div>

        {isSuccess ? (
          <div className="py-10 text-center">
            <CheckCircle2 className="mx-auto size-12 text-emerald-600" />
            <h3 className="mt-3 text-lg font-bold text-slate-950">Gửi đánh giá thành công!</h3>
            <p className="mt-1 text-sm text-slate-500">Cảm ơn bạn đã đóng góp phản hồi về sản phẩm.</p>
          </div>
        ) : (
          <form className="mt-5 space-y-5" onSubmit={handleSubmit}>
            {errorMessage ? (
              <div className="flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-800">
                <AlertCircle className="mt-0.5 size-4 shrink-0 text-rose-600" />
                <span>{errorMessage}</span>
              </div>
            ) : null}

            {/* Order Item ID */}
            {!defaultOrderItemId ? (
              <div>
                <Input
                  label="Mã dòng đơn hàng (OrderItem ID)"
                  name="orderItemId"
                  placeholder="Nhập UUID dòng đơn hàng đã mua..."
                  required
                  value={orderItemId}
                  onChange={(e) => setOrderItemId(e.target.value)}
                />
                <div className="mt-1.5 flex items-center justify-between text-xs text-slate-500">
                  <span>Chỉ dòng đơn hoàn tất (COMPLETED) mới được đánh giá.</span>
                  <Link className="font-semibold text-brand hover:underline" href="/customer/account/orders">
                    Vào trang Đơn hàng →
                  </Link>
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-slate-100 bg-slate-50 p-3 text-xs text-slate-600">
                <span className="font-semibold text-slate-900">Sản phẩm đánh giá:</span>{" "}
                <span className="text-slate-800">{productName || "Sản phẩm trong đơn"}</span>
              </div>
            )}

            {/* Star Rating Selection */}
            <div>
              <span className="block text-sm font-semibold text-slate-900">
                Chất lượng sản phẩm <span className="text-rose-500">*</span>
              </span>
              <div className="mt-2 flex items-center gap-2">
                <div className="flex gap-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      className="rounded p-1 transition hover:scale-110 focus:outline-none"
                      key={star}
                      onClick={() => setRating(star)}
                      onMouseEnter={() => setHoverRating(star)}
                      onMouseLeave={() => setHoverRating(0)}
                      type="button"
                    >
                      <Star
                        className={`size-7 transition-colors ${
                          star <= activeRating
                            ? "fill-amber-400 text-amber-500"
                            : "fill-transparent text-slate-300"
                        }`}
                      />
                    </button>
                  ))}
                </div>
                <span className="ml-2 text-sm font-medium text-amber-700">
                  {RATING_LABELS[activeRating]}
                </span>
              </div>
            </div>

            {/* Content Review */}
            <div>
              <label className="block text-sm font-semibold text-slate-900" htmlFor="review-content">
                Nội dung nhận xét
              </label>
              <textarea
                className="mt-1.5 min-h-[100px] w-full rounded-xl border border-slate-200 p-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand/20"
                id="review-content"
                maxLength={2000}
                placeholder="Hãy chia sẻ trải nghiệm sử dụng, độ hài lòng hoặc điểm cần cải thiện của sản phẩm..."
                rows={4}
                value={content}
                onChange={(e) => setContent(e.target.value)}
              />
              <div className="mt-1 flex justify-between text-xs text-slate-400">
                <span>Nội dung sẽ được kiểm duyệt trước khi hiển thị công khai</span>
                <span>{content.length}/2000 ký tự</span>
              </div>
            </div>

            {/* Image URLs */}
            <div>
              <label className="block text-sm font-semibold text-slate-900" htmlFor="image-url-input">
                Hình ảnh thực tế (tối đa 5 ảnh)
              </label>
              <div className="mt-1.5 flex gap-2">
                <input
                  className="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none transition placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand/20"
                  id="image-url-input"
                  placeholder="Dán link ảnh (https://...)"
                  type="url"
                  value={newImageUrl}
                  onChange={(e) => setNewImageUrl(e.target.value)}
                />
                <Button
                  disabled={imageUrls.length >= 5 || !newImageUrl.trim()}
                  onClick={handleAddImageUrl}
                  type="button"
                  variant="outline"
                >
                  <ImagePlus className="size-4" />
                  Thêm
                </Button>
              </div>

              {imageUrls.length > 0 ? (
                <div className="mt-3 flex flex-wrap gap-2.5">
                  {imageUrls.map((url, idx) => (
                    <div
                      className="group relative size-16 overflow-hidden rounded-lg border border-slate-200 bg-slate-50"
                      key={url}
                    >
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        alt={`Ảnh đánh giá ${idx + 1}`}
                        className="size-full object-cover"
                        src={url}
                      />
                      <button
                        aria-label="Xóa ảnh"
                        className="absolute inset-0 flex items-center justify-center bg-black/60 opacity-0 transition group-hover:opacity-100"
                        onClick={() => handleRemoveImageUrl(idx)}
                        type="button"
                      >
                        <Trash2 className="size-4 text-white" />
                      </button>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
              <Button disabled={submitting} onClick={onClose} type="button" variant="outline">
                Hủy bỏ
              </Button>
              <Button disabled={submitting} type="submit">
                {submitting ? "Đang gửi…" : "Gửi đánh giá"}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
