"use client";

import { TicketPercent } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { customerApi } from "../api/customer.api";
import type { VoucherWalletItem } from "../types/customer.types";

export function VoucherWalletPage() {
  const [items, setItems] = useState<VoucherWalletItem[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(false);
  useEffect(() => { customerApi.vouchers().then(setItems).catch(() => setError(true)).finally(() => setLoading(false)); }, []);
  return <div className="space-y-7"><PageHeader eyebrow="Ưu đãi" title="Ví mã giảm giá" description="Điều kiện và số tiền giảm cuối cùng luôn được máy chủ kiểm tra lại khi tạo đơn." />
    {loading ? <SurfacePanel>Đang tải voucher…</SurfacePanel> : error ? <SurfacePanel className="text-red-600">Không thể tải ví voucher.</SurfacePanel> : items.length === 0 ? <SurfacePanel>Ví voucher hiện đang trống.</SurfacePanel> :
      <div className="grid gap-4 lg:grid-cols-2">{items.map((voucher) => <SurfacePanel key={voucher.id} className={!voucher.eligible ? "opacity-70" : ""}><div className="flex items-start gap-4"><span className="rounded-2xl bg-amber-50 p-3 text-amber-700"><TicketPercent /></span><div className="min-w-0 flex-1"><div className="flex flex-wrap gap-2"><code className="font-black text-slate-950">{voucher.code}</code><StatusBadge label={voucher.eligible ? "Có thể dùng" : "Chưa đủ điều kiện"} tone={voucher.eligible ? "success" : "warning"} /></div><h2 className="mt-2 font-bold">{voucher.name}</h2><p className="mt-1 text-sm text-slate-600">{voucher.description}</p><p className="mt-3 text-xs text-slate-500">Hạn dùng {new Date(voucher.endsAt).toLocaleString("vi-VN")} · {voucher.distributionMode.replaceAll("_", " ")}</p>{voucher.ineligibleReason ? <p className="mt-2 text-sm font-medium text-amber-700">{voucher.ineligibleReason}</p> : null}</div></div></SurfacePanel>)}</div>}
  </div>;
}
