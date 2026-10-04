"use client";

import { MapPin, Pencil, Plus, Star, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { customerApi } from "../api/customer.api";
import type { Address, LocationOption } from "../types/customer.types";

export function AddressBookPage() {
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [provinces, setProvinces] = useState<LocationOption[]>([]);
  const [wards, setWards] = useState<LocationOption[]>([]);
  const [provinceId, setProvinceId] = useState(0);
  const [wardId, setWardId] = useState(0);
  const [editing, setEditing] = useState<Address | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [message, setMessage] = useState("Đang tải…");

  const reload = () => customerApi.addresses().then((value) => { setAddresses(value); setMessage(""); }).catch(() => setMessage("Không thể tải sổ địa chỉ."));
  useEffect(() => { reload(); customerApi.provinces().then(setProvinces).catch(() => setMessage("Không thể tải danh mục tỉnh/thành.")); }, []);
  useEffect(() => { if (provinceId) customerApi.wards(provinceId).then(setWards).catch(() => setWards([])); }, [provinceId]);

  const openCreate = () => { setEditing(null); setProvinceId(0); setWardId(0); setWards([]); setShowForm(true); };
  const openEdit = (address: Address) => { setEditing(address); setProvinceId(address.provinceId); setWardId(address.wardId); setShowForm(true); };
  const closeForm = () => { setEditing(null); setProvinceId(0); setWardId(0); setWards([]); setShowForm(false); };
  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault(); const data = new FormData(event.currentTarget);
    const province = provinces.find((value) => value.id === provinceId); const ward = wards.find((value) => value.id === wardId);
    if (!province || !ward) { setMessage("Vui lòng chọn địa giới hợp lệ."); return; }
    const body = { recipientName: String(data.get("recipientName")), phone: String(data.get("phone")), addressLine: String(data.get("addressLine")), provinceId, provinceName: province.name, wardId, wardName: ward.name, defaultAddress: editing?.defaultAddress || data.get("defaultAddress") === "on" };
    try { if (editing) await customerApi.updateAddress(editing.id, body); else await customerApi.createAddress(body); closeForm(); reload(); }
    catch { setMessage("Không thể lưu địa chỉ. Hãy kiểm tra lại địa giới."); }
  };
  const makeDefault = async (id: string) => { try { await customerApi.makeDefault(id); reload(); } catch { setMessage("Không thể đặt địa chỉ mặc định."); } };
  const deactivate = async (id: string) => { if (!window.confirm("Ngừng sử dụng địa chỉ này?")) return; try { await customerApi.deactivateAddress(id); reload(); } catch { setMessage("Không thể ngừng sử dụng địa chỉ."); } };

  return <div className="space-y-7"><PageHeader eyebrow="Giao hàng" title="Sổ địa chỉ" description="Quản lý địa chỉ nhận hàng. Tỉnh/Thành phố và Phường/Xã được lấy từ danh mục địa giới và kiểm tra lại ở máy chủ." action={<Button size="lg" onClick={openCreate}><Plus />Thêm địa chỉ</Button>} />
    {message ? <SurfacePanel className={message.startsWith("Không") ? "text-red-600" : ""}>{message}</SurfacePanel> : null}
    {showForm ? <SurfacePanel><div className="mb-6"><h2 className="text-xl font-black text-stone-950">{editing ? "Sửa địa chỉ" : "Thêm địa chỉ mới"}</h2><p className="mt-1 text-sm text-stone-500">Chỉ nhập số nhà và tên đường; địa giới phải được chọn từ danh mục.</p></div><form key={editing?.id ?? "new"} className="grid gap-5 md:grid-cols-2" onSubmit={(event) => void submit(event)}>
      <Input name="recipientName" label="Người nhận" required defaultValue={editing?.recipientName} /><Input name="phone" label="Số điện thoại" required defaultValue={editing?.phone} />
      <Select name="provinceId" label="Tỉnh hoặc thành phố" required value={provinceId || ""} onChange={(event) => { setProvinceId(Number(event.target.value)); setWardId(0); setWards([]); }}><option value="" disabled>Chọn tỉnh thành</option>{provinces.map((value) => <option key={value.id} value={value.id}>{value.name}</option>)}</Select>
      <Select name="wardId" label="Phường hoặc xã" required value={wardId || ""} onChange={(event) => setWardId(Number(event.target.value))} disabled={!provinceId}><option value="" disabled>Chọn phường xã</option>{wards.map((value) => <option key={value.id} value={value.id}>{value.name}</option>)}</Select>
      <Input name="addressLine" className="md:col-span-2" label="Số nhà và tên đường" required defaultValue={editing?.addressLine} />
      {!editing?.defaultAddress ? <label className="flex items-center gap-3 text-sm font-bold text-stone-700"><input className="size-4 accent-rose-600" name="defaultAddress" type="checkbox" /> Đặt làm địa chỉ mặc định</label> : <p className="text-sm font-bold text-emerald-700">Đây là địa chỉ mặc định.</p>}
      <div className="flex flex-wrap gap-3 border-t border-stone-200 pt-5 md:col-span-2 md:justify-end"><Button type="button" variant="outline" onClick={closeForm}>Hủy</Button><Button size="lg" type="submit">{editing ? "Lưu thay đổi" : "Lưu địa chỉ"}</Button></div>
    </form></SurfacePanel> : null}
    {addresses.length === 0 && !message ? <SurfacePanel><div className="py-7 text-center"><MapPin className="mx-auto size-10 text-stone-300" /><h2 className="mt-4 font-black text-stone-900">Chưa có địa chỉ</h2><p className="mt-2 text-sm text-stone-500">Thêm địa chỉ để sử dụng khi xác nhận đơn hàng.</p><Button className="mt-5" onClick={openCreate}><Plus />Thêm địa chỉ</Button></div></SurfacePanel> : <div className="grid gap-4 lg:grid-cols-2">{addresses.map((address) => <SurfacePanel className={address.defaultAddress ? "border-rose-300 ring-1 ring-rose-100" : ""} key={address.id}><div className="flex items-start gap-3"><span className="rounded-xl bg-rose-50 p-2.5 text-rose-600"><MapPin /></span><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="font-black text-stone-950">{address.recipientName}</h2>{address.defaultAddress ? <span className="rounded-full bg-rose-100 px-2.5 py-1 text-xs font-bold text-rose-700">Mặc định</span> : null}</div><p className="mt-1 text-sm font-semibold text-stone-600">{address.phone}</p><p className="mt-3 text-sm leading-6 text-stone-600">{address.addressLine}, {address.wardName}, {address.provinceName}</p></div></div><div className="mt-5 flex flex-wrap gap-2 border-t border-stone-100 pt-4"><Button variant="outline" onClick={() => openEdit(address)}><Pencil />Sửa</Button>{!address.defaultAddress ? <Button variant="outline" onClick={() => void makeDefault(address.id)}><Star />Đặt mặc định</Button> : null}<Button variant="destructive" onClick={() => void deactivate(address.id)}><Trash2 />Ngừng dùng</Button></div></SurfacePanel>)}</div>}
  </div>;
}
