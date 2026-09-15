import type { Metadata } from "next";
import { AuthPageContent, RegisterForm, safeReturnTo } from "@/features/auth";

export const metadata: Metadata = { title: "Đăng ký" };

export default async function RegisterPage({ searchParams }: Readonly<{ searchParams: Promise<{ returnTo?: string }> }>) {
  const { returnTo } = await searchParams;
  return (
    <AuthPageContent eyebrow="Bắt đầu mua sắm" title="Tạo tài khoản mới" description="Chỉ mất một phút để nhận voucher và quản lý đơn hàng của riêng bạn.">
      <RegisterForm returnTo={safeReturnTo(returnTo)} />
    </AuthPageContent>
  );
}
