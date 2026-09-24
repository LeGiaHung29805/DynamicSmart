"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { isApiError } from "@/lib/api/error";
import { resetPassword } from "../api/auth.api";

export function ResetPasswordForm({ token }: Readonly<{ token?: string }>) {
  const [pending, setPending] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) { setErrors({ form: "Liên kết đặt lại mật khẩu thiếu token." }); return; }
    const data = new FormData(event.currentTarget);
    const password = String(data.get("password") ?? "");
    const confirmation = String(data.get("confirmation") ?? "");
    const next: Record<string, string> = {};
    if (password.length < 8) next.password = "Mật khẩu cần tối thiểu 8 ký tự.";
    if (password !== confirmation) next.confirmation = "Mật khẩu nhập lại chưa khớp.";
    if (Object.keys(next).length) { setErrors(next); return; }
    try {
      setPending(true); setErrors({});
      await resetPassword(token, password);
      setSuccess(true);
    } catch (caught) {
      setErrors({ form: isApiError(caught) ? caught.message : "Liên kết không hợp lệ hoặc đã hết hạn." });
    } finally { setPending(false); }
  }

  if (success) return <div className="text-center"><CheckCircle2 className="mx-auto size-14 text-brand" /><h2 className="mt-4 text-lg font-black">Đã đổi mật khẩu</h2><p className="mt-2 text-sm text-slate-500">Bạn có thể đăng nhập bằng mật khẩu mới.</p><Link className="mt-6 inline-flex rounded-xl bg-emerald-950 px-5 py-3 text-sm font-bold text-white" href="/login">Đăng nhập</Link></div>;
  return <form className="space-y-4" onSubmit={submit} noValidate>{errors.form ? <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-danger" role="alert">{errors.form}</p> : null}<Input autoComplete="new-password" error={errors.password} label="Mật khẩu mới" name="password" type="password" /><Input autoComplete="new-password" error={errors.confirmation} label="Nhập lại mật khẩu" name="confirmation" type="password" /><Button className="h-11 w-full bg-emerald-950 text-white hover:bg-brand" disabled={pending || !token} type="submit">{pending ? "Đang cập nhật..." : "Đặt lại mật khẩu"}</Button></form>;
}
