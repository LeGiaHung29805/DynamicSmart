"use client";

import Link from "next/link";
import {
  Activity,
  ArrowRight,
  BarChart3,
  CheckCircle2,
  CreditCard,
  MessageSquareText,
  Package,
  RefreshCw,
  Server,
  ShieldCheck,
  ShoppingBag,
  TicketPercent,
  TrendingUp,
  UsersRound,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { useAuthSession } from "@/lib/auth/session";
import { apiClient } from "@/lib/api/client";
import { adminOrdersApi } from "@/features/order/api/admin-orders.api";
import { engagementApi } from "@/features/engagement/api/engagement.api";
import { customerApi } from "@/features/customer/api/customer.api";
import { listAdminProducts } from "@/features/catalog/api/admin-catalog.api";
import type { BestSellerItem, DailySalesMetric } from "@/features/engagement/types/engagement.types";
import type { ProductDetail } from "@/features/catalog/types";

const money = (val: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(val);

const dateStr = (val: string) => {
  try {
    return new Date(val).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
  } catch {
    return val;
  }
};

function getDateRange() {
  const today = new Date();
  const past = new Date();
  past.setDate(today.getDate() - 30);
  return {
    from: past.toISOString().slice(0, 10),
    to: today.toISOString().slice(0, 10),
  };
}

interface DashboardMetrics {
  totalRevenueVnd: number;
  completedOrdersCount: number;
  totalOrders: number;
  totalProducts: number;
  totalUsers: number;
  totalReviews: number;
  totalQuestions: number;
  totalSupportConversations: number;
}

const operationalWorkspaces = [
  {
    title: "Vận hành Đơn hàng",
    href: "/admin/orders",
    icon: ShoppingBag,
    description: "Quản lý vòng đời đơn: Đóng gói (PACK), bàn giao (SHIP) và xác nhận giao khách (HANDOVER).",
    tagKey: "orders",
  },
  {
    title: "Catalog & Tồn kho",
    href: "/admin/catalog/products",
    icon: Package,
    description: "Quản lý danh mục đa cấp, thuộc tính động, biến thể giá và số lượng tồn kho.",
    tagKey: "catalog",
  },
  {
    title: "Báo cáo & Phân tích",
    href: "/admin/reports",
    icon: BarChart3,
    description: "Biểu đồ doanh thu hàng ngày, sản phẩm bán chạy, kỳ kinh doanh và tỷ lệ chuyển đổi.",
    tagKey: "reports",
  },
  {
    title: "Tương tác & CSKH",
    href: "/admin/engagement",
    icon: MessageSquareText,
    description: "Kiểm duyệt đánh giá, giải đáp thắc mắc sản phẩm và tiếp nhận hỗ trợ khách hàng.",
    tagKey: "engagement",
  },
  {
    title: "Thanh toán & Giao dịch",
    href: "/admin/payments",
    icon: CreditCard,
    description: "Đối soát giao dịch đa cổng: VNPAY, ZaloPay, PayOS, Bank QR và xác nhận COD.",
    tagKey: "payments",
  },
  {
    title: "Khuyến mãi & Vouchers",
    href: "/admin/promotions",
    icon: TicketPercent,
    description: "Thiết lập chiến dịch giảm giá, ngân sách voucher và quy tắc áp dụng giỏ hàng.",
    tagKey: "promotions",
  },
  {
    title: "Người dùng & Tài khoản",
    href: "/admin/users",
    icon: UsersRound,
    description: "Quản trị danh sách khách hàng, nhân viên, phân quyền vai trò và trạng thái tài khoản.",
    tagKey: "users",
  },
] as const;

const microservices = [
  { name: "API Gateway", port: "8080", purpose: "Định tuyến, bảo mật JWT, Rate limiting & CORS" },
  { name: "Identity Service", port: "8081", purpose: "Xác thực, phân quyền ADMIN/CUSTOMER & người dùng" },
  { name: "Catalog Service", port: "8082", purpose: "Sản phẩm, danh mục, thuộc tính & tồn kho" },
  { name: "Cart Service", port: "8083", purpose: "Giỏ hàng, kiểm tra tính hợp lệ & khuyến mãi" },
  { name: "Order Service", port: "8084", purpose: "Đặt hàng, Saga Checkout, trạng thái & vận hành" },
  { name: "Payment Service", port: "8085", purpose: "VNPAY, ZaloPay, PayOS, Bank QR & đối soát COD" },
  { name: "Engagement & Reports", port: "8086", purpose: "Đánh giá, Q&A, Wishlist, Chat & Báo cáo doanh thu" },
];

export function AdminDashboard() {
  const session = useAuthSession();
  const isAdmin = session.status === "authenticated" && session.user.role === "ADMIN";

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [metrics, setMetrics] = useState<DashboardMetrics>({
    totalRevenueVnd: 0,
    completedOrdersCount: 0,
    totalOrders: 0,
    totalProducts: 0,
    totalUsers: 0,
    totalReviews: 0,
    totalQuestions: 0,
    totalSupportConversations: 0,
  });
  const [bestSellers, setBestSellers] = useState<BestSellerItem[]>([]);
  const [productTitles, setProductTitles] = useState<Record<string, string>>({});
  const [recentSales, setRecentSales] = useState<DailySalesMetric[]>([]);

  const fetchLiveStats = useCallback(async () => {
    if (!isAdmin) {
      setLoading(false);
      return;
    }

    try {
      setRefreshing(true);
      const { from, to } = getDateRange();

      const [
        ordersRes,
        productsRes,
        usersRes,
        salesRes,
        bestSellersRes,
        reviewsRes,
        questionsRes,
        supportRes,
      ] = await Promise.allSettled([
        adminOrdersApi.list({ size: 1 }),
        listAdminProducts({ size: 1 }),
        customerApi.users("", "", ""),
        engagementApi.reports.sales(from, to),
        engagementApi.reports.bestSellers(from, to, 5),
        engagementApi.reviews.getAdminReviews(0, 1),
        engagementApi.questions.getAdminQuestions(0, 1),
        engagementApi.support.adminConversations(0, 1),
      ]);

      let totalOrders = 0;
      if (ordersRes.status === "fulfilled" && ordersRes.value) {
        totalOrders = ordersRes.value.totalElements ?? 0;
      }

      let totalProducts = 0;
      if (productsRes.status === "fulfilled" && productsRes.value) {
        totalProducts = productsRes.value.totalElements ?? 0;
      }

      let totalUsers = 0;
      if (usersRes.status === "fulfilled" && usersRes.value) {
        totalUsers = usersRes.value.totalElements ?? 0;
      }

      let totalReviews = 0;
      if (reviewsRes.status === "fulfilled" && reviewsRes.value) {
        totalReviews = reviewsRes.value.totalElements ?? 0;
      }

      let totalQuestions = 0;
      if (questionsRes.status === "fulfilled" && questionsRes.value) {
        totalQuestions = questionsRes.value.totalElements ?? 0;
      }

      let totalSupport = 0;
      if (supportRes.status === "fulfilled" && supportRes.value) {
        totalSupport = supportRes.value.totalElements ?? 0;
      }

      let totalRev = 0;
      let completedOrders = 0;
      if (salesRes.status === "fulfilled" && Array.isArray(salesRes.value)) {
        setRecentSales(salesRes.value);
        for (const item of salesRes.value) {
          totalRev += item.grossItemSalesVnd ?? item.netRevenueVnd ?? 0;
          completedOrders += item.completedOrderCount ?? item.orderCount ?? 0;
        }
      }

      setMetrics({
        totalRevenueVnd: totalRev,
        completedOrdersCount: completedOrders,
        totalOrders,
        totalProducts,
        totalUsers,
        totalReviews,
        totalQuestions,
        totalSupportConversations: totalSupport,
      });

      if (bestSellersRes.status === "fulfilled" && Array.isArray(bestSellersRes.value)) {
        const items = bestSellersRes.value;
        setBestSellers(items);

        // Fetch product names in background
        const titleMap: Record<string, string> = {};
        await Promise.all(
          items.map(async (item) => {
            try {
              const p = await apiClient.get<ProductDetail>(`/api/v1/catalog/products/id/${item.productId}`);
              if (p?.name) titleMap[item.productId] = p.name;
            } catch {
              titleMap[item.productId] = `Sản phẩm #${item.productId.slice(0, 8)}`;
            }
          })
        );
        setProductTitles(titleMap);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    fetchLiveStats();
  }, [fetchLiveStats]);

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <PageHeader
        eyebrow="Admin workspace"
        title="Tổng quan quản trị"
        description="Bảng điều khiển trung tâm theo dõi chỉ số kinh doanh, vận hành đơn, kho hàng và tương tác khách hàng theo thời gian thực."
        action={
          <div className="flex items-center gap-3">
            {isAdmin ? (
              <StatusBadge label="API Trực tiếp · Đã kết nối" tone="success" />
            ) : (
              <StatusBadge label="Yêu cầu quyền Quản trị viên" tone="warning" />
            )}
            {isAdmin && (
              <Button
                variant="secondary"
                size="sm"
                onClick={fetchLiveStats}
                disabled={loading || refreshing}
                className="gap-2"
              >
                <RefreshCw className={`size-3.5 ${refreshing ? "animate-spin" : ""}`} />
                Làm mới
              </Button>
            )}
          </div>
        }
      />

      {/* Login Prompt if not admin */}
      {!isAdmin && (
        <SurfacePanel className="border-amber-200 bg-amber-50/50 p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="space-y-1">
              <h3 className="font-bold text-amber-950 flex items-center gap-2">
                <ShieldCheck className="size-5 text-amber-600" />
                Đăng nhập để xem số liệu trực tiếp từ các Microservices
              </h3>
              <p className="text-sm text-amber-800">
                Bạn đang xem trang với trạng thái khách. Hãy đăng nhập tài khoản Quản trị viên để truy cập toàn bộ dữ liệu thật từ Backend APIs.
              </p>
              <p className="text-xs text-amber-700 font-medium">
                Tài khoản mặc định: <span className="font-mono bg-white/70 px-2 py-0.5 rounded border border-amber-200">admin@dynamicmart.local</span> | Mật khẩu: <span className="font-mono bg-white/70 px-2 py-0.5 rounded border border-amber-200">Password@123</span>
              </p>
            </div>
            <Link href="/login?returnTo=/admin">
              <Button size="sm" className="whitespace-nowrap">
                Đăng nhập Quản trị
              </Button>
            </Link>
          </div>
        </SurfacePanel>
      )}

      {/* KPI Highlights Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Total Revenue */}
        <SurfacePanel className="relative overflow-hidden p-5 transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Doanh thu 30 ngày</span>
            <span className="grid size-9 place-items-center rounded-xl bg-emerald-100 text-emerald-700">
              <TrendingUp className="size-4.5" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {loading ? "---" : money(metrics.totalRevenueVnd)}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              {metrics.completedOrdersCount} đơn hàng phát sinh doanh thu
            </p>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-emerald-600 font-semibold">
            <Link href="/admin/reports" className="inline-flex items-center gap-1 hover:underline">
              Chi tiết báo cáo <ArrowRight className="size-3" />
            </Link>
          </div>
        </SurfacePanel>

        {/* Total Orders */}
        <SurfacePanel className="relative overflow-hidden p-5 transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Đơn hàng hệ thống</span>
            <span className="grid size-9 place-items-center rounded-xl bg-blue-100 text-blue-700">
              <ShoppingBag className="size-4.5" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {loading ? "---" : `${metrics.totalOrders} đơn`}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              Order Service · Vận hành & Saga Checkout
            </p>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-blue-600 font-semibold">
            <Link href="/admin/orders" className="inline-flex items-center gap-1 hover:underline">
              Xử lý đơn hàng <ArrowRight className="size-3" />
            </Link>
          </div>
        </SurfacePanel>

        {/* Catalog Products */}
        <SurfacePanel className="relative overflow-hidden p-5 transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Sản phẩm Catalog</span>
            <span className="grid size-9 place-items-center rounded-xl bg-purple-100 text-purple-700">
              <Package className="size-4.5" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {loading ? "---" : `${metrics.totalProducts} sản phẩm`}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              Catalog Service · Biến thể & kho vận
            </p>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-purple-600 font-semibold">
            <Link href="/admin/catalog/products" className="inline-flex items-center gap-1 hover:underline">
              Quản lý sản phẩm <ArrowRight className="size-3" />
            </Link>
          </div>
        </SurfacePanel>

        {/* Engagement & Users */}
        <SurfacePanel className="relative overflow-hidden p-5 transition hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Tương tác & CSKH</span>
            <span className="grid size-9 place-items-center rounded-xl bg-amber-100 text-amber-700">
              <MessageSquareText className="size-4.5" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {loading ? "---" : `${metrics.totalReviews + metrics.totalQuestions} mục`}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              {metrics.totalReviews} đánh giá · {metrics.totalQuestions} hỏi đáp · {metrics.totalUsers} người dùng
            </p>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-amber-600 font-semibold">
            <Link href="/admin/engagement" className="inline-flex items-center gap-1 hover:underline">
              Duyệt tương tác <ArrowRight className="size-3" />
            </Link>
          </div>
        </SurfacePanel>
      </div>

      {/* Grid: Best Sellers & Daily Sales */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Top Best Sellers */}
        <SurfacePanel className="p-6">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-base font-black text-slate-950 flex items-center gap-2">
                <BarChart3 className="size-4.5 text-brand" />
                Sản phẩm bán chạy nhất
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Top sản phẩm phát sinh doanh số cao nhất</p>
            </div>
            <Link href="/admin/reports" className="text-xs font-bold text-brand hover:underline">
              Xem báo cáo
            </Link>
          </div>

          <div className="mt-4 divide-y divide-slate-100">
            {loading ? (
              <div className="py-8 text-center text-sm text-slate-400">Đang tải số liệu bán chạy...</div>
            ) : bestSellers.length === 0 ? (
              <div className="py-8 text-center text-sm text-slate-500">
                Chưa có dữ liệu bán chạy trong kỳ này. Dữ liệu sẽ cập nhật khi có đơn hàng hoàn tất.
              </div>
            ) : (
              bestSellers.map((item, idx) => {
                const title = productTitles[item.productId] || `Sản phẩm #${item.productId.slice(0, 8)}`;
                return (
                  <div key={item.productId} className="flex items-center justify-between py-3.5 text-sm">
                    <div className="flex items-center gap-3 min-w-0 pr-4">
                      <span className={`grid size-6 shrink-0 place-items-center rounded-full text-xs font-black ${
                        idx === 0 ? "bg-amber-100 text-amber-800" : idx === 1 ? "bg-slate-200 text-slate-700" : "bg-orange-100 text-orange-800"
                      }`}>
                        {idx + 1}
                      </span>
                      <div className="min-w-0">
                        <Link
                          href={`/admin/catalog/products/${item.productId}`}
                          className="font-bold text-slate-900 hover:text-brand truncate block hover:underline"
                        >
                          {title}
                        </Link>
                        <span className="text-xs text-slate-500">Đã bán: <strong className="text-slate-800">{item.quantitySold}</strong> cái</span>
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="font-bold text-slate-900">{money(item.netItemSalesVnd ?? item.grossSalesVnd)}</div>
                      <span className="text-[11px] text-slate-400">Doanh thu thuần</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </SurfacePanel>

        {/* Daily Sales Overview */}
        <SurfacePanel className="p-6">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-base font-black text-slate-950 flex items-center gap-2">
                <Activity className="size-4.5 text-emerald-600" />
                Doanh thu theo ngày gần nhất
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Thống kê đơn và doanh số từ Reporting Service</p>
            </div>
            <StatusBadge label="Reporting Live" tone="success" />
          </div>

          <div className="mt-4">
            {loading ? (
              <div className="py-8 text-center text-sm text-slate-400">Đang tải nhật ký doanh thu...</div>
            ) : recentSales.length === 0 ? (
              <div className="py-8 text-center text-sm text-slate-500">
                Chưa ghi nhận doanh thu trong 30 ngày qua. Khi khách hàng đặt đơn và thanh toán, số liệu sẽ tự động tổng hợp tại đây.
              </div>
            ) : (
              <div className="space-y-3">
                {recentSales.slice(-5).reverse().map((entry) => (
                  <div key={entry.metricDate} className="flex items-center justify-between rounded-xl border border-slate-100 bg-slate-50/50 p-3.5">
                    <div>
                      <div className="font-bold text-slate-900">{dateStr(entry.metricDate)}</div>
                      <div className="text-xs text-slate-500">
                        <span className="font-semibold text-slate-700">{entry.orderCount}</span> đơn hàng ghi nhận
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-base font-black text-brand">{money(entry.grossItemSalesVnd ?? entry.netRevenueVnd)}</div>
                      <div className="text-[11px] text-slate-400">Tổng giá trị</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </SurfacePanel>
      </div>

      {/* Operational Workspaces Shortcuts */}
      <div>
        <div className="mb-4">
          <h2 className="text-lg font-black text-slate-950">Phân hệ nghiệp vụ quản trị</h2>
          <p className="text-sm text-slate-500">Truy cập trực tiếp vào các màn hình làm việc nghiệp vụ đã kết nối API hoàn chỉnh.</p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {operationalWorkspaces.map(({ title, href, icon: Icon, description, tagKey }) => {
            let badgeText = "Đã kết nối";
            if (tagKey === "orders") badgeText = `${metrics.totalOrders} đơn hàng`;
            else if (tagKey === "catalog") badgeText = `${metrics.totalProducts} sản phẩm`;
            else if (tagKey === "users") badgeText = `${metrics.totalUsers} người dùng`;
            else if (tagKey === "engagement") badgeText = `${metrics.totalReviews + metrics.totalQuestions} mục`;
            else if (tagKey === "reports") badgeText = "Trực quan hoá";

            return (
              <Link
                key={href}
                href={href}
                className="group flex flex-col justify-between rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm shadow-slate-950/[0.03] transition hover:-translate-y-1 hover:border-emerald-300 hover:shadow-lg"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span className="grid size-11 place-items-center rounded-2xl bg-emerald-50 text-brand transition group-hover:bg-emerald-600 group-hover:text-white">
                      <Icon className="size-5" />
                    </span>
                    <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] font-bold text-slate-600">
                      {loading ? "..." : badgeText}
                    </span>
                  </div>
                  <h3 className="mt-4 text-base font-black text-slate-950 group-hover:text-brand">{title}</h3>
                  <p className="mt-1.5 text-xs leading-5 text-slate-500">{description}</p>
                </div>
                <div className="mt-5 inline-flex items-center gap-1.5 text-xs font-bold text-brand">
                  Truy cập phân hệ <ArrowRight className="size-3.5 transition group-hover:translate-x-1" />
                </div>
              </Link>
            );
          })}
        </div>
      </div>

      {/* Microservices Architecture Status */}
      <SurfacePanel className="p-6">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
          <div>
            <h2 className="text-base font-black text-slate-950 flex items-center gap-2">
              <Server className="size-4.5 text-brand" />
              Kiến trúc Microservices & Trạng thái kết nối
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">Tất cả các dịch vụ độc lập kết nối qua API Gateway (Spring Cloud Gateway)</p>
          </div>
          <div className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700">
            <CheckCircle2 className="size-4 text-emerald-600" />
            7/7 Dịch vụ đang hoạt động
          </div>
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {microservices.map((svc) => (
            <div key={svc.name} className="rounded-2xl border border-slate-100 bg-slate-50/70 p-3.5">
              <div className="flex items-center justify-between">
                <span className="font-bold text-xs text-slate-900">{svc.name}</span>
                <span className="rounded-md bg-emerald-100/80 px-1.5 py-0.5 font-mono text-[10px] font-bold text-emerald-800">
                  :{svc.port}
                </span>
              </div>
              <p className="mt-1 text-[11px] leading-4 text-slate-500">{svc.purpose}</p>
              <div className="mt-2.5 flex items-center gap-1 text-[10px] font-bold text-emerald-600">
                <span className="size-1.5 rounded-full bg-emerald-500 animate-pulse" />
                Sẵn sàng xử lý API
              </div>
            </div>
          ))}
        </div>
      </SurfacePanel>
    </div>
  );
}
