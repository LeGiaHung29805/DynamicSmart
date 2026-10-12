"use client";

import { BadgePercent, CalendarClock, Plus, TicketPercent } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { listAdminCategories, listAdminProducts } from "@/features/catalog/api/admin-catalog.api";
import type { AdminProduct, CategoryAdmin } from "@/features/catalog/types";
import { useAuthSession } from "@/lib/auth/session";
import { promotionApi, type VoucherDto, type VoucherReservationDto } from "../api/promotion.api";

type Row = { id: string; name: string; code: string; status: string; value: string; scope: string; startsAt: string; endsAt: string };

const promotionScopeLabel = (value: string) => ({ ORDER_DISCOUNT: "Giảm tiền hàng", PRODUCT_DISCOUNT: "Giảm tiền hàng", PRODUCT_LIST_DISCOUNT: "Giảm tiền hàng", CATEGORY_DISCOUNT: "Giảm tiền hàng", SHIPPING_DISCOUNT: "Giảm phí vận chuyển" } as Record<string, string>)[value] ?? "Giảm tiền hàng";

export function PromotionAdminPage() {
  const session = useAuthSession();
  const [rows, setRows] = useState<Row[]>([]); const [showForm, setShowForm] = useState(false); const [voucherTarget, setVoucherTarget] = useState<"MERCHANDISE" | "SHIPPING">("MERCHANDISE"); const [method, setMethod] = useState("PERCENTAGE"); const [message, setMessage] = useState("Đang tải…");
  const [products, setProducts] = useState<AdminProduct[]>([]); const [categories, setCategories] = useState<CategoryAdmin[]>([]); const [selectedProductIds, setSelectedProductIds] = useState<string[]>([]); const [selectedCategoryIds, setSelectedCategoryIds] = useState<string[]>([]);
  const [reservations, setReservations] = useState<VoucherReservationDto[]>([]);
  const [voucherRules, setVoucherRules] = useState<VoucherDto[]>([]); const [editingVoucher, setEditingVoucher] = useState<VoucherDto | null>(null);
  const load = async () => {
    try {
      const [vouchers, history] = await Promise.all([promotionApi.vouchers(), promotionApi.reservationHistory()]);
      setVoucherRules(vouchers); setRows(vouchers.map(toVoucherRow)); setReservations(history); setMessage("");
    } catch { setMessage("Không thể tải danh sách voucher."); }
  };
  useEffect(() => {
    if (session.status === "loading") return;
    if (session.status !== "authenticated" || session.user.role !== "ADMIN") return;
    Promise.all([promotionApi.vouchers(), promotionApi.reservationHistory()]).then(([vouchers, history]) => { setVoucherRules(vouchers); setRows(vouchers.map(toVoucherRow)); setReservations(history); setMessage(""); }).catch(() => setMessage("Không thể tải danh sách voucher."));
    Promise.all([listAdminProducts({ size: 100 }), listAdminCategories()]).then(([productPage, categoryList]) => { setProducts(productPage.content); setCategories(categoryList); }).catch(() => setMessage("Không thể tải danh mục và sản phẩm. Hãy kiểm tra Catalog Service."));
  }, [session]);
  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault(); const data = new FormData(event.currentTarget); const value = Number(data.get("value"));
    const common = { name: String(data.get("name")), description: String(data.get("description") ?? ""), discountMethod: method,
      fixedDiscountVnd: method === "FIXED_AMOUNT" ? value : undefined, discountRateBps: method === "PERCENTAGE" ? Math.round(value * 100) : undefined,
      maxDiscountVnd: data.get("maxDiscount") ? Number(data.get("maxDiscount")) : undefined,
      startsAt: new Date(String(data.get("startsAt"))).toISOString(), endsAt: new Date(String(data.get("endsAt"))).toISOString(), reason: editingVoucher ? "Cập nhật voucher từ trang quản trị" : "Tạo voucher từ trang quản trị" };
    try {
      const distributionMode = "CODE_ONLY";
      const restrictedProductIds = products.filter((product) => selectedProductIds.includes(product.id) || selectedCategoryIds.includes(product.category.id)).map((product) => product.id);
      const hasProductRestriction = voucherTarget === "MERCHANDISE" && restrictedProductIds.length > 0;
      const scope = voucherTarget === "SHIPPING" ? "SHIPPING_DISCOUNT" : hasProductRestriction ? "PRODUCT_LIST_DISCOUNT" : "ORDER_DISCOUNT";
      const voucherBody = { ...common, code: String(data.get("code")), scope, minimumOrderVnd: data.get("minimumOrder") ? Number(data.get("minimumOrder")) : undefined,
        usageLimit: data.get("usageLimit") ? Number(data.get("usageLimit")) : undefined,
        usageLimitPerCustomer: data.get("perCustomer") ? Number(data.get("perCustomer")) : undefined, distributionMode, defaultVoucher: false,
        productIds: hasProductRestriction ? restrictedProductIds : [], categoryIds: [] };
      if (editingVoucher) await promotionApi.updateVoucher(editingVoucher.id, voucherBody); else await promotionApi.createVoucher(voucherBody);
      setShowForm(false); setEditingVoucher(null); setSelectedProductIds([]); setSelectedCategoryIds([]); await load();
    } catch { setMessage("Không thể lưu voucher. Hãy kiểm tra mã, thời gian và điều kiện giảm giá."); }
  };
  const toggle = async (row: Row) => {
    const status = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    try { await promotionApi.setVoucherStatus(row.id, status, "Thay đổi trạng thái từ trang quản trị"); await load(); }
    catch { setMessage("Không thể đổi trạng thái; voucher có thể đã hết hạn."); }
  };
  const remove = async (row: Row) => {
    if (!window.confirm(`Xóa voucher ${row.code}?`)) return;
    try { await promotionApi.deleteVoucher(row.id); if (editingVoucher?.id === row.id) { setEditingVoucher(null); setShowForm(false); } await load(); }
    catch { setMessage("Không thể xóa Voucher đã được cấp hoặc đã có lịch sử sử dụng."); }
  };
  const edit = async (row: Row) => {
    const value = voucherRules.find((item) => item.id === row.id); if (!value) return;
    setEditingVoucher(value); setMethod(value.discountMethod); setVoucherTarget(value.scope === "SHIPPING_DISCOUNT" ? "SHIPPING" : "MERCHANDISE"); setSelectedProductIds(value.productIds ?? []); setSelectedCategoryIds(value.categoryIds ?? []); setShowForm(true); window.scrollTo({ top: 0, behavior: "smooth" });
  };
  const displayedMessage = session.status !== "loading" && (session.status !== "authenticated" || session.user.role !== "ADMIN")
    ? "Vui lòng đăng nhập bằng tài khoản Admin để quản lý khuyến mãi."
    : message;
  return <div className="space-y-7"><PageHeader eyebrow="Khuyến mãi" title="Voucher" action={<Button onClick={() => { setEditingVoucher(null); setVoucherTarget("MERCHANDISE"); setMethod("PERCENTAGE"); setSelectedProductIds([]); setSelectedCategoryIds([]); setShowForm(true); }}><Plus />Tạo voucher</Button>} />
    <div className="grid gap-4 sm:grid-cols-3">
      <PromotionSummary icon={TicketPercent} label="Tổng voucher" value={rows.length} />
      <PromotionSummary icon={BadgePercent} label="Đang hoạt động" value={rows.filter((row) => row.status === "ACTIVE").length} />
      <PromotionSummary icon={CalendarClock} label="Lượt giữ voucher" value={reservations.length} />
    </div>
    {displayedMessage ? <SurfacePanel className={displayedMessage.startsWith("Không") || displayedMessage.startsWith("Vui lòng") ? "text-red-600" : ""}>{displayedMessage}</SurfacePanel> : null}
    {showForm ? <SurfacePanel className="border-rose-200"><div className="mb-5 border-b border-stone-100 pb-4"><p className="text-xs font-black uppercase tracking-[0.18em] text-rose-600">Thiết lập voucher</p><h2 className="mt-1 text-xl font-black text-stone-900">{editingVoucher ? "Sửa voucher" : "Tạo voucher mới"}</h2></div><form className="grid gap-4 md:grid-cols-2" key={editingVoucher?.id ?? "new-voucher"} onSubmit={(event) => void submit(event)}>
      <Input defaultValue={editingVoucher?.name ?? ""} name="name" label="Tên voucher" required /><Input defaultValue={editingVoucher?.code ?? ""} name="code" label="Mã voucher" readOnly={Boolean(editingVoucher)} required />
      <Select label="Giảm cho" value={voucherTarget} onChange={(event) => { setVoucherTarget(event.target.value as typeof voucherTarget); setSelectedCategoryIds([]); setSelectedProductIds([]); }}><option value="MERCHANDISE">Sản phẩm</option><option value="SHIPPING">Phí vận chuyển</option></Select>
      <Select label="Cách giảm" value={method} onChange={(event) => setMethod(event.target.value)}><option value="PERCENTAGE">Phần trăm</option><option value="FIXED_AMOUNT">Số tiền cố định</option></Select><Input defaultValue={editingVoucher ? (method === "PERCENTAGE" ? (editingVoucher.discountRateBps ?? 0) / 100 : editingVoucher.fixedDiscountVnd ?? 0) : ""} key={method} name="value" label={method === "PERCENTAGE" ? "Phần trăm giảm" : "Số tiền giảm (VND)"} type="number" min="1" max={method === "PERCENTAGE" ? 100 : undefined} required />
      <Input defaultValue={editingVoucher?.maxDiscountVnd ?? ""} name="maxDiscount" label="Mức giảm tối đa (tùy chọn)" type="number" min="0" />
      {voucherTarget === "MERCHANDISE" ? <CatalogScopePicker categories={categories} products={products} selectedCategoryIds={selectedCategoryIds} selectedProductIds={selectedProductIds} setSelectedCategoryIds={setSelectedCategoryIds} setSelectedProductIds={setSelectedProductIds} /> : null}
      <Input defaultValue={editingVoucher?.minimumOrderVnd ?? ""} name="minimumOrder" label="Tổng đơn tối thiểu" type="number" min="0" /><Input defaultValue={editingVoucher?.usageLimit ?? ""} name="usageLimit" label="Tổng lượt dùng" type="number" min="1" /><Input defaultValue={editingVoucher?.usageLimitPerCustomer ?? ""} name="perCustomer" label="Lượt dùng mỗi khách" type="number" min="1" />
      <Input defaultValue={editingVoucher ? toDateTimeLocal(editingVoucher.startsAt) : ""} name="startsAt" label="Bắt đầu" type="datetime-local" required /><Input defaultValue={editingVoucher ? toDateTimeLocal(editingVoucher.endsAt) : ""} name="endsAt" label="Kết thúc" type="datetime-local" required /><Input defaultValue={editingVoucher?.description ?? ""} name="description" label="Mô tả" />
      <div className="flex gap-2 md:col-span-2"><Button type="submit">Lưu</Button><Button type="button" variant="outline" onClick={() => { setShowForm(false); setEditingVoucher(null); }}>Hủy</Button></div>
    </form></SurfacePanel> : null}
    <SurfacePanel className="overflow-hidden p-0"><div className="flex items-center gap-3 border-b border-stone-200 bg-stone-50/80 px-5 py-4"><span className="grid h-10 w-10 place-items-center rounded-xl bg-rose-100 text-rose-700"><TicketPercent className="h-5 w-5" /></span><div><h2 className="text-lg font-black text-stone-900">Danh sách voucher</h2></div></div>{rows.length === 0 && !message ? <p className="px-5 py-14 text-center text-sm text-stone-500">Chưa có voucher.</p> : <div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-white text-xs uppercase tracking-wide text-stone-500"><tr><th className="px-5 py-4">Voucher</th><th className="px-5 py-4">Loại</th><th className="px-5 py-4">Giá trị</th><th className="px-5 py-4">Thời gian</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4" /></tr></thead><tbody>{rows.map((row) => <tr className="border-t border-stone-100 transition hover:bg-rose-50/30" key={row.id}><td className="px-5 py-4"><strong className="block text-stone-900">{row.name}</strong><code className="mt-1 inline-block rounded bg-rose-50 px-2 py-1 text-xs font-bold text-rose-700">{row.code}</code></td><td className="px-5 py-4 font-semibold text-stone-700">{row.scope}</td><td className="px-5 py-4 font-black text-rose-600">{row.value}</td><td className="px-5 py-4 text-stone-500">{new Date(row.startsAt).toLocaleDateString("vi-VN")} – {new Date(row.endsAt).toLocaleDateString("vi-VN")}</td><td className="px-5 py-4"><StatusBadge label={promotionStatusLabel(row.status)} tone={row.status === "ACTIVE" ? "success" : "neutral"} /></td><td className="px-5 py-4"><div className="flex min-w-64 flex-wrap gap-2"><Button variant="outline" onClick={() => void toggle(row)}>{row.status === "ACTIVE" ? "Tạm dừng" : "Kích hoạt"}</Button><Button variant="outline" onClick={() => void edit(row)}>Sửa</Button><Button variant="destructive" onClick={() => void remove(row)}>Xóa</Button></div></td></tr>)}</tbody></table></div>}</SurfacePanel>
    <SurfacePanel className="overflow-hidden p-0"><div className="flex items-center gap-2 border-b border-stone-200 bg-stone-50/80 px-5 py-4"><CalendarClock className="h-5 w-5 text-rose-600" /><h2 className="font-black">Lịch sử giữ, dùng và trả voucher</h2></div>{reservations.length === 0 ? <p className="px-5 py-10 text-center text-sm text-stone-500">Chưa có giao dịch voucher.</p> : <div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="text-xs uppercase tracking-wide text-stone-500"><tr><th className="px-5 py-4">Voucher</th><th className="px-5 py-4">Khách hàng</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4">Giảm hàng</th><th className="px-5 py-4">Giảm vận chuyển</th><th className="px-5 py-4">Giữ đến</th></tr></thead><tbody>{reservations.map((item) => <tr className="border-t border-stone-100" key={item.id}><td className="px-5 py-4"><code className="text-xs">{item.voucherId}</code></td><td className="px-5 py-4"><code className="text-xs">{item.customerId}</code></td><td className="px-5 py-4"><StatusBadge label={promotionStatusLabel(item.status)} tone={item.status === "CONSUMED" ? "success" : item.status === "RESERVED" ? "warning" : "neutral"} /></td><td className="px-5 py-4 font-semibold">{item.discountAmountVnd.toLocaleString("vi-VN")}đ</td><td className="px-5 py-4 font-semibold">{item.shippingDiscountVnd.toLocaleString("vi-VN")}đ</td><td className="px-5 py-4 text-stone-500">{new Date(item.reservedUntil).toLocaleString("vi-VN")}</td></tr>)}</tbody></table></div>}</SurfacePanel>
  </div>;
}

