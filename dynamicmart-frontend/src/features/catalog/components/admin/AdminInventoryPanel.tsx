"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { History, RefreshCw, Warehouse } from "lucide-react";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import { adjustInventory, listInventory, listInventoryAdjustments } from "../../api/admin-catalog.api";
import type { AdminInventoryItem, InventoryAdjustment } from "../../types";
import { errorMessage, fieldClass, Label, textAreaClass } from "./form-utils";

export function AdminInventoryPanel() {
  const { showToast } = useToast();
  const [items, setItems] = useState<AdminInventoryItem[]>([]);
  const [selectedVariantId, setSelectedVariantId] = useState<string>();
  const [history, setHistory] = useState<InventoryAdjustment[]>([]);
  const [quantityDelta, setQuantityDelta] = useState("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const selected = useMemo(() => items.find((item) => item.variantId === selectedVariantId), [items, selectedVariantId]);

  const load = useCallback(async () => {
    try {
      setLoading(true); setError(undefined);
      const page = await listInventory(0, 100);
      setItems(page.content);
      setSelectedVariantId((current) => current && page.content.some((item) => item.variantId === current) ? current : page.content[0]?.variantId);
    } catch (caught) { setError(errorMessage(caught, "Không thể tải tồn kho.")); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => {
    // Initial API synchronization is intentionally started after the client mounts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  useEffect(() => {
    let active = true;
    if (!selectedVariantId) return;
    listInventoryAdjustments(selectedVariantId, 0, 50)
      .then((page) => { if (active) setHistory(page.content); })
      .catch((caught) => { if (active) setError(errorMessage(caught, "Không thể tải lịch sử tồn kho.")); });
    return () => { active = false; };
  }, [selectedVariantId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!selectedVariantId) return;
    try {
      setBusy(true); setError(undefined);
      const adjustment = await adjustInventory(selectedVariantId, Number(quantityDelta), reason.trim());
      setHistory((current) => [adjustment, ...current]);
      setItems((current) => current.map((item) => item.variantId !== selectedVariantId ? item : {
        ...item,
        onHandQuantity: adjustment.onHandAfter,
        availableQuantity: adjustment.onHandAfter - item.reservedQuantity,
        version: item.version + 1,
      }));
      setQuantityDelta(""); setReason("");
      showToast("Đã điều chỉnh tồn kho và ghi lịch sử.", "success");
    } catch (caught) { setError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  return (
    <div className="space-y-6">
      {error ? <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p> : null}
      <SurfacePanel>
        <div className="flex items-center justify-between gap-3"><div><h2 className="text-xl font-black">Tồn kho theo biến thể</h2><p className="mt-1 text-sm text-slate-500">Available = On hand − Reserved; mọi điều chỉnh đều có audit log.</p></div><button aria-label="Tải lại" className="grid size-10 place-items-center rounded-xl border border-slate-200" onClick={() => void load()} type="button"><RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} /></button></div>
        <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Sản phẩm / SKU</th><th className="px-4 py-3 text-right">On hand</th><th className="px-4 py-3 text-right">Reserved</th><th className="px-4 py-3 text-right">Available</th><th className="px-4 py-3">Trạng thái</th></tr></thead><tbody className="divide-y divide-slate-100">{items.map((item) => <tr className={`cursor-pointer ${selectedVariantId === item.variantId ? "bg-emerald-50/60" : "hover:bg-slate-50"}`} key={item.variantId} onClick={() => setSelectedVariantId(item.variantId)}><td className="px-4 py-3"><p className="font-bold text-slate-900">{item.productName}{item.variantName ? ` · ${item.variantName}` : ""}</p><p className="font-mono text-xs text-slate-500">{item.sku}</p></td><td className="px-4 py-3 text-right font-bold tabular-nums">{item.onHandQuantity}</td><td className="px-4 py-3 text-right tabular-nums text-amber-700">{item.reservedQuantity}</td><td className="px-4 py-3 text-right font-black tabular-nums text-emerald-800">{item.availableQuantity}</td><td className="px-4 py-3"><StatusBadge label={item.variantStatus} tone={item.variantStatus === "ACTIVE" ? "success" : "neutral"} /></td></tr>)}</tbody></table>{!loading && !items.length ? <p className="p-8 text-center text-sm text-slate-500">Chưa có biến thể để quản lý tồn kho.</p> : null}</div>
      </SurfacePanel>

      {selected ? <div className="grid items-start gap-6 xl:grid-cols-[minmax(320px,.7fr)_minmax(0,1.3fr)]">
        <SurfacePanel>
          <div className="flex items-center gap-3"><span className="grid size-11 place-items-center rounded-xl bg-emerald-100 text-emerald-900"><Warehouse className="size-5" /></span><div><p className="text-xs font-bold text-slate-500">Điều chỉnh tồn kho</p><h3 className="font-black">{selected.sku}</h3></div></div>
          <div className="mt-5 grid grid-cols-3 gap-2 text-center"><Metric label="On hand" value={selected.onHandQuantity} /><Metric label="Reserved" value={selected.reservedQuantity} /><Metric label="Available" value={selected.availableQuantity} /></div>
          <form className="mt-5 space-y-4" onSubmit={submit}><label><Label>Thay đổi số lượng</Label><input className={fieldClass} onChange={(event) => setQuantityDelta(event.target.value)} placeholder="Ví dụ: 10 hoặc -3" required step="1" type="number" value={quantityDelta} /></label><label><Label>Lý do</Label><textarea className={textAreaClass} maxLength={500} onChange={(event) => setReason(event.target.value)} placeholder="Nhập kho, kiểm kê, hư hỏng..." required value={reason} /></label><button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50" disabled={busy || Number(quantityDelta) === 0} type="submit">{busy ? "Đang ghi nhận..." : "Xác nhận điều chỉnh"}</button></form>
        </SurfacePanel>

        <SurfacePanel>
          <div className="flex items-center gap-3"><History className="size-5 text-brand" /><div><h3 className="text-xl font-black">Lịch sử điều chỉnh</h3><p className="mt-1 text-sm text-slate-500">Audit gần nhất của biến thể đã chọn.</p></div></div>
          <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Thời gian</th><th className="px-4 py-3">Thay đổi</th><th className="px-4 py-3">Trước → Sau</th><th className="px-4 py-3">Lý do</th><th className="px-4 py-3">Admin</th></tr></thead><tbody className="divide-y divide-slate-100">{history.map((entry) => <tr key={entry.id}><td className="whitespace-nowrap px-4 py-3 text-xs">{new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(new Date(entry.createdAt))}</td><td className={`px-4 py-3 font-black ${entry.quantityDelta > 0 ? "text-emerald-700" : "text-red-700"}`}>{entry.quantityDelta > 0 ? "+" : ""}{entry.quantityDelta}</td><td className="px-4 py-3 font-mono text-xs">{entry.onHandBefore} → {entry.onHandAfter}</td><td className="max-w-xs px-4 py-3">{entry.reason}</td><td className="px-4 py-3 font-mono text-[10px] text-slate-500">{entry.actorAdminId}</td></tr>)}</tbody></table>{!history.length ? <p className="p-8 text-center text-sm text-slate-500">Chưa có lần điều chỉnh nào.</p> : null}</div>
        </SurfacePanel>
      </div> : null}
    </div>
  );
}

function Metric({ label, value }: Readonly<{ label: string; value: number }>) {
  return <div className="rounded-xl bg-slate-50 px-2 py-3"><p className="text-[10px] font-bold tracking-wide text-slate-500 uppercase">{label}</p><p className="mt-1 text-xl font-black tabular-nums">{value}</p></div>;
}
