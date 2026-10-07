"use client";

import { AlertTriangle, Minus, PackageSearch, Plus, ShoppingCart, Trash2 } from "lucide-react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { cartApi } from "../api/cart.api";
import type { CartDto, CartItemDto } from "../types/cart.types";

export function CartPage() {
  const router = useRouter();
  const [cart, setCart] = useState<CartDto | null>(null); const [message, setMessage] = useState("Đang tải giỏ hàng…");
  useEffect(() => { cartApi.get().then((value) => { setCart(value); setMessage(""); }).catch(() => setMessage("Không thể tải giỏ hàng.")); }, []);
  const update = async (item: CartItemDto, quantity: number, selected: boolean) => {
    try { setCart(await cartApi.update(item.id, quantity, selected, item.version)); setMessage(""); }
    catch { setMessage("Giỏ hàng vừa thay đổi hoặc dữ liệu không hợp lệ. Vui lòng tải lại."); }
  };
  const remove = async (id: string) => { if (!window.confirm("Xóa dòng hàng này?")) return; try { await cartApi.remove(id); setCart(await cartApi.get()); } catch { setMessage("Không thể xóa dòng hàng."); } };
  const selected = cart?.items.filter((item) => item.selected).length ?? 0;
  const selectedItems = cart?.items.filter((item) => item.selected) ?? [];
  const allSelected = Boolean(cart?.items.length && cart.items.every((item) => item.selected || !item.purchasable));
  const canCheckout = selectedItems.length > 0 && selectedItems.every((item) => item.purchasable);
  const subtotal = selectedItems.reduce((sum, item) => sum + item.salePriceVnd * item.quantity, 0);
  const toggleAll = async () => {
    if (!cart) return;
    const target = !allSelected;
    try {
      setMessage("Đang cập nhật lựa chọn…");
      await Promise.all(cart.items.filter((item) => item.purchasable && item.selected !== target).map((item) => cartApi.update(item.id, item.quantity, target, item.version)));
      setCart(await cartApi.get()); setMessage("");
    } catch { setMessage("Giỏ hàng vừa thay đổi. Vui lòng tải lại trước khi chọn tất cả."); }
  };
  return <div className="mx-auto w-full max-w-7xl space-y-7 px-4 py-8 sm:px-6 lg:px-8 lg:py-12"><PageHeader eyebrow="Mua sắm" title={`Giỏ hàng${cart ? ` (${cart.items.reduce((sum, item) => sum + item.quantity, 0)})` : ""}`} description="Chọn đúng sản phẩm cần mua; giá, trạng thái bán và tồn kho luôn được máy chủ kiểm tra lại." />
    {message ? <div className={`rounded-xl border px-4 py-3 text-sm font-semibold ${message.startsWith("Không") || message.startsWith("Giỏ") ? "border-red-200 bg-red-50 text-red-700" : "border-stone-200 bg-white text-stone-500"}`}>{message}</div> : null}
    {!cart || cart.items.length === 0 ? <SurfacePanel><div className="py-12 text-center"><PackageSearch className="mx-auto size-12 text-stone-300" /><h2 className="mt-4 text-xl font-black text-stone-900">Giỏ hàng đang trống</h2><p className="mt-2 text-sm text-stone-500">Hãy chọn sản phẩm và phiên bản phù hợp để bắt đầu đơn hàng.</p><Button className="mt-6" size="lg" onClick={() => router.push("/products")}>Xem sản phẩm</Button></div></SurfacePanel> : <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
      <div className="space-y-4">
        <div className="flex items-center justify-between rounded-2xl border border-stone-200 bg-white px-5 py-4 shadow-sm"><label className="flex cursor-pointer items-center gap-3 text-sm font-bold text-stone-800"><input className="size-5 accent-rose-600" type="checkbox" checked={allSelected} onChange={() => void toggleAll()} />Chọn tất cả sản phẩm có thể mua</label><span className="text-xs text-stone-500">{selected}/{cart.items.length} dòng đã chọn</span></div>
        {cart.items.map((item) => <article className={`rounded-2xl border bg-white p-4 shadow-sm transition sm:p-5 ${item.selected ? "border-rose-200 ring-1 ring-rose-100" : "border-stone-200"}`} key={item.id}>
          <div className="grid grid-cols-[24px_88px_minmax(0,1fr)] gap-3 sm:grid-cols-[28px_128px_minmax(0,1fr)] sm:gap-5">
            <div className="pt-9 sm:pt-12"><input aria-label={`Chọn ${item.productName ?? item.variantId}`} className="size-5 accent-rose-600 sm:size-6" type="checkbox" checked={item.selected} disabled={!item.purchasable} onChange={(event) => void update(item, item.quantity, event.target.checked)} /></div>
            <div className="relative aspect-square overflow-hidden rounded-xl bg-stone-100">{item.imageUrl ? <Image alt={item.productName ?? "Sản phẩm trong giỏ"} className="object-cover" fill sizes="128px" src={item.imageUrl} unoptimized /> : <span className="grid size-full place-items-center text-2xl font-black text-stone-300">{(item.productName ?? "SP").slice(0, 2).toUpperCase()}</span>}</div>
            <div className="min-w-0"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><h2 className="truncate font-black text-stone-950">{item.productName ?? `Product ${item.productId}`}</h2><p className="mt-1 text-xs text-stone-500 sm:text-sm">{item.variantName ?? `Variant ${item.variantId}`}</p></div><p className="shrink-0 font-black text-rose-600">{(item.salePriceVnd * item.quantity).toLocaleString("vi-VN")}đ</p></div>
              {!item.purchasable ? <div className="mt-3 flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm font-semibold text-amber-800"><AlertTriangle className="mt-0.5 size-4 shrink-0" />{item.unavailableReason ?? "Sản phẩm không thể mua."}</div> : null}
              <div className="mt-4 flex flex-wrap items-end justify-between gap-3 border-t border-stone-100 pt-4"><div><p className="text-xs text-stone-500">Đơn giá</p><div className="mt-1 flex items-center gap-2"><p className="text-sm font-black text-stone-900">{item.salePriceVnd.toLocaleString("vi-VN")}đ</p>{item.listPriceVnd > item.salePriceVnd ? <del className="text-xs text-stone-400">{item.listPriceVnd.toLocaleString("vi-VN")}đ</del> : null}</div>{item.purchasable ? <p className="mt-2 text-xs font-semibold text-emerald-600">Còn {item.availableQuantity} sản phẩm</p> : null}</div>
                <div className="flex items-center gap-2"><div className="flex overflow-hidden rounded-xl border border-stone-300"><Button aria-label="Giảm số lượng" className="rounded-none" size="icon-lg" variant="ghost" disabled={item.quantity <= 1} onClick={() => void update(item, item.quantity - 1, item.selected)}><Minus /></Button><span className="grid w-11 place-items-center border-x border-stone-200 text-sm font-black">{item.quantity}</span><Button aria-label="Tăng số lượng" className="rounded-none" size="icon-lg" variant="ghost" disabled={!item.purchasable || item.quantity >= Math.min(99, item.availableQuantity)} onClick={() => void update(item, item.quantity + 1, item.selected)}><Plus /></Button></div><Button aria-label="Xóa khỏi giỏ" size="icon-lg" variant="destructive" onClick={() => void remove(item.id)}><Trash2 /></Button></div>
              </div>
            </div>
          </div>
        </article>)}
      </div>
      <SurfacePanel className="h-fit lg:sticky lg:top-24"><div className="flex items-center gap-3"><span className="rounded-xl bg-rose-50 p-2.5 text-rose-600"><ShoppingCart /></span><div><h2 className="text-lg font-black text-stone-950">Tóm tắt giỏ hàng</h2><p className="text-xs text-stone-500">Chỉ tính những dòng đã chọn</p></div></div><div className="mt-6 space-y-3 text-sm"><div className="flex justify-between"><span className="text-stone-500">Dòng hàng đã chọn</span><strong>{selected}</strong></div><div className="flex justify-between"><span className="text-stone-500">Số lượng sản phẩm</span><strong>{selectedItems.reduce((sum, item) => sum + item.quantity, 0)}</strong></div><div className="flex justify-between border-t border-stone-200 pt-4 text-base"><span className="font-bold">Tạm tính</span><strong className="text-xl text-rose-600">{subtotal.toLocaleString("vi-VN")}đ</strong></div></div><Button className="mt-6 w-full" size="lg" disabled={!canCheckout} onClick={() => router.push("/checkout")}>Mua hàng ({selected})</Button><p className="mt-4 text-xs leading-5 text-stone-500">Giá và tồn kho được tải lại từ Catalog. Checkout sẽ kiểm tra lần cuối trước khi tạo đơn.</p></SurfacePanel>
    </div>}
  </div>;
}
