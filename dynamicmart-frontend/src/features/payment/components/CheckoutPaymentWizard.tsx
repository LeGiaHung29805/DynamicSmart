"use client";

import { Check, ChevronLeft, ChevronRight, CreditCard, MapPin, Plus, Truck } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { customerApi } from "@/features/customer/api/customer.api";
import type { Address, LocationOption, VoucherWalletItem } from "@/features/customer/types/customer.types";
import { checkoutApi, type CheckoutPreview, type OrderResult } from "../api/checkout.api";

type Step = "address" | "shipping" | "payment" | "confirm";
type PaymentChoice = "COD" | "VNPAY_PREPAID" | "VNPAY_POSTPAID" | "ZALOPAY_PREPAID" | "PAYOS_PREPAID" | "BANK_QR_PREPAID";
const steps = [{ id: "address", label: "Địa chỉ", icon: MapPin }, { id: "shipping", label: "Giao hàng & mã giảm", icon: Truck }, { id: "payment", label: "Thanh toán", icon: CreditCard }, { id: "confirm", label: "Xác nhận", icon: Check }] as const;

export function CheckoutPaymentWizard() {
  const [step, setStep] = useState<Step>("address");
  const [addresses, setAddresses] = useState<Address[]>([]); const [vouchers, setVouchers] = useState<VoucherWalletItem[]>([]);
  const [addressId, setAddressId] = useState(""); const [merchandiseVoucherId, setMerchandiseVoucherId] = useState(""); const [shippingVoucherId, setShippingVoucherId] = useState("");
  const [preview, setPreview] = useState<CheckoutPreview | null>(null); const [payment, setPayment] = useState<PaymentChoice>("COD");
  const [loading, setLoading] = useState(true); const [message, setMessage] = useState(""); const [showAddressForm, setShowAddressForm] = useState(false);
  const [order, setOrder] = useState<OrderResult | null>(null);
  const idempotencyKey = useRef<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const current = steps.findIndex((item) => item.id === step);
  const merchandise = useMemo(() => vouchers.filter((value) => value.scope !== "SHIPPING_DISCOUNT"), [vouchers]);
  const shipping = useMemo(() => vouchers.filter((value) => value.scope === "SHIPPING_DISCOUNT"), [vouchers]);

  const load = async () => {
    try { const [addressRows, voucherRows] = await Promise.all([customerApi.addresses(), customerApi.vouchers()]); setAddresses(addressRows); setVouchers(voucherRows); setAddressId((value) => value || addressRows.find((item) => item.defaultAddress)?.id || addressRows[0]?.id || ""); setMessage(""); }
    catch { setMessage("Không thể tải dữ liệu Checkout."); } finally { setLoading(false); }
  };
  useEffect(() => {
    Promise.all([customerApi.addresses(), customerApi.vouchers()]).then(([addressRows, voucherRows]) => {
      setAddresses(addressRows); setVouchers(voucherRows);
      setAddressId(addressRows.find((item) => item.defaultAddress)?.id || addressRows[0]?.id || ""); setMessage("");
    }).catch(() => setMessage("Không thể tải dữ liệu Checkout.")).finally(() => setLoading(false));
  }, []);
  const refreshPreview = async () => {
    if (!addressId) return; setMessage("Đang kiểm tra giá, tồn kho và phí GHN…");
    try { const result = await checkoutApi.preview({ addressId, merchandiseVoucherId: merchandiseVoucherId || undefined, shippingVoucherId: shippingVoucherId || undefined }); setPreview(result); setMessage(""); }
    catch { setPreview(null); setMessage("Không thể tạo bản xem trước. Hãy kiểm tra tồn kho, địa chỉ và điều kiện voucher."); }
  };
  const continueToShipping = async () => { await refreshPreview(); setStep("shipping"); };
  const createOrder = async () => {
    if (!preview || submitting) return; setSubmitting(true); setMessage("Đang tạo đơn hàng…");
    const paymentTiming = payment.endsWith("_PREPAID") ? "PREPAID" : "POSTPAID";
    const paymentMethod = payment.replace(/_(PREPAID|POSTPAID)$/, "");
    idempotencyKey.current ??= crypto.randomUUID();
    try { const result = await checkoutApi.createOrder({ cartId: preview.cartId, addressId, merchandiseVoucherId: merchandiseVoucherId || undefined, shippingVoucherId: shippingVoucherId || undefined, quoteId: preview.shippingQuote.quoteId, paymentTiming, paymentMethod }, idempotencyKey.current); setOrder(result); setMessage(""); if (result.redirectUrl) window.location.assign(result.redirectUrl); }
    catch { setMessage("Không thể tạo đơn. Dữ liệu có thể đã thay đổi; hãy quay lại kiểm tra."); }
    finally { setSubmitting(false); }
  };

  if (loading) return <main className="mx-auto max-w-4xl px-4 py-12">Đang tải Checkout…</main>;
  return <main className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6"><p className="text-sm font-semibold text-brand">Thanh toán an toàn</p><h1 className="mt-1 text-3xl font-bold">Hoàn tất đơn hàng</h1>
    <ol className="my-8 grid grid-cols-4 gap-2">{steps.map((item, index) => { const Icon = item.icon; const active = index <= current; return <li key={item.id} className={`flex items-center gap-2 text-xs font-medium ${active ? "text-brand" : "text-slate-400"}`}><span className={`grid size-8 place-items-center rounded-full border ${active ? "border-brand bg-brand text-white" : "border-slate-200"}`}><Icon className="size-4" /></span><span className="hidden sm:inline">{item.label}</span></li>; })}</ol>
    {message ? <div className={`mb-5 rounded-xl border p-4 text-sm ${message.startsWith("Không") ? "border-red-200 bg-red-50 text-red-700" : "border-blue-200 bg-blue-50 text-blue-700"}`}>{message}</div> : null}
    <div className="grid gap-6 lg:grid-cols-[1fr_330px]"><section className="rounded-xl border bg-white p-5 sm:p-7">
      {step === "address" ? <AddressStep addresses={addresses} addressId={addressId} select={(id) => { setAddressId(id); setPreview(null); }} showForm={showAddressForm} toggleForm={() => setShowAddressForm((value) => !value)} saved={async (address) => { await load(); setAddressId(address.id); setShowAddressForm(false); setPreview(null); }} next={() => void continueToShipping()} /> : null}
      {step === "shipping" ? <div className="space-y-5"><h2 className="text-xl font-semibold">2. Giao hàng và mã giảm giá</h2><VoucherSelect label="Mã giảm hàng hóa" values={merchandise} value={merchandiseVoucherId} change={(value) => { setMerchandiseVoucherId(value); setPreview(null); }} /><VoucherSelect label="Mã giảm phí giao hàng" values={shipping} value={shippingVoucherId} change={(value) => { setShippingVoucherId(value); setPreview(null); }} />{preview?.merchandiseVoucher && !preview.merchandiseVoucher.eligible ? <p className="text-sm text-amber-700">{preview.merchandiseVoucher.ineligibleReason}</p> : null}{preview?.shippingVoucher && !preview.shippingVoucher.eligible ? <p className="text-sm text-amber-700">{preview.shippingVoucher.ineligibleReason}</p> : null}<div className="flex justify-between"><Button variant="outline" onClick={() => setStep("address")}><ChevronLeft />Quay lại</Button><Button onClick={async () => { await refreshPreview(); setStep("payment"); }}>Kiểm tra lại và tiếp tục<ChevronRight /></Button></div></div> : null}
      {step === "payment" ? <div className="space-y-5"><h2 className="text-xl font-semibold">3. Phương thức thanh toán</h2>{([['COD','COD khi nhận hàng'],['VNPAY_PREPAID','VNPay trả trước'],['VNPAY_POSTPAID','VNPay trả sau'],['ZALOPAY_PREPAID','ZaloPay'],['PAYOS_PREPAID','PayOS'],['BANK_QR_PREPAID','Chuyển khoản QR']] as const).map(([value, label]) => <label className="flex items-center gap-3 rounded-xl border p-4" key={value}><input type="radio" checked={payment === value} onChange={() => setPayment(value)} />{label}</label>)}<div className="flex justify-between"><Button variant="outline" onClick={() => setStep("shipping")}><ChevronLeft />Quay lại</Button><Button disabled={!preview} onClick={() => setStep("confirm")}>Tiếp tục<ChevronRight /></Button></div></div> : null}
      {step === "confirm" ? order ? <div className="py-8 text-center"><Check className="mx-auto size-12 text-emerald-600" /><h2 className="mt-4 text-xl font-bold">Đã tạo đơn {order.orderNumber}</h2><p className="mt-2 text-slate-500">Trạng thái: {order.status}</p></div> : <div className="space-y-5"><h2 className="text-xl font-semibold">4. Xác nhận đơn hàng</h2><p className="text-sm text-slate-600">Máy chủ sẽ kiểm tra lại giá, tồn kho, địa chỉ, báo giá và giữ lượt voucher ngay trước khi tạo đơn.</p><div className="flex justify-between"><Button variant="outline" onClick={() => setStep("payment")}><ChevronLeft />Quay lại</Button><Button disabled={!preview || submitting} onClick={() => void createOrder()}>{submitting ? "Đang tạo đơn…" : payment.endsWith("_PREPAID") ? "Tạo đơn và thanh toán" : "Xác nhận tạo đơn"}</Button></div></div> : null}
    </section><Summary preview={preview} /></div>
  </main>;
}

