"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { ArrowRight, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useToast } from "@/components/ui/Toast";
import { isApiError } from "@/lib/api/error";
import { login } from "../api/auth.api";
import { safeReturnTo } from "../safeReturnTo";

interface LoginFormProps {
  returnTo: string;
}

export function LoginForm({ returnTo }: Readonly<LoginFormProps>) {
  const router = useRouter();
  const { showToast } = useToast();
  const [pending, setPending] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const email = String(formData.get("email") ?? "").trim();
    const password = String(formData.get("password") ?? "");
    const nextErrors: Record<string, string> = {};

    if (!/^\S+@\S+\.\S+$/.test(email)) nextErrors.email = "Nhập đúng địa chỉ email của bạn.";
    if (!password) nextErrors.password = "Nhập mật khẩu để tiếp tục.";
    if (Object.keys(nextErrors).length) {
      setErrors(nextErrors);
      return;
    }

    try {
      setPending(true);
      setErrors({});
      await login({ email, password });
      showToast("Đăng nhập thành công. Chào mừng bạn trở lại!", "success");
      router.replace(safeReturnTo(returnTo));
      router.refresh();
    } catch (error) {
      const message = isApiError(error) ? error.message : "Đăng nhập chưa thành công. Vui lòng thử lại.";
      setErrors({ form: message });
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="space-y-5" onSubmit={handleSubmit} noValidate>
      {errors.form ? <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-danger" role="alert">{errors.form}</p> : null}
      <Input autoComplete="email" error={errors.email} label="Email" name="email" placeholder="ban@vidu.com" type="email" />
      <div className="space-y-1.5">
        <div className="flex items-center justify-between gap-3"><label className="text-sm font-medium text-foreground" htmlFor="password">Mật khẩu</label><Link className="text-xs font-bold text-brand hover:text-brand-strong hover:underline" href="/forgot-password">Quên mật khẩu?</Link></div>
        <div className="relative"><Input autoComplete="current-password" className="pr-11" error={errors.password} id="password" name="password" placeholder="Nhập mật khẩu" type={showPassword ? "text" : "password"} /><button aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"} className="absolute top-2.5 right-3 text-slate-400 hover:text-brand" onClick={() => setShowPassword((current) => !current)} type="button">{showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}</button></div>
      </div>
      <Button className="h-11 w-full bg-emerald-950 text-white hover:bg-brand" disabled={pending} type="submit">{pending ? "Đang đăng nhập..." : <>Đăng nhập <ArrowRight className="size-4" /></>}</Button>
      <p className="text-center text-sm text-slate-500">Chưa có tài khoản? <Link className="font-bold text-brand hover:underline" href={`/register?returnTo=${encodeURIComponent(returnTo)}`}>Đăng ký miễn phí</Link></p>
    </form>
  );
}
