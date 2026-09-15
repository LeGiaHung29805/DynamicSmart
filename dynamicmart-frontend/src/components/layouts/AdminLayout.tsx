import Link from "next/link";

const adminLinks = [
  ["Người dùng", "/admin/users"],
  ["Danh mục & sản phẩm", "/admin/catalog"],
  ["Đơn hàng", "/admin/orders"],
  ["Báo cáo", "/admin/reports"],
] as const;

export function AdminLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="min-h-screen bg-slate-100 md:grid md:grid-cols-[240px_1fr]">
      <aside className="border-r border-slate-700 bg-slate-900 p-5 text-slate-100">
        <Link className="text-xl font-bold" href="/admin">DynamicMart Admin</Link>
        <nav aria-label="Điều hướng quản trị" className="mt-8 flex gap-1 overflow-x-auto md:flex-col">
          {adminLinks.map(([label, href]) => (
            <Link className="rounded-md px-3 py-2 text-sm hover:bg-slate-800" href={href} key={href}>
              {label}
            </Link>
          ))}
        </nav>
      </aside>
      <main className="p-4 sm:p-8">{children}</main>
    </div>
  );
}
