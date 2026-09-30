import type { Metadata } from "next";
import { AuthPageContent, ResetPasswordForm } from "@/features/auth";

export const metadata: Metadata = { title: "Đặt lại mật khẩu" };

export default async function ResetPasswordPage({ searchParams }: Readonly<{ searchParams: Promise<{ token?: string }> }>) {
  const { token } = await searchParams;
  return <AuthPageContent eyebrow="Bảo mật tài khoản" title="Đặt lại mật khẩu" description="Tạo mật khẩu mới cho tài khoản DynamicMart của bạn."><ResetPasswordForm token={token} /></AuthPageContent>;
}
