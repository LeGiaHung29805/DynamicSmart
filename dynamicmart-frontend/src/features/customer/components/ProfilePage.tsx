"use client";

import { useCallback, useEffect, useState } from "react";
import { CalendarDays, CheckCircle2, Mail, Phone, ShieldCheck, Star, UserRound } from "lucide-react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/common/PageHeader";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useToast } from "@/components/ui/Toast";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";
import { customerApi } from "../api/customer.api";
import type { Profile } from "../types/customer.types";

export function ProfilePage() {
  const router = useRouter();
  const session = useAuthSession();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const { showToast } = useToast();

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try { setProfile(await customerApi.profile()); }
    catch (cause) { setError(isApiError(cause) ? cause.message : "Không thể tải hồ sơ."); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => {
    if (session.status !== "authenticated") return;
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load, session.status]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!profile) return;
    const data = new FormData(event.currentTarget);
    setSaving(true);
    setError("");
    try {
      setProfile(await customerApi.updateProfile({ fullName: String(data.get("fullName") ?? "").trim(), phone: String(data.get("phone") ?? "").trim() }));
      showToast("Đã lưu thông tin hồ sơ.", "success");
    } catch (cause) { setError(isApiError(cause) ? cause.message : "Không thể lưu hồ sơ."); }
    finally { setSaving(false); }
  }

  return (
    <div className="space-y-7">
      <PageHeader eyebrow="Tài khoản" title="Hồ sơ cá nhân" description="Cập nhật thông tin liên hệ dùng cho mua sắm và nhận hàng." />
      {session.status === "loading" ? <LoadingState /> : session.status === "anonymous" ? <SurfacePanel className="py-12 text-center"><UserRound className="mx-auto size-11 text-emerald-700" /><h2 className="mt-4 text-xl font-black">Đăng nhập để xem hồ sơ</h2><Button className="mt-5 bg-emerald-800 text-white hover:bg-emerald-700" onClick={() => router.push("/login?returnTo=%2Fcustomer%2Faccount%2Fprofile")}>Đăng nhập</Button></SurfacePanel> : loading ? <LoadingState /> : error && !profile ? <ErrorState description={error} onAction={load} /> : profile ? (
        <div className="grid gap-6 xl:grid-cols-[300px_minmax(0,1fr)]">
          <SurfacePanel className="overflow-hidden p-0 sm:p-0">
            <div className="bg-gradient-to-br from-slate-950 via-emerald-950 to-emerald-800 p-7 text-white">
              <div className="grid size-20 place-items-center rounded-3xl bg-white/15 text-3xl font-black ring-1 ring-white/20">{profile.fullName.trim().charAt(0).toUpperCase() || "T"}</div>
              <h2 className="mt-5 text-xl font-black">{profile.fullName}</h2>
              <p className="mt-1 break-all text-sm text-emerald-100/80">{profile.email}</p>
            </div>
            <div className="space-y-4 p-6 text-sm">
              <div className="flex items-center gap-3"><span className="grid size-9 place-items-center rounded-xl bg-emerald-50 text-emerald-700"><ShieldCheck className="size-4" /></span><div><p className="text-xs text-slate-400">Vai trò</p><p className="font-bold">{profile.role === "ADMIN" ? "Quản trị viên" : "Khách hàng"}</p></div></div>
              <div className="flex items-center gap-3"><span className="grid size-9 place-items-center rounded-xl bg-sky-50 text-sky-700"><CheckCircle2 className="size-4" /></span><div><p className="text-xs text-slate-400">Trạng thái</p><p className="font-bold text-emerald-700">{profile.status === "ACTIVE" ? "Đang hoạt động" : profile.status}</p></div></div>
              <div className="flex items-center gap-3"><span className="grid size-9 place-items-center rounded-xl bg-amber-50 text-amber-700"><CalendarDays className="size-4" /></span><div><p className="text-xs text-slate-400">Tham gia từ</p><p className="font-bold">{profile.createdAt ? new Date(profile.createdAt).toLocaleDateString("vi-VN") : "Đang cập nhật"}</p></div></div>
            </div>
          </SurfacePanel>
          <SurfacePanel>
            <div className="flex items-start gap-4 border-b border-slate-100 pb-5"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-emerald-50 text-emerald-700"><UserRound className="size-5" /></span><div><h2 className="text-lg font-black">Thông tin liên hệ</h2><p className="mt-1 text-sm text-slate-500">Thông tin này chỉ bạn mới có thể xem và chỉnh sửa.</p></div></div>
            <form className="mt-6 grid gap-5 sm:grid-cols-2" key={profile.updatedAt} onSubmit={submit}>
              <div className="sm:col-span-2"><Input label="Họ và tên" name="fullName" required maxLength={150} defaultValue={profile.fullName} /></div>
              <div className="relative"><Mail className="pointer-events-none absolute top-[2.45rem] left-3 size-4 text-slate-400" /><Input className="pl-10" label="Email đăng nhập" value={profile.email} disabled /></div>
              <div className="relative"><Phone className="pointer-events-none absolute top-[2.45rem] left-3 size-4 text-slate-400" /><Input className="pl-10" label="Số điện thoại" name="phone" required inputMode="tel" pattern="[0-9+ ]{8,15}" defaultValue={profile.phone ?? ""} /></div>
              {error ? <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700 sm:col-span-2">{error}</p> : null}
              <div className="flex justify-end sm:col-span-2"><Button className="h-11 min-w-36 rounded-xl bg-emerald-800 px-5 text-white hover:bg-emerald-700" disabled={saving} type="submit">{saving ? "Đang lưu..." : "Lưu thay đổi"}</Button></div>
            </form>

            <div className="mt-8 border-t border-slate-100 pt-6">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 rounded-2xl bg-amber-50/60 border border-amber-200/60 p-4 sm:p-5">
                <div className="flex items-center gap-3.5">
                  <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-amber-100 text-amber-700">
                    <Star className="size-6 fill-amber-400" />
                  </span>
                  <div>
                    <h3 className="text-base font-black text-slate-950">Đánh giá cá nhân của tôi</h3>
                    <p className="text-xs text-slate-600">
                      Đánh giá các sản phẩm đã mua hoặc xem lại các phản hồi bạn đã đóng góp.
                    </p>
                  </div>
                </div>
                <Button
                  className="bg-amber-600 hover:bg-amber-700 text-white font-bold shrink-0 shadow-xs"
                  size="sm"
                  onClick={() => router.push("/customer/account/reviews")}
                >
                  Vào mục Đánh giá →
                </Button>
              </div>
            </div>
          </SurfacePanel>
        </div>
      ) : null}
    </div>
  );
}
