"use client";

import { Plus, TicketPercent } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { promotionApi, type DirectSaleDto, type PromotionAuditDto, type VoucherDto, type VoucherReservationDto } from "../api/promotion.api";

type Row = { id: string; kind: "DIRECT_SALE" | "VOUCHER"; name: string; code?: string; status: string; value: string; scope: string; startsAt: string; endsAt: string };

export function PromotionAdminPage() {
  const [rows, setRows] = useState<Row[]>([]); const [showForm, setShowForm] = useState(false); const [kind, setKind] = useState<"DIRECT_SALE" | "VOUCHER">("DIRECT_SALE"); const [method, setMethod] = useState("PERCENTAGE"); const [message, setMessage] = useState("Đang tải…");
  const [audits, setAudits] = useState<PromotionAuditDto[]>([]); const [reservations, setReservations] = useState<VoucherReservationDto[]>([]);
  const [sales, setSales] = useState<DirectSaleDto[]>([]); const [voucherRules, setVoucherRules] = useState<VoucherDto[]>([]);
  const load = async () => {
    try {
      const [sales, vouchers, history] = await Promise.all([promotionApi.directSales(), promotionApi.vouchers(), promotionApi.reservationHistory()]);
      setSales(sales); setVoucherRules(vouchers); setRows([...sales.map(toSaleRow), ...vouchers.map(toVoucherRow)]); setReservations(history); setMessage("");
    } catch { setMessage("Không thể tải danh sách khuyến mãi."); }
  };
  useEffect(() => { Promise.all([promotionApi.directSales(), promotionApi.vouchers(), promotionApi.reservationHistory()]).then(([sales, vouchers, history]) => { setSales(sales); setVoucherRules(vouchers); setRows([...sales.map(toSaleRow), ...vouchers.map(toVoucherRow)]); setReservations(history); setMessage(""); }).catch(() => setMessage("Không thể tải danh sách khuyến mãi.")); }, []);
  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault(); const data = new FormData(event.currentTarget); const value = Number(data.get("value"));
    const common = { name: String(data.get("name")), description: String(data.get("description") ?? ""), discountMethod: method,
      fixedDiscountVnd: method === "FIXED_AMOUNT" ? value : undefined, discountRateBps: method === "PERCENTAGE" ? Math.round(value * 100) : undefined,
      maxDiscountVnd: data.get("maxDiscount") ? Number(data.get("maxDiscount")) : undefined,
      startsAt: new Date(String(data.get("startsAt"))).toISOString(), endsAt: new Date(String(data.get("endsAt"))).toISOString(), reason: String(data.get("reason")) };
    try {
      if (kind === "DIRECT_SALE") await promotionApi.createDirectSale({ ...common, variantIds: String(data.get("targets")).split(",").map((id) => id.trim()).filter(Boolean) });
      else {
        const distributionMode = String(data.get("distributionMode")); const scope = String(data.get("scope"));
        const targets = String(data.get("targets")).split(",").map((id) => id.trim()).filter(Boolean);
        await promotionApi.createVoucher({ ...common, code: String(data.get("code")), scope, minimumOrderVnd: data.get("minimumOrder") ? Number(data.get("minimumOrder")) : undefined,
          minimumEligibleSubtotalVnd: data.get("minimumEligible") ? Number(data.get("minimumEligible")) : undefined, usageLimit: data.get("usageLimit") ? Number(data.get("usageLimit")) : undefined,
          usageLimitPerCustomer: data.get("perCustomer") ? Number(data.get("perCustomer")) : undefined, distributionMode, defaultVoucher: distributionMode === "DEFAULT_FOR_ELIGIBLE",
          productIds: scope.includes("PRODUCT") ? targets : [], categoryIds: scope === "CATEGORY_DISCOUNT" ? targets : [] });
      }
      setShowForm(false); await load();
    } catch { setMessage("Không thể lưu chương trình. Hãy kiểm tra UUID, thời gian và rule giảm giá."); }
  };
  const toggle = async (row: Row) => {
    const status = row.status === "ACTIVE" ? (row.kind === "DIRECT_SALE" ? "PAUSED" : "DISABLED") : "ACTIVE";
    try { if (row.kind === "DIRECT_SALE") await promotionApi.setDirectSaleStatus(row.id, status, "Thay đổi trạng thái từ trang quản trị"); else await promotionApi.setVoucherStatus(row.id, status, "Thay đổi trạng thái từ trang quản trị"); await load(); }
    catch { setMessage("Không thể đổi trạng thái; có thể campaign đang trùng Variant hoặc đã hết hạn."); }
  };
  const showAudits = async (row: Row) => { try { setAudits(row.kind === "DIRECT_SALE" ? await promotionApi.directSaleAudits(row.id) : await promotionApi.voucherAudits(row.id)); } catch { setMessage("Không thể tải lịch sử thay đổi."); } };
  const assign = async (row: Row) => {
    const customerId = window.prompt("Customer UUID cần cấp voucher:")?.trim(); if (!customerId) return;
    const reason = window.prompt("Lý do cấp voucher:")?.trim(); if (!reason) return;
    try { await promotionApi.assignVoucher(row.id, customerId, undefined, reason); await showAudits(row); } catch { setMessage("Không thể cấp voucher; chỉ voucher ASSIGNED_ONLY được phép cấp riêng."); }
  };
  const edit = async (row: Row) => {
    const name = window.prompt("Tên mới:", row.name)?.trim(); if (!name) return; const reason = window.prompt("Lý do cập nhật:")?.trim(); if (!reason) return;
    try {
      if (row.kind === "DIRECT_SALE") {
        const value = sales.find((item) => item.id === row.id); if (!value) return;
        await promotionApi.updateDirectSale(row.id, { name, description: value.description, discountMethod: value.discountMethod, fixedDiscountVnd: value.fixedDiscountVnd, discountRateBps: value.discountRateBps, maxDiscountVnd: value.maxDiscountVnd, startsAt: value.startsAt, endsAt: value.endsAt, variantIds: value.variantIds, reason });
      } else {
        const value = voucherRules.find((item) => item.id === row.id); if (!value) return;
        await promotionApi.updateVoucher(row.id, { code: value.code, name, description: value.description, scope: value.scope, discountMethod: value.discountMethod, fixedDiscountVnd: value.fixedDiscountVnd, discountRateBps: value.discountRateBps, maxDiscountVnd: value.maxDiscountVnd, minimumOrderVnd: value.minimumOrderVnd, minimumEligibleSubtotalVnd: value.minimumEligibleSubtotalVnd, usageLimit: value.usageLimit, usageLimitPerCustomer: value.usageLimitPerCustomer, startsAt: value.startsAt, endsAt: value.endsAt, distributionMode: value.distributionMode, defaultVoucher: value.defaultVoucher, productIds: value.productIds, categoryIds: value.categoryIds, reason });
      }
      await load();
    } catch { setMessage("Không thể cập nhật chương trình."); }
  };
  return <div className="space-y-7"><PageHeader eyebrow="Khuyến mãi" title="Direct sale và mã giảm giá" description="Quản lý thời gian, phạm vi, điều kiện và phân phối. Mọi phép tính giảm giá nằm ở backend." action={<Button onClick={() => setShowForm(!showForm)}><Plus />Tạo chương trình</Button>} />
    {message ? <SurfacePanel className={message.startsWith("Không") ? "text-red-600" : ""}>{message}</SurfacePanel> : null}
    {showForm ? <SurfacePanel><form className="grid gap-4 md:grid-cols-2" onSubmit={(event) => void submit(event)}>
      <Input name="name" label="Tên chương trình" required /><Select label="Loại" value={kind} onChange={(event) => setKind(event.target.value as typeof kind)}><option value="DIRECT_SALE">Direct sale</option><option value="VOUCHER">Voucher</option></Select>
      {kind === "VOUCHER" ? <><Input name="code" label="Mã voucher" required /><Select name="scope" label="Phạm vi"><option value="ORDER_DISCOUNT">Toàn đơn</option><option value="SHIPPING_DISCOUNT">Phí giao hàng</option><option value="PRODUCT_DISCOUNT">Sản phẩm</option><option value="CATEGORY_DISCOUNT">Danh mục</option><option value="PRODUCT_LIST_DISCOUNT">Danh sách sản phẩm</option></Select><Select name="distributionMode" label="Phân phối"><option value="DEFAULT_FOR_ELIGIBLE">Mặc định khi đủ điều kiện</option><option value="ASSIGNED_ONLY">Cấp riêng</option><option value="CODE_ONLY">Nhập mã</option></Select></> : null}
      <Select label="Cách giảm" value={method} onChange={(event) => setMethod(event.target.value)}><option value="PERCENTAGE">Phần trăm</option><option value="FIXED_AMOUNT">Số tiền cố định</option></Select><Input name="value" label={method === "PERCENTAGE" ? "Phần trăm giảm" : "Số tiền giảm (VND)"} type="number" min="1" max={method === "PERCENTAGE" ? 100 : undefined} required />
      <Input name="maxDiscount" label="Mức giảm tối đa (tùy chọn)" type="number" min="0" /><Input name="targets" label={kind === "DIRECT_SALE" ? "Variant UUID, cách nhau dấu phẩy" : "Product/Category UUID nếu cần"} required={kind === "DIRECT_SALE"} />
      {kind === "VOUCHER" ? <><Input name="minimumOrder" label="Tổng đơn tối thiểu" type="number" min="0" /><Input name="minimumEligible" label="Tổng hàng đủ điều kiện tối thiểu" type="number" min="0" /><Input name="usageLimit" label="Tổng lượt dùng" type="number" min="1" /><Input name="perCustomer" label="Lượt dùng mỗi khách" type="number" min="1" /></> : null}
      <Input name="startsAt" label="Bắt đầu" type="datetime-local" required /><Input name="endsAt" label="Kết thúc" type="datetime-local" required /><Input name="description" label="Mô tả" /><Input name="reason" label="Lý do tạo" required />
      <div className="flex gap-2 md:col-span-2"><Button type="submit">Lưu bản nháp</Button><Button type="button" variant="outline" onClick={() => setShowForm(false)}>Hủy</Button></div>
    </form></SurfacePanel> : null}
    <SurfacePanel><div className="flex items-center gap-2"><TicketPercent className="text-brand" /><h2 className="text-lg font-black">Chương trình hiện có</h2></div>{rows.length === 0 && !message ? <p className="mt-5 text-sm text-slate-500">Chưa có chương trình.</p> : <div className="mt-5 overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Chương trình</th><th className="px-4 py-3">Loại</th><th className="px-4 py-3">Giá trị</th><th className="px-4 py-3">Phạm vi</th><th className="px-4 py-3">Thời gian</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3" /></tr></thead><tbody>{rows.map((row) => <tr className="border-t" key={`${row.kind}-${row.id}`}><td className="px-4 py-4 font-bold">{row.name}{row.code ? <code className="ml-2 text-xs">{row.code}</code> : null}</td><td className="px-4 py-4">{row.kind === "DIRECT_SALE" ? "Direct sale" : "Voucher"}</td><td className="px-4 py-4">{row.value}</td><td className="px-4 py-4">{row.scope}</td><td className="px-4 py-4 text-slate-500">{new Date(row.startsAt).toLocaleDateString("vi-VN")} – {new Date(row.endsAt).toLocaleDateString("vi-VN")}</td><td className="px-4 py-4"><StatusBadge label={row.status} tone={row.status === "ACTIVE" ? "success" : "neutral"} /></td><td className="px-4 py-4"><div className="flex flex-wrap gap-2"><Button variant="outline" onClick={() => void toggle(row)}>{row.status === "ACTIVE" ? "Tạm dừng" : "Kích hoạt"}</Button><Button variant="outline" onClick={() => void edit(row)}>Sửa</Button>{row.kind === "VOUCHER" ? <Button variant="outline" onClick={() => void assign(row)}>Cấp voucher</Button> : null}<Button variant="ghost" onClick={() => void showAudits(row)}>Audit</Button></div></td></tr>)}</tbody></table></div>}</SurfacePanel>
    {audits.length > 0 ? <SurfacePanel><h2 className="font-black">Lịch sử thay đổi đã chọn</h2><div className="mt-4 space-y-2">{audits.map((audit) => <div className="rounded-xl border p-3 text-sm" key={audit.id}><strong>{audit.action}</strong> · {audit.reason}<span className="ml-2 text-slate-500">{new Date(audit.createdAt).toLocaleString("vi-VN")}</span></div>)}</div></SurfacePanel> : null}
    <SurfacePanel><h2 className="font-black">Lịch sử giữ, dùng và trả voucher</h2>{reservations.length === 0 ? <p className="mt-3 text-sm text-slate-500">Chưa có reservation.</p> : <div className="mt-4 overflow-x-auto"><table className="min-w-full text-left text-sm"><thead><tr><th className="px-3 py-2">Voucher</th><th className="px-3 py-2">Customer</th><th className="px-3 py-2">Trạng thái</th><th className="px-3 py-2">Giảm hàng</th><th className="px-3 py-2">Giảm ship</th><th className="px-3 py-2">Giữ đến</th></tr></thead><tbody>{reservations.map((item) => <tr className="border-t" key={item.id}><td className="px-3 py-2"><code>{item.voucherId}</code></td><td className="px-3 py-2"><code>{item.customerId}</code></td><td className="px-3 py-2"><StatusBadge label={item.status} tone={item.status === "CONSUMED" ? "success" : item.status === "RESERVED" ? "warning" : "neutral"} /></td><td className="px-3 py-2">{item.discountAmountVnd.toLocaleString("vi-VN")}đ</td><td className="px-3 py-2">{item.shippingDiscountVnd.toLocaleString("vi-VN")}đ</td><td className="px-3 py-2">{new Date(item.reservedUntil).toLocaleString("vi-VN")}</td></tr>)}</tbody></table></div>}</SurfacePanel>
  </div>;
}

function toSaleRow(value: DirectSaleDto): Row { return { id: value.id, kind: "DIRECT_SALE", name: value.name, status: value.status, value: value.discountMethod === "PERCENTAGE" ? `${(value.discountRateBps ?? 0) / 100}%` : `${(value.fixedDiscountVnd ?? 0).toLocaleString("vi-VN")}đ`, scope: `${value.variantIds.length} Variant`, startsAt: value.startsAt, endsAt: value.endsAt }; }
function toVoucherRow(value: VoucherDto): Row { return { id: value.id, kind: "VOUCHER", name: value.name, code: value.code, status: value.status, value: value.discountMethod === "PERCENTAGE" ? `${(value.discountRateBps ?? 0) / 100}%` : `${(value.fixedDiscountVnd ?? 0).toLocaleString("vi-VN")}đ`, scope: value.scope, startsAt: value.startsAt, endsAt: value.endsAt }; }
