"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import {
  ChevronLeft, ChevronRight, History, Minus, Plus, RefreshCw, Search, SlidersHorizontal,
  TriangleAlert, Warehouse, X,
} from "lucide-react";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import { adjustInventory, listInventory, listInventoryAdjustments } from "../../api/admin-catalog.api";
import type { AdminInventoryItem, InventoryAdjustment } from "../../types";
import { catalogStatusLabel } from "../../utils/catalog-format";
import { errorMessage, fieldClass, Label, textAreaClass } from "./form-utils";

type StockFilter = "ALL" | "LOW" | "OUT";
type AdjustmentMode = "INCREASE" | "DECREASE";
type DrawerTab = "ADJUST" | "HISTORY";

const LOW_STOCK_THRESHOLD = 5;
const INVENTORY_PAGE_SIZE = 25;
const QUICK_QUANTITIES = [10, 20, 50];

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
  const [historyLoading, setHistoryLoading] = useState(false);
  const [drawerTab, setDrawerTab] = useState<DrawerTab>("ADJUST");
  const [adjustmentMode, setAdjustmentMode] = useState<AdjustmentMode>("INCREASE");
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [keyword, setKeyword] = useState("");
  const [stockFilter, setStockFilter] = useState<StockFilter>("ALL");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const [drawerError, setDrawerError] = useState<string>();

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
  const totalPages = Math.max(1, Math.ceil(filteredItems.length / INVENTORY_PAGE_SIZE));
  const visibleItems = filteredItems.slice(
    page * INVENTORY_PAGE_SIZE,
    (page + 1) * INVENTORY_PAGE_SIZE,
  );

  const amount = quantity.trim() ? Number(quantity) : 0;
  const delta = adjustmentMode === "INCREASE" ? amount : -amount;
  const projectedOnHand = selected ? selected.onHandQuantity + delta : 0;
  const projectedAvailable = selected ? projectedOnHand - selected.reservedQuantity : 0;
  const invalidAdjustment = !Number.isInteger(amount)
    || amount <= 0
    || !selected
    || projectedOnHand < selected.reservedQuantity;

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      const result = await listInventory(0, 100);
      setItems(result.content);
      setPage(0);
      setSelectedVariantId((current) =>
        current && result.content.some((item) => item.variantId === current) ? current : undefined,
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
      .then((result) => {
        if (active) setHistory(result.content);
      })
      .catch((caught) => {
        if (active) setDrawerError(errorMessage(caught, "Không thể tải lịch sử tồn kho."));
      })
      .finally(() => {
        if (active) setHistoryLoading(false);
      });
    return () => {
      active = false;
    };
  }, [selectedVariantId]);

  useEffect(() => {
    if (!selectedVariantId) return;
    const previousOverflow = document.body.style.overflow;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setSelectedVariantId(undefined);
    };
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [selectedVariantId]);

  function selectFilter(filter: StockFilter) {
    setStockFilter(filter);
    setPage(0);
  }

  function openAdjustment(item: AdminInventoryItem) {
    setSelectedVariantId(item.variantId);
    setHistory([]);
    setHistoryLoading(true);
    setDrawerTab("ADJUST");
    setAdjustmentMode("INCREASE");
    setQuantity("");
    setReason("");
    setDrawerError(undefined);
  }

  function closeDrawer() {
    setSelectedVariantId(undefined);
    setQuantity("");
    setReason("");
    setDrawerError(undefined);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!selectedVariantId || invalidAdjustment) return;

    if (delta < 0 && !window.confirm(
      `Bạn sắp giảm ${amount} sản phẩm của SKU ${selected?.sku}. Bạn có chắc muốn tiếp tục?`,
    )) return;

    try {
      setBusy(true);
      setDrawerError(undefined);
      const adjustment = await adjustInventory(selectedVariantId, delta, reason.trim());
      setHistory((current) => [adjustment, ...current]);
      setItems((current) => current.map((item) => item.variantId !== selectedVariantId ? item : {
        ...item,
        onHandQuantity: adjustment.onHandAfter,
        availableQuantity: adjustment.onHandAfter - item.reservedQuantity,
        version: item.version + 1,
      }));
      setQuantity("");
      setReason("");
      setPage(0);
      showToast("Đã điều chỉnh tồn kho và ghi lịch sử.", "success");
    } catch (caught) {
      setDrawerError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  const rangeStart = filteredItems.length ? page * INVENTORY_PAGE_SIZE + 1 : 0;
  const rangeEnd = Math.min((page + 1) * INVENTORY_PAGE_SIZE, filteredItems.length);

  return (
    <div className="space-y-5">
      {error ? (
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p>
      ) : null}

      <div className="grid gap-3 sm:grid-cols-3">
        <StockSummaryButton
          active={stockFilter === "ALL"}
          count={stockSummary.all}
          label="Tất cả biến thể"
          onClick={() => selectFilter("ALL")}
          tone="neutral"
        />
        <StockSummaryButton
          active={stockFilter === "LOW"}
          count={stockSummary.low}
          label="Sắp hết hàng"
          onClick={() => selectFilter("LOW")}
          tone="warning"
        />
        <StockSummaryButton
          active={stockFilter === "OUT"}
          count={stockSummary.out}
          label="Hết hàng"
          onClick={() => selectFilter("OUT")}
          tone="danger"
        />
      </div>

      <SurfacePanel className="overflow-hidden p-0 sm:p-0">
        <div className="flex flex-col justify-between gap-4 border-b border-slate-200 p-5 lg:flex-row lg:items-end sm:p-6">
          <div>
            <h2 className="text-xl font-black">Tồn kho theo biến thể</h2>
            <p className="mt-1 text-sm text-slate-500">
              Có thể bán = Tồn thực tế − Đang giữ. Chọn “Điều chỉnh” ngay trên SKU cần thao tác.
            </p>
          </div>
          <div className="flex w-full gap-2 lg:w-auto">
            <label className="relative min-w-0 flex-1 lg:w-80">
              <span className="sr-only">Tìm theo sản phẩm hoặc SKU</span>
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
              <input
                className={`${fieldClass} pl-9`}
                onChange={(event) => {
                  setKeyword(event.target.value);
                  setPage(0);
                }}
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

        <div className="max-h-[58vh] overflow-auto">
          <table className="min-w-[940px] text-left text-sm">
            <thead className="sticky top-0 z-10 bg-slate-50 text-xs uppercase text-slate-500 shadow-[0_1px_0_0_rgb(226_232_240)]">
              <tr>
                <th className="px-5 py-3">Sản phẩm / SKU</th>
                <th className="px-4 py-3 text-right">Tồn thực tế</th>
                <th className="px-4 py-3 text-right">Đang giữ</th>
                <th className="px-4 py-3 text-right">Có thể bán</th>
                <th className="px-4 py-3">Mức tồn</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-5 py-3 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {visibleItems.map((item) => {
                const stockState = item.availableQuantity <= 0
                  ? { label: "Hết hàng", tone: "danger" as const }
                  : item.availableQuantity <= LOW_STOCK_THRESHOLD
                    ? { label: "Sắp hết", tone: "warning" as const }
                    : { label: "Ổn định", tone: "success" as const };
                return (
                  <tr className="hover:bg-slate-50" key={item.variantId}>
                    <td className="px-5 py-3">
                      <span className="block font-bold text-slate-900">
                        {item.productName}{item.variantName ? ` · ${item.variantName}` : ""}
                      </span>
                      <span className="font-mono text-xs text-slate-500">{item.sku}</span>
                    </td>
                    <td className="px-4 py-3 text-right font-bold tabular-nums">{item.onHandQuantity}</td>
                    <td className="px-4 py-3 text-right tabular-nums text-amber-700">{item.reservedQuantity}</td>
                    <td className="px-4 py-3 text-right font-black tabular-nums text-emerald-800">{item.availableQuantity}</td>
                    <td className="px-4 py-3"><StatusBadge label={stockState.label} tone={stockState.tone} /></td>
                    <td className="px-4 py-3">
                      <StatusBadge label={catalogStatusLabel(item.variantStatus)} tone={item.variantStatus === "ACTIVE" ? "success" : "neutral"} />
                    </td>
                    <td className="px-5 py-3 text-right">
                      <button
                        className="inline-flex min-h-9 items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-xs font-black text-slate-800 transition hover:border-emerald-300 hover:text-emerald-800"
                        onClick={() => openAdjustment(item)}
                        type="button"
                      >
                        <SlidersHorizontal className="size-3.5" /> Điều chỉnh
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {!loading && !visibleItems.length ? (
            <p className="p-10 text-center text-sm text-slate-500">
              {items.length ? "Không có biến thể phù hợp với bộ lọc." : "Chưa có biến thể để quản lý tồn kho."}
            </p>
          ) : null}
        </div>

        <div className="flex flex-col gap-3 border-t border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-slate-500">
            Hiển thị <strong className="text-slate-900">{rangeStart}–{rangeEnd}</strong> trong {filteredItems.length} biến thể
          </p>
          <div className="flex items-center gap-2">
            <button
              aria-label="Trang tồn kho trước"
              className="grid size-9 place-items-center rounded-xl border border-slate-200 disabled:opacity-40"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              type="button"
            >
              <ChevronLeft className="size-4" />
            </button>
            <span className="min-w-24 text-center text-sm font-bold text-slate-700">
              Trang {page + 1} / {totalPages}
            </span>
            <button
              aria-label="Trang tồn kho sau"
              className="grid size-9 place-items-center rounded-xl border border-slate-200 disabled:opacity-40"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
              type="button"
            >
              <ChevronRight className="size-4" />
            </button>
          </div>
        </div>
      </SurfacePanel>

      {selected ? (
        <InventoryAdjustmentDrawer
          activeTab={drawerTab}
          adjustmentMode={adjustmentMode}
          busy={busy}
          error={drawerError}
          history={history}
          historyLoading={historyLoading}
          invalidAdjustment={invalidAdjustment}
          item={selected}
          onClose={closeDrawer}
          onModeChange={(mode) => {
            setAdjustmentMode(mode);
            setDrawerError(undefined);
          }}
          onQuantityChange={setQuantity}
          onReasonChange={setReason}
          onSubmit={submit}
          onTabChange={setDrawerTab}
          projectedAvailable={projectedAvailable}
          projectedOnHand={projectedOnHand}
          quantity={quantity}
          reason={reason}
        />
      ) : null}
    </div>
  );
}

function InventoryAdjustmentDrawer({
  activeTab,
  adjustmentMode,
  busy,
  error,
  history,
  historyLoading,
  invalidAdjustment,
  item,
  onClose,
  onModeChange,
  onQuantityChange,
  onReasonChange,
  onSubmit,
  onTabChange,
  projectedAvailable,
  projectedOnHand,
  quantity,
  reason,
}: Readonly<{
  activeTab: DrawerTab;
  adjustmentMode: AdjustmentMode;
  busy: boolean;
  error?: string;
  history: InventoryAdjustment[];
  historyLoading: boolean;
  invalidAdjustment: boolean;
  item: AdminInventoryItem;
  onClose: () => void;
  onModeChange: (mode: AdjustmentMode) => void;
  onQuantityChange: (value: string) => void;
  onReasonChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
  onTabChange: (tab: DrawerTab) => void;
  projectedAvailable: number;
  projectedOnHand: number;
  quantity: string;
  reason: string;
}>) {
  const hasQuantity = Boolean(quantity.trim());
  return (
    <div className="fixed inset-0 z-[80]" role="presentation">
      <button
        aria-label="Đóng bảng điều chỉnh tồn kho"
        className="absolute inset-0 bg-slate-950/45 backdrop-blur-[2px]"
        onClick={onClose}
        tabIndex={-1}
        type="button"
      />
      <aside
        aria-labelledby="inventory-drawer-title"
        aria-modal="true"
        className="absolute inset-x-0 bottom-0 flex h-[92dvh] flex-col overflow-hidden rounded-t-3xl bg-white shadow-2xl sm:inset-y-0 sm:right-0 sm:left-auto sm:h-full sm:w-[460px] sm:rounded-none"
        role="dialog"
      >
        <header className="border-b border-slate-200 px-5 pt-5 sm:px-6">
          <div className="flex items-start justify-between gap-4">
            <div className="flex min-w-0 items-start gap-3">
              <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-emerald-100 text-emerald-900">
                <Warehouse className="size-5" />
              </span>
              <div className="min-w-0">
                <p className="text-xs font-black tracking-wider text-emerald-700 uppercase">Quản lý tồn kho</p>
                <h2 className="mt-1 truncate text-lg font-black text-slate-950" id="inventory-drawer-title">
                  {item.productName}
                </h2>
                <p className="truncate text-sm text-slate-500">{item.variantName || "Biến thể mặc định"} · {item.sku}</p>
              </div>
            </div>
            <button
              aria-label="Đóng"
              autoFocus
              className="grid size-9 shrink-0 place-items-center rounded-xl text-slate-500 hover:bg-slate-100"
              onClick={onClose}
              type="button"
            >
              <X className="size-5" />
            </button>
          </div>

          <div className="mt-5 grid grid-cols-3 gap-2 text-center">
            <Metric label="Tồn thực tế" value={item.onHandQuantity} />
            <Metric label="Đang giữ" value={item.reservedQuantity} />
            <Metric label="Có thể bán" value={item.availableQuantity} />
          </div>

          <div className="mt-5 grid grid-cols-2 gap-1 rounded-xl bg-slate-100 p-1" role="tablist">
            <DrawerTabButton active={activeTab === "ADJUST"} onClick={() => onTabChange("ADJUST")}>
              <SlidersHorizontal className="size-4" /> Điều chỉnh
            </DrawerTabButton>
            <DrawerTabButton active={activeTab === "HISTORY"} onClick={() => onTabChange("HISTORY")}>
              <History className="size-4" /> Lịch sử
            </DrawerTabButton>
          </div>
        </header>

        {activeTab === "ADJUST" ? (
          <form className="flex min-h-0 flex-1 flex-col" onSubmit={onSubmit}>
            <div className="flex-1 space-y-5 overflow-y-auto px-5 py-5 sm:px-6">
              {error ? <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-800" role="alert">{error}</p> : null}

              <div>
                <Label>Loại điều chỉnh</Label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    className={`flex min-h-12 items-center justify-center gap-2 rounded-xl border text-sm font-black transition ${
                      adjustmentMode === "INCREASE"
                        ? "border-emerald-700 bg-emerald-50 text-emerald-900 ring-2 ring-emerald-700/10"
                        : "border-slate-200 text-slate-600 hover:bg-slate-50"
                    }`}
                    onClick={() => onModeChange("INCREASE")}
                    type="button"
                  >
                    <Plus className="size-4" /> Nhập thêm
                  </button>
                  <button
                    className={`flex min-h-12 items-center justify-center gap-2 rounded-xl border text-sm font-black transition ${
                      adjustmentMode === "DECREASE"
                        ? "border-red-600 bg-red-50 text-red-800 ring-2 ring-red-600/10"
                        : "border-slate-200 text-slate-600 hover:bg-slate-50"
                    }`}
                    onClick={() => onModeChange("DECREASE")}
                    type="button"
                  >
                    <Minus className="size-4" /> Giảm kho
                  </button>
                </div>
              </div>

              <label>
                <Label>{adjustmentMode === "INCREASE" ? "Số lượng nhập thêm" : "Số lượng giảm"}</Label>
                <input
                  className={`${fieldClass} text-lg font-black tabular-nums`}
                  min="1"
                  onChange={(event) => onQuantityChange(event.target.value)}
                  placeholder="Nhập số lượng"
                  required
                  step="1"
                  type="number"
                  value={quantity}
                />
              </label>

              <div>
                <p className="mb-2 text-xs font-bold tracking-wide text-slate-500 uppercase">Chọn nhanh</p>
                <div className="flex flex-wrap gap-2">
                  {QUICK_QUANTITIES.map((value) => (
                    <button
                      className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-black text-slate-700 hover:border-emerald-300 hover:bg-emerald-50"
                      key={value}
                      onClick={() => onQuantityChange(String(value))}
                      type="button"
                    >
                      {adjustmentMode === "INCREASE" ? "+" : "−"}{value}
                    </button>
                  ))}
                </div>
              </div>

              {hasQuantity ? (
                <div className={`rounded-2xl border p-4 ${
                  invalidAdjustment ? "border-red-200 bg-red-50" : "border-emerald-200 bg-emerald-50"
                }`}>
                  <p className="text-xs font-bold tracking-wide text-slate-600 uppercase">Kết quả sau điều chỉnh</p>
                  <div className="mt-3 flex items-center justify-between gap-3">
                    <span className="text-sm text-slate-600">Tồn thực tế</span>
                    <strong className="tabular-nums">{item.onHandQuantity} → {projectedOnHand}</strong>
                  </div>
                  <div className="mt-1.5 flex items-center justify-between gap-3">
                    <span className="text-sm text-slate-600">Có thể bán</span>
                    <strong className="tabular-nums">{item.availableQuantity} → {projectedAvailable}</strong>
                  </div>
                  {invalidAdjustment ? (
                    <p className="mt-3 flex gap-2 text-sm font-semibold text-red-800">
                      <TriangleAlert className="mt-0.5 size-4 shrink-0" />
                      Số lượng phải là số nguyên dương và tồn sau điều chỉnh không được thấp hơn lượng đang giữ.
                    </p>
                  ) : null}
                </div>
              ) : null}

              <label>
                <Label>Lý do</Label>
                <textarea
                  className={textAreaClass}
                  maxLength={500}
                  onChange={(event) => onReasonChange(event.target.value)}
                  placeholder={adjustmentMode === "INCREASE" ? "Ví dụ: Nhập hàng từ nhà cung cấp" : "Ví dụ: Kiểm kê, hư hỏng hoặc thất thoát"}
                  required
                  value={reason}
                />
              </label>
            </div>
            <footer className="border-t border-slate-200 bg-white p-4 sm:px-6">
              <button
                className={`min-h-12 w-full rounded-xl px-4 text-sm font-black text-white disabled:opacity-50 ${
                  adjustmentMode === "INCREASE" ? "bg-emerald-950 hover:bg-emerald-900" : "bg-red-700 hover:bg-red-800"
                }`}
                disabled={busy || invalidAdjustment || !reason.trim()}
                type="submit"
              >
                {busy ? "Đang ghi nhận..." : adjustmentMode === "INCREASE" ? "Xác nhận nhập thêm" : "Xác nhận giảm kho"}
              </button>
            </footer>
          </form>
        ) : (
          <div className="flex-1 overflow-y-auto px-5 py-5 sm:px-6">
            {error ? <p className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-800" role="alert">{error}</p> : null}
            {historyLoading ? (
              <p className="rounded-xl bg-slate-50 p-6 text-center text-sm text-slate-500">Đang tải lịch sử...</p>
            ) : history.length ? (
              <ol className="space-y-3">
                {history.map((entry) => (
                  <li className="rounded-2xl border border-slate-200 p-4" key={entry.id}>
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <strong className={`text-lg tabular-nums ${entry.quantityDelta > 0 ? "text-emerald-700" : "text-red-700"}`}>
                          {entry.quantityDelta > 0 ? "+" : ""}{entry.quantityDelta}
                        </strong>
                        <p className="mt-0.5 text-sm font-bold text-slate-800">{entry.onHandBefore} → {entry.onHandAfter} sản phẩm</p>
                      </div>
                      <time className="text-right text-xs text-slate-500" dateTime={entry.createdAt}>
                        {new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(new Date(entry.createdAt))}
                      </time>
                    </div>
                    <p className="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">{entry.reason}</p>
                    <p className="mt-2 truncate font-mono text-[11px] text-slate-400" title={entry.actorAdminId}>Admin: {entry.actorAdminId}</p>
                  </li>
                ))}
              </ol>
            ) : (
              <div className="rounded-2xl border border-dashed border-slate-300 p-8 text-center">
                <History className="mx-auto size-8 text-slate-300" />
                <p className="mt-3 font-bold text-slate-700">Chưa có lần điều chỉnh nào</p>
                <p className="mt-1 text-sm text-slate-500">Các lần nhập thêm hoặc giảm kho sẽ xuất hiện tại đây.</p>
              </div>
            )}
          </div>
        )}
      </aside>
    </div>
  );
}

function DrawerTabButton({ active, children, onClick }: Readonly<{
  active: boolean;
  children: React.ReactNode;
  onClick: () => void;
}>) {
  return (
    <button
      aria-selected={active}
      className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-lg text-sm font-black transition ${
        active ? "bg-white text-slate-950 shadow-sm" : "text-slate-500 hover:text-slate-800"
      }`}
      onClick={onClick}
      role="tab"
      type="button"
    >
      {children}
    </button>
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
      className={`rounded-2xl border px-4 py-3 text-left transition hover:-translate-y-0.5 hover:shadow-sm ${toneClass} ${
        active ? "ring-2 ring-emerald-900 ring-offset-2" : ""
      }`}
      onClick={onClick}
      type="button"
    >
      <span className="block text-xs font-bold opacity-70">{label}</span>
      <span className="mt-0.5 block text-2xl font-black tabular-nums">{count}</span>
    </button>
  );
}

function Metric({ label, value }: Readonly<{ label: string; value: number }>) {
  return (
    <div className="rounded-xl bg-slate-50 px-2 py-3">
      <p className="text-[10px] font-bold tracking-wide text-slate-600 uppercase">{label}</p>
      <p className="mt-1 text-xl font-black tabular-nums">{value}</p>
    </div>
  );
}