function AddressStep({ addresses, addressId, select, showForm, toggleForm, saved, next }: Readonly<{ addresses: Address[]; addressId: string; select: (id: string) => void; showForm: boolean; toggleForm: () => void; saved: (address: Address) => Promise<void>; next: () => void }>) {
  return <div className="space-y-5"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-xl font-black text-stone-950">1. Chọn địa chỉ giao hàng</h2><p className="mt-1 text-sm text-stone-500">Chỉ địa chỉ đang hoạt động và thuộc tài khoản của bạn mới được máy chủ chấp nhận.</p></div><Button variant="outline" onClick={toggleForm}><Plus />{showForm ? "Đóng biểu mẫu" : "Thêm địa chỉ"}</Button></div>{showForm ? <InlineAddressForm saved={saved} /> : null}{addresses.length === 0 ? <div className="rounded-2xl border border-dashed border-stone-300 p-7 text-center"><MapPin className="mx-auto size-9 text-stone-300" /><p className="mt-3 font-bold text-stone-800">Bạn chưa có địa chỉ nhận hàng</p><p className="mt-1 text-sm text-stone-500">Thêm địa chỉ mới để tiếp tục Checkout.</p></div> : <div className="grid gap-3 sm:grid-cols-2">{addresses.map((address) => <label className={`relative block cursor-pointer rounded-2xl border p-4 transition ${addressId === address.id ? "border-rose-400 bg-rose-50 ring-1 ring-rose-100" : "border-stone-200 hover:border-rose-200"}`} key={address.id}><div className="flex items-start gap-3"><input className="mt-1 size-4 accent-rose-600" type="radio" checked={addressId === address.id} onChange={() => select(address.id)} /><div><div className="flex flex-wrap items-center gap-2"><strong className="text-stone-950">{address.recipientName}</strong>{address.defaultAddress ? <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-700">Mặc định</span> : null}</div><p className="mt-1 text-sm font-semibold text-stone-600">{address.phone}</p><p className="mt-2 text-sm leading-6 text-stone-600">{address.addressLine}, {address.wardName}, {address.provinceName}</p></div></div></label>)}</div>}<div className="flex justify-end border-t border-stone-200 pt-5"><Button size="lg" disabled={!addressId} onClick={next}>Lấy báo giá GHN<ChevronRight /></Button></div></div>;
}