function CatalogScopePicker({ categories, products, selectedCategoryIds, selectedProductIds, setSelectedCategoryIds, setSelectedProductIds }: Readonly<{ categories: CategoryAdmin[]; products: AdminProduct[]; selectedCategoryIds: string[]; selectedProductIds: string[]; setSelectedCategoryIds: (ids: string[]) => void; setSelectedProductIds: (ids: string[]) => void }>) {
  const appliesToAll = selectedCategoryIds.length === 0 && selectedProductIds.length === 0;
  return <fieldset className="rounded-2xl border border-stone-200 bg-stone-50/70 p-4 md:col-span-2">
    <legend className="px-2 text-sm font-bold text-stone-800">Phạm vi sản phẩm</legend>
    <div className="grid gap-5 md:grid-cols-2"><CategoryPicker categories={categories} selectedIds={selectedCategoryIds} setSelectedIds={setSelectedCategoryIds} /><ProductPicker products={products} selectedIds={selectedProductIds} setSelectedIds={setSelectedProductIds} /></div>
    <p className="mt-4 text-sm font-bold text-rose-700">{appliesToAll ? "Đang áp dụng: Tất cả sản phẩm" : `Đã chọn ${selectedCategoryIds.length} danh mục và ${selectedProductIds.length} sản phẩm`}</p>
  </fieldset>;
}

