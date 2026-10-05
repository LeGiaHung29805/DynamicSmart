"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { LogOut, ShieldCheck, UserRound } from "lucide-react";
import { useAuthSession } from "@/lib/auth/session";
import { logout } from "../api/auth.api";

export function AccountMenu() {
  const session = useAuthSession();
  const router = useRouter();
  if (session.status === "loading") return <span aria-label="Đang kiểm tra phiên đăng nhập" className="hidden h-10 w-24 animate-pulse rounded-xl bg-slate-100 sm:block" />;
  if (session.status === "anonymous") return <Link className="hidden h-10 items-center gap-2 rounded-xl px-2 text-sm font-bold text-slate-600 transition hover:bg-emerald-50 hover:text-brand sm:flex lg:px-3" href="/login"><UserRound className="size-[1.15rem]" /><span className="hidden lg:inline">Đăng nhập</span></Link>;
  return (
    <div className="group relative hidden sm:block">
      <Link className="flex h-10 items-center gap-2 rounded-xl bg-emerald-50 px-2 text-sm font-bold text-brand lg:px-3" href={session.user.role === "ADMIN" ? "/admin" : "/customer/account/profile"}>{session.user.role === "ADMIN" ? <ShieldCheck className="size-[1.15rem]" /> : <UserRound className="size-[1.15rem]" />}<span className="hidden lg:inline">{session.user.role === "ADMIN" ? "Quản trị" : "Hồ sơ"}</span></Link>
      <div className="invisible absolute top-full right-0 z-50 w-56 translate-y-2 pt-2 opacity-0 transition group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
        <div className="rounded-2xl border border-slate-200 bg-white p-2 shadow-xl"><p className="truncate px-3 py-2 text-xs font-bold text-slate-700">{session.user.fullName}</p><button className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-slate-600 hover:bg-slate-50" onClick={async () => { await logout(); router.replace("/"); router.refresh(); }} type="button"><LogOut className="size-4" /> Đăng xuất</button></div>
      </div>
    </div>
  );
}
