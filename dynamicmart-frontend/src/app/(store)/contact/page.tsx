import type { Metadata } from "next";
import { Mail, Phone, MapPin, Clock, MessageSquare, Send } from "lucide-react";

export const metadata: Metadata = {
  title: "Liên hệ | DynamicMart",
  description: "Thông tin liên hệ, tổng đài hỗ trợ và địa chỉ văn phòng của DynamicMart.",
};

export default function ContactPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-sky-950 p-8 text-white shadow-xl sm:p-12">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full bg-sky-500/10 px-3.5 py-1 text-xs font-semibold text-sky-400 ring-1 ring-sky-500/20">
            <MessageSquare className="size-3.5" />
            Hỗ trợ 24/7
          </div>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-white sm:text-4xl">
            Liên hệ với DynamicMart
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-300 sm:text-base">
            Bạn có câu hỏi, phản ánh chất lượng dịch vụ hay muốn hợp tác kinh doanh? Hãy gửi thông điệp cho chúng tôi hoặc liên hệ qua các kênh trực tiếp dưới đây.
          </p>
        </div>
      </div>

      <div className="mt-12 grid gap-8 lg:grid-cols-3">
        <div className="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
          <div className="grid size-12 place-items-center rounded-xl bg-sky-50 text-sky-600">
            <Phone className="size-6" />
          </div>
          <h3 className="mt-4 text-base font-bold text-slate-900">Tổng đài CSKH</h3>
          <p className="mt-1 text-xs text-slate-500">Hỗ trợ tất cả các ngày trong tuần</p>
          <p className="mt-3 text-lg font-black text-sky-600">1900 8888</p>
          <p className="text-xs text-slate-400">8:00 - 21:00 (Cước phí 1.000đ/phút)</p>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
          <div className="grid size-12 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
            <Mail className="size-6" />
          </div>
          <h3 className="mt-4 text-base font-bold text-slate-900">Email hỗ trợ</h3>
          <p className="mt-1 text-xs text-slate-500">Phản hồi trong vòng 24 giờ</p>
          <p className="mt-3 text-base font-bold text-slate-900">support@dynamicmart.vn</p>
          <p className="text-xs text-slate-400">Tiếp nhận yêu cầu bảo hành & khiếu nại</p>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
          <div className="grid size-12 place-items-center rounded-xl bg-amber-50 text-amber-600">
            <MapPin className="size-6" />
          </div>
          <h3 className="mt-4 text-base font-bold text-slate-900">Trụ sở chính</h3>
          <p className="mt-1 text-xs text-slate-500">Văn phòng vận hành</p>
          <p className="mt-3 text-sm font-semibold text-slate-800">Tòa nhà Công nghệ Dynamic, Cầu Giấy, Hà Nội</p>
          <p className="text-xs text-slate-400">Thứ 2 - Thứ 6: 8:30 - 17:30</p>
        </div>
      </div>
    </div>
  );
}
