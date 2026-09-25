"use client";

import { MapPin, Plus, Star, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { customerApi } from "../api/customer.api";
import type { Address, LocationOption } from "../types/customer.types";

export function AddressBookPage() {
  const [addresses, setAddresses] = useState<Address[]>([]); const [provinces, setProvinces] = useState<LocationOption[]>([]);
  const [wards, setWards] = useState<LocationOption[]>([]); const [provinceId, setProvinceId] = useState(0);
  const [showForm, setShowForm] = useState(false); const [message, setMessage] = useState("Đang tải…");
  const reload = () => customerApi.addresses().then((value) => { setAddresses(value); setMessage(""); }).catch(() => setMessage("Không thể tải sổ địa chỉ."));
  useEffect(() => { reload(); customerApi.provinces().then(setProvinces).catch(() => setMessage("Không thể tải danh mục tỉnh/thành.")); }, []);
  useEffect(() => { if (provinceId) customerApi.wards(provinceId).then(setWards).catch(() => setWards([])); }, [provinceId]);
  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault(); const data = new FormData(event.currentTarget); const wardId = Number(data.get("wardId"));
    const province = provinces.find((value) => value.id === provinceId); const ward = wards.find((value) => value.id === wardId);
    if (!province || !ward) return;
    try {
      await customerApi.createAddress({ recipientName: String(data.get("recipientName")), phone: String(data.get("phone")), addressLine: String(data.get("addressLine")), provinceId, provinceName: province.name, wardId, wardName: ward.name, defaultAddress: data.get("defaultAddress") === "on" });
      setShowForm(false); setProvinceId(0); reload();
    } catch { setMessage("Không thể lưu địa chỉ. Hãy kiểm tra lại địa giới."); }
  };
  const makeDefault = async (id: string) => { try { await customerApi.makeDefault(id); reload(); } catch { setMessage("Không thể đặt địa chỉ mặc định."); } };
  const deactivate = async (id: string) => { if (!window.confirm("Ngừng sử dụng địa chỉ này?")) return; try { await customerApi.deactivateAddress(id); reload(); } catch { setMessage("Không thể ngừng sử dụng địa chỉ."); } };
  return <div className="space-y-7"><PageHeader eyebrow="Giao hàng" title="Sổ địa chỉ" description="Tỉnh thành và phường xã được chọn từ danh mục GHN; hệ thống kiểm tra lại trước khi lưu." action={<Button onClick={() => setShowForm(!showForm)}><Plus />Thêm địa chỉ</Button>} />
    {message ? <SurfacePanel className={message.startsWith("Không") ? "text-red-600" : ""}>{message}</SurfacePanel> : null}
    {showForm ? <SurfacePanel><form className="grid gap-4 md:grid-cols-2" onSubmit={(event) => void submit(event)}>
      <Input name="recipientName" label="Người nhận" required /><Input name="phone" label="Số điện thoại" required />
      <Select name="provinceId" label="Tỉnh hoặc thành phố" required value={provinceId || ""} onChange={(event) => { setWards([]); setProvinceId(Number(event.target.value)); }}><option value="" disabled>Chọn tỉnh thành</option>{provinces.map((value) => <option key={value.id} value={value.id}>{value.name}</option>)}</Select>
      <Select name="wardId" label="Phường hoặc xã" required defaultValue="" disabled={!provinceId}><option value="" disabled>Chọn phường xã</option>{wards.map((value) => <option key={value.id} value={value.id}>{value.name}</option>)}</Select>
      <Input name="addressLine" className="md:col-span-2" label="Số nhà và tên đường" required />
      <label className="flex items-center gap-2 text-sm"><input name="defaultAddress" type="checkbox" /> Đặt làm địa chỉ mặc định</label>
      <div className="flex gap-2 md:col-span-2"><Button type="submit">Lưu địa chỉ</Button><Button type="button" variant="outline" onClick={() => setShowForm(false)}>Hủy</Button></div>
    </form></SurfacePanel> : null}
    {addresses.length === 0 && !message ? <SurfacePanel>Bạn chưa có địa chỉ đang hoạt động.</SurfacePanel> : <div className="grid gap-4 lg:grid-cols-2">{addresses.map((address) => <SurfacePanel key={address.id}><div className="flex items-start gap-3"><span className="rounded-xl bg-emerald-50 p-2 text-brand"><MapPin /></span><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="font-bold text-slate-950">{address.recipientName}</h2>{address.defaultAddress ? <span className="rounded-full bg-emerald-50 px-2 py-1 text-xs font-bold text-emerald-700">Mặc định</span> : null}</div><p className="mt-1 text-sm text-slate-600">{address.phone}</p><p className="mt-2 text-sm leading-6 text-slate-600">{address.addressLine}, {address.wardName}, {address.provinceName}</p></div></div><div className="mt-5 flex flex-wrap gap-2">{!address.defaultAddress ? <Button variant="outline" onClick={() => void makeDefault(address.id)}><Star />Đặt mặc định</Button> : null}<Button variant="destructive" onClick={() => void deactivate(address.id)}><Trash2 />Ngừng dùng</Button></div></SurfacePanel>)}</div>}
  </div>;
}
