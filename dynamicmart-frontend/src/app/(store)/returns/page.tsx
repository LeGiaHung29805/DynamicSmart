import type { Metadata } from "next";
import Link from "next/link";
import { 
  RotateCcw, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  PackageCheck, 
  CreditCard, 
  ShieldCheck, 
  HelpCircle,
  ArrowRight
} from "lucide-react";

export const metadata: Metadata = {
  title: "Chính sách Đổi trả & Hoàn tiền | DynamicMart",
  description: "Chính sách đổi trả minh bạch, đổi sản phẩm miễn phí trong 7 ngày tại DynamicMart.",
};

const steps = [
  {
    step: "01",
    title: "Gửi yêu cầu",
    desc: "Liên hệ bộ phận CSKH qua hotline 1900 8888 hoặc tạo yêu cầu hỗ trợ kèm ảnh/video chụp sản phẩm trong vòng 7 ngày kể từ ngày nhận hàng.",
    icon: RotateCcw
  },
  {
    step: "02",
    title: "Xác nhận & Đóng gói",
    desc: "Nhân viên CSKH tiếp nhận và xác thực yêu cầu. Bạn chỉ cần đóng gói sản phẩm nguyên vẹn cùng hóa đơn và phụ kiện đi kèm.",
    icon: PackageCheck
  },
  {
    step: "03",
    title: "Thu hồi tận nơi",
    desc: "Shipper của đối tác vận chuyển GHN/DynamicMart Express sẽ đến tận địa chỉ của bạn để nhận lại kiện hàng hoàn toàn miễn phí.",
    icon: Clock
  },
  {
    step: "04",
    title: "Đổi mới hoặc Hoàn tiền",
    desc: "Ngay khi kho nhận được sản phẩm kiểm định, chúng tôi sẽ gửi sản phẩm mới thay thế hoặc hoàn tiền về tài khoản của bạn trong 24-48 giờ.",
    icon: CreditCard
  }
];

