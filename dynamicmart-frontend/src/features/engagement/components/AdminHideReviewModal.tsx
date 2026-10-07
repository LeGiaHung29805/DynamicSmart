"use client";

import { AlertTriangle, EyeOff, ShieldAlert, X } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { engagementApi } from "../api/engagement.api";
import type { ReviewItem } from "../types/engagement.types";

export interface AdminHideReviewModalProps {
  isOpen: boolean;
  review: ReviewItem | null;
  onClose: () => void;
  onSuccess?: (reviewId: string, reason: string) => void;
}

const COMMON_REASONS = [
  "Spam hoặc quảng cáo sản phẩm/dịch vụ khác",
  "Ngôn từ xúc phạm, đồi trụy hoặc bạo lực",
  "Nội dung không liên quan đến sản phẩm",
  "Tiết lộ thông tin cá nhân của người khác",
  "Đánh giá tiêu cực có tính chất công kích cá nhân",
];

export function AdminHideReviewModal({
  isOpen,
  review,
  onClose,
  onSuccess,
}: Readonly<AdminHideReviewModalProps>) {
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  if (!isOpen || !review) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setErrorMessage("Vui lòng nhập lý do ẩn đánh giá.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");

    try {
      await engagementApi.reviews.hideReview(review.id, { reason: trimmedReason });
      onSuccess?.(review.id, trimmedReason);
      onClose();
      setReason("");
    } catch (err: unknown) {
      const errorObj = err as { message?: string; payload?: { message?: string } };
      const serverMsg =
        errorObj.payload?.message ||
        errorObj.message ||
        "Không thể ẩn đánh giá. Vui lòng kiểm tra lại quyền quản trị.";
      setErrorMessage(serverMsg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm"
      role="dialog"
    >
      <div className="relative w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-7">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-4">
          <div className="flex items-center gap-2.5">
            <span className="grid size-9 place-items-center rounded-xl bg-rose-50 text-rose-600">
              <ShieldAlert className="size-5" />
            </span>
            <div>
              <p className="text-xs font-bold tracking-wider text-rose-600 uppercase">Kiểm duyệt nội dung</p>
              <h2 className="text-lg font-black text-slate-950">Ẩn đánh giá vi phạm</h2>
            </div>
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

        {/* Tóm tắt review đang ẩn */}
        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Người gửi: <strong className="text-slate-800">{review.customerName || review.customerId}</strong></span>
            <span>⭐ {review.rating}/5</span>
          </div>
          <p className="mt-2 text-slate-700 italic">
            &ldquo;{review.content || "(Không có nội dung chữ)"}&rdquo;
          </p>
        </div>

        {/* Form nhập lý do */}
        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          {errorMessage ? (
            <div className="flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
              <AlertTriangle className="mt-0.5 size-4 shrink-0 text-rose-600" />
              <span>{errorMessage}</span>
            </div>
          ) : null}

          <div>
            <label className="block text-sm font-semibold text-slate-900" htmlFor="hide-reason">
              Lý do ẩn đánh giá <span className="text-rose-500">*</span>
            </label>
            <textarea
              className="mt-1.5 min-h-[90px] w-full rounded-xl border border-slate-200 p-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
              id="hide-reason"
              maxLength={500}
              placeholder="Nhập lý do kiểm duyệt (bắt buộc lưu vào nhật ký audit)..."
              required
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <div className="mt-1 flex justify-between text-xs text-slate-400">
              <span>Hệ thống không xóa cứng đánh giá để phục vụ tra cứu sau này</span>
              <span>{reason.length}/500 ký tự</span>
            </div>
          </div>

          {/* Gợi ý lý do nhanh */}
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase">Gợi ý lý do phổ biến:</span>
            <div className="mt-2 flex flex-wrap gap-1.5">
              {COMMON_REASONS.map((common) => (
                <button
                  className="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
                  key={common}
                  onClick={() => setReason(common)}
                  type="button"
                >
                  {common}
                </button>
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <Button disabled={submitting} onClick={onClose} type="button" variant="outline">
              Hủy bỏ
            </Button>
            <Button
              className="bg-rose-600 text-white hover:bg-rose-700"
              disabled={submitting || !reason.trim()}
              type="submit"
            >
              <EyeOff className="size-4" />
              {submitting ? "Đang xử lý…" : "Xác nhận ẩn đánh giá"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
