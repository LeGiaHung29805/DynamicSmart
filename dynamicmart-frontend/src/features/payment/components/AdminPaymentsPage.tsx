"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ErrorState, LoadingState } from "@/components/common/PageState";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { paymentMethodLabel, paymentTimingLabel } from "@/features/order/utils/order-format";
import {
  adminPaymentsApi,
  type AdminPayment,
  type CallbackAudit,
  type PaymentAttempt,
  type PaymentPage,
} from "../api/admin-payments.api";
import {
  callbackResultLabel,
  paymentAttemptStatusLabel,
  paymentStatusLabel,
  paymentStatusTone,
} from "../utils/payment-format";

const money = (value: number) => new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
}).format(value);

const dateTime = (value?: string) => value ? new Date(value).toLocaleString("vi-VN") : "—";

export function AdminPaymentsPage() {
  const router = useRouter();
  const [data, setData] = useState<PaymentPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<AdminPayment | null>(null);
  const [attempts] = useState<PaymentAttempt[]>([]);
  const [audits] = useState<CallbackAudit[]>([]);
  const detailLoading = false;
  const detailError: string | null = null;
  const detailRequest = useRef(0);

  useEffect(() => {
    void load(page);
  }, [page]);

  async function load(nextPage: number) {
    setLoading(true);
    setError(null);
    try {
      setData(await adminPaymentsApi.list(nextPage));
    } catch {
      setError("Không thể tải dữ liệu thanh toán. Hãy kiểm tra quyền quản trị viên và kết nối cổng API.");
    } finally {
      setLoading(false);
    }
  }

  function inspect(payment: AdminPayment) {
    router.push(`/admin/payments/${payment.id}`);
  }

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Vận hành thanh toán"
        title="Thanh toán"
        description="Theo dõi COD, các lần mở cổng thanh toán và thông báo đối soát đã lọc dữ liệu nhạy cảm."
      />

      <div className={selected ? "grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(380px,.8fr)]" : ""}>
      <div>
      {loading ? (
        <LoadingState title="Đang tải thanh toán" />
      ) : error ? (
        <ErrorState description={error} onAction={() => void load(page)} />
      ) : (
        <SurfacePanel>
          <div className="mb-5 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-black text-slate-950">Danh sách thanh toán</h2>
              <p className="mt-1 text-sm text-slate-500">{data?.totalElements ?? 0} khoản thanh toán</p>
            </div>
            <StatusBadge label="Dữ liệu từ máy chủ" tone="success" />
          </div>

          <div className="overflow-x-auto rounded-2xl border border-slate-200">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-bold tracking-wide text-slate-500 uppercase">
                <tr>
                  <th className="px-5 py-4">Mã đơn</th>
                  <th className="px-5 py-4">Phương thức</th>
                  <th className="px-5 py-4">Số tiền</th>
                  <th className="px-5 py-4">Trạng thái</th>
                  <th className="px-5 py-4">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.length ? data.content.map((payment) => (
                  <tr className="border-t border-slate-100" key={payment.id}>
                    <td className="px-5 py-4 font-mono text-xs">{payment.orderId}</td>
                    <td className="px-5 py-4">
                      <span className="block font-semibold">{paymentMethodLabel(payment.method)}</span>
                      <span className="mt-1 block text-xs text-slate-500">{paymentTimingLabel(payment.timing)}</span>
                    </td>
                    <td className="px-5 py-4 font-semibold">{money(payment.amountVnd)}</td>
                    <td className="px-5 py-4">
                      <StatusBadge label={paymentStatusLabel(payment.status)} tone={paymentStatusTone(payment.status)} />
                    </td>
                    <td className="px-5 py-4">
                      <Button variant="outline" onClick={() => void inspect(payment)}>Chi tiết</Button>
                    </td>
                  </tr>
                )) : (
                  <tr><td className="px-5 py-12 text-center text-slate-500" colSpan={5}>Chưa có khoản thanh toán.</td></tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="mt-5 flex items-center justify-end gap-3">
            <Button variant="outline" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Trang trước</Button>
            <span className="text-sm text-slate-500">Trang {page + 1}/{Math.max(data?.totalPages ?? 1, 1)}</span>
            <Button variant="outline" disabled={!data || page + 1 >= data.totalPages} onClick={() => setPage((value) => value + 1)}>Trang sau</Button>
          </div>
        </SurfacePanel>
      )}

      </div>
      {selected ? (
        <div className="xl:sticky xl:top-24">
        <PaymentDetail
          payment={selected}
          attempts={attempts}
          audits={audits}
          loading={detailLoading}
          error={detailError}
          retry={() => void inspect(selected)}
          close={() => {
            detailRequest.current += 1;
            setSelected(null);
          }}
        />
        </div>
      ) : null}
      </div>
    </div>
  );
}

