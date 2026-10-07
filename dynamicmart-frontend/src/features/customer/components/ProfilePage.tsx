"use client";

import { CalendarDays, Mail, Phone, ShieldCheck, UserRound } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useAuthSession } from "@/lib/auth/session";
import { customerApi } from "../api/customer.api";
import type { Profile } from "../types/customer.types";

const accountStatusLabel = (status: string) => ({ ACTIVE: "Đang hoạt động", LOCKED: "Đã khóa", DISABLED: "Đã vô hiệu hóa" } as Record<string, string>)[status] ?? "Chưa xác định";

export function ProfilePage() {
  const session = useAuthSession();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [state, setState] = useState<"loading" | "idle" | "saving" | "saved" | "error">("loading");
  useEffect(() => {
    if (session.status === "loading") return;
    if (session.status === "anonymous") return;
    let cancelled = false;
    customerApi.profile().then((value) => {
      if (!cancelled) { setProfile(value); setState("idle"); }
    }).catch(() => { if (!cancelled) setState("error"); });
    return () => { cancelled = true; };
  }, [session.status]);
  const save = async () => {
    if (!profile) return;
    setState("saving");
    try {
      setProfile(await customerApi.updateProfile({ fullName: profile.fullName, phone: profile.phone }));
      setState("saved");
    } catch {
      setState("error");
    }
  };
  return <div className="space-y-7">
    <PageHeader eyebrow="Tài khoản" title="Hồ sơ của tôi" description="Cập nhật thông tin liên hệ dùng cho tài khoản, địa chỉ giao hàng và đơn hàng của bạn." />
    {session.status === "anonymous" ? <SurfacePanel className="text-center"><p className="text-sm font-semibold text-stone-700">Bạn cần đăng nhập để xem hồ sơ.</p><Link className="mt-4 inline-flex rounded-xl bg-stone-950 px-5 py-3 text-sm font-black text-white" href="/login?returnTo=%2Fcustomer%2Faccount%2Fprofile">Đăng nhập</Link></SurfacePanel> : session.status === "loading" || state === "loading" ? <SurfacePanel><p className="text-sm text-stone-500">Đang tải hồ sơ…</p></SurfacePanel> : !profile ? <SurfacePanel className="border-red-200 text-red-600">Không thể tải hồ sơ. Vui lòng đăng nhập lại hoặc thử sau.</SurfacePanel> : <>
      <SurfacePanel className="overflow-hidden p-0 sm:p-0">
        <div className="bg-gradient-to-r from-stone-950 via-stone-900 to-rose-950 px-6 py-7 text-white sm:px-8">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <span className="grid size-16 shrink-0 place-items-center rounded-full bg-rose-600 text-2xl font-black ring-4 ring-white/10" aria-hidden="true">{profile.fullName.trim().charAt(0).toUpperCase() || "D"}</span>
            <div className="min-w-0"><h2 className="truncate text-2xl font-black">{profile.fullName}</h2><p className="mt-1 break-all text-sm text-stone-300">{profile.email}</p><div className="mt-3 flex flex-wrap gap-2"><StatusBadge label={accountStatusLabel(profile.status)} tone={profile.status === "ACTIVE" ? "success" : "warning"} /><StatusBadge label={profile.role === "ADMIN" ? "Quản trị viên" : "Khách hàng"} tone="neutral" /></div></div>
          </div>
        </div>
        <dl className="grid gap-px bg-stone-200 sm:grid-cols-2 lg:grid-cols-4">
          <Info icon={Mail} label="Email" value={profile.email} />
          <Info icon={Phone} label="Số điện thoại" value={profile.phone || "Chưa cập nhật"} />
          <Info icon={ShieldCheck} label="Quyền tài khoản" value={profile.role === "ADMIN" ? "Quản trị viên" : "Khách hàng"} />
          <Info icon={CalendarDays} label="Thành viên từ" value={profile.createdAt ? new Date(profile.createdAt).toLocaleDateString("vi-VN") : "Đang cập nhật"} />
        </dl>
      </SurfacePanel>
      <SurfacePanel>
        <div className="flex items-start gap-3"><span className="rounded-xl bg-rose-50 p-2.5 text-rose-600"><UserRound className="size-5" /></span><div><h2 className="text-lg font-black text-stone-950">Thông tin cá nhân</h2><p className="mt-1 text-sm text-stone-500">Email, quyền và trạng thái tài khoản chỉ có thể thay đổi qua đúng quy trình bảo mật.</p></div></div>
        <form className="mt-6 grid gap-5 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); void save(); }}>
          <Input disabled label="Email" value={profile.email} /><Input disabled label="Trạng thái" value={accountStatusLabel(profile.status)} />
          <Input label="Họ và tên" value={profile.fullName} onChange={(event) => { setState("idle"); setProfile({ ...profile, fullName: event.target.value }); }} required />
          <Input label="Số điện thoại" value={profile.phone ?? ""} onChange={(event) => { setState("idle"); setProfile({ ...profile, phone: event.target.value }); }} pattern="[0-9+() .-]{8,20}" placeholder="0912345678" required />
          <div className="flex flex-wrap items-center gap-3 border-t border-stone-200 pt-5 sm:col-span-2 sm:justify-end">{state === "saved" ? <StatusBadge label="Đã lưu thay đổi" tone="success" /> : null}{state === "error" ? <span className="text-sm font-semibold text-red-600">Thao tác thất bại. Vui lòng kiểm tra dữ liệu và thử lại.</span> : null}<Button size="lg" type="submit" disabled={state === "saving"}>{state === "saving" ? "Đang lưu…" : "Cập nhật hồ sơ"}</Button></div>
        </form>
      </SurfacePanel>
    </>}
  </div>;
}

function Info({ icon: Icon, label, value }: Readonly<{ icon: typeof Mail; label: string; value: string }>) {
  return <div className="flex min-w-0 items-start gap-3 bg-white px-5 py-4"><Icon className="mt-0.5 size-4 shrink-0 text-rose-600" /><div className="min-w-0"><dt className="text-xs font-bold uppercase tracking-wide text-stone-400">{label}</dt><dd className="mt-1 truncate text-sm font-semibold text-stone-800">{value}</dd></div></div>;
}
