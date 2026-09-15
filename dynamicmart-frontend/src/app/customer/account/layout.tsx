import { CustomerLayout } from "@/components/layouts/CustomerLayout";

export default function AccountLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <CustomerLayout>{children}</CustomerLayout>;
}
