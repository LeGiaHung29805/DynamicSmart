"use client";

import { Heart } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useToast } from "@/components/ui/Toast";
import { useAuthSession } from "@/lib/auth/session";
import { useWishlist } from "./WishlistProvider";

export function WishlistButton({ productId, className = "" }: Readonly<{ productId: string; className?: string }>) {
  const session = useAuthSession();
  const pathname = usePathname();
  const router = useRouter();
  const { showToast } = useToast();
  const { isFavorite, pending, toggle } = useWishlist();
  const active = isFavorite(productId);

  async function handleClick() {
    if (session.status !== "authenticated") { router.push(`/login?returnTo=${encodeURIComponent(pathname)}`); return; }
    try {
      const added = await toggle(productId);
      showToast(added ? "Đã thêm vào Yêu thích." : "Đã bỏ khỏi Yêu thích.", "success");
    } catch { showToast("Chưa thể cập nhật Yêu thích. Vui lòng thử lại.", "error"); }
  }

  return <button aria-label={active ? "Bỏ khỏi Yêu thích" : "Thêm vào Yêu thích"} aria-pressed={active}
    className={`grid size-11 place-items-center rounded-full border border-white/70 bg-white/95 shadow-md transition hover:scale-105 hover:text-rose-600 disabled:cursor-wait disabled:opacity-60 ${active ? "text-rose-600" : "text-slate-600"} ${className}`}
    disabled={pending.has(productId) || session.status === "loading"} onClick={handleClick} type="button">
    <Heart className="size-5" fill={active ? "currentColor" : "none"} />
  </button>;
}
