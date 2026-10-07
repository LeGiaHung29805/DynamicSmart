"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Boxes, FolderTree, PackageSearch, SlidersHorizontal } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { useAuthSession } from "@/lib/auth/session";

const tabs = [
  { href: "/admin/catalog/products", label: "Sản phẩm", icon: PackageSearch },
  { href: "/admin/catalog/categories", label: "Danh mục", icon: FolderTree },
  { href: "/admin/catalog/attributes", label: "Thương hiệu & thuộc tính", icon: SlidersHorizontal },
  { href: "/admin/catalog/inventory", label: "Tồn kho", icon: Boxes },
] as const;

export function AdminCatalogWorkspace({ children }: Readonly<{ children: React.ReactNode }>) {
  const session = useAuthSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (session.status === "anonymous") {
      router.replace(`/login?returnTo=${encodeURIComponent(pathname)}`);
    }
  }, [pathname, router, session.status]);

  if (session.status === "loading" || session.status === "anonymous") {
    return <LoadingState title="Đang xác minh quyền quản trị" />;
  }
  if (session.user.role !== "ADMIN") {
    return <ErrorState actionLabel="Về trang chủ" description="Tài khoản hiện tại không có quyền quản trị Catalog." onAction={() => router.replace("/")} title="Không có quyền truy cập" />;
  }

  return (
    <div className="space-y-7">
      <PageHeader
        description="Mỗi nghiệp vụ có một không gian riêng để thao tác nhanh, rõ trạng thái và dễ quay lại công việc đang làm."
        eyebrow="Catalog & inventory"
        title="Quản trị kho sản phẩm"
      />
      <nav aria-label="Khu vực quản trị catalog" className="sticky top-0 z-20 flex gap-2 overflow-x-auto rounded-2xl border border-slate-200 bg-white/95 p-2 shadow-sm backdrop-blur">
        {tabs.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          return <Link
            aria-current={active ? "page" : undefined}
            className={`inline-flex min-h-11 shrink-0 items-center gap-2 rounded-xl px-4 text-sm font-black transition ${active ? "bg-emerald-950 text-white" : "text-slate-600 hover:bg-slate-100"}`}
            href={href}
            key={href}
          >
            <Icon className="size-4" />{label}
          </Link>;
        })}
      </nav>
      {children}
    </div>
  );
}
