"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { History, RefreshCw, Search, TriangleAlert, Warehouse } from "lucide-react";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import { adjustInventory, listInventory, listInventoryAdjustments } from "../../api/admin-catalog.api";
import type { AdminInventoryItem, InventoryAdjustment } from "../../types";
import { errorMessage, fieldClass, Label, textAreaClass } from "./form-utils";

type StockFilter = "ALL" | "LOW" | "OUT";

const LOW_STOCK_THRESHOLD = 5;

function matchesStockFilter(item: AdminInventoryItem, filter: StockFilter) {
  if (filter === "OUT") return item.availableQuantity <= 0;
  if (filter === "LOW") return item.availableQuantity > 0 && item.availableQuantity <= LOW_STOCK_THRESHOLD;
  return true;
}

export function AdminInventoryPanel() {
  const { showToast } = useToast();
  const [items, setItems] = useState<AdminInventoryItem[]>([]);
  const [selectedVariantId, setSelectedVariantId] = useState<string>();
  const [history, setHistory] = useState<InventoryAdjustment[]>([]);
  const [quantityDelta, setQuantityDelta] = useState("");
  const [reason, setReason] = useState("");
  const [keyword, setKeyword] = useState("");
  const [stockFilter, setStockFilter] = useState<StockFilter>("ALL");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const selected = useMemo(
    () => items.find((item) => item.variantId === selectedVariantId),
    [items, selectedVariantId],
  );
  const stockSummary = useMemo(() => ({
    all: items.length,
    low: items.filter((item) => matchesStockFilter(item, "LOW")).length,
    out: items.filter((item) => matchesStockFilter(item, "OUT")).length,
  }), [items]);
  const filteredItems = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLocaleLowerCase("vi");
    return items.filter((item) => {
      const matchesKeyword = !normalizedKeyword || [item.productName, item.variantName, item.sku]
        .filter(Boolean)
        .some((value) => value?.toLocaleLowerCase("vi").includes(normalizedKeyword));
      return matchesKeyword && matchesStockFilter(item, stockFilter);
    });
  }, [items, keyword, stockFilter]);

  const delta = quantityDelta.trim() ? Number(quantityDelta) : 0;
  const projectedOnHand = selected ? selected.onHandQuantity + delta : 0;
  const projectedAvailable = selected ? projectedOnHand - selected.reservedQuantity : 0;
  const invalidAdjustment = !Number.isInteger(delta)
    || delta === 0
    || !selected
    || projectedOnHand < selected.reservedQuantity;

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      const page = await listInventory(0, 100);
      setItems(page.content);
      setSelectedVariantId((current) =>
        current && page.content.some((item) => item.variantId === current) ? current : page.content[0]?.variantId,
      );
    } catch (caught) {
      setError(errorMessage(caught, "Không thể tải tồn kho."));
    } finally {
      setLoading(false);
    }
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
      .then((page) => {
        if (active) setHistory(page.content);
      })
      .catch((caught) => {
        if (active) setError(errorMessage(caught, "Không thể tải lịch sử tồn kho."));
      });
    return () => {
      active = false;
    };
  }, [selectedVariantId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!selectedVariantId || invalidAdjustment) return;

    if (delta < 0 && !window.confirm(
      `Bạn sắp giảm ${Math.abs(delta)} sản phẩm của SKU ${selected?.sku}. Bạn có chắc muốn tiếp tục?`,
    )) return;

    try {
      setBusy(true);
      setError(undefined);
      const adjustment = await adjustInventory(selectedVariantId, delta, reason.trim());
      setHistory((current) => [adjustment, ...current]);
      setItems((current) => current.map((item) => item.variantId !== selectedVariantId ? item : {
        ...item,
        onHandQuantity: adjustment.onHandAfter,
        availableQuantity: adjustment.onHandAfter - item.reservedQuantity,
        version: item.version + 1,
      }));
      setQuantityDelta("");
      setReason("");
      showToast("Đã điều chỉnh tồn kho và ghi lịch sử.", "success");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-6">
      {error ? (
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p>
      ) : null}

      <div className="grid gap-3 sm:grid-cols-3">
        <StockSummaryButton
          active={stockFilter === "ALL"}
          count={stockSummary.all}
          label="Tất cả biến thể"
          onClick={() => setStockFilter("ALL")}
          tone="neutral"
        />
        <StockSummaryButton
          active={stockFilter === "LOW"}
          count={stockSummary.low}
          label="Sắp hết hàng"
          onClick={() => setStockFilter("LOW")}
          tone="warning"
        />
        <StockSummaryButton
          active={stockFilter === "OUT"}
          count={stockSummary.out}
          label="Hết hàng"
          onClick={() => setStockFilter("OUT")}
          tone="danger"
        />
      </div>

      <SurfacePanel>
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
          <div>
            <h2 className="text-xl font-black">Tồn kho theo biến thể</h2>
            <p className="mt-1 text-sm text-slate-500">
              Có thể bán = Tồn thực tế − Đang giữ; mọi điều chỉnh đều được lưu lịch sử.
            </p>
          </div>
          <div className="flex w-full gap-2 lg:w-auto">
            <label className="relative min-w-0 flex-1 lg:w-80">
              <span className="sr-only">Tìm theo sản phẩm hoặc SKU</span>
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
              <input
                className={`${fieldClass} pl-9`}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm sản phẩm hoặc SKU..."
                type="search"
                value={keyword}
              />
            </label>
            <button
              aria-label="Tải lại tồn kho"
              className="grid size-11 shrink-0 place-items-center rounded-xl border border-slate-200 hover:bg-slate-50"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>

        <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Sản phẩm / SKU</th>
                <th className="px-4 py-3 text-right">Tồn thực tế</th>
                <th className="px-4 py-3 text-right">Đang giữ</th>
                <th className="px-4 py-3 text-right">Có thể bán</th>
                <th className="px-4 py-3">Mức tồn</th>
                <th className="px-4 py-3">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredItems.map((item) => {
                const stockState = item.availableQuantity <= 0
                  ? { label: "Hết hàng", tone: "danger" as const }
                  : item.availableQuantity <= LOW_STOCK_THRESHOLD
                    ? { label: "Sắp hết", tone: "warning" as const }
                    : { label: "Ổn định", tone: "success" as const };
                return (
                  <tr
                    className={selectedVariantId === item.variantId ? "bg-emerald-50/60" : "hover:bg-slate-50"}
                    key={item.variantId}
                  >
                    <td className="px-4 py-3">
                      <button className="w-full text-left" onClick={() => setSelectedVariantId(item.variantId)} type="button">
                        <span className="block font-bold text-slate-900">
                          {item.productName}{item.variantName ? ` · ${item.variantName}` : ""}
                        </span>
                        <span className="font-mono text-xs text-slate-500">{item.sku}</span>
                      </button>
                    </td>
                    <td className="px-4 py-3 text-right font-bold tabular-nums">{item.onHandQuantity}</td>
                    <td className="px-4 py-3 text-right tabular-nums text-amber-700">{item.reservedQuantity}</td>
                    <td className="px-4 py-3 text-right font-black tabular-nums text-emerald-800">{item.availableQuantity}</td>
                    <td className="px-4 py-3"><StatusBadge label={stockState.label} tone={stockState.tone} /></td>
                    <td className="px-4 py-3">
                      <StatusBadge label={item.variantStatus} tone={item.variantStatus === "ACTIVE" ? "success" : "neutral"} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {!loading && !filteredItems.length ? (
            <p className="p-8 text-center text-sm text-slate-500">
              {items.length ? "Không có biến thể phù hợp với bộ lọc." : "Chưa có biến thể để quản lý tồn kho."}
            </p>
          ) : null}
        </div>
      </SurfacePanel>

      {selected ? (
        <div className="grid items-start gap-6 xl:grid-cols-[minmax(340px,.72fr)_minmax(0,1.28fr)]">
          <SurfacePanel className="xl:sticky xl:top-40">
            <div className="flex items-center gap-3">
              <span className="grid size-11 place-items-center rounded-xl bg-emerald-100 text-emerald-900">
                <Warehouse className="size-5" />
              </span>
              <div>
                <p className="text-xs font-bold text-slate-500">Điều chỉnh biến thể</p>
                <h3 className="font-black">{selected.sku}</h3>
              </div>
            </div>
            <div className="mt-5 grid grid-cols-3 gap-2 text-center">
              <Metric label="Tồn thực tế" value={selected.onHandQuantity} />
              <Metric label="Đang giữ" value={selected.reservedQuantity} />
              <Metric label="Có thể bán" value={selected.availableQuantity} />
            </div>
            <form className="mt-5 space-y-4" onSubmit={submit}>
              <label>
                <Label>Thay đổi số lượng</Label>
                <input
                  className={fieldClass}
                  onChange={(event) => setQuantityDelta(event.target.value)}
                  placeholder="Ví dụ: 10 hoặc -3"
                  required
                  step="1"
                  type="number"
                  value={quantityDelta}
                />
                <span className="mt-1 block text-xs text-slate-500">Số dương để nhập thêm, số âm để giảm kho.</span>
              </label>

              {quantityDelta.trim() ? (
                <div className={`rounded-xl border p-4 ${
                  invalidAdjustment ? "border-red-200 bg-red-50" : "border-emerald-200 bg-emerald-50"
                }`}>
                  <p className="text-xs font-bold tracking-wide text-slate-600 uppercase">Kết quả sau điều chỉnh</p>
                  <div className="mt-2 flex items-center justify-between gap-3">
                    <span className="text-sm text-slate-600">Tồn thực tế</span>
                    <strong className="tabular-nums">{selected.onHandQuantity} → {projectedOnHand}</strong>
                  </div>
                  <div className="mt-1 flex items-center justify-between gap-3">
                    <span className="text-sm text-slate-600">Có thể bán</span>
                    <strong className="tabular-nums">{selected.availableQuantity} → {projectedAvailable}</strong>
                  </div>
                  {invalidAdjustment ? (
                    <p className="mt-3 flex gap-2 text-sm font-semibold text-red-800">
                      <TriangleAlert className="mt-0.5 size-4 shrink-0" />
                      Số lượng sau điều chỉnh không được nhỏ hơn lượng hàng đang giữ.
                    </p>
                  ) : null}
                </div>
              ) : null}

              <label>
                <Label>Lý do</Label>
                <textarea
                  className={textAreaClass}
                  maxLength={500}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="Nhập kho, kiểm kê, hư hỏng..."
                  required
                  value={reason}
                />
              </label>
              <button
                className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50"
                disabled={busy || invalidAdjustment || !reason.trim()}
                type="submit"
              >
                {busy ? "Đang ghi nhận..." : "Xác nhận điều chỉnh"}
              </button>
            </form>
          </SurfacePanel>

          <SurfacePanel>
            <div className="flex items-center gap-3">
              <History className="size-5 text-brand" />
              <div>
                <h3 className="text-xl font-black">Lịch sử điều chỉnh</h3>
                <p className="mt-1 text-sm text-slate-500">Các thay đổi gần nhất của SKU {selected.sku}.</p>
              </div>
            </div>
            <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">Thời gian</th>
                    <th className="px-4 py-3">Thay đổi</th>
                    <th className="px-4 py-3">Trước → Sau</th>
                    <th className="px-4 py-3">Lý do</th>
                    <th className="px-4 py-3">Mã Admin</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {history.map((entry) => (
                    <tr key={entry.id}>
                      <td className="whitespace-nowrap px-4 py-3 text-xs">
                        {new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(new Date(entry.createdAt))}
                      </td>
                      <td className={`px-4 py-3 font-black ${entry.quantityDelta > 0 ? "text-emerald-700" : "text-red-700"}`}>
                        {entry.quantityDelta > 0 ? "+" : ""}{entry.quantityDelta}
                      </td>
                      <td className="px-4 py-3 font-mono text-xs">{entry.onHandBefore} → {entry.onHandAfter}</td>
                      <td className="max-w-xs px-4 py-3">{entry.reason}</td>
                      <td className="px-4 py-3 font-mono text-xs text-slate-600">{entry.actorAdminId}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!history.length ? (
                <p className="p-8 text-center text-sm text-slate-500">Chưa có lần điều chỉnh nào.</p>
              ) : null}
            </div>
          </SurfacePanel>
        </div>
      ) : null}
    </div>
  );
}

function StockSummaryButton({
  active,
  count,
  label,
  onClick,
  tone,
}: Readonly<{
  active: boolean;
  count: number;
  label: string;
  onClick: () => void;
  tone: "neutral" | "warning" | "danger";
}>) {
  const toneClass = {
    neutral: "border-slate-200 bg-white text-slate-950",
    warning: "border-amber-200 bg-amber-50 text-amber-950",
    danger: "border-red-200 bg-red-50 text-red-950",
  }[tone];
  return (
    <button
      className={`rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 hover:shadow-sm ${toneClass} ${
        active ? "ring-2 ring-emerald-900 ring-offset-2" : ""
      }`}
      onClick={onClick}
      type="button"
    >
      <span className="block text-sm font-bold opacity-70">{label}</span>
      <span className="mt-1 block text-2xl font-black tabular-nums">{count}</span>
    </button>
  );
}

function Metric({ label, value }: Readonly<{ label: string; value: number }>) {
  return (
    <div className="rounded-xl bg-slate-50 px-2 py-3">
      <p className="text-[11px] font-bold tracking-wide text-slate-600 uppercase">{label}</p>
      <p className="mt-1 text-xl font-black tabular-nums">{value}</p>
    </div>
  );
}
