import { AuthPageLayout } from "@/features/auth";

export default function AuthLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <AuthPageLayout>{children}</AuthPageLayout>;
}
