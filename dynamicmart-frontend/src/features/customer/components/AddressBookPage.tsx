"use client";

import { useCallback, useEffect, useState } from "react";
import { MapPin, Pencil, Phone, Plus, Star, Trash2, UserRound } from "lucide-react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/common/PageHeader";
import { EmptyState, LoadingState } from "@/components/common/PageState";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { useToast } from "@/components/ui/Toast";
import { locationApi } from "@/features/shipping";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";
import { addressApi } from "../api/address.api";
import type { Address, LocationOption } from "../types/customer.types";

export function AddressBookPage() {
  const router = useRouter();
  const session = useAuthSession();
  const [rows, setRows] = useState<Address[]>([]);
  const [provinces, setProvinces] = useState<LocationOption[]>([]);
  const [wards, setWards] = useState<LocationOption[]>([]);
  const [provinceId, setProvinceId] = useState(0);
  const [wardId, setWardId] = useState(0);
  const [editing, setEditing] = useState<Address | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const { showToast } = useToast();

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const [addressesResult, provincesResult] = await Promise.allSettled([addressApi.list(), locationApi.provinces()]);
    if (addressesResult.status === "fulfilled") setRows(addressesResult.value);
    else setError(isApiError(addressesResult.reason) ? addressesResult.reason.message : "Không thể tải sổ địa chỉ.");
    if (provincesResult.status === "fulfilled") setProvinces(provincesResult.value);
    else setError((current) => current || (isApiError(provincesResult.reason) ? provincesResult.reason.message : "Không thể tải danh mục Tỉnh/Thành phố."));
    setLoading(false);
  }, []);

  useEffect(() => {
    if (session.status !== "authenticated") return;
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load, session.status]);

  async function selectProvince(value: number, selectedWard = 0) {
    setProvinceId(value);
    setWardId(selectedWard);
    setWards([]);
    if (!value) return;
    try { setWards(await locationApi.wards(value)); }
    catch (cause) { setError(isApiError(cause) ? cause.message : "Không thể tải danh mục Phường/Xã."); }
  }

  async function edit(row: Address) {
    setEditing(row);
    setError("");
    await selectProvince(row.provinceId, row.wardId);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetForm() {
    setEditing(null);
    setProvinceId(0);
    setWardId(0);
    setWards([]);
    setError("");
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const province = provinces.find((item) => item.id === provinceId);
    const ward = wards.find((item) => item.id === wardId);
    if (!province || !ward) {
      setError("Vui lòng chọn đầy đủ Tỉnh/Thành phố và Phường/Xã.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const input = {
        recipientName: String(data.get("recipientName") ?? "").trim(),
        phone: String(data.get("phone") ?? "").trim(),
        addressLine: String(data.get("addressLine") ?? "").trim(),
        provinceId,
        wardId,
        defaultAddress: editing?.defaultAddress || data.get("defaultAddress") === "on",
      };
      if (editing) await addressApi.update(editing.id, input);
      else await addressApi.create(input);
      showToast(editing ? "Đã cập nhật địa chỉ." : "Đã thêm địa chỉ mới.", "success");
      resetForm();
      form.reset();
      await load();
    } catch (cause) { setError(isApiError(cause) ? cause.message : "Không thể lưu địa chỉ."); }
    finally { setSaving(false); }
  }

  async function makeDefault(id: string) {
    try { await addressApi.makeDefault(id); showToast("Đã đặt làm địa chỉ mặc định.", "success"); await load(); }
    catch (cause) { setError(isApiError(cause) ? cause.message : "Không thể đặt địa chỉ mặc định."); }
  }

  async function deactivate(id: string) {
    if (!window.confirm("Ngừng sử dụng địa chỉ này? Địa chỉ sẽ không bị xóa khỏi lịch sử.")) return;
    try { await addressApi.deactivate(id); showToast("Địa chỉ đã được ngừng sử dụng.", "success"); await load(); }
    catch (cause) { setError(isApiError(cause) ? cause.message : "Không thể ngừng sử dụng địa chỉ."); }
  }

  return (
    <div className="space-y-7">
      <div className="flex flex-wrap items-end justify-between gap-4"><PageHeader eyebrow="Tài khoản" title="Sổ địa chỉ" description="Quản lý địa chỉ nhận hàng; địa giới được kiểm tra trước khi lưu." /><span className="rounded-full bg-emerald-50 px-4 py-2 text-sm font-bold text-emerald-800">{rows.length} địa chỉ đang dùng</span></div>
      {session.status === "loading" ? <LoadingState /> : session.status === "anonymous" ? <SurfacePanel className="py-12 text-center"><MapPin className="mx-auto size-11 text-emerald-700" /><h2 className="mt-4 text-xl font-black">Đăng nhập để xem sổ địa chỉ</h2><Button className="mt-5 bg-emerald-800 text-white hover:bg-emerald-700" onClick={() => router.push("/login?returnTo=%2Fcustomer%2Faccount%2Faddresses")}>Đăng nhập</Button></SurfacePanel> : <>
        <SurfacePanel className="border-emerald-100 bg-gradient-to-br from-white to-emerald-50/40">
          <div className="flex items-start gap-4"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-emerald-700 text-white"><Plus className="size-5" /></span><div><h2 className="text-lg font-black">{editing ? "Sửa địa chỉ" : "Thêm địa chỉ mới"}</h2><p className="mt-1 text-sm text-slate-500">Tất cả 5 trường thông tin địa chỉ đều bắt buộc.</p></div></div>
          <form className="mt-6 grid gap-5 sm:grid-cols-2" key={editing?.id ?? "new"} onSubmit={submit}>
            <div className="relative"><UserRound className="pointer-events-none absolute top-[2.45rem] left-3 size-4 text-slate-400" /><Input className="pl-10" name="recipientName" label="Người nhận" required minLength={2} maxLength={150} defaultValue={editing?.recipientName ?? ""} /></div>
            <div className="relative"><Phone className="pointer-events-none absolute top-[2.45rem] left-3 size-4 text-slate-400" /><Input className="pl-10" name="phone" label="Số điện thoại" required inputMode="tel" minLength={8} pattern="[0-9+() .-]{8,20}" defaultValue={editing?.phone ?? ""} /></div>
            <div className="relative sm:col-span-2"><MapPin className="pointer-events-none absolute top-[2.45rem] left-3 size-4 text-slate-400" /><Input className="pl-10" name="addressLine" label="Số nhà, tên đường" required minLength={3} maxLength={255} defaultValue={editing?.addressLine ?? ""} /></div>
            <Select name="provinceId" label="Tỉnh/Thành phố" required value={provinceId || ""} onChange={(event) => void selectProvince(Number(event.target.value))}><option value="">Chọn Tỉnh/Thành phố</option>{provinces.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select>
            <Select name="wardId" label="Phường/Xã" required disabled={!provinceId} value={wardId || ""} onChange={(event) => setWardId(Number(event.target.value))}><option value="">{provinceId ? "Chọn Phường/Xã" : "Chọn Tỉnh/Thành phố trước"}</option>{wards.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select>
            {!editing?.defaultAddress ? <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium"><input className="size-4 accent-emerald-700" name="defaultAddress" type="checkbox" defaultChecked={rows.length === 0} /><Star className="size-4 text-amber-500" />Đặt làm địa chỉ mặc định</label> : <p className="flex items-center gap-2 text-sm font-bold text-emerald-700"><Star className="size-4" />Đây là địa chỉ mặc định</p>}
            <div className="flex flex-wrap gap-2 sm:justify-end"><Button className="h-11 rounded-xl bg-emerald-800 px-5 text-white hover:bg-emerald-700" disabled={saving} type="submit">{saving ? "Đang lưu..." : editing ? "Lưu địa chỉ" : "Thêm địa chỉ"}</Button>{editing ? <Button className="h-11 rounded-xl px-5" variant="outline" type="button" onClick={resetForm}>Hủy</Button> : null}</div>
          </form>
          {error ? <p className="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p> : null}
        </SurfacePanel>

        {loading ? <LoadingState /> : rows.length === 0 ? <EmptyState title="Chưa có địa chỉ" description="Thêm địa chỉ giao hàng đầu tiên bằng biểu mẫu phía trên." /> : <div className="grid gap-4 xl:grid-cols-2">{rows.map((row) => <SurfacePanel className={`relative overflow-hidden ${row.defaultAddress ? "border-emerald-300 ring-2 ring-emerald-100" : ""}`} key={row.id}>{row.defaultAddress ? <span className="absolute top-0 right-0 rounded-bl-2xl bg-emerald-700 px-4 py-2 text-xs font-bold text-white"><Star className="mr-1 inline size-3" />Mặc định</span> : null}<div className="flex gap-4"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-slate-100 text-slate-600"><MapPin className="size-5" /></span><div className="min-w-0 pr-14"><h3 className="font-black text-slate-950">{row.recipientName}</h3><p className="mt-1 text-sm font-medium text-slate-500">{row.phone}</p><p className="mt-3 text-sm leading-6 text-slate-700">{row.addressLine}, {row.wardName}, {row.provinceName}</p></div></div><div className="mt-5 flex flex-wrap gap-2 border-t border-slate-100 pt-4"><Button size="sm" variant="outline" onClick={() => void edit(row)}><Pencil />Sửa</Button>{!row.defaultAddress ? <Button size="sm" variant="outline" onClick={() => void makeDefault(row.id)}><Star />Đặt mặc định</Button> : null}<Button className="ml-auto" size="sm" variant="destructive" onClick={() => void deactivate(row.id)}><Trash2 />Ngừng dùng</Button></div></SurfacePanel>)}</div>}
      </>}
    </div>
  );
}