function InlineAddressForm({ saved }: Readonly<{ saved: (address: Address) => Promise<void> }>) {
  const [provinces, setProvinces] = useState<LocationOption[]>([]); const [wards, setWards] = useState<LocationOption[]>([]); const [provinceId, setProvinceId] = useState(0);
  const [submitting, setSubmitting] = useState(false); const [error, setError] = useState("");
  useEffect(() => { customerApi.provinces().then(setProvinces).catch(() => setProvinces([])); }, []);
  useEffect(() => { if (provinceId) customerApi.wards(provinceId).then(setWards).catch(() => setWards([])); }, [provinceId]);
  return <form className="grid gap-4 rounded-2xl border border-rose-100 bg-rose-50/50 p-5 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); const wardId = Number(data.get("wardId")); const province = provinces.find((item) => item.id === provinceId); const ward = wards.find((item) => item.id === wardId); if (!province || !ward) { setError("Vui lòng chọn Tỉnh/Thành phố và Phường/Xã hợp lệ."); return; } setSubmitting(true); setError(""); void customerApi.createAddress({ recipientName: String(data.get("recipientName")), phone: String(data.get("phone")), addressLine: String(data.get("addressLine")), provinceId, provinceName: province.name, wardId, wardName: ward.name, defaultAddress: addressesDefault(data) }).then(saved).catch(() => setError("Không thể lưu địa chỉ. Máy chủ đã từ chối địa giới hoặc dữ liệu không hợp lệ.")).finally(() => setSubmitting(false)); }}><div className="sm:col-span-2"><h3 className="font-black text-stone-900">Địa chỉ mới</h3><p className="mt-1 text-xs text-stone-500">Địa chỉ được lưu vào sổ và tự động chọn cho lần Checkout này.</p></div><Input name="recipientName" label="Người nhận" required /><Input name="phone" label="Số điện thoại" required /><Select label="Tỉnh/Thành phố" value={provinceId || ""} onChange={(event) => { setProvinceId(Number(event.target.value)); setWards([]); }} required><option value="">Chọn tỉnh/thành</option>{provinces.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select><Select name="wardId" label="Phường/Xã" required disabled={!provinceId}><option value="">Chọn phường/xã</option>{wards.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select><Input name="addressLine" className="sm:col-span-2" label="Số nhà, tên đường" required /><label className="flex items-center gap-2 text-sm font-bold text-stone-700"><input name="defaultAddress" type="checkbox" className="size-4 accent-rose-600" />Đặt làm mặc định</label>{error ? <p className="text-sm font-semibold text-red-600 sm:col-span-2">{error}</p> : null}<div className="flex justify-end sm:col-span-2"><Button size="lg" type="submit" disabled={submitting}>{submitting ? "Đang lưu…" : "Lưu và chọn địa chỉ"}</Button></div></form>;
}

