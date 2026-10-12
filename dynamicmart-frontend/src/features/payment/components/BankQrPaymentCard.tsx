"use client";

/* eslint-disable @next/next/no-img-element -- The configured VietQR image is returned by Payment Service. */

import Link from "next/link";
import { Clock3, Landmark } from "lucide-react";
import { Button } from "@/components/ui/Button";

type BankQrPaymentCardProps = {
  orderId: string;
  orderNumber: string;
  qrUrl: string;
  expiresAt?: string;
};

export function BankQrPaymentCard({ orderId, orderNumber, qrUrl, expiresAt }: Readonly<BankQrPaymentCardProps>) {
  return (
    <section className="mx-auto max-w-md rounded-3xl border border-emerald-200 bg-emerald-50/70 p-5 text-center shadow-sm sm:p-7">
      <span className="mx-auto grid size-11 place-items-center rounded-2xl bg-emerald-700 text-white"><Landmark className="size-5" /></span>
      <p className="mt-4 text-xs font-black tracking-[0.16em] text-emerald-700 uppercase">Chuyển khoản QR</p>
      <h2 className="mt-1 text-xl font-black text-emerald-950">Quét mã trong ứng dụng ngân hàng</h2>
      <p className="mt-2 text-sm leading-6 text-emerald-900/80">Mã thanh toán được giữ ngay trong trang này. Không cần mở ảnh QR ở một trang khác.</p>
      <div className="mx-auto mt-5 grid w-fit place-items-center rounded-2xl border border-emerald-200 bg-white p-3 shadow-sm">
        <img alt={`Mã QR thanh toán đơn ${orderNumber}`} className="size-56 max-w-full rounded-xl object-contain sm:size-64" src={qrUrl} />
      </div>
      {expiresAt ? <p className="mt-4 inline-flex items-center gap-1.5 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-emerald-900"><Clock3 className="size-3.5" />Có hiệu lực đến {new Date(expiresAt).toLocaleString("vi-VN")}</p> : null}
      <p className="mt-4 text-xs leading-5 text-emerald-900/75">Sau khi chuyển khoản, hệ thống tự đối soát giao dịch. Không cần tải ảnh biên nhận.</p>
      <Link className="mt-5 inline-flex" href={`/customer/account/orders/${orderId}`}><Button variant="outline">Theo dõi trạng thái đơn</Button></Link>
    </section>
  );
}