function PaymentDetail({ payment, attempts, audits, loading, error, retry, close }: Readonly<{
  payment: AdminPayment;
  attempts: PaymentAttempt[];
  audits: CallbackAudit[];
  loading: boolean;
  error: string | null;
  retry: () => void;
  close: () => void;
}>) {
  return (
    <SurfacePanel>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-black tracking-wider text-brand uppercase">Chi tiết thanh toán</p>
          <h2 className="mt-1 break-all font-mono text-sm font-bold text-slate-950">{payment.id}</h2>
        </div>
        <Button variant="outline" onClick={close}>Đóng</Button>
      </div>

      {loading ? (
        <div className="mt-5"><LoadingState title="Đang tải lịch sử" /></div>
      ) : error ? (
        <div className="mt-5"><ErrorState description={error} onAction={retry} /></div>
      ) : (
        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <section>
            <h3 className="font-bold">Khoản thanh toán</h3>
            <dl className="mt-3 grid grid-cols-2 gap-3 text-sm">
              <dt className="text-slate-500">Trạng thái</dt>
              <dd><StatusBadge label={paymentStatusLabel(payment.status)} tone={paymentStatusTone(payment.status)} /></dd>
              <dt className="text-slate-500">Phương thức</dt><dd>{paymentMethodLabel(payment.method)}</dd>
              <dt className="text-slate-500">Thời điểm</dt><dd>{paymentTimingLabel(payment.timing)}</dd>
              <dt className="text-slate-500">Số tiền</dt><dd>{money(payment.amountVnd)}</dd>
              <dt className="text-slate-500">Đã thu</dt><dd>{dateTime(payment.paidAt)}</dd>
              <dt className="text-slate-500">Xác nhận COD</dt><dd>{payment.method === "COD" ? "Khách bấm Đã nhận hàng" : "—"}</dd>
            </dl>
            {payment.timing === "PREPAID" && (payment.status === "FAILED" || payment.status === "EXPIRED") ? <section className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950"><p className="font-black">Quy trình đối soát khi khách báo đã bị trừ tiền</p><ol className="mt-2 list-decimal space-y-1.5 pl-5 text-xs leading-5"><li>So khớp mã tham chiếu ở lần thanh toán với dashboard của nhà cung cấp.</li><li>Kiểm tra callback audit: chữ ký và số tiền phải hợp lệ trước khi kết luận.</li><li>Nếu nhà cung cấp không ghi nhận thành công: thông báo khách đặt lại đơn; đơn cũ đã được hủy và Saga hoàn giữ chỗ.</li><li>Nếu nhà cung cấp xác nhận đã thu tiền: không tự ý chuyển đơn hủy sang giao hàng; lưu biên bản đối soát và hoàn tiền thủ công theo quy trình nội bộ.</li></ol></section> : null}
          </section>
          <section className="space-y-6">
            <AuditTable
              title="Các lần thanh toán"
              empty="Chưa có lần thanh toán nào."
              rows={attempts.map((item) => [String(item.attemptNo), paymentMethodLabel(item.provider), paymentAttemptStatusLabel(item.status), dateTime(item.createdAt)])}
            />
            <AuditTable
              title="Kiểm tra thông báo từ cổng thanh toán"
              empty="Chưa có thông báo nào."
              rows={audits.map((item) => [callbackResultLabel(item.processedResult), item.checksumValid ? "Chữ ký hợp lệ" : "Chữ ký không hợp lệ", item.amountValid ? "Số tiền khớp" : "Số tiền không khớp", dateTime(item.receivedAt)])}
            />
          </section>
        </div>
      )}
    </SurfacePanel>
  );
}

function AuditTable({ title, empty, rows }: Readonly<{ title: string; empty: string; rows: string[][] }>) {
  return (
    <div>
      <h3 className="font-bold">{title}</h3>
      <div className="mt-3 overflow-x-auto rounded-xl border border-slate-200">
        <table className="min-w-full text-left text-xs">
          <tbody>
            {rows.length ? rows.map((row, index) => (
              <tr className="border-t border-slate-100 first:border-0" key={`${title}-${index}`}>
                {row.map((cell, cellIndex) => <td className="px-3 py-2" key={`${title}-${index}-${cellIndex}`}>{cell}</td>)}
              </tr>
            )) : (
              <tr><td className="px-3 py-4 text-slate-500">{empty}</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
