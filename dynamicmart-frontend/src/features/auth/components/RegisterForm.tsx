"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { ArrowRight, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useToast } from "@/components/ui/Toast";
import { isApiError } from "@/lib/api/error";
import { register } from "../api/auth.api";
import { safeReturnTo } from "../safeReturnTo";

interface RegisterFormProps {
  returnTo: string;
}

export function RegisterForm({ returnTo }: Readonly<RegisterFormProps>) {
  const router = useRouter();
  const { showToast } = useToast();
  const [pending, setPending] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const fullName = String(formData.get("fullName") ?? "").trim();
    const email = String(formData.get("email") ?? "").trim();
    const password = String(formData.get("password") ?? "");
    const confirmPassword = String(formData.get("confirmPassword") ?? "");
    const nextErrors: Record<string, string> = {};

    if (fullName.length < 2) nextErrors.fullName = "Nhập họ tên có ít nhất 2 ký tự.";
    if (!/^\S+@\S+\.\S+$/.test(email)) nextErrors.email = "Nhập đúng địa chỉ email của bạn.";
    if (password.length < 8) nextErrors.password = "Mật khẩu cần tối thiểu 8 ký tự.";
    if (password !== confirmPassword) nextErrors.confirmPassword = "Mật khẩu nhập lại chưa khớp.";
    if (Object.keys(nextErrors).length) {
      setErrors(nextErrors);
      return;
    }

    try {
      setPending(true);
      setErrors({});
      await register({ fullName, email, password });
      showToast("Tạo tài khoản thành công. Chào mừng bạn đến với DynamicMart!", "success");
      router.replace(safeReturnTo(returnTo));
      router.refresh();
    } catch (error) {
      const message = isApiError(error) ? error.message : "Chưa thể tạo tài khoản. Vui lòng thử lại.";
      setErrors({ form: message });
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="space-y-4" onSubmit={handleSubmit} noValidate>
      {errors.form ? <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-danger" role="alert">{errors.form}</p> : null}
      <Input autoComplete="name" error={errors.fullName} label="Họ và tên" name="fullName" placeholder="Nguyễn Văn A" />
      <Input autoComplete="email" error={errors.email} label="Email" name="email" placeholder="ban@vidu.com" type="email" />
      <div className="space-y-1.5"><label className="text-sm font-medium text-foreground" htmlFor="password">Mật khẩu</label><div className="relative"><Input autoComplete="new-password" className="pr-11" error={errors.password} id="password" name="password" placeholder="Tối thiểu 8 ký tự" type={showPassword ? "text" : "password"} /><button aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"} className="absolute top-2.5 right-3 text-slate-400 hover:text-brand" onClick={() => setShowPassword((current) => !current)} type="button">{showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}</button></div></div>
      <Input autoComplete="new-password" error={errors.confirmPassword} label="Nhập lại mật khẩu" name="confirmPassword" placeholder="Nhập lại mật khẩu" type="password" />
      <p className="text-xs leading-5 text-slate-500">Bằng việc đăng ký, bạn đồng ý với Điều khoản và Chính sách bảo mật của DynamicMart.</p>
      <Button className="h-11 w-full bg-emerald-950 text-white hover:bg-brand" disabled={pending} type="submit">{pending ? "Đang tạo tài khoản..." : <>Tạo tài khoản <ArrowRight className="size-4" /></>}</Button>
      <p className="text-center text-sm text-slate-500">Đã có tài khoản? <Link className="font-bold text-brand hover:underline" href={`/login?returnTo=${encodeURIComponent(returnTo)}`}>Đăng nhập</Link></p>
    </form>
  );
}
