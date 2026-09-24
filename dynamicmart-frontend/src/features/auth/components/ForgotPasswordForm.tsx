"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { ArrowLeft, MailCheck } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { isApiError } from "@/lib/api/error";
import { requestPasswordReset } from "../api/auth.api";

export function ForgotPasswordForm() {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();
  const [submitted, setSubmitted] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const email = String(new FormData(event.currentTarget).get("email") ?? "").trim();
    if (!/^\S+@\S+\.\S+$/.test(email)) { setError("Nhập đúng địa chỉ email của bạn."); return; }
    try {
      setPending(true); setError(undefined);
      await requestPasswordReset(email);
      setSubmitted(true);
    } catch (caught) {
      setError(isApiError(caught) ? caught.message : "Chưa thể gửi yêu cầu đặt lại mật khẩu.");
    } finally { setPending(false); }
  }

  if (submitted) return <div className="text-center"><span className="mx-auto grid size-14 place-items-center rounded-2xl bg-emerald-50 text-brand"><MailCheck className="size-6" /></span><h2 className="mt-5 text-lg font-black">Kiểm tra hộp thư của bạn</h2><p className="mt-2 text-sm leading-6 text-slate-500">Nếu email thuộc một tài khoản hợp lệ, hệ thống đã gửi liên kết đặt lại mật khẩu. Thông báo này không tiết lộ email có tồn tại hay không.</p><Link className="mt-7 inline-flex items-center gap-2 text-sm font-bold text-brand hover:underline" href="/login"><ArrowLeft className="size-4" /> Quay lại đăng nhập</Link></div>;

  return <form className="space-y-5" onSubmit={submit} noValidate>{error ? <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-danger" role="alert">{error}</p> : null}<Input autoComplete="email" label="Email tài khoản" name="email" placeholder="ban@vidu.com" type="email" /><Button className="h-11 w-full bg-emerald-950 text-white hover:bg-brand" disabled={pending} type="submit">{pending ? "Đang gửi..." : "Gửi liên kết khôi phục"}</Button><Link className="flex items-center justify-center gap-2 text-sm font-bold text-brand hover:underline" href="/login"><ArrowLeft className="size-4" /> Quay lại đăng nhập</Link></form>;
}
