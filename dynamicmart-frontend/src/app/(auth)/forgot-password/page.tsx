import type { Metadata } from "next";

import { AuthPageContent, ForgotPasswordForm } from "@/features/auth";

export const metadata: Metadata = {
  title: "Khôi phục mật khẩu",
};

export default function ForgotPasswordPage() {
  return (
    <AuthPageContent
      eyebrow="Khôi phục tài khoản"
      title="Quên mật khẩu?"
      description="Nhập email đã đăng ký. DynamicMart sẽ gửi hướng dẫn đặt lại mật khẩu nếu tài khoản tồn tại."
    >
      <ForgotPasswordForm />
    </AuthPageContent>
  );
}