export default function ReturnsPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      {/* Header Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-rose-950 p-8 text-white shadow-xl sm:p-12">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full bg-rose-500/10 px-3.5 py-1 text-xs font-semibold text-rose-400 ring-1 ring-rose-500/20">
            <RotateCcw className="size-3.5" />
            Bảo đảm quyền lợi người mua
          </div>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-white sm:text-4xl">
            Chính sách Đổi trả & Hoàn tiền
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-300 sm:text-base">
            Tại DynamicMart, sự hài lòng của bạn là ưu tiên hàng đầu. Chúng tôi cam kết hỗ trợ đổi trả sản phẩm nhanh chóng, minh bạch và hoàn toàn miễn phí trong vòng 7 ngày.
          </p>
        </div>
        <div className="pointer-events-none absolute -right-16 -top-16 size-80 rounded-full bg-rose-500/10 blur-3xl" />
      </div>

      {/* 4-Step Process */}
      <div className="mt-12">
        <div className="text-center sm:text-left">
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Quy trình đổi trả 4 bước đơn giản</h2>
          <p className="mt-1 text-sm text-slate-500">Thực hiện thuận tiện, tiết kiệm tối đa thời gian của bạn</p>
        </div>

        <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((item, idx) => {
            const Icon = item.icon;
            return (
              <div 
                key={idx} 
                className="relative flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm transition hover:border-rose-400/50 hover:shadow-md"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span className="text-2xl font-black text-rose-500/30">{item.step}</span>
                    <div className="grid size-10 place-items-center rounded-xl bg-rose-50 text-rose-600">
                      <Icon className="size-5" />
                    </div>
                  </div>
                  <h3 className="mt-4 text-base font-bold text-slate-900">{item.title}</h3>
                  <p className="mt-2 text-xs leading-5 text-slate-600">{item.desc}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Conditions Grid */}
      <div className="mt-14 grid gap-8 lg:grid-cols-2">
        {/* Applicable conditions */}
        <div className="rounded-3xl border border-emerald-200 bg-emerald-50/50 p-6 sm:p-8">
          <div className="flex items-center gap-3">
            <div className="grid size-10 place-items-center rounded-xl bg-emerald-600 text-white">
              <CheckCircle2 className="size-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900">Trường hợp được chấp nhận đổi trả</h3>
              <p className="text-xs text-slate-500">Miễn phí 100% chi phí vận chuyển thu hồi</p>
            </div>
          </div>
          <ul className="mt-6 space-y-3.5 text-sm leading-6 text-slate-700">
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-emerald-600" />
              <span>Sản phẩm bị lỗi kỹ thuật hoặc hư hỏng từ nhà sản xuất.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-emerald-600" />
              <span>Giao sai mã sản phẩm, sai màu sắc, phiên bản hoặc thông số so với đơn đặt hàng.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-emerald-600" />
              <span>Sản phẩm bị móp méo, trầy xước, nứt vỡ trong quá trình đơn vị vận chuyển giao hàng.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-emerald-600" />
              <span>Sản phẩm còn đầy đủ hộp, sách hướng dẫn, phụ kiện và quà tặng kèm (nếu có).</span>
            </li>
          </ul>
        </div>

        {/* Non-applicable conditions */}
        <div className="rounded-3xl border border-rose-200 bg-rose-50/50 p-6 sm:p-8">
          <div className="flex items-center gap-3">
            <div className="grid size-10 place-items-center rounded-xl bg-rose-600 text-white">
              <AlertCircle className="size-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900">Trường hợp không áp dụng đổi trả</h3>
              <p className="text-xs text-slate-500">Quý khách vui lòng lưu ý khi kiểm tra hàng</p>
            </div>
          </div>
          <ul className="mt-6 space-y-3.5 text-sm leading-6 text-slate-700">
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-rose-600" />
              <span>Quá 07 ngày kể từ khi quý khách nhận hàng từ nhân viên giao vận.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-rose-600" />
              <span>Sản phẩm bị rách tem niêm phong, mất phụ kiện hoặc có dấu hiệu cấn móp do va đập cá nhân.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-rose-600" />
              <span>Hư hại do sử dụng sai hướng dẫn kỹ thuật hoặc tự ý sửa chữa, can thiệp phần cứng.</span>
            </li>
            <li className="flex items-start gap-2.5">
              <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-rose-600" />
              <span>Các mặt hàng vệ sinh cá nhân, hàng tiêu dùng nhanh đã bóc seal mở nắp.</span>
            </li>
          </ul>
        </div>
      </div>

      {/* Refund Method Section */}
      <div className="mt-14 rounded-3xl border border-slate-200/80 bg-white p-8 shadow-sm sm:p-10">
        <h3 className="text-xl font-bold text-slate-900">Phương thức và thời gian hoàn tiền</h3>
        <p className="mt-1 text-sm text-slate-500">Số tiền hoàn trả sẽ được chuyển lại đúng kênh thanh toán ban đầu của quý khách</p>

        <div className="mt-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
            <h4 className="text-sm font-bold text-slate-900">Thanh toán COD</h4>
            <p className="mt-1 text-xs text-slate-500">Chuyển khoản trực tiếp vào tài khoản ngân hàng chính chủ của quý khách trong <strong>24 - 48 giờ</strong> sau khi xác nhận nhận hàng trả.</p>
          </div>
          <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
            <h4 className="text-sm font-bold text-slate-900">Ví điện tử & QR Code</h4>
            <p className="mt-1 text-xs text-slate-500">Hoàn về tài khoản ví ZaloPay, PayOS hoặc tài khoản nguồn VietQR trong <strong>1 - 2 ngày làm việc</strong>.</p>
          </div>
          <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
            <h4 className="text-sm font-bold text-slate-900">Thẻ VNPAY / Thẻ tín dụng</h4>
            <p className="mt-1 text-xs text-slate-500">Hoàn tiền qua cổng VNPAY về thẻ ngân hàng hoặc thẻ Visa/Mastercard trong vòng <strong>3 - 7 ngày làm việc</strong> tùy chính sách ngân hàng phát hành.</p>
          </div>
        </div>

        <div className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 pt-6">
          <p className="text-sm text-slate-600">Bạn muốn tra cứu trạng thái đơn hàng hoặc gửi yêu cầu đổi trả ngay?</p>
          <div className="flex gap-3">
            <Link 
              href="/customer/orders" 
              className="inline-flex items-center gap-1.5 rounded-xl bg-slate-950 px-4 py-2.5 text-xs font-bold text-white transition hover:bg-rose-600"
            >
              Xem đơn hàng của tôi <ArrowRight className="size-3.5" />
            </Link>
            <Link 
              href="/help" 
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-bold text-slate-700 transition hover:bg-slate-50"
            >
              Trung tâm trợ giúp
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