function CategoryPicker({ categories, selectedIds, setSelectedIds }: Readonly<{ categories: CategoryAdmin[]; selectedIds: string[]; setSelectedIds: (ids: string[]) => void }>) {
  return <label className="block text-sm font-bold text-stone-800">Danh mục
    <select className="mt-2 min-h-40 w-full rounded-xl border border-stone-300 bg-white p-3 font-medium outline-none focus:border-rose-500" multiple onChange={(event) => setSelectedIds(Array.from(event.currentTarget.selectedOptions, (option) => option.value))} value={selectedIds}>
      {categories.map((category) => <option className="rounded px-2 py-1" key={category.id} value={category.id}>{category.name}</option>)}
    </select>
  </label>;
}

function ProductPicker({ products, selectedIds, setSelectedIds }: Readonly<{ products: AdminProduct[]; selectedIds: string[]; setSelectedIds: (ids: string[]) => void }>) {
  return <label className="block text-sm font-bold text-stone-800">Sản phẩm
    <select className="mt-2 min-h-40 w-full rounded-xl border border-stone-300 bg-white p-3 font-medium outline-none focus:border-rose-500" multiple onChange={(event) => setSelectedIds(Array.from(event.currentTarget.selectedOptions, (option) => option.value))} value={selectedIds}>
      {products.map((product) => <option className="rounded px-2 py-1" key={product.id} value={product.id}>{product.name}</option>)}
    </select>
  </label>;
}

