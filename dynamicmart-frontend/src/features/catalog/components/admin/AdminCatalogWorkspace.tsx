"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Boxes, FolderTree, PackageSearch, SlidersHorizontal } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { useAuthSession } from "@/lib/auth/session";
import { AdminAttributePanel } from "./AdminAttributePanel";
import { AdminCategoryPanel } from "./AdminCategoryPanel";
import { AdminInventoryPanel } from "./AdminInventoryPanel";
import { AdminProductPanel } from "./AdminProductPanel";

const tabs = [
  { id: "products", label: "Sản phẩm", icon: PackageSearch },
  { id: "categories", label: "Danh mục", icon: FolderTree },
  { id: "attributes", label: "Thuộc tính", icon: SlidersHorizontal },
  { id: "inventory", label: "Tồn kho", icon: Boxes },
] as const;

type TabId = (typeof tabs)[number]["id"];

export function AdminCatalogWorkspace() {
  const session = useAuthSession();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<TabId>("products");

  useEffect(() => {
    if (session.status === "anonymous") {
      router.replace(`/login?returnTo=${encodeURIComponent("/admin/catalog")}`);
    }
  }, [router, session.status]);

  if (session.status === "loading" || session.status === "anonymous") {
    return <LoadingState title="Đang xác minh quyền quản trị" />;
  }
  if (session.user.role !== "ADMIN") {
    return <ErrorState actionLabel="Về trang chủ" description="Tài khoản hiện tại không có quyền quản trị Catalog." onAction={() => router.replace("/")} title="Không có quyền truy cập" />;
  }

  return (
    <div className="space-y-7">
      <PageHeader
        description="Quản lý danh mục, thuộc tính động, sản phẩm, biến thể, hình ảnh và tồn kho trên cùng một không gian làm việc."
        eyebrow="Catalog & inventory"
        title="Quản trị kho sản phẩm"
      />
      <nav aria-label="Khu vực quản trị catalog" className="flex gap-2 overflow-x-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
        {tabs.map(({ id, label, icon: Icon }) => (
          <button
            aria-current={activeTab === id ? "page" : undefined}
            className={`inline-flex min-h-11 shrink-0 items-center gap-2 rounded-xl px-4 text-sm font-black transition ${activeTab === id ? "bg-emerald-950 text-white" : "text-slate-600 hover:bg-slate-100"}`}
            key={id}
            onClick={() => setActiveTab(id)}
            type="button"
          >
            <Icon className="size-4" />{label}
          </button>
        ))}
      </nav>
      {activeTab === "products" ? <AdminProductPanel /> : null}
      {activeTab === "categories" ? <AdminCategoryPanel /> : null}
      {activeTab === "attributes" ? <AdminAttributePanel /> : null}
      {activeTab === "inventory" ? <AdminInventoryPanel /> : null}
    </div>
  );
}
