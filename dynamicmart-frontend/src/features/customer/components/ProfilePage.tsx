"use client";

import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { customerApi } from "../api/customer.api";
import type { Profile } from "../types/customer.types";

export function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [state, setState] = useState<"loading" | "idle" | "saving" | "saved" | "error">("loading");
  useEffect(() => { customerApi.profile().then((value) => { setProfile(value); setState("idle"); }).catch(() => setState("error")); }, []);
  const save = async () => {
    if (!profile) return; setState("saving");
    try { setProfile(await customerApi.updateProfile({ fullName: profile.fullName, phone: profile.phone })); setState("saved"); }
    catch { setState("error"); }
  };
  return <div className="space-y-7"><PageHeader eyebrow="Tài khoản" title="Hồ sơ cá nhân" description="Thông tin này được dùng cho liên hệ và gợi ý khi thêm địa chỉ giao hàng." />
    <SurfacePanel>{state === "loading" ? <p>Đang tải hồ sơ…</p> : !profile ? <p className="text-red-600">Không thể tải hồ sơ.</p> :
      <form className="grid gap-5 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); void save(); }}>
        <Input disabled label="Email" value={profile.email} /><Input disabled label="Trạng thái" value={profile.status} />
        <Input label="Họ và tên" value={profile.fullName} onChange={(event) => { setState("idle"); setProfile({ ...profile, fullName: event.target.value }); }} required />
        <Input label="Số điện thoại" value={profile.phone ?? ""} onChange={(event) => { setState("idle"); setProfile({ ...profile, phone: event.target.value }); }} pattern="[0-9+() .-]{8,20}" required />
        <div className="flex items-center gap-3 sm:col-span-2"><Button type="submit" disabled={state === "saving"}>{state === "saving" ? "Đang lưu…" : "Lưu thay đổi"}</Button>{state === "saved" ? <StatusBadge label="Đã lưu" tone="success" /> : null}{state === "error" ? <span className="text-sm text-red-600">Thao tác thất bại. Vui lòng thử lại.</span> : null}</div>
      </form>}</SurfacePanel></div>;
}
