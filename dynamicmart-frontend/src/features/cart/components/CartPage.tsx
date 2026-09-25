"use client";

import { Minus, Plus, ShoppingCart, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { cartApi } from "../api/cart.api";
import type { CartDto, CartItemDto } from "../types/cart.types";

export function CartPage() {
  const [cart, setCart] = useState<CartDto | null>(null); const [message, setMessage] = useState("Đang tải giỏ hàng…");
  useEffect(() => { cartApi.get().then((value) => { setCart(value); setMessage(""); }).catch(() => setMessage("Không thể tải giỏ hàng.")); }, []);
  const update = async (item: CartItemDto, quantity: number, selected: boolean) => {
    try { setCart(await cartApi.update(item.id, quantity, selected, item.version)); setMessage(""); }
    catch { setMessage("Giỏ hàng vừa thay đổi hoặc dữ liệu không hợp lệ. Vui lòng tải lại."); }
  };
  const remove = async (id: string) => { if (!window.confirm("Xóa dòng hàng này?")) return; try { await cartApi.remove(id); setCart(await cartApi.get()); } catch { setMessage("Không thể xóa dòng hàng."); } };
  const selected = cart?.items.filter((item) => item.selected).length ?? 0;
  return <div className="mx-auto w-full max-w-6xl space-y-7 px-4 py-8 sm:px-6"><PageHeader eyebrow="Mua sắm" title="Giỏ hàng" description="Giỏ chỉ giữ Product/Variant, số lượng và lựa chọn. Giá, trạng thái bán và tồn kho được Catalog/Checkout kiểm tra lại ở máy chủ." />
    {message ? <SurfacePanel className={message.startsWith("Không") || message.startsWith("Giỏ") ? "text-red-600" : ""}>{message}</SurfacePanel> : null}
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]"><SurfacePanel>{cart && cart.items.length > 0 ? <div className="space-y-4">{cart.items.map((item) => <article className="grid gap-4 border-b border-slate-100 pb-4 sm:grid-cols-[auto_1fr_auto]" key={item.id}>
      <input aria-label={`Chọn Variant ${item.variantId}`} type="checkbox" checked={item.selected} onChange={(event) => void update(item, item.quantity, event.target.checked)} />
      <div><h2 className="font-bold text-slate-950">Variant {item.variantId}</h2><p className="mt-1 text-xs text-slate-500">Product {item.productId}</p><p className="mt-3 text-sm font-medium text-amber-700">Giá và tồn kho sẽ được kiểm tra ở bước xác nhận đơn.</p></div>
      <div className="flex items-center gap-2 sm:flex-col sm:items-end"><div className="flex items-center rounded-lg border"><Button aria-label="Giảm số lượng" size="icon-sm" variant="ghost" disabled={item.quantity <= 1} onClick={() => void update(item, item.quantity - 1, item.selected)}><Minus /></Button><span className="w-8 text-center text-sm font-bold">{item.quantity}</span><Button aria-label="Tăng số lượng" size="icon-sm" variant="ghost" disabled={item.quantity >= 99} onClick={() => void update(item, item.quantity + 1, item.selected)}><Plus /></Button></div><Button aria-label="Xóa khỏi giỏ" size="icon-sm" variant="destructive" onClick={() => void remove(item.id)}><Trash2 /></Button></div>
    </article>)}</div> : <p>Giỏ hàng đang trống.</p>}</SurfacePanel><SurfacePanel className="h-fit lg:sticky lg:top-6"><div className="flex items-center gap-2"><ShoppingCart className="text-brand" /><h2 className="text-lg font-black">Tóm tắt giỏ hàng</h2></div><div className="mt-5 flex justify-between text-sm"><span>Dòng hàng đã chọn</span><strong>{selected}</strong></div><Button className="mt-5 w-full" size="lg" disabled={selected === 0}>Tiếp tục đặt hàng</Button><p className="mt-3 text-xs leading-5 text-slate-500">Không hiển thị tổng tiền giả. Checkout sẽ trả breakdown authoritative gồm direct sale, voucher và phí giao hàng.</p></SurfacePanel></div>
  </div>;
}
