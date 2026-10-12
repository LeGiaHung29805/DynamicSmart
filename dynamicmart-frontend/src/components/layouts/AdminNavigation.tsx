"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChartNoAxesCombined, CircleHelp, CreditCard, Headphones, House, LayoutDashboard, MessageSquareText, Package, ShoppingBag, Star, TicketPercent, UsersRound } from "lucide-react";

const primaryLinks = [
  { label: "Tổng quan", href: "/admin", icon: LayoutDashboard }, { label: "Người dùng", href: "/admin/users", icon: UsersRound },
  { label: "Danh mục & sản phẩm", href: "/admin/catalog", icon: Package }, { label: "Khuyến mãi", href: "/admin/promotions", icon: TicketPercent },
  { label: "Đơn hàng", href: "/admin/orders", icon: ShoppingBag }, { label: "Thanh toán", href: "/admin/payments", icon: CreditCard },
] as const;
const engagementLinks = [
  { label: "Đánh giá sản phẩm", href: "/admin/engagement/reviews", icon: Star },
  { label: "Hỏi đáp sản phẩm", href: "/admin/engagement/questions", icon: CircleHelp },
  { label: "Hỗ trợ khách hàng", href: "/admin/engagement/support", icon: Headphones },
] as const;

export function AdminNavigation() {
  const pathname = usePathname(); const engagementActive = pathname.startsWith("/admin/engagement");
  const linkClass = (active: boolean) => `inline-flex shrink-0 items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${active ? "bg-white text-emerald-950 shadow-sm" : "text-emerald-50/75 hover:bg-white/10 hover:text-white"}`;
  return <nav aria-label="Điều hướng quản trị" className="mt-8 flex gap-1 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden md:flex-col md:overflow-visible">
    {primaryLinks.map(({ label, href, icon: Icon }) => <Link aria-current={pathname === href ? "page" : undefined} className={linkClass(pathname === href || (href !== "/admin" && pathname.startsWith(`${href}/`)))} href={href} key={href}><Icon className="size-4" />{label}</Link>)}
    <div className="mt-1 rounded-xl border border-white/10 bg-white/5 p-1.5"><Link aria-current={engagementActive ? "page" : undefined} className={linkClass(engagementActive)} href="/admin/engagement/reviews"><MessageSquareText className="size-4" />Tương tác</Link><div className="mt-1 space-y-0.5 border-l border-emerald-200/25 pl-2">{engagementLinks.map(({ label, href, icon: Icon }) => <Link aria-current={pathname === href ? "page" : undefined} className={`flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-semibold transition ${pathname === href ? "bg-white/15 text-white" : "text-emerald-100/70 hover:bg-white/10 hover:text-white"}`} href={href} key={href}><Icon className="size-3.5" />{label}</Link>)}</div></div>
    <Link className={linkClass(pathname === "/admin/reports")} href="/admin/reports"><ChartNoAxesCombined className="size-4" />Báo cáo</Link>
    <Link className="inline-flex shrink-0 items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-emerald-50/75 transition hover:bg-white/10 hover:text-white md:mt-6" href="/"><House className="size-4" />Về cửa hàng</Link>
  </nav>;
}
