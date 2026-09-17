"use client";

import { ErrorState, LoadingState } from "@/components/common/PageState";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { locationApi, type LocationOption } from "@/features/shipping";
import { ApiError } from "@/lib/api/error";
import { Check, ChevronLeft, ChevronRight, CreditCard, MapPin, Truck } from "lucide-react";
import { useEffect, useState } from "react";

type Step = "address" | "shipping" | "payment" | "confirm";
type PaymentChoice = "VNPAY_PREPAID" | "VNPAY_POSTPAID" | "COD";

const steps: { id: Step; label: string; icon: typeof MapPin }[] = [
  { id: "address", label: "Địa chỉ", icon: MapPin },
  { id: "shipping", label: "Giao hàng", icon: Truck },
  { id: "payment", label: "Thanh toán", icon: CreditCard },
  { id: "confirm", label: "Xác nhận", icon: Check },
];

const fallbackProvinces: LocationOption[] = [
  { id: 202, name: "Hồ Chí Minh" }, { id: 201, name: "Hà Nội" },
];
const fallbackWards: LocationOption[] = [
  { id: 10001, name: "Phường Minh Khai" }, { id: 10002, name: "Phường Bến Nghé" },
];

function formatVnd(value: number) { return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value); }

export function CheckoutPaymentWizard() {
  const [step, setStep] = useState<Step>("address");
  const [provinces, setProvinces] = useState<LocationOption[]>([]);
  const [wards, setWards] = useState<LocationOption[]>([]);
  const [loadingLocations, setLoadingLocations] = useState(true);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [loadingWards, setLoadingWards] = useState(false);
  const [address, setAddress] = useState({ recipientName: "", phone: "", provinceId: "", wardId: "", addressLine: "" });
  const [payment, setPayment] = useState<PaymentChoice>("VNPAY_PREPAID");
  const [quoteReady, setQuoteReady] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);

  useEffect(() => { void loadProvinces(); }, []);
  useEffect(() => {
    if (address.provinceId) void loadWards(Number(address.provinceId));
  }, [address.provinceId]);

  async function loadProvinces() {
    setLoadingLocations(true); setLocationError(null);
    try { setProvinces(await locationApi.provinces()); }
    catch (error) { setProvinces(fallbackProvinces); setLocationError(error instanceof ApiError ? error.message : "Không thể tải danh mục địa giới."); }
    finally { setLoadingLocations(false); }
  }
  async function loadWards(provinceId: number) {
    setLoadingWards(true); setLocationError(null);
    try { setWards(await locationApi.wards(provinceId)); }
    catch (error) { setWards(fallbackWards); setLocationError(error instanceof ApiError ? error.message : "Không thể tải danh mục Phường/Xã."); }
    finally { setLoadingWards(false); }
  }
  function updateAddress<K extends keyof typeof address>(key: K, value: (typeof address)[K]) { setAddress((current) => ({ ...current, [key]: value })); }
  function addressComplete() { return Object.values(address).every(Boolean); }
  function createQuote() { setQuoteReady(true); setStep("shipping"); }
  async function confirm() {
    setSubmitting(true);
    // Order-service will create Order and send its trusted OrderPaymentContext to payment-service.
    await new Promise((resolve) => window.setTimeout(resolve, 450));
    setSubmitting(false); setCompleted(true);
  }

  const subtotal = 699000;
  const shippingFee = quoteReady ? 30000 : 0;
  const total = subtotal + shippingFee;
  const stepIndex = steps.findIndex((item) => item.id === step);

  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:py-12">
      <p className="text-sm font-semibold text-brand">Thanh toán an toàn</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight">Hoàn tất đơn hàng</h1>
      <p className="mt-2 text-sm text-muted">Đi lần lượt qua các bước để hệ thống kiểm tra địa chỉ, phí giao hàng và phương thức thanh toán.</p>

      <ol className="my-8 grid grid-cols-4 gap-2" aria-label="Tiến trình thanh toán">
        {steps.map((item, index) => { const Icon = item.icon; const active = index === stepIndex; const done = index < stepIndex || completed; return (
          <li className="min-w-0" key={item.id}><div className={`flex items-center gap-2 text-xs font-medium ${active || done ? "text-brand" : "text-muted"}`}><span className={`grid size-8 place-items-center rounded-full border ${active || done ? "border-brand bg-brand text-white" : "border-border bg-surface"}`}>{done ? <Check className="size-4" /> : <Icon className="size-4" />}</span><span className="hidden sm:inline">{item.label}</span></div></li>
        ); })}
      </ol>

      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <section className="rounded-xl border border-border bg-surface p-5 sm:p-7">
          {step === "address" ? <AddressStep address={address} update={updateAddress} provinces={provinces} wards={wards} loading={loadingLocations} wardsLoading={loadingWards} error={locationError} retry={loadProvinces} onNext={createQuote} valid={addressComplete()} /> : null}
          {step === "shipping" ? <ShippingStep onBack={() => setStep("address")} onNext={() => setStep("payment")} quoteReady={quoteReady} /> : null}
          {step === "payment" ? <PaymentStep payment={payment} setPayment={setPayment} onBack={() => setStep("shipping")} onNext={() => setStep("confirm")} /> : null}
          {step === "confirm" ? <ConfirmStep address={address} payment={payment} total={total} onBack={() => setStep("payment")} onConfirm={confirm} submitting={submitting} completed={completed} /> : null}
        </section>
        <aside className="h-fit rounded-xl border border-border bg-surface p-5">
          <h2 className="font-semibold">Tóm tắt đơn hàng</h2>
          <div className="mt-4 space-y-3 text-sm"><div className="flex justify-between"><span className="text-muted">Tạm tính</span><span>{formatVnd(subtotal)}</span></div><div className="flex justify-between"><span className="text-muted">Phí GHN</span><span>{quoteReady ? formatVnd(shippingFee) : "Chưa báo giá"}</span></div><div className="border-t border-border pt-3 text-base font-semibold"><div className="flex justify-between"><span>Tổng cộng</span><span className="text-brand">{formatVnd(total)}</span></div></div></div>
          <p className="mt-4 text-xs leading-5 text-muted">Phí chỉ là bản xem trước ở giao diện. Khi tạo đơn, Checkout sẽ yêu cầu báo giá GHN phía máy chủ và kiểm tra lại toàn bộ dữ liệu.</p>
        </aside>
      </div>
    </div>
  );
}

