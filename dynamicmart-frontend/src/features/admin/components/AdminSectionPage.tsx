import { Search } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { adminSections, type AdminSection } from "../sections";

export function AdminSectionPage({ section }: Readonly<{ section: AdminSection }>) {
  const content = adminSections[section];
  return (
    <div className="space-y-7">
      <PageHeader description={content.description} eyebrow={content.eyebrow} title={content.title} />
      <SurfacePanel>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div><h2 className="text-lg font-black text-slate-950">Danh sách {content.title.toLocaleLowerCase("vi-VN")}</h2><p className="mt-1 text-sm text-slate-500">Bố cục mẫu, chưa hiển thị dữ liệu quản trị thật.</p></div>
          <StatusBadge label="Chưa kết nối API" tone="warning" />
        </div>
        <div className="mt-6 grid gap-4 sm:grid-cols-[minmax(0,1fr)_180px]">
          <Input disabled label="Tìm kiếm" placeholder={`Tìm trong ${content.title.toLocaleLowerCase("vi-VN")}...`} />
          <Select disabled label="Trạng thái"><option>Tất cả trạng thái</option></Select>
        </div>
        <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs font-bold tracking-wide text-slate-500 uppercase"><tr>{content.columns.map((column) => <th className="whitespace-nowrap px-5 py-4" key={column} scope="col">{column}</th>)}</tr></thead>
            <tbody><tr><td className="px-5 py-12 text-center text-sm text-slate-500" colSpan={content.columns.length}><Search className="mx-auto mb-3 size-7 text-slate-300" />Dữ liệu sẽ xuất hiện tại đây khi API quản trị sẵn sàng.</td></tr></tbody>
          </table>
        </div>
      </SurfacePanel>
    </div>
  );
}
