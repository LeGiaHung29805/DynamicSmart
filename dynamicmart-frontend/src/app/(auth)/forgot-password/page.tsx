import Link from "next/link";
import type { Metadata } from "next";
import { ArrowLeft, KeyRound } from "lucide-react";
import { AuthPageContent } from "@/features/auth";

export const metadata: Metadata = { title: "Khôi phục mật khẩu" };

export default function ForgotPasswordPage() {
  return (
    <AuthPageContent eyebrow="Bảo mật tài khoản" title="Khôi phục mật khẩu" description="Lấy lại quyền truy cập tài khoản của bạn.">
      <div className="text-center">
        <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-emerald-50 text-brand"><KeyRound className="size-6" /></span>
        <p className="mt-5 text-sm leading-6 text-slate-500">Chức năng gửi email và đặt lại mật khẩu an toàn đang được hoàn thiện.</p>
        <Link className="mt-7 inline-flex items-center gap-2 text-sm font-bold text-brand hover:underline" href="/login"><ArrowLeft className="size-4" /> Quay lại đăng nhập</Link>
      </div>
    </AuthPageContent>
  );
}