function toVoucherRow(value: VoucherDto): Row { return { id: value.id, name: value.name, code: value.code, status: value.status, value: value.discountMethod === "PERCENTAGE" ? `${(value.discountRateBps ?? 0) / 100}%` : `${(value.fixedDiscountVnd ?? 0).toLocaleString("vi-VN")}đ`, scope: promotionScopeLabel(value.scope), startsAt: value.startsAt, endsAt: value.endsAt }; }

function toDateTimeLocal(value: string) {
  const date = new Date(value); const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function PromotionSummary({ icon: Icon, label, value }: { icon: typeof TicketPercent; label: string; value: number }) { return <div className="rounded-2xl border border-stone-200 bg-white p-5 shadow-sm"><div className="flex items-center justify-between"><div><p className="text-sm font-semibold text-stone-500">{label}</p><p className="mt-1 text-3xl font-black text-stone-900">{value}</p></div><span className="grid h-11 w-11 place-items-center rounded-2xl bg-rose-50 text-rose-600"><Icon className="h-5 w-5" /></span></div></div>; }

function promotionStatusLabel(status: string) { if (status === "ACTIVE") return "Đang hoạt động"; if (status === "DRAFT") return "Bản nháp"; if (status === "PAUSED") return "Tạm dừng"; if (status === "DISABLED") return "Đã tắt"; if (status === "RESERVED") return "Đang giữ"; if (status === "CONSUMED") return "Đã sử dụng"; if (status === "RELEASED") return "Đã hoàn"; return status; }
