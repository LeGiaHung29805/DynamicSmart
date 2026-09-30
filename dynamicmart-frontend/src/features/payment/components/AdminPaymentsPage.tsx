"use client";

import { ErrorState, LoadingState } from "@/components/common/PageState";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useEffect, useState } from "react";
import { adminPaymentsApi, type AdminPayment, type CallbackAudit, type PaymentAttempt, type PaymentPage } from "../api/admin-payments.api";

const money = (value: number) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
const dateTime = (value?: string) => value ? new Date(value).toLocaleString("vi-VN") : "—";

export function AdminPaymentsPage() {
  const [data, setData] = useState<PaymentPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<AdminPayment | null>(null);
  const [attempts, setAttempts] = useState<PaymentAttempt[]>([]);
  const [audits, setAudits] = useState<CallbackAudit[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [receiptNo, setReceiptNo] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [commandError, setCommandError] = useState<string | null>(null);

  useEffect(() => { void load(page); }, [page]);
  async function load(nextPage: number) {
    setLoading(true); setError(null);
    try { setData(await adminPaymentsApi.list(nextPage)); } catch { setError("Không thể tải dữ liệu thanh toán. Hãy kiểm tra quyền ADMIN và kết nối Gateway."); }
    finally { setLoading(false); }
  }
  async function inspect(payment: AdminPayment) {
    setSelected(payment); setDetailLoading(true); setCommandError(null); setReceiptNo("");
    try { const [nextAttempts, nextAudits] = await Promise.all([adminPaymentsApi.attempts(payment.id), adminPaymentsApi.audits(payment.id)]); setAttempts(nextAttempts); setAudits(nextAudits); }
    catch { setCommandError("Không thể tải lịch sử attempt/callback."); }
    finally { setDetailLoading(false); }
  }
  async function confirmCod() {
    if (!selected || !receiptNo.trim()) return;
    setSubmitting(true); setCommandError(null);
    try {
      const updated = await adminPaymentsApi.confirmCod(selected.id, selected.amountVnd, receiptNo.trim());
      setSelected(updated); setData((current) => current ? { ...current, content: current.content.map((item) => item.id === updated.id ? updated : item) } : current);
      await inspect(updated);
    } catch { setCommandError("Không thể xác nhận COD. Kiểm tra biên nhận và trạng thái Payment."); }
    finally { setSubmitting(false); }
  }

  return <div className="space-y-7"><PageHeader eyebrow="Vận hành" title="Thanh toán" description="Theo dõi COD, VNPay, attempt và callback đã được lọc dữ liệu nhạy cảm." />{loading ? <LoadingState title="Đang tải thanh toán" /> : error ? <ErrorState description={error} onAction={() => void load(page)} /> : <SurfacePanel><div className="mb-5 flex items-center justify-between"><div><h2 className="text-lg font-black text-slate-950">Danh sách Payment</h2><p className="mt-1 text-sm text-slate-500">{data?.totalElements ?? 0} khoản thanh toán</p></div><StatusBadge label="Dữ liệu thật" tone="success" /></div><div className="overflow-x-auto rounded-2xl border border-slate-200"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs font-bold tracking-wide text-slate-500 uppercase"><tr><th className="px-5 py-4">Mã đơn</th><th className="px-5 py-4">Phương thức</th><th className="px-5 py-4">Số tiền</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4">Thao tác</th></tr></thead><tbody>{data?.content.map((payment) => <tr className="border-t border-slate-100" key={payment.id}><td className="px-5 py-4 font-mono text-xs">{payment.orderId}</td><td className="px-5 py-4">{payment.timing} · {payment.method}</td><td className="px-5 py-4 font-semibold">{money(payment.amountVnd)}</td><td className="px-5 py-4"><StatusBadge label={payment.status} tone={payment.status === "PAID" ? "success" : payment.status === "PENDING" ? "warning" : "danger"} /></td><td className="px-5 py-4"><Button variant="outline" onClick={() => void inspect(payment)}>Chi tiết</Button></td></tr>)}</tbody></table></div><div className="mt-5 flex items-center justify-end gap-3"><Button variant="outline" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Trang trước</Button><span className="text-sm text-slate-500">Trang {page + 1}/{Math.max(data?.totalPages ?? 1, 1)}</span><Button variant="outline" disabled={!data || page + 1 >= data.totalPages} onClick={() => setPage((value) => value + 1)}>Trang sau</Button></div></SurfacePanel>}{selected ? <PaymentDetail payment={selected} attempts={attempts} audits={audits} loading={detailLoading} receiptNo={receiptNo} setReceiptNo={setReceiptNo} submitting={submitting} error={commandError} confirmCod={confirmCod} close={() => setSelected(null)} /> : null}</div>;
}

function PaymentDetail({ payment, attempts, audits, loading, receiptNo, setReceiptNo, submitting, error, confirmCod, close }: Readonly<{ payment: AdminPayment; attempts: PaymentAttempt[]; audits: CallbackAudit[]; loading: boolean; receiptNo: string; setReceiptNo: (value: string) => void; submitting: boolean; error: string | null; confirmCod: () => Promise<void>; close: () => void }>) {
  return <SurfacePanel><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-black tracking-wider text-brand uppercase">Payment detail</p><h2 className="mt-1 font-mono text-sm font-bold text-slate-950">{payment.id}</h2></div><Button variant="outline" onClick={close}>Đóng</Button></div>{loading ? <div className="mt-5"><LoadingState title="Đang tải lịch sử" /></div> : <div className="mt-6 grid gap-6 lg:grid-cols-2"><section><h3 className="font-bold">Payment</h3><dl className="mt-3 grid grid-cols-2 gap-3 text-sm"><dt className="text-slate-500">Trạng thái</dt><dd>{payment.status}</dd><dt className="text-slate-500">Số tiền</dt><dd>{money(payment.amountVnd)}</dd><dt className="text-slate-500">Đã thu</dt><dd>{dateTime(payment.paidAt)}</dd><dt className="text-slate-500">Biên nhận COD</dt><dd>{payment.codReceiptNo ?? "—"}</dd></dl>{payment.method === "COD" && payment.status === "PENDING" ? <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4"><h4 className="font-bold text-amber-950">Xác nhận đã thu COD</h4><p className="mt-1 text-xs text-amber-900">Số tiền được khóa theo Order: {money(payment.amountVnd)}.</p><Input className="mt-3 bg-white" label="Mã biên nhận/đối soát" value={receiptNo} onChange={(event) => setReceiptNo(event.target.value)} maxLength={100} required /><Button className="mt-3" disabled={!receiptNo.trim() || submitting} onClick={() => void confirmCod()}>{submitting ? "Đang xác nhận…" : "Xác nhận thu COD"}</Button></div> : null}{error ? <p className="mt-3 text-sm text-danger">{error}</p> : null}</section><section className="space-y-6"><AuditTable title="Payment attempts" empty="Chưa có attempt." rows={attempts.map((item) => [String(item.attemptNo), item.provider, item.status, dateTime(item.createdAt)])} /><AuditTable title="Callback audits" empty="Chưa có callback." rows={audits.map((item) => [item.processedResult, item.checksumValid ? "Chữ ký đúng" : "Sai chữ ký", item.amountValid ? "Đúng tiền" : "Sai tiền", dateTime(item.receivedAt)])} /></section></div>}</SurfacePanel>;
}

function AuditTable({ title, empty, rows }: Readonly<{ title: string; empty: string; rows: string[][] }>) {
  return <div><h3 className="font-bold">{title}</h3><div className="mt-3 overflow-x-auto rounded-xl border border-slate-200"><table className="min-w-full text-left text-xs"><tbody>{rows.length ? rows.map((row, index) => <tr className="border-t border-slate-100 first:border-0" key={`${title}-${index}`}>{row.map((cell, cellIndex) => <td className="px-3 py-2" key={`${title}-${index}-${cellIndex}`}>{cell}</td>)}</tr>) : <tr><td className="px-3 py-4 text-slate-500">{empty}</td></tr>}</tbody></table></div></div>;
}