function AddressStep({ address, update, provinces, wards, loading, wardsLoading, error, retry, onNext, valid }: Readonly<{ address: { recipientName: string; phone: string; provinceId: string; wardId: string; addressLine: string }; update: <K extends keyof typeof address>(key: K, value: (typeof address)[K]) => void; provinces: LocationOption[]; wards: LocationOption[]; loading: boolean; wardsLoading: boolean; error: string | null; retry: () => Promise<void>; onNext: () => void; valid: boolean }>) {
  if (loading) return <LoadingState title="Đang tải địa giới GHN" />;
  return <div className="space-y-5"><div><h2 className="text-xl font-semibold">1. Thông tin nhận hàng</h2><p className="mt-1 text-sm text-muted">Chọn địa giới GHN trước, sau đó nhập số nhà và tên đường.</p></div>{error ? <ErrorState title="Đang dùng dữ liệu minh họa" description={error} actionLabel="Tải lại" onAction={() => void retry()} /> : null}<div className="grid gap-4 sm:grid-cols-2"><Input label="Người nhận" value={address.recipientName} onChange={(e) => update("recipientName", e.target.value)} required /><Input label="Số điện thoại" inputMode="tel" value={address.phone} onChange={(e) => update("phone", e.target.value)} required /><Select label="Tỉnh/Thành phố" value={address.provinceId} onChange={(e) => { update("provinceId", e.target.value); update("wardId", ""); }} required><option value="">Chọn Tỉnh/Thành phố</option>{provinces.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</Select><Select label="Phường/Xã" value={address.wardId} disabled={!address.provinceId || wardsLoading} onChange={(e) => update("wardId", e.target.value)} required><option value="">{wardsLoading ? "Đang tải…" : "Chọn Phường/Xã"}</option>{wards.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</Select></div><Input label="Số nhà, tên đường" value={address.addressLine} onChange={(e) => update("addressLine", e.target.value)} required /><div className="flex justify-end"><Button disabled={!valid} onClick={onNext}>Tiếp tục báo giá <ChevronRight data-icon="inline-end" /></Button></div></div>;
}
function ShippingStep({ onBack, onNext, quoteReady }: Readonly<{ onBack: () => void; onNext: () => void; quoteReady: boolean }>) { return <div className="space-y-6"><div><h2 className="text-xl font-semibold">2. Báo giá giao hàng</h2><p className="mt-1 text-sm text-muted">Phí được tính từ địa chỉ, hàng hóa và kho gửi cố định.</p></div>{quoteReady ? <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900"><p className="font-medium">Đã có báo giá GHN</p><p className="mt-1">Giao tiêu chuẩn · Dự kiến 2–4 ngày · Báo giá có hạn 15 phút.</p></div> : <ErrorState title="Chưa có báo giá" description="Hãy quay lại để kiểm tra địa chỉ." />}<StepActions onBack={onBack} onNext={onNext} nextLabel="Chọn thanh toán" disabled={!quoteReady} /></div>; }
function PaymentStep({ payment, setPayment, onBack, onNext }: Readonly<{ payment: PaymentChoice; setPayment: (choice: PaymentChoice) => void; onBack: () => void; onNext: () => void }>) { const options: { value: PaymentChoice; title: string; description: string }[] = [{ value: "VNPAY_PREPAID", title: "VNPay — Thanh toán trước", description: "Chuyển sang VNPay ngay sau khi tạo đơn." }, { value: "VNPAY_POSTPAID", title: "VNPay — Thanh toán khi bàn giao", description: "Nhận QR thanh toán khi đơn chờ bàn giao." }, { value: "COD", title: "Thanh toán khi nhận hàng (COD)", description: "Thanh toán cho nhân viên giao hàng khi nhận đơn." }]; return <div className="space-y-5"><div><h2 className="text-xl font-semibold">3. Phương thức thanh toán</h2><p className="mt-1 text-sm text-muted">Chỉ các tổ hợp thanh toán được máy chủ cho phép mới hiển thị.</p></div><div className="space-y-3">{options.map((option) => <label className={`block cursor-pointer rounded-lg border p-4 ${payment === option.value ? "border-brand bg-brand/5" : "border-border"}`} key={option.value}><input className="sr-only" type="radio" name="payment" checked={payment === option.value} onChange={() => setPayment(option.value)} /><span className="font-medium">{option.title}</span><span className="mt-1 block text-sm text-muted">{option.description}</span></label>)}</div><StepActions onBack={onBack} onNext={onNext} nextLabel="Xem lại đơn" /></div>; }
function ConfirmStep({ address, payment, total, onBack, onConfirm, submitting, completed }: Readonly<{ address: { recipientName: string; phone: string; provinceId: string; wardId: string; addressLine: string }; payment: PaymentChoice; total: number; onBack: () => void; onConfirm: () => Promise<void>; submitting: boolean; completed: boolean }>) { if (completed) return <div className="py-6 text-center"><span className="mx-auto grid size-12 place-items-center rounded-full bg-emerald-100 text-emerald-700"><Check /></span><h2 className="mt-4 text-xl font-semibold">Đơn hàng đang được tạo</h2><p className="mt-2 text-sm text-muted">Hệ thống sẽ chuyển bạn sang VNPay nếu bạn đã chọn thanh toán trước; các phương thức khác sẽ được lưu trên đơn.</p></div>; const label = payment === "VNPAY_PREPAID" ? "Tạo đơn và đến VNPay" : "Xác nhận tạo đơn"; return <div className="space-y-5"><div><h2 className="text-xl font-semibold">4. Xác nhận</h2><p className="mt-1 text-sm text-muted">Kiểm tra lại thông tin trước khi tạo đơn.</p></div><dl className="space-y-3 rounded-lg bg-slate-50 p-4 text-sm"><div><dt className="text-muted">Người nhận</dt><dd className="font-medium">{address.recipientName} · {address.phone}</dd></div><div><dt className="text-muted">Địa chỉ</dt><dd className="font-medium">{address.addressLine}</dd></div><div><dt className="text-muted">Thanh toán</dt><dd className="font-medium">{payment === "COD" ? "COD" : payment === "VNPAY_PREPAID" ? "VNPay trả trước" : "VNPay trả sau"} · {formatVnd(total)}</dd></div></dl><StepActions onBack={onBack} onNext={() => void onConfirm()} nextLabel={label} submitting={submitting} /></div>; }
function StepActions({ onBack, onNext, nextLabel, disabled, submitting }: Readonly<{ onBack: () => void; onNext: () => void; nextLabel: string; disabled?: boolean; submitting?: boolean }>) { return <div className="flex items-center justify-between pt-2"><Button variant="outline" onClick={onBack}><ChevronLeft data-icon="inline-start" /> Quay lại</Button><Button disabled={disabled || submitting} onClick={onNext}>{submitting ? "Đang xử lý…" : nextLabel}<ChevronRight data-icon="inline-end" /></Button></div>; }
