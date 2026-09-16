import Link from "next/link";
import { ArrowRight, ChartNoAxesCombined, Package, ShoppingBag, UsersRound } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";

const shortcuts = [
  { label: "Người dùng", href: "/admin/users", icon: UsersRound, detail: "Vai trò và trạng thái tài khoản" },
  { label: "Catalog", href: "/admin/catalog", icon: Package, detail: "Sản phẩm, danh mục và tồn kho" },
  { label: "Đơn hàng", href: "/admin/orders", icon: ShoppingBag, detail: "Đơn, thanh toán và giao hàng" },
  { label: "Báo cáo", href: "/admin/reports", icon: ChartNoAxesCombined, detail: "Các chỉ số vận hành" },
] as const;

export function AdminDashboard() {
  return (
    <div className="space-y-8">
      <PageHeader eyebrow="Admin workspace" title="Tổng quan quản trị" description="Điểm bắt đầu chung cho các nhóm nghiệp vụ. Các ô dưới đây là khung giao diện, chưa phải số liệu thật." action={<StatusBadge label="UI mẫu · chưa nối API" tone="warning" />} />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {shortcuts.map(({ label, href, icon: Icon, detail }) => (
          <Link className="group rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm shadow-slate-950/[0.03] transition hover:-translate-y-1 hover:border-emerald-200 hover:shadow-lg" href={href} key={href}>
            <span className="grid size-11 place-items-center rounded-2xl bg-emerald-50 text-brand"><Icon className="size-5" /></span>
            <h2 className="mt-5 text-lg font-black text-slate-950">{label}</h2>
            <p className="mt-1 text-sm leading-5 text-slate-500">{detail}</p>
            <span className="mt-5 inline-flex items-center gap-2 text-xs font-bold text-brand">Mở trang <ArrowRight className="size-3.5 transition group-hover:translate-x-1" /></span>
          </Link>
        ))}
      </div>
      <SurfacePanel>
        <h2 className="text-xl font-black text-slate-950">Quy ước giao diện chung</h2>
        <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600">Mỗi trang dùng cùng PageHeader, SurfacePanel, Input, Select và StatusBadge. Khi nối API, thay phần dữ liệu trống bằng danh sách thật; không cần dựng lại khung, bảng, khoảng cách hay trạng thái từ đầu.</p>
      </SurfacePanel>
    </div>
  );
}
