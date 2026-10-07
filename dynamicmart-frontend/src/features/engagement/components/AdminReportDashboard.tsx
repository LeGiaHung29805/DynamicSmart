"use client";

import {
  AlertCircle,
  Calendar,
  ChartNoAxesCombined,
  Package,
  RefreshCw,
  ShoppingBag,
  TrendingUp,
  Trophy,
} from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { engagementApi } from "../api/engagement.api";
import { mockBestSellers, mockSalesMetrics } from "../mock-data";
import type { BestSellerItem, DailySalesMetric } from "../types/engagement.types";
import { money } from "./EngagementShared";

function getInitialDates() {
  const today = new Date();
  const past = new Date();
  past.setDate(today.getDate() - 14); // 14 ngày gần nhất
  return {
    from: past.toISOString().slice(0, 10),
    to: today.toISOString().slice(0, 10),
  };
}

export function AdminReportDashboard() {
  const initialDates = getInitialDates();
  const [fromDate, setFromDate] = useState(initialDates.from);
  const [toDate, setToDate] = useState(initialDates.to);
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [salesMetrics, setSalesMetrics] = useState<DailySalesMetric[]>([]);
  const [bestSellers, setBestSellers] = useState<BestSellerItem[]>([]);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        const [salesData, bestSellersData] = await Promise.all([
          engagementApi.reports.sales(fromDate, toDate),
          engagementApi.reports.bestSellers(fromDate, toDate, 10),
        ]);

        if (!ignore) {
          if (salesData && salesData.length > 0) {
            setSalesMetrics(salesData);
            setBestSellers(bestSellersData || []);
            setIsLive(true);
          } else {
            setSalesMetrics([]);
            setBestSellers(bestSellersData || []);
            setIsLive(true);
          }
        }
      } catch {
        if (!ignore) {
          setIsLive(false);
          setErrorMsg("Không thể kết nối Engagement API. Đang hiển thị dữ liệu mô phỏng.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      ignore = true;
    };
  }, [fromDate, toDate, refreshTrigger]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setRefreshTrigger((prev) => prev + 1);
  };

  // Tính toán số liệu thống kê
  const hasRealSales = isLive && salesMetrics.length > 0;
  const displaySales = hasRealSales
    ? salesMetrics.map((item) => ({
        date: item.metricDate,
        revenue: item.netRevenueVnd,
        orders: item.completedOrderCount,
      }))
    : mockSalesMetrics;

  const hasRealBestSellers = isLive && bestSellers.length > 0;
  const displayBestSellers = hasRealBestSellers
    ? bestSellers.map((item) => ({
        product: `Sản phẩm #${item.productId.slice(0, 8)}`,
        variant: item.variantId ? `Biến thể #${item.variantId.slice(0, 6)}` : "Mặc định",
        quantity: item.quantitySold,
        revenue: item.netItemSalesVnd || item.grossSalesVnd,
      }))
    : mockBestSellers;

  const totalRevenue = displaySales.reduce((sum, item) => sum + item.revenue, 0);
  const totalOrders = displaySales.reduce((sum, item) => sum + item.orders, 0);
  const topProduct = displayBestSellers[0]?.product || "Chưa có";
  const maxRevenue = Math.max(...displaySales.map((item) => item.revenue), 1);

  return (
    <div className="space-y-7">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Theo dõi doanh thu P0 từ các đơn hàng đã hoàn tất và xếp hạng sản phẩm bán chạy nhất."
          eyebrow="Báo cáo quản trị"
          title="Doanh thu & Bán chạy"
        />

        {/* Trạng thái dữ liệu Live/Demo */}
        <div className="flex items-center gap-2">
          {isLive ? (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-800 shadow-sm">
              <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
              API Trực tiếp
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800 shadow-sm">
              <AlertCircle className="size-3.5" />
              Dữ liệu mô phỏng
            </span>
          )}
        </div>
      </div>

      {/* Bộ lọc khoảng ngày */}
      <SurfacePanel className="p-4 sm:p-5">
        <form className="flex flex-wrap items-end gap-3" onSubmit={handleFilterSubmit}>
          <div className="flex-1 min-w-[140px] space-y-1">
            <label className="text-xs font-bold text-slate-600 flex items-center gap-1" htmlFor="from-date">
              <Calendar className="size-3.5 text-brand" /> Từ ngày
            </label>
            <input
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-900 outline-none focus:border-brand focus:ring-2 focus:ring-brand/20"
              id="from-date"
              max={toDate}
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>

          <div className="flex-1 min-w-[140px] space-y-1">
            <label className="text-xs font-bold text-slate-600 flex items-center gap-1" htmlFor="to-date">
              <Calendar className="size-3.5 text-brand" /> Đến ngày
            </label>
            <input
              className="w-full rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-900 outline-none focus:border-brand focus:ring-2 focus:ring-brand/20"
              id="to-date"
              min={fromDate}
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>

          <div className="flex gap-2">
            <Button disabled={loading} size="sm" type="submit">
              {loading ? (
                <RefreshCw className="size-3.5 animate-spin" />
              ) : (
                <TrendingUp className="size-3.5" />
              )}
              Lọc báo cáo
            </Button>
            <Button
              disabled={loading}
              size="sm"
              type="button"
              onClick={() => {
                setLoading(true);
                setRefreshTrigger((prev) => prev + 1);
              }}
            >
              <RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />
              Làm mới
            </Button>
          </div>
        </form>

        {errorMsg && (
          <p className="mt-3 text-xs text-amber-700 bg-amber-50/60 p-2 rounded-lg border border-amber-100">
            {errorMsg}
          </p>
        )}
      </SurfacePanel>

      {/* Thẻ tổng hợp 3 chỉ số chính */}
      <div className="grid gap-4 md:grid-cols-3">
        <SurfacePanel className="relative overflow-hidden border-emerald-100 bg-gradient-to-br from-white to-emerald-50/30">
          <div className="flex items-center justify-between">
            <span className="grid size-10 place-items-center rounded-xl bg-emerald-100 text-emerald-800">
              <ChartNoAxesCombined className="size-5" />
            </span>
            <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider">Doanh thu chốt</span>
          </div>
          <p className="mt-4 text-xs font-medium text-slate-500">Tổng doanh thu ({displaySales.length} ngày)</p>
          <p className="mt-1 text-2xl font-black text-slate-950 tracking-tight">{money(totalRevenue)}</p>
        </SurfacePanel>

        <SurfacePanel className="relative overflow-hidden border-blue-100 bg-gradient-to-br from-white to-blue-50/30">
          <div className="flex items-center justify-between">
            <span className="grid size-10 place-items-center rounded-xl bg-blue-100 text-blue-800">
              <ShoppingBag className="size-5" />
            </span>
            <span className="text-xs font-bold text-blue-700 uppercase tracking-wider">Đơn thành công</span>
          </div>
          <p className="mt-4 text-xs font-medium text-slate-500">Đơn hàng hoàn tất</p>
          <p className="mt-1 text-2xl font-black text-slate-950 tracking-tight">{totalOrders} đơn</p>
        </SurfacePanel>

        <SurfacePanel className="relative overflow-hidden border-amber-100 bg-gradient-to-br from-white to-amber-50/30">
          <div className="flex items-center justify-between">
            <span className="grid size-10 place-items-center rounded-xl bg-amber-100 text-amber-800">
              <Trophy className="size-5" />
            </span>
            <span className="text-xs font-bold text-amber-700 uppercase tracking-wider">Top bán chạy</span>
          </div>
          <p className="mt-4 text-xs font-medium text-slate-500">Sản phẩm dẫn đầu</p>
          <p className="mt-1 text-lg font-black text-slate-950 truncate">{topProduct}</p>
        </SurfacePanel>
      </div>

      {/* Chi tiết 2 bảng: Doanh thu theo ngày & Sản phẩm bán chạy */}
      <div className="grid gap-5 lg:grid-cols-[1.2fr_.8fr]">
        <SurfacePanel>
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div>
              <h2 className="font-black text-slate-950">Doanh thu theo ngày</h2>
              <p className="text-xs text-slate-500">Dữ liệu tổng hợp từ các đơn hàng hoàn tất</p>
            </div>
            <span className="text-xs font-bold text-slate-400">{displaySales.length} mốc ngày</span>
          </div>

          <div className="mt-4 space-y-3">
            {displaySales.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-400">Không có dữ liệu trong khoảng ngày đã chọn.</p>
            ) : (
              displaySales.map((item) => {
                const ratio = Math.max(8, Math.round((item.revenue / maxRevenue) * 100));
                return (
                  <div
                    className="grid grid-cols-[105px_1fr_125px] items-center gap-3 rounded-lg p-2 transition hover:bg-slate-50"
                    key={item.date}
                  >
                    <div>
                      <span className="font-semibold text-xs sm:text-sm text-slate-900">{item.date}</span>
                      <span className="block text-[11px] text-slate-400">{item.orders} đơn hoàn tất</span>
                    </div>

                    <div className="h-3 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-emerald-600 to-teal-500 transition-all duration-500"
                        style={{ width: `${ratio}%` }}
                      />
                    </div>

                    <span className="text-right text-xs sm:text-sm font-bold text-slate-950">
                      {money(item.revenue)}
                    </span>
                  </div>
                );
              })
            )}
          </div>
        </SurfacePanel>

        <SurfacePanel>
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div>
              <h2 className="font-black text-slate-950">Top sản phẩm bán chạy</h2>
              <p className="text-xs text-slate-500">Xếp hạng theo số lượng đã hoàn tất</p>
            </div>
            <span className="text-xs font-bold text-slate-400">Top {displayBestSellers.length}</span>
          </div>

          <div className="mt-4 space-y-3">
            {displayBestSellers.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-400">Chưa có dữ liệu bán chạy.</p>
            ) : (
              displayBestSellers.map((item, index) => (
                <div
                  className="flex items-center justify-between rounded-xl border border-slate-100 bg-slate-50/50 p-3.5 transition hover:bg-slate-50"
                  key={`${item.product}-${index}`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <span
                      className={`grid size-7 shrink-0 place-items-center rounded-lg text-xs font-black ${
                        index === 0
                          ? "bg-amber-100 text-amber-800"
                          : index === 1
                          ? "bg-slate-200 text-slate-800"
                          : index === 2
                          ? "bg-amber-50 text-amber-700"
                          : "bg-white text-slate-500 border border-slate-200"
                      }`}
                    >
                      {index + 1}
                    </span>
                    <div className="min-w-0">
                      <p className="font-bold text-sm text-slate-950 truncate">{item.product}</p>
                      <p className="text-xs text-slate-500">{item.variant} · {money(item.revenue)}</p>
                    </div>
                  </div>

                  <span className="ml-3 inline-flex shrink-0 items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-black text-emerald-800 border border-emerald-100">
                    <Package className="size-3" />
                    {item.quantity}
                  </span>
                </div>
              ))
            )}
          </div>
        </SurfacePanel>
      </div>
    </div>
  );
}
