import type { Metadata } from "next";
import Link from "next/link";
import { 
  Truck, 
  Clock, 
  MapPin, 
  ShieldCheck, 
  CheckCircle2, 
  Package, 
  ArrowRight 
} from "lucide-react";

export const metadata: Metadata = {
  title: "Chính sách Vận chuyển | DynamicMart",
  description: "Thông tin đơn vị vận chuyển, thời gian giao hàng và biểu phí vận chuyển tại DynamicMart.",
};

const shippingMethods = [
  {
    name: "Giao hàng Tiêu chuẩn (GHN)",
    time: "2 - 4 ngày làm việc",
    fee: "25.000đ - 35.000đ (Miễn phí từ 500k)",
    desc: "Áp dụng cho toàn bộ 63 tỉnh thành trên toàn quốc qua đối tác vận chuyển Giao Hàng Nhanh (GHN).",
    highlight: false
  },
  {
    name: "Giao hàng Hỏa tốc 2H",
    time: "Nhận trong 2 - 4 giờ",
    fee: "45.000đ - 60.000đ",
    desc: "Áp dụng tại các quận nội thành Hà Nội & TP. Hồ Chí Minh cho các sản phẩm có sẵn tại kho trung tâm.",
    highlight: true
  },
  {
    name: "Giao hàng Cồng kềnh & Lắp đặt",
    time: "1 - 3 ngày làm việc",
    fee: "Theo quy chuẩn kích thước",
    desc: "Dành cho đồ gia dụng kích thước lớn, có đội ngũ kỹ thuật viên hỗ trợ vận chuyển và lắp đặt tận nhà.",
    highlight: false
  }
];

export default function ShippingPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      {/* Header Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-teal-950 p-8 text-white shadow-xl sm:p-12">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full bg-teal-500/10 px-3.5 py-1 text-xs font-semibold text-teal-400 ring-1 ring-teal-500/20">
            <Truck className="size-3.5" />
            Vận chuyển nhanh chóng & an toàn
          </div>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-white sm:text-4xl">
            Chính sách Vận chuyển & Giao nhận
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-300 sm:text-base">
            DynamicMart hợp tác cùng các đơn vị vận chuyển hàng đầu như Giao Hàng Nhanh (GHN) để bảo đảm hàng hóa đến tay bạn an toàn, nguyên vẹn và đúng lịch hẹn.
          </p>
        </div>
        <div className="pointer-events-none absolute -right-16 -top-16 size-80 rounded-full bg-teal-500/10 blur-3xl" />
      </div>

      {/* Shipping Methods Cards */}
      <div className="mt-12">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">Phương thức giao hàng</h2>
        <p className="mt-1 text-sm text-slate-500">Lựa chọn giải pháp vận chuyển phù hợp với nhu cầu của bạn</p>

        <div className="mt-6 grid gap-6 md:grid-cols-3">
          {shippingMethods.map((method, idx) => (
            <div 
              key={idx}
              className={`flex flex-col justify-between rounded-2xl border p-6 shadow-sm transition hover:shadow-md ${
                method.highlight 
                  ? "border-teal-500/80 bg-teal-50/30" 
                  : "border-slate-200/80 bg-white"
              }`}
            >
              <div>
                {method.highlight && (
                  <span className="inline-block rounded-full bg-teal-600 px-3 py-0.5 text-[11px] font-bold text-white mb-3">
                    Phổ biến nhất
                  </span>
                )}
                <h3 className="text-lg font-bold text-slate-900">{method.name}</h3>
                <div className="mt-3 space-y-2 text-xs text-slate-600">
                  <div className="flex items-center gap-2">
                    <Clock className="size-4 text-teal-600 shrink-0" />
                    <span>Thời gian: <strong>{method.time}</strong></span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Package className="size-4 text-teal-600 shrink-0" />
                    <span>Cước phí: <strong>{method.fee}</strong></span>
                  </div>
                </div>
                <p className="mt-4 text-xs leading-5 text-slate-500">{method.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Process & Commitments */}
      <div className="mt-14 rounded-3xl border border-slate-200/80 bg-white p-8 shadow-sm sm:p-10">
        <h3 className="text-xl font-bold text-slate-900">Cam kết giao hàng của DynamicMart</h3>
        <div className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <div className="flex gap-3.5">
            <div className="grid size-10 shrink-0 place-items-center rounded-xl bg-teal-50 text-teal-600">
              <CheckCircle2 className="size-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-slate-900">Đồng kiểm khi nhận hàng</h4>
              <p className="mt-1 text-xs leading-5 text-slate-500">Khách hàng được quyền mở hộp kiểm tra ngoại quan sản phẩm trước khi thanh toán cho shipper.</p>
            </div>
          </div>
          <div className="flex gap-3.5">
            <div className="grid size-10 shrink-0 place-items-center rounded-xl bg-teal-50 text-teal-600">
              <ShieldCheck className="size-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-slate-900">Bảo hiểm 100% giá trị hàng hóa</h4>
              <p className="mt-1 text-xs leading-5 text-slate-500">Đền bù 100% giá trị nếu kiện hàng bị thất lạc, bể vỡ trong quá trình lưu chuyển của bưu tá.</p>
            </div>
          </div>
          <div className="flex gap-3.5">
            <div className="grid size-10 shrink-0 place-items-center rounded-xl bg-teal-50 text-teal-600">
              <MapPin className="size-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-slate-900">Theo dõi hành trình thời gian thực</h4>
              <p className="mt-1 text-xs leading-5 text-slate-500">Cập nhật vị trí kiện hàng liên tục qua hệ thống mã vận đơn GHN trực tiếp trong trang quản lý đơn mua.</p>
            </div>
          </div>
        </div>

        <div className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 pt-6">
          <p className="text-sm text-slate-600">Có thắc mắc về cước phí hoặc hành trình đơn hàng hiện tại?</p>
          <div className="flex gap-3">
            <Link 
              href="/help" 
              className="inline-flex items-center gap-1.5 rounded-xl bg-slate-950 px-4 py-2.5 text-xs font-bold text-white transition hover:bg-teal-600"
            >
              Hỏi đáp vận chuyển <ArrowRight className="size-3.5" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
