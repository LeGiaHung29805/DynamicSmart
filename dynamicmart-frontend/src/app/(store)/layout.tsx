import { StoreLayout } from "@/components/layouts/StoreLayout";

export default function StorePagesLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <StoreLayout>{children}</StoreLayout>;
}
