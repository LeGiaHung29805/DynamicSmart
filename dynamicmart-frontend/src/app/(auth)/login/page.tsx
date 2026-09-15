import type { Metadata } from "next";
import { AuthPageContent, LoginForm, safeReturnTo } from "@/features/auth";

export const metadata: Metadata = { title: "Đăng nhập" };

export default async function LoginPage({ searchParams }: Readonly<{ searchParams: Promise<{ returnTo?: string }> }>) {
  const { returnTo } = await searchParams;
  return (
    <AuthPageContent eyebrow="Chào mừng trở lại" title="Đăng nhập tài khoản" description="Đăng nhập để tiếp tục mua sắm và nhận ưu đãi phù hợp với bạn.">
      <LoginForm returnTo={safeReturnTo(returnTo)} />
    </AuthPageContent>
  );
}
