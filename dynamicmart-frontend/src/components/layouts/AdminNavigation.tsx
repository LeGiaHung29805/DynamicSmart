"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChartNoAxesCombined, CreditCard, House, LayoutDashboard, Package, ShoppingBag, UsersRound } from "lucide-react";

const adminLinks = [
  { label: "Tổng quan", href: "/admin", icon: LayoutDashboard },
  { label: "Người dùng", href: "/admin/users", icon: UsersRound },
  { label: "Danh mục & sản phẩm", href: "/admin/catalog", icon: Package },
  { label: "Đơn hàng", href: "/admin/orders", icon: ShoppingBag },
  { label: "Thanh toán", href: "/admin/payments", icon: CreditCard },
  { label: "Báo cáo", href: "/admin/reports", icon: ChartNoAxesCombined },
] as const;

export function AdminNavigation() {
  const pathname = usePathname();
  return (
    <nav aria-label="Điều hướng quản trị" className="mt-8 flex gap-1 overflow-x-auto pb-1 md:flex-col md:overflow-visible">
      {adminLinks.map(({ label, href, icon: Icon }) => {
        const active = pathname === href || (href !== "/admin" && pathname.startsWith(`${href}/`));
        return (
          <Link
            aria-current={active ? "page" : undefined}
            className={`inline-flex shrink-0 items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${active ? "bg-white text-emerald-950 shadow-sm" : "text-emerald-50/75 hover:bg-white/10 hover:text-white"}`}
            href={href}
            key={href}
          >
            <Icon className="size-4" />{label}
          </Link>
        );
      })}
      <Link className="inline-flex shrink-0 items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-emerald-50/75 transition hover:bg-white/10 hover:text-white md:mt-6" href="/"><House className="size-4" />Về cửa hàng</Link>
    </nav>
  );
}
