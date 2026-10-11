"use client";

/* eslint-disable @next/next/no-img-element -- Catalog image URLs may come from arbitrary configured hosts. */

import Link from "next/link";
import { Check, ChevronLeft, ChevronRight, MapPin, Plus, ShieldCheck, TimerReset, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { cartApi } from "@/features/cart";
import { addressApi } from "@/features/customer/api/address.api";
import { customerApi } from "@/features/customer/api/customer.api";
import type { Address, LocationOption, VoucherWalletItem } from "@/features/customer/types/customer.types";
import { locationApi } from "@/features/shipping";
import type { PaymentMethod, PaymentTiming } from "@/features/order/types/order.types";
import { orderStatusLabel } from "@/features/order/utils/order-format";
import { paymentMethodsApi, type AvailablePaymentMethod } from "@/features/payment/api/payment-methods.api";
import { isApiError } from "@/lib/api/error";
import { useAuthSession } from "@/lib/auth/session";
import { checkoutApi } from "../api/checkout.api";
import type { CheckoutPreview, CheckoutSession, CreateOrderResult } from "../types/checkout.types";

type Step = "delivery" | "review";
type PaymentChoice = { label: string; description: string; method: PaymentMethod; timing: PaymentTiming };

const steps = [
  { id: "delivery", label: "Giao hàng", icon: MapPin },
  { id: "review", label: "Thanh toán & xác nhận", icon: Check },
] as const;

const paymentChoices: PaymentChoice[] = [
  { method: "COD", timing: "POSTPAID", label: "Thanh toán khi nhận hàng", description: "Thanh toán khi đơn được giao thành công." },
  { method: "VNPAY", timing: "PREPAID", label: "VNPay", description: "Thanh toán an toàn qua cổng VNPay." },
  { method: "ZALOPAY", timing: "PREPAID", label: "ZaloPay", description: "Thanh toán bằng ví ZaloPay." },
  { method: "PAYOS", timing: "PREPAID", label: "PayOS", description: "Thanh toán qua liên kết PayOS." },
  { method: "BANK_QR", timing: "PREPAID", label: "Chuyển khoản QR", description: "Quét mã QR để chuyển khoản." },
];

function errorMessage(cause: unknown, fallback: string) {
  return isApiError(cause) ? cause.message : fallback;
}

function timingFor(method: PaymentMethod): PaymentTiming {
  return method === "COD" ? "POSTPAID" : "PREPAID";
}

export function CheckoutPage({ initialSessionId, initialVoucherId }: Readonly<{ initialSessionId?: string; initialVoucherId?: string }>) {
  const router = useRouter();
  const auth = useAuthSession();
  const [step, setStep] = useState<Step>("delivery");
  const [session, setSession] = useState<CheckoutSession | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [vouchers, setVouchers] = useState<VoucherWalletItem[]>([]);
  const [addressId, setAddressId] = useState("");
  const [merchandiseVoucherId, setMerchandiseVoucherId] = useState("");
  const [shippingVoucherId, setShippingVoucherId] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("COD");
  const [paymentTiming, setPaymentTiming] = useState<PaymentTiming>("POSTPAID");
  const [availablePaymentMethods, setAvailablePaymentMethods] = useState<AvailablePaymentMethod[]>(["COD"]);
  const [preview, setPreview] = useState<CheckoutPreview | null>(null);
  const [order, setOrder] = useState<CreateOrderResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [message, setMessage] = useState("");
  const [showAddressForm, setShowAddressForm] = useState(false);
  const cartBootstrap = useRef<Promise<CheckoutSession> | null>(null);
  const idempotencyKey = useRef<string | null>(null);

  const current = steps.findIndex((item) => item.id === step);
  const merchandiseVouchers = useMemo(
    () => vouchers.filter((value) => value.scope !== "SHIPPING_DISCOUNT"),
    [vouchers],
  );
  const shippingVouchers = useMemo(
    () => vouchers.filter((value) => value.scope === "SHIPPING_DISCOUNT"),
    [vouchers],
  );
  const availablePaymentChoices = useMemo(
    () => paymentChoices.filter((choice) => availablePaymentMethods.includes(choice.method as AvailablePaymentMethod)),
    [availablePaymentMethods],
  );

  useEffect(() => {
    if (auth.status === "loading") return;
    if (auth.status === "anonymous") {
      const returnTo = initialSessionId ? `/checkout?sessionId=${encodeURIComponent(initialSessionId)}` : "/checkout";
      router.replace(`/login?returnTo=${encodeURIComponent(returnTo)}`);
      return;
    }

    let ignored = false;
    async function load() {
      setLoading(true);
      setMessage("");
      try {
        let sessionPromise: Promise<CheckoutSession>;
        if (initialSessionId) {
          sessionPromise = checkoutApi.getSession(initialSessionId);
        } else {
          const bootstrap = cartBootstrap.current ?? cartApi
            .get()
            .then((cart) => checkoutApi.createSession({ source: "CART", cartId: cart.id }));
          cartBootstrap.current = bootstrap;
          sessionPromise = bootstrap;
        }
        const [nextSession, nextAddresses, nextVouchers, paymentMethods] = await Promise.all([
          sessionPromise,
          addressApi.list(),
          customerApi.vouchers(),
          paymentMethodsApi.available().catch(() => ({ methods: ["COD"] as AvailablePaymentMethod[] })),
        ]);
        if (ignored) return;
        setSession(nextSession);
        setAddresses(nextAddresses);
        setVouchers(nextVouchers);
        setAvailablePaymentMethods(paymentMethods.methods);
        if (initialVoucherId) {
          const initialVoucher = nextVouchers.find((item) => item.id === initialVoucherId && item.scope !== "SHIPPING_DISCOUNT");
          if (initialVoucher) setMerchandiseVoucherId(initialVoucher.id);
        }
        setAddressId(nextSession.addressId ?? nextAddresses.find((item) => item.defaultAddress)?.id ?? nextAddresses[0]?.id ?? "");
        const restoredMethod = nextSession.paymentMethod
          && paymentMethods.methods.includes(nextSession.paymentMethod as AvailablePaymentMethod)
          ? nextSession.paymentMethod
          : "COD";
        setPaymentMethod(restoredMethod);
        setPaymentTiming(timingFor(restoredMethod));
        if (!initialSessionId) router.replace(`/checkout?sessionId=${encodeURIComponent(nextSession.id)}`);
      } catch (cause) {
        if (!ignored) setMessage(errorMessage(cause, "Không thể khởi tạo phiên thanh toán."));
      } finally {
        if (!ignored) setLoading(false);
      }
    }
    void load();
    return () => { ignored = true; };
  }, [auth.status, initialSessionId, initialVoucherId, router]);

  function invalidateDraft() {
    setPreview(null);
    setOrder(null);
    idempotencyKey.current = null;
  }

  async function reloadAccountData() {
    const [nextAddresses, nextVouchers] = await Promise.all([addressApi.list(), customerApi.vouchers()]);
    setAddresses(nextAddresses);
    setVouchers(nextVouchers);
    return nextAddresses;
  }

  async function saveAndPreview(nextStep?: Step) {
    if (!session || !addressId || working) return false;
    setWorking(true);
    setMessage("Đang kiểm tra lại giá, tồn kho, mã giảm giá và phí giao hàng…");
    try {
      const updated = await checkoutApi.updateSession(session.id, { addressId, paymentMethod, paymentTiming });
      const nextPreview = await checkoutApi.preview(session.id, {
        merchandiseVoucherId: merchandiseVoucherId || undefined,
        shippingVoucherId: shippingVoucherId || undefined,
      });
      setSession({
        ...updated,
        paymentMethod: nextPreview.paymentMethod,
        paymentTiming: nextPreview.paymentTiming,
      });
      setPaymentMethod(nextPreview.paymentMethod);
      setPaymentTiming(nextPreview.paymentTiming);
      setPreview(nextPreview);
      setMessage("");
      if (nextStep) setStep(nextStep);
      return true;
    } catch (cause) {
      setPreview(null);
      setMessage(errorMessage(cause, "Không thể tính lại đơn hàng. Vui lòng kiểm tra thông tin và thử lại."));
      return false;
    } finally {
      setWorking(false);
    }
  }

  async function createOrder() {
    if (!session || working) return;
    setWorking(true);
    setMessage("Đang tạo đơn hàng…");
    idempotencyKey.current ??= crypto.randomUUID();
    try {
      const result = await checkoutApi.createOrder(session.id, idempotencyKey.current);
      setOrder(result);
      setSession((value) => value ? { ...value, status: "COMPLETED" } : value);
      setMessage("");
      if (result.redirectUrl) window.location.assign(result.redirectUrl);
    } catch (cause) {
      setMessage(errorMessage(cause, "Không thể tạo đơn. Hãy kiểm tra lại thông tin đặt hàng và thử lại."));
    } finally {
      setWorking(false);
    }
  }

  async function confirmAndCreate() {
    const previewed = await saveAndPreview();
    if (previewed) await createOrder();
  }

  async function cancelSession() {
    if (!session || working || !window.confirm("Hủy phiên đặt hàng hiện tại?")) return;
    setWorking(true);
    setMessage("Đang hủy phiên đặt hàng…");
    try {
      await checkoutApi.cancel(session.id);
      router.replace("/cart");
    } catch (cause) {
      setMessage(errorMessage(cause, "Không thể hủy phiên đặt hàng."));
    } finally {
      setWorking(false);
    }
  }

  if (auth.status === "loading" || loading) {
    return <main className="mx-auto min-h-[60vh] max-w-5xl px-4 py-12"><LoadingState title="Đang chuẩn bị đặt hàng" /></main>;
  }
  if (auth.status === "anonymous") return null;
  if (!session) {
    return <main className="mx-auto min-h-[60vh] max-w-3xl px-4 py-12"><ErrorState description={message || "Không tìm thấy phiên đặt hàng."} onAction={() => router.refresh()} /></main>;
  }
  if (session.status !== "ACTIVE" && !order) {
    const sessionLabels = { ACTIVE: "đang hoạt động", COMPLETED: "đã tạo đơn", CANCELLED: "đã hủy", EXPIRED: "đã hết hạn" } as const;
    return <main className="mx-auto min-h-[60vh] max-w-3xl px-4 py-12"><ErrorState actionLabel="Quay lại giỏ hàng" description={`Phiên đặt hàng ${sessionLabels[session.status]} và không thể tiếp tục.`} onAction={() => router.push("/cart")} title="Phiên đặt hàng không còn hiệu lực" /></main>;
  }

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6">
      <section className="relative overflow-hidden rounded-[2rem] bg-slate-950 px-6 py-8 text-white shadow-xl sm:px-8">
        <div className="absolute -right-16 -top-20 size-56 rounded-full bg-rose-500/25 blur-3xl" />
        <div className="relative flex flex-wrap items-start justify-between gap-5">
          <div className="max-w-2xl">
            <p className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1.5 text-xs font-bold uppercase tracking-[0.16em] text-rose-100"><ShieldCheck className="size-4" />Thanh toán an toàn</p>
            <h1 className="mt-4 text-3xl font-black sm:text-4xl">Hoàn tất đơn hàng</h1>
            <p className="mt-3 text-sm leading-6 text-slate-300">Chỉ cần chọn thông tin giao hàng, sau đó kiểm tra và xác nhận đơn. Giá, tồn kho, ưu đãi và phí giao hàng đều được máy chủ kiểm tra lại.</p>
            <p className="mt-4 flex items-center gap-2 text-xs text-slate-400"><TimerReset className="size-4" />Phiên hết hạn {new Date(session.expiresAt).toLocaleString("vi-VN")}</p>
          </div>
          <Button className="border-white/20 bg-transparent text-white hover:bg-white/10" variant="outline" disabled={working} onClick={() => void cancelSession()}><X />Hủy phiên</Button>
        </div>
      </section>

      <ol className="my-8 grid grid-cols-2 gap-3">
        {steps.map((item, index) => {
          const Icon = item.icon;
          const active = index <= current;
          return <li className={`flex items-center gap-3 rounded-2xl border p-3 text-sm font-bold ${active ? "border-rose-200 bg-rose-50 text-rose-700" : "border-slate-200 bg-white text-slate-400"}`} key={item.id}><span className={`grid size-8 place-items-center rounded-full ${active ? "bg-rose-600 text-white" : "bg-slate-100"}`}><Icon className="size-4" /></span><span>{index + 1}. {item.label}</span></li>;
        })}
      </ol>

      {message ? <div className={`mb-5 rounded-xl border p-4 text-sm ${message.startsWith("Đang") ? "border-blue-200 bg-blue-50 text-blue-700" : "border-red-200 bg-red-50 text-red-700"}`}>{message}</div> : null}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
        <section className="rounded-3xl border bg-white p-5 shadow-sm sm:p-7">
          {step === "delivery" ? <DeliveryStep addresses={addresses} addressId={addressId} disabled={working} merchandise={merchandiseVouchers} merchandiseVoucherId={merchandiseVoucherId} shipping={shippingVouchers} shippingVoucherId={shippingVoucherId} selectAddress={(id) => { setAddressId(id); invalidateDraft(); }} showForm={showAddressForm} toggleForm={() => setShowAddressForm((value) => !value)} saved={async (address) => { const rows = await reloadAccountData(); setAddressId(rows.find((item) => item.id === address.id)?.id ?? address.id); setShowAddressForm(false); invalidateDraft(); }} changeMerchandise={(value) => { setMerchandiseVoucherId(value); invalidateDraft(); }} changeShipping={(value) => { setShippingVoucherId(value); invalidateDraft(); }} next={() => void saveAndPreview("review")} /> : null}
          {step === "review" ? <ReviewStep choices={availablePaymentChoices} disabled={working} method={paymentMethod} preview={preview} order={order} back={() => setStep("delivery")} select={(choice) => { setPaymentMethod(choice.method); setPaymentTiming(choice.timing); setOrder(null); idempotencyKey.current = null; }} create={() => void confirmAndCreate()} /> : null}
        </section>
        <CheckoutSummary preview={preview} session={session} />
      </div>
    </main>
  );
}