function addressesDefault(data: FormData) { return data.get("defaultAddress") === "on"; }
function VoucherSelect({ label, values, value, change }: Readonly<{ label: string; values: VoucherWalletItem[]; value: string; change: (value: string) => void }>) { return <Select label={label} value={value} onChange={(event) => change(event.target.value)}><option value="">Không dùng mã</option>{values.map((item) => <option value={item.id} key={item.id}>{item.code} — {item.name}</option>)}</Select>; }
function Summary({ preview }: Readonly<{ preview: CheckoutPreview | null }>) { return <aside className="h-fit rounded-xl border bg-white p-5"><h2 className="font-semibold">Tóm tắt đơn hàng</h2>{preview ? <div className="mt-4 space-y-3 text-sm">{preview.items.map((item) => <div className="flex justify-between gap-3" key={item.cartItemId}><span>{item.productName} × {item.quantity}</span><strong>{money(item.lineTotalVnd)}</strong></div>)}<Row label="Giá niêm yết" value={money(preview.listSubtotalVnd)} /><Row label="Direct sale" value={`−${money(preview.directSaleDiscountVnd)}`} /><Row label="Voucher hàng" value={`−${money(preview.merchandiseDiscountVnd)}`} /><Row label={`GHN · ${preview.shippingQuote.serviceName}`} value={money(preview.shippingQuote.feeVnd)} /><Row label="Voucher vận chuyển" value={`−${money(preview.shippingDiscountVnd)}`} /><div className="flex justify-between border-t pt-3 text-base font-bold"><span>Tổng cộng</span><span className="text-brand">{money(preview.finalTotalVnd)}</span></div><p className="text-xs text-slate-500">Báo giá hết hạn {new Date(preview.shippingQuote.expiresAt).toLocaleString("vi-VN")}</p></div> : <p className="mt-4 text-sm text-slate-500">Chọn địa chỉ để máy chủ kiểm tra giỏ hàng và lấy báo giá.</p>}</aside>; }
function Row({ label, value }: Readonly<{ label: string; value: string }>) { return <div className="flex justify-between"><span className="text-slate-500">{label}</span><span>{value}</span></div>; }
function money(value: number) { return `${value.toLocaleString("vi-VN")}đ`; }
