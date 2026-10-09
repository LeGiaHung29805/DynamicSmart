"use client";

import { AlertTriangle, ArrowRight, CheckCircle2, Minus, PackageSearch, Plus, ShieldCheck, ShoppingBag, Tag, TicketPercent, Trash2, Truck, X } from "lucide-react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useToast } from "@/components/ui/Toast";
import { customerApi } from "@/features/customer/api/customer.api";
import type { VoucherWalletItem } from "@/features/customer/types/customer.types";
import { useAuthSession } from "@/lib/auth/session";
import { cartApi } from "../api/cart.api";
import type { CartDto, CartItemDto } from "../types/cart.types";

export function CartPage() {
  const router = useRouter();
  const session = useAuthSession();
  const [cart, setCart] = useState<CartDto | null>(null); const [message, setMessage] = useState("");
  const [voucherPickerOpen, setVoucherPickerOpen] = useState(false);
  const [availableVouchers, setAvailableVouchers] = useState<VoucherWalletItem[]>([]);
  const [selectedVoucher, setSelectedVoucher] = useState<VoucherWalletItem | null>(null);
  const [voucherCode, setVoucherCode] = useState("");
  const [checkingVoucher, setCheckingVoucher] = useState(false);
  const [voucherLoading, setVoucherLoading] = useState(false);
  const [voucherError, setVoucherError] = useState("");
  const { showToast } = useToast();
  useEffect(() => {
    if (session.status !== "authenticated") return;
    cartApi.get().then((value) => { setCart(value); setMessage(""); }).catch(() => setMessage("Không thể tải giỏ hàng."));
  }, [session.status]);
  const update = async (item: CartItemDto, quantity: number, selected: boolean) => {
    try { setCart(await cartApi.update(item.id, quantity, selected, item.version)); setSelectedVoucher(null); setMessage(""); }
    catch { setMessage("Giỏ hàng vừa thay đổi hoặc dữ liệu không hợp lệ. Vui lòng tải lại."); }
  };
  const remove = async (id: string) => { if (!window.confirm("Xóa dòng hàng này?")) return; try { await cartApi.remove(id); setCart(await cartApi.get()); setSelectedVoucher(null); } catch { setMessage("Không thể xóa dòng hàng."); } };
  const selected = cart?.items.filter((item) => item.selected).length ?? 0;
  const selectedItems = cart?.items.filter((item) => item.selected) ?? [];
  const allSelected = Boolean(cart?.items.length && cart.items.every((item) => item.selected || !item.purchasable));
  const canCheckout = selectedItems.length > 0 && selectedItems.every((item) => item.purchasable);
  const subtotal = selectedItems.reduce((sum, item) => sum + item.salePriceVnd * item.quantity, 0);
  const estimatedTotal = Math.max(0, subtotal - (selectedVoucher?.discountAmountVnd ?? 0));
  const toggleAll = async () => {
    if (!cart) return;
    const target = !allSelected;
    try {
      setMessage("Đang cập nhật lựa chọn…");
      await Promise.all(cart.items.filter((item) => item.purchasable && item.selected !== target).map((item) => cartApi.update(item.id, item.quantity, target, item.version)));
      setCart(await cartApi.get()); setSelectedVoucher(null); setMessage("");
    } catch { setMessage("Giỏ hàng vừa thay đổi. Vui lòng tải lại trước khi chọn tất cả."); }
  };

  const openVoucherPicker = async () => {
    if (!subtotal || !selectedItems.length) {
      setMessage("Vui lòng chọn ít nhất một sản phẩm trước khi chọn voucher.");
      return;
    }
    setVoucherPickerOpen(true);
    setVoucherLoading(true);
    setVoucherError("");
    try {
      const wallet = await customerApi.vouchers();
      const productIds = [...new Set(selectedItems.map((item) => item.productId))];
      const previews = await Promise.all(wallet.filter((item) => item.scope !== "SHIPPING_DISCOUNT").map(async (item) => {
        try {
          return await customerApi.previewVoucher({ voucherId: item.id, orderSubtotalVnd: subtotal, eligibleSubtotalVnd: subtotal, shippingFeeVnd: 0, productIds, categoryIds: [] });
        } catch {
          return null;
        }
      }));
      setAvailableVouchers(previews.filter((item): item is VoucherWalletItem => Boolean(item?.eligible)));
    } catch {
      setVoucherError("Không thể tải danh sách voucher hợp lệ.");
    } finally {
      setVoucherLoading(false);
    }
  };

  const chooseVoucher = (voucher: VoucherWalletItem) => {
    setSelectedVoucher(voucher);
    setVoucherCode(voucher.code);
    setVoucherPickerOpen(false);
    setMessage("");
  };

  const applyVoucher = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!voucherCode.trim() || !subtotal) return;
    setCheckingVoucher(true);
    setMessage("");
    try {
      const productIds = [...new Set(selectedItems.map((item) => item.productId))];
      const preview = await customerApi.previewVoucher({ code: voucherCode.trim().toUpperCase(), orderSubtotalVnd: subtotal, eligibleSubtotalVnd: subtotal, shippingFeeVnd: 0, productIds, categoryIds: [] });
      setVoucherCode(preview.code);
      setSelectedVoucher(preview);
      showToast(`Đã áp dụng mã ${preview.code}.`, "success");
    } catch {
      setSelectedVoucher(null);
      setMessage("Mã giảm giá không hợp lệ hoặc chưa đủ điều kiện áp dụng.");
    } finally {
      setCheckingVoucher(false);
    }
  };
  return <div className="mx-auto w-full max-w-7xl space-y-7 px-4 py-10 sm:px-6 lg:px-8"><div className="flex flex-wrap items-end justify-between gap-4"><PageHeader eyebrow="Mua sắm" title="Giỏ hàng của bạn" description="Kiểm tra sản phẩm, số lượng và ưu đãi trước khi thanh toán." />{cart ? <span className="rounded-full bg-emerald-50 px-4 py-2 text-sm font-bold text-emerald-800">{cart.items.reduce((sum, item) => sum + item.quantity, 0)} sản phẩm</span> : null}</div>
    {message ? <div className={`rounded-xl border px-4 py-3 text-sm font-semibold ${message.startsWith("Không") || message.startsWith("Giỏ") ? "border-red-200 bg-red-50 text-red-700" : "border-stone-200 bg-white text-stone-500"}`}>{message}</div> : null}
    {session.status === "loading" ? <SurfacePanel><p className="py-8 text-center text-sm font-semibold text-slate-500">Đang khôi phục phiên đăng nhập…</p></SurfacePanel> : session.status === "anonymous" ? <SurfacePanel><div className="py-12 text-center"><ShoppingBag className="mx-auto size-12 text-emerald-700" /><h2 className="mt-4 text-xl font-black text-slate-900">Đăng nhập để xem giỏ hàng</h2><p className="mt-2 text-sm text-slate-500">Giỏ hàng được lưu riêng theo tài khoản của bạn.</p><Button className="mt-6 bg-emerald-800 text-white hover:bg-emerald-700" size="lg" onClick={() => router.push("/login?returnTo=%2Fcart")}>Đăng nhập</Button></div></SurfacePanel> : !cart ? <SurfacePanel><p className="py-8 text-center text-sm font-semibold text-slate-500">Đang tải giỏ hàng…</p></SurfacePanel> : cart.items.length === 0 ? <SurfacePanel><div className="py-12 text-center"><PackageSearch className="mx-auto size-12 text-slate-300" /><h2 className="mt-4 text-xl font-black text-slate-900">Giỏ hàng đang trống</h2><p className="mt-2 text-sm text-slate-500">Khám phá sản phẩm và thêm món đồ bạn yêu thích vào giỏ.</p><Button className="mt-6 bg-emerald-800 text-white hover:bg-emerald-700" size="lg" onClick={() => router.push("/products")}>Xem sản phẩm</Button></div></SurfacePanel> : <div className="grid items-start gap-7 lg:grid-cols-[minmax(0,1fr)_380px]">
      <div className="space-y-4">
        <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm"><label className="flex cursor-pointer items-center gap-3 text-sm font-bold text-slate-800"><input className="size-5 accent-emerald-700" type="checkbox" checked={allSelected} onChange={() => void toggleAll()} />Chọn tất cả sản phẩm có thể mua</label><span className="text-xs text-slate-500">{selected}/{cart.items.length} dòng đã chọn</span></div>
        {cart.items.map((item) => <article className={`group rounded-3xl border bg-white p-4 shadow-sm transition sm:p-5 ${item.selected ? "border-emerald-200" : "border-slate-200 opacity-70"}`} key={item.id}>
          <div className="grid grid-cols-[24px_88px_minmax(0,1fr)] gap-3 sm:grid-cols-[28px_128px_minmax(0,1fr)] sm:gap-5">
            <div className="pt-9 sm:pt-12"><input aria-label={`Chọn ${item.productName ?? item.variantId}`} className="size-5 accent-emerald-700 sm:size-6" type="checkbox" checked={item.selected} disabled={!item.purchasable} onChange={(event) => void update(item, item.quantity, event.target.checked)} /></div>
            <div className="relative aspect-square overflow-hidden rounded-2xl bg-slate-100">{item.imageUrl ? <Image alt={item.productName ?? "Sản phẩm trong giỏ"} className="object-cover transition duration-500 group-hover:scale-105" fill sizes="128px" src={item.imageUrl} unoptimized /> : <span className="grid size-full place-items-center bg-gradient-to-br from-emerald-50 to-slate-100 text-2xl font-black text-emerald-800">{(item.productName ?? "SP").slice(0, 2).toUpperCase()}</span>}</div>
            <div className="min-w-0"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><h2 className="truncate font-black text-slate-950">{item.productName ?? `Product ${item.productId}`}</h2><p className="mt-1 text-xs text-slate-500 sm:text-sm">{item.variantName ?? `Variant ${item.variantId}`}</p></div><p className="shrink-0 font-black text-emerald-800">{(item.salePriceVnd * item.quantity).toLocaleString("vi-VN")}đ</p></div>
              {!item.purchasable ? <div className="mt-3 flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm font-semibold text-amber-800"><AlertTriangle className="mt-0.5 size-4 shrink-0" />{item.unavailableReason ?? "Sản phẩm không thể mua."}</div> : null}
              <div className="mt-4 flex flex-wrap items-end justify-between gap-3 border-t border-stone-100 pt-4"><div><p className="text-xs text-stone-500">Đơn giá</p><div className="mt-1 flex items-center gap-2"><p className="text-sm font-black text-stone-900">{item.salePriceVnd.toLocaleString("vi-VN")}đ</p>{item.listPriceVnd > item.salePriceVnd ? <del className="text-xs text-stone-400">{item.listPriceVnd.toLocaleString("vi-VN")}đ</del> : null}</div>{item.purchasable ? <p className="mt-2 text-xs font-semibold text-emerald-600">Còn {item.availableQuantity} sản phẩm</p> : null}</div>
                <div className="flex items-center gap-2"><div className="flex overflow-hidden rounded-xl border border-stone-300"><Button aria-label="Giảm số lượng" className="rounded-none" size="icon-lg" variant="ghost" disabled={item.quantity <= 1} onClick={() => void update(item, item.quantity - 1, item.selected)}><Minus /></Button><span className="grid w-11 place-items-center border-x border-stone-200 text-sm font-black">{item.quantity}</span><Button aria-label="Tăng số lượng" className="rounded-none" size="icon-lg" variant="ghost" disabled={!item.purchasable || item.quantity >= Math.min(99, item.availableQuantity)} onClick={() => void update(item, item.quantity + 1, item.selected)}><Plus /></Button></div><Button aria-label="Xóa khỏi giỏ" size="icon-lg" variant="destructive" onClick={() => void remove(item.id)}><Trash2 /></Button></div>
              </div>
            </div>
          </div>
        </article>)}
      </div>
      <aside className="space-y-4 lg:sticky lg:top-24">
        <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <div className="flex items-center gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-amber-50 text-amber-700"><Tag className="size-5" /></span><div><h2 className="font-black text-slate-950">Mã giảm giá</h2><p className="text-xs text-slate-500">Nhập mã hoặc chọn từ kho voucher</p></div></div>
          <form className="mt-5 flex gap-2" onSubmit={applyVoucher}>
            <Input aria-label="Mã giảm giá" className="min-w-0 flex-1 uppercase" placeholder="Nhập mã voucher" value={voucherCode} onChange={(event) => setVoucherCode(event.target.value)} />
            <Button className="shrink-0 bg-slate-950 px-4 text-white hover:bg-emerald-800" disabled={checkingVoucher || !voucherCode.trim() || !canCheckout} type="submit">{checkingVoucher ? "Đang kiểm tra…" : "Áp dụng"}</Button>
          </form>
          {selectedVoucher ? <div className="mt-3 flex items-center justify-between gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-3.5 py-3"><div className="min-w-0"><p className="truncate text-sm font-black text-emerald-800">{selectedVoucher.code}</p><p className="truncate text-xs text-slate-500">{selectedVoucher.name}</p></div><CheckCircle2 className="size-5 shrink-0 text-emerald-600" /></div> : null}
          <button className="mt-4 inline-flex items-center gap-2 text-sm font-black text-emerald-800 transition hover:text-emerald-600" type="button" onClick={() => void openVoucherPicker()}><TicketPercent className="size-4" />{selectedVoucher ? "Đổi voucher" : "Chọn voucher hợp lệ"}<ArrowRight className="size-4" /></button>
        </section>
        <section className="overflow-hidden rounded-3xl bg-slate-950 text-white shadow-xl shadow-slate-300/40">
          <div className="border-b border-white/10 p-5 sm:p-6"><div className="flex items-center gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-emerald-500/15 text-emerald-300"><ShoppingBag className="size-5" /></span><div><h2 className="font-black">Tóm tắt đơn hàng</h2><p className="text-xs text-slate-400">{selected} dòng sản phẩm đã chọn</p></div></div></div>
          <div className="space-y-3 p-5 text-sm sm:p-6"><div className="flex justify-between text-slate-300"><span>Tạm tính</span><strong className="text-white">{subtotal.toLocaleString("vi-VN")}đ</strong></div><div className="flex justify-between text-slate-300"><span>Số lượng</span><strong className="text-white">{selectedItems.reduce((sum, item) => sum + item.quantity, 0)}</strong></div>{selectedVoucher?.discountAmountVnd ? <div className="flex justify-between text-emerald-300"><span>Voucher {selectedVoucher.code}</span><strong>-{selectedVoucher.discountAmountVnd.toLocaleString("vi-VN")}đ</strong></div> : null}<div className="flex items-end justify-between border-t border-white/10 pt-4"><span className="font-bold text-slate-200">Tổng dự kiến</span><strong className="text-2xl font-black text-emerald-300">{estimatedTotal.toLocaleString("vi-VN")}đ</strong></div>
            <Button className="mt-3 w-full bg-emerald-500 text-slate-950 hover:bg-emerald-400" size="lg" disabled={!canCheckout} onClick={() => router.push(selectedVoucher ? `/checkout?voucherId=${encodeURIComponent(selectedVoucher.id)}` : "/checkout")}>Tiến hành thanh toán <ArrowRight /></Button>
            <div className="grid gap-2 pt-2 text-xs leading-5 text-slate-400"><p className="flex gap-2"><ShieldCheck className="mt-0.5 size-4 shrink-0 text-emerald-400" />Giá và voucher được máy chủ kiểm tra lại trước khi tạo đơn.</p><p className="flex gap-2"><Truck className="mt-0.5 size-4 shrink-0 text-emerald-400" />Phí vận chuyển được tính ở bước thanh toán.</p></div>
          </div>
        </section>
      </aside>
    </div>}
    {voucherPickerOpen ? <div className="fixed inset-0 z-[70] flex items-end justify-center bg-stone-950/55 backdrop-blur-sm sm:items-center sm:p-5" role="dialog" aria-modal="true" aria-label="Chọn voucher hợp lệ">
      <button aria-label="Đóng danh sách voucher" className="absolute inset-0" type="button" onClick={() => setVoucherPickerOpen(false)} />
      <section className="relative z-10 flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-t-3xl bg-white shadow-2xl sm:rounded-3xl">
        <header className="flex items-start justify-between gap-4 border-b border-stone-100 px-5 py-5 sm:px-6"><div><p className="text-xs font-black tracking-[0.16em] text-emerald-700 uppercase">Ưu đãi dành cho bạn</p><h2 className="mt-1 text-xl font-black text-stone-950">Chọn voucher hợp lệ</h2><p className="mt-1 text-sm text-stone-500">Danh sách đã được kiểm tra theo sản phẩm và tổng tiền hiện tại.</p></div><Button aria-label="Đóng" size="icon" variant="ghost" onClick={() => setVoucherPickerOpen(false)}><X /></Button></header>
        <div className="overflow-y-auto p-5 sm:p-6">{voucherLoading ? <p className="py-10 text-center text-sm font-semibold text-stone-500">Đang kiểm tra voucher…</p> : voucherError ? <p className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">{voucherError}</p> : availableVouchers.length === 0 ? <div className="rounded-2xl border border-dashed border-stone-200 bg-stone-50 px-6 py-10 text-center"><TicketPercent className="mx-auto size-10 text-stone-300" /><p className="mt-3 font-black text-stone-800">Chưa có voucher phù hợp</p><p className="mt-1 text-sm text-stone-500">Hãy chọn thêm sản phẩm hoặc tăng giá trị đơn hàng.</p></div> : <div className="grid gap-3 sm:grid-cols-2">{availableVouchers.map((item) => <button className={`group rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 hover:border-emerald-300 hover:shadow-md ${selectedVoucher?.id === item.id ? "border-emerald-400 bg-emerald-50" : "border-stone-200"}`} key={item.id} type="button" onClick={() => chooseVoucher(item)}><div className="flex items-start justify-between gap-3"><span className="grid size-10 place-items-center rounded-xl bg-emerald-100 text-emerald-800"><TicketPercent className="size-5" /></span><span className="rounded-full bg-amber-50 px-2.5 py-1 text-[10px] font-black text-amber-700 uppercase">Hợp lệ</span></div><p className="mt-4 text-lg font-black text-emerald-800">{item.code}</p><p className="mt-1 font-bold text-stone-900">{item.name}</p><p className="mt-1 line-clamp-2 text-xs leading-5 text-stone-500">{item.description}</p>{item.discountAmountVnd ? <p className="mt-2 text-sm font-black text-rose-600">Giảm {item.discountAmountVnd.toLocaleString("vi-VN")}đ</p> : null}<span className="mt-4 inline-flex items-center gap-1 text-xs font-black text-emerald-700">Chọn mã này <ArrowRight className="size-3.5 transition group-hover:translate-x-0.5" /></span></button>)}</div>}</div>
      </section>
    </div> : null}
  </div>;
}