function DeliveryStep({ addresses, addressId, disabled, merchandise, merchandiseVoucherId, shipping, shippingVoucherId, selectAddress, showForm, toggleForm, saved, changeMerchandise, changeShipping, next }: Readonly<{ addresses: Address[]; addressId: string; disabled: boolean; merchandise: VoucherWalletItem[]; merchandiseVoucherId: string; shipping: VoucherWalletItem[]; shippingVoucherId: string; selectAddress: (id: string) => void; showForm: boolean; toggleForm: () => void; saved: (address: Address) => Promise<void>; changeMerchandise: (value: string) => void; changeShipping: (value: string) => void; next: () => void }>) {
  return <div className="space-y-6"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-xl font-black text-stone-950">1. Thông tin giao hàng</h2><p className="mt-1 text-sm text-stone-500">Chọn nơi nhận, mã giảm giá và lấy báo giá GHN trong một bước.</p></div><Button variant="outline" disabled={disabled} onClick={toggleForm}><Plus />{showForm ? "Đóng biểu mẫu" : "Thêm địa chỉ"}</Button></div>{showForm ? <InlineAddressForm saved={saved} /> : null}{addresses.length === 0 ? <div className="rounded-2xl border border-dashed border-stone-300 p-7 text-center"><MapPin className="mx-auto size-9 text-stone-300" /><p className="mt-3 font-bold text-stone-800">Bạn chưa có địa chỉ nhận hàng</p></div> : <div className="grid gap-3 sm:grid-cols-2">{addresses.map((address) => <label className={`relative block cursor-pointer rounded-2xl border p-4 transition ${addressId === address.id ? "border-rose-400 bg-rose-50 ring-1 ring-rose-100" : "border-stone-200 hover:border-rose-200"}`} key={address.id}><div className="flex items-start gap-3"><input className="mt-1 size-4 accent-rose-600" type="radio" checked={addressId === address.id} disabled={disabled} onChange={() => selectAddress(address.id)} /><div><div className="flex flex-wrap items-center gap-2"><strong>{address.recipientName}</strong>{address.defaultAddress ? <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-bold text-rose-700">Mặc định</span> : null}</div><p className="mt-1 text-sm font-semibold text-stone-600">{address.phone}</p><p className="mt-2 text-sm leading-6 text-stone-600">{address.addressLine}, {address.wardName}, {address.provinceName}</p></div></div></label>)}</div>}<div className="grid gap-4 border-t border-stone-200 pt-5 sm:grid-cols-2"><VoucherSelect label="Mã giảm hàng hóa" values={merchandise} value={merchandiseVoucherId} change={changeMerchandise} /><VoucherSelect label="Mã giảm phí giao hàng" values={shipping} value={shippingVoucherId} change={changeShipping} /></div><div className="flex justify-end border-t border-stone-200 pt-5"><Button size="lg" disabled={!addressId || disabled} onClick={next}>Lấy báo giá GHN & tiếp tục<ChevronRight /></Button></div></div>;
}

