"use client";

import Link from "next/link";
import { ArrowLeft, House, MapPin, Package, UserRound } from "lucide-react";
import { usePathname } from "next/navigation";

const accountLinks = [
  ["Hồ sơ", "/customer/account/profile", UserRound],
  ["Sổ địa chỉ", "/customer/account/addresses", MapPin],
  ["Đơn hàng", "/customer/account/orders", Package],
] as const;

export function CustomerLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const pathname = usePathname();
  return (
    <div className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:grid-cols-[260px_minmax(0,1fr)] lg:px-8 lg:py-12">
      <aside className="h-fit overflow-hidden rounded-2xl bg-stone-950 p-5 text-white shadow-sm lg:sticky lg:top-24">
        <div className="flex items-center gap-3 border-b border-stone-800 pb-5">
          <span className="grid size-12 place-items-center rounded-full bg-rose-600 text-lg font-black" aria-hidden="true">D</span>
          <div className="min-w-0 flex-1"><h1 className="font-black">Tài khoản của tôi</h1><p className="mt-1 text-xs text-stone-400">Quản lý mua sắm và giao hàng</p></div>
          <Link aria-label="Về trang chủ" className="grid size-9 shrink-0 place-items-center rounded-xl bg-white/10 text-white transition hover:bg-white/20" href="/"><House className="size-4" /></Link>
        </div>
        <nav aria-label="Điều hướng tài khoản" className="mt-5 flex gap-2 overflow-x-auto lg:flex-col">
          {accountLinks.map(([label, href, Icon]) => {
            const active = pathname === href;
            return <Link aria-current={active ? "page" : undefined} className={`flex shrink-0 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold transition ${active ? "bg-rose-600 text-white" : "text-stone-300 hover:bg-stone-800 hover:text-white"}`} href={href} key={href}>
              <Icon className="size-4" />{label}
            </Link>;
          })}
        </nav>
        <Link className="mt-5 flex items-center justify-center gap-2 border-t border-stone-800 pt-5 text-sm font-bold text-stone-300 transition hover:text-white" href="/products"><ArrowLeft className="size-4" />Tiếp tục mua sắm</Link>
      </aside>
      <main className="min-w-0">{children}</main>
    </div>
  );
}