function InlineAddressForm({ saved }: Readonly<{ saved: (address: Address) => Promise<void> }>) {
  const [provinces, setProvinces] = useState<LocationOption[]>([]);
  const [wards, setWards] = useState<LocationOption[]>([]);
  const [provinceId, setProvinceId] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { locationApi.provinces().then(setProvinces).catch(() => setProvinces([])); }, []);
  useEffect(() => { if (provinceId) locationApi.wards(provinceId).then(setWards).catch(() => setWards([])); }, [provinceId]);
  return <form className="grid gap-4 rounded-2xl border border-rose-100 bg-rose-50/50 p-5 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); const form = event.currentTarget; if (!form.checkValidity()) { form.reportValidity(); return; } const data = new FormData(form); const wardId = Number(data.get("wardId")); const province = provinces.find((item) => item.id === provinceId); const ward = wards.find((item) => item.id === wardId); if (!province || !ward) { setError("Vui lòng chọn Tỉnh/Thành phố và Phường/Xã do GHN hỗ trợ."); return; } setSubmitting(true); setError(""); void addressApi.create({ recipientName: String(data.get("recipientName")).trim(), phone: String(data.get("phone")).trim(), addressLine: String(data.get("addressLine")).trim(), provinceId, wardId, defaultAddress: data.get("defaultAddress") === "on" }).then(saved).catch((cause) => setError(errorMessage(cause, "Không thể lưu địa chỉ."))).finally(() => setSubmitting(false)); }}><div className="sm:col-span-2"><h3 className="font-black">Địa chỉ mới</h3><p className="mt-1 text-xs text-stone-500">Tỉnh/Thành phố và Phường/Xã được máy chủ đối chiếu với danh mục GHN trước khi lưu.</p></div><Input autoComplete="name" name="recipientName" label="Người nhận" required minLength={2} maxLength={150} /><Input autoComplete="tel" inputMode="tel" name="phone" label="Số điện thoại" required pattern="^(?:0|\+84)(?:3|5|7|8|9)\d{8}$" title="Nhập số điện thoại Việt Nam hợp lệ, ví dụ 0374505367 hoặc +84374505367." /><Select label="Tỉnh/Thành phố" value={provinceId || ""} onChange={(event) => { setProvinceId(Number(event.target.value)); setWards([]); }} required><option value="">Chọn tỉnh/thành</option>{provinces.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select><Select name="wardId" label="Phường/Xã" required disabled={!provinceId}><option value="">Chọn phường/xã</option>{wards.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select><div className="sm:col-span-2"><Input autoComplete="street-address" name="addressLine" label="Số nhà, tên đường" required minLength={3} maxLength={500} /></div><label className="flex items-center gap-2 text-sm font-bold"><input name="defaultAddress" type="checkbox" />Đặt làm mặc định</label>{error ? <p className="text-sm text-danger sm:col-span-2">{error}</p> : null}<div className="flex justify-end sm:col-span-2"><Button type="submit" disabled={submitting}>{submitting ? "Đang lưu…" : "Lưu địa chỉ"}</Button></div></form>;
}

function ReviewStep({ choices, disabled, method, preview, order, back, select, create }: Readonly<{ choices: PaymentChoice[]; disabled: boolean; method: PaymentMethod; preview: CheckoutPreview | null; order: CreateOrderResult | null; back: () => void; select: (choice: PaymentChoice) => void; create: () => void }>) {
  if (order) return <div className="py-8 text-center"><Check className="mx-auto size-12 text-emerald-600" /><h2 className="mt-4 text-xl font-bold">Đã tạo đơn {order.orderNumber}</h2><p className="mt-2 text-slate-500">Trạng thái: {orderStatusLabel(order.status)}</p>{order.paymentId ? <p className="mt-1 text-xs text-slate-400">Mã thanh toán: {order.paymentId}</p> : null}<Link className="mt-6 inline-flex rounded-xl bg-emerald-950 px-5 py-3 text-sm font-black text-white" href={`/customer/account/orders/${order.orderId}`}>Xem chi tiết đơn hàng</Link></div>;
  return <div className="space-y-6"><div><h2 className="text-xl font-black text-stone-950">2. Thanh toán và xác nhận</h2><p className="mt-1 text-sm text-stone-500">Kiểm tra báo giá GHN, chọn một phương thức thanh toán rồi đặt đơn.</p></div>{preview ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900"><strong>GHN · {preview.shipping.serviceName}</strong><p className="mt-1">Dự kiến giao: {preview.shipping.eta || "Theo lịch GHN"}</p><p className="mt-1 text-xs">Báo giá có hiệu lực đến {new Date(preview.shipping.expiresAt).toLocaleString("vi-VN")}</p></div> : <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">Chưa có báo giá GHN. Hãy quay lại bước giao hàng.</div>}<div className="grid gap-3 sm:grid-cols-2">{choices.map((choice) => { const selected = method === choice.method; return <label className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition ${selected ? "border-rose-400 bg-rose-50 ring-1 ring-rose-100" : "border-slate-200 hover:border-rose-200"}`} key={choice.method}><input className="mt-1 accent-rose-600" type="radio" checked={selected} disabled={disabled} onChange={() => select(choice)} /><span><strong className="block text-sm">{choice.label}</strong><span className="mt-1 block text-xs leading-5 text-slate-500">{choice.description}</span></span></label>; })}</div><p className="rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-500">Khi xác nhận, hệ thống sẽ kiểm tra lại giá, tồn kho, ưu đãi và phí giao hàng để bảo đảm đơn chính xác.</p><div className="flex justify-between gap-3 border-t border-stone-200 pt-5"><Button variant="outline" disabled={disabled} onClick={back}><ChevronLeft />Quay lại</Button><Button size="lg" disabled={disabled || !preview || choices.length === 0} onClick={create}>{disabled ? "Đang xử lý…" : "Xác nhận đặt hàng"}<ChevronRight /></Button></div></div>;
}

function VoucherSelect({ label, values, value, change }: Readonly<{ label: string; values: VoucherWalletItem[]; value: string; change: (value: string) => void }>) {
  return <Select label={label} value={value} onChange={(event) => change(event.target.value)}><option value="">Không dùng mã</option>{values.map((item) => <option value={item.id} key={item.id}>{item.code} — {item.name}</option>)}</Select>;
}

function CheckoutSummary({ preview, session }: Readonly<{ preview: CheckoutPreview | null; session: CheckoutSession }>) {
  return <aside className="h-fit rounded-3xl border bg-white p-5 shadow-sm"><h2 className="font-black">Tóm tắt đơn hàng</h2><div className="mt-4 space-y-3 text-sm">{session.items.map((item) => <div className="flex items-center justify-between gap-3" key={item.id}><div className="flex min-w-0 items-center gap-3">{item.imageUrl ? <img alt={item.productName} className="size-11 shrink-0 rounded-lg object-cover" src={item.imageUrl} /> : <div className="grid size-11 shrink-0 place-items-center rounded-lg bg-slate-100 text-[10px] text-slate-400">Ảnh</div>}<span className="min-w-0"><strong className="block truncate">{item.productName}</strong><span className="text-xs text-slate-500">{item.variantName || item.sku} × {item.quantity}</span></span></div><strong className="shrink-0">{money(item.unitPriceVnd * item.quantity)}</strong></div>)}{preview ? <><div className="border-t pt-3" /><Row label="Giá niêm yết" value={money(preview.money.itemsListSubtotalVnd)} /><Row label="Giảm giá trực tiếp" value={`−${money(preview.money.directSaleDiscountVnd)}`} /><Row label="Mã giảm sản phẩm/đơn" value={`−${money(preview.money.productDiscountVnd + preview.money.orderDiscountVnd)}`} /><Row label={`GHN · ${preview.shipping.serviceName}`} value={money(preview.money.shippingFeeVnd)} /><Row label="Mã giảm phí giao hàng" value={`−${money(preview.money.shippingDiscountVnd)}`} /><div className="flex justify-between border-t pt-3 text-base font-bold"><span>Tổng cộng</span><span className="text-brand">{money(preview.money.finalTotalVnd)}</span></div><p className="text-xs text-slate-500">Báo giá hết hạn {new Date(preview.shipping.expiresAt).toLocaleString("vi-VN")}</p></> : <p className="border-t pt-3 text-xs text-slate-500">Tiền và phí giao hàng sẽ được máy chủ tính sau khi chọn địa chỉ.</p>}</div></aside>;
}

function Row({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="flex justify-between gap-3"><span className="text-slate-500">{label}</span><span>{value}</span></div>;
}

function money(value: number) {
  return `${value.toLocaleString("vi-VN")}đ`;
}