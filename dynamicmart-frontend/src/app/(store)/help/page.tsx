import type { Metadata } from "next";
import Link from "next/link";
import { 
  HelpCircle, 
  ShoppingBag, 
  CreditCard, 
  Truck, 
  RotateCcw, 
  ShieldCheck, 
  MessageSquareText, 
  Mail, 
  Phone, 
  ChevronRight,
  Search
} from "lucide-react";

export const metadata: Metadata = {
  title: "Trung tâm trợ giúp | DynamicMart",
  description: "Giải đáp thắc mắc về đơn hàng, thanh toán, vận chuyển và chính sách tại DynamicMart.",
};

const faqs = [
  {
    category: "Đặt hàng",
    question: "Làm thế nào để tôi đặt hàng trên DynamicMart?",
    answer: "Bạn chỉ cần chọn sản phẩm, kích thước/màu sắc mong muốn, bấm 'Thêm vào giỏ hàng' hoặc 'Mua ngay'. Tại trang thanh toán, nhập địa chỉ nhận hàng, chọn phương thức thanh toán và mã giảm giá (nếu có), sau đó xác nhận đặt hàng."
  },
  {
    category: "Thanh toán",
    question: "DynamicMart hỗ trợ những phương thức thanh toán nào?",
    answer: "Chúng tôi hỗ trợ đa dạng phương thức: Thanh toán khi nhận hàng (COD), Cổng VNPAY (thẻ ATM nội địa, thẻ quốc tế Visa/Mastercard), ZaloPay, PayOS và quét mã VietQR tự động qua ứng dụng ngân hàng."
  },
  {
    category: "Vận chuyển",
    question: "Thời gian giao hàng mất bao lâu?",
    answer: "Đơn hàng nội thành Hà Nội & TP.HCM thường được giao trong 1-2 ngày làm việc. Các tỉnh thành khác dao động từ 2-4 ngày làm việc. Bạn có thể theo dõi hành trình đơn hàng tại mục 'Đơn mua' trong hồ sơ cá nhân."
  },
  {
    category: "Đổi trả",
    question: "Chính sách đổi trả hàng được áp dụng như thế nào?",
    answer: "Khách hàng được hỗ trợ đổi trả miễn phí trong vòng 7 ngày kể từ khi nhận hàng đối với sản phẩm lỗi do nhà sản xuất, sai mẫu mã hoặc hư hỏng trong quá trình vận chuyển. Sản phẩm cần giữ nguyên tem mác và hóa đơn."
  },
  {
    category: "Đánh giá",
    question: "Làm thế nào để tôi viết đánh giá cho sản phẩm đã mua?",
    answer: "Sau khi đơn hàng chuyển sang trạng thái đã giao thành công, bạn vào mục 'Hồ sơ cá nhân' -> 'Đánh giá của tôi' hoặc vào chi tiết đơn hàng để gửi nhận xét và chấm điểm sao cho từng sản phẩm."
  },
  {
    category: "Bảo mật",
    question: "Thông tin cá nhân của tôi có được bảo mật an toàn?",
    answer: "DynamicMart tuân thủ tiêu chuẩn bảo mật mã hóa cao cấp SSL/TLS và chuẩn phân quyền JWT hiện đại. Chúng tôi cam kết tuyệt đối không chia sẻ thông tin cá nhân của quý khách cho bên thứ ba."
  }
];

export default function HelpPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      {/* Header Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-emerald-950 p-8 text-white shadow-xl sm:p-12">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full bg-emerald-500/10 px-3.5 py-1 text-xs font-semibold text-emerald-400 ring-1 ring-emerald-500/20">
            <HelpCircle className="size-3.5" />
            Trung tâm hỗ trợ khách hàng
          </div>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-white sm:text-4xl">
            Chúng tôi có thể giúp gì cho bạn?
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-300 sm:text-base">
            Tìm câu trả lời nhanh chóng cho các câu hỏi phổ biến hoặc liên hệ ngay với đội ngũ chăm sóc khách hàng của DynamicMart.
          </p>
        </div>
        <div className="pointer-events-none absolute -right-16 -top-16 size-80 rounded-full bg-emerald-500/10 blur-3xl" />
      </div>

      {/* Quick Navigation Cards */}
      <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Link 
          href="/shipping" 
          className="group flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:border-emerald-500/50 hover:shadow-md"
        >
          <div>
            <div className="grid size-12 place-items-center rounded-xl bg-emerald-50 text-emerald-600 transition group-hover:bg-emerald-600 group-hover:text-white">
              <Truck className="size-6" />
            </div>
            <h3 className="mt-4 text-base font-bold text-slate-900">Vận chuyển & Giao hàng</h3>
            <p className="mt-1 text-xs leading-5 text-slate-500">Tra cứu thời gian, đơn vị vận chuyển và biểu phí giao nhận toàn quốc.</p>
          </div>
          <span className="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-emerald-600">
            Xem chi tiết <ChevronRight className="size-3.5 transition group-hover:translate-x-1" />
          </span>
        </Link>

        <Link 
          href="/returns" 
          className="group flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:border-rose-500/50 hover:shadow-md"
        >
          <div>
            <div className="grid size-12 place-items-center rounded-xl bg-rose-50 text-rose-600 transition group-hover:bg-rose-600 group-hover:text-white">
              <RotateCcw className="size-6" />
            </div>
            <h3 className="mt-4 text-base font-bold text-slate-900">Đổi trả & Hoàn tiền</h3>
            <p className="mt-1 text-xs leading-5 text-slate-500">Quy trình đổi hàng 7 ngày miễn phí và điều kiện hoàn tiền minh bạch.</p>
          </div>
          <span className="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-rose-600">
            Xem chính sách <ChevronRight className="size-3.5 transition group-hover:translate-x-1" />
          </span>
        </Link>

        <Link 
          href="/contact" 
          className="group flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:border-sky-500/50 hover:shadow-md"
        >
          <div>
            <div className="grid size-12 place-items-center rounded-xl bg-sky-50 text-sky-600 transition group-hover:bg-sky-600 group-hover:text-white">
              <MessageSquareText className="size-6" />
            </div>
            <h3 className="mt-4 text-base font-bold text-slate-900">Liên hệ trực tiếp</h3>
            <p className="mt-1 text-xs leading-5 text-slate-500">Tổng đài hỗ trợ 1900 8888 và hòm thư phản hồi ý kiến khách hàng.</p>
          </div>
          <span className="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-sky-600">
            Gửi yêu cầu <ChevronRight className="size-3.5 transition group-hover:translate-x-1" />
          </span>
        </Link>

        <Link 
          href="/terms" 
          className="group flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:border-amber-500/50 hover:shadow-md"
        >
          <div>
            <div className="grid size-12 place-items-center rounded-xl bg-amber-50 text-amber-600 transition group-hover:bg-amber-600 group-hover:text-white">
              <ShieldCheck className="size-6" />
            </div>
            <h3 className="mt-4 text-base font-bold text-slate-900">Điều khoản & Bảo mật</h3>
            <p className="mt-1 text-xs leading-5 text-slate-500">Quy định giao dịch, chính sách bảo mật dữ liệu và quyền lợi hội viên.</p>
          </div>
          <span className="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-amber-600">
            Đọc điều khoản <ChevronRight className="size-3.5 transition group-hover:translate-x-1" />
          </span>
        </Link>
      </div>

      {/* FAQ Accordion Section */}
      <div className="mt-14">
        <div className="text-center">
          <h2 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Câu hỏi thường gặp</h2>
          <p className="mt-2 text-sm text-slate-500">Những thắc mắc phổ biến nhất khi mua sắm tại DynamicMart</p>
        </div>

        <div className="mt-8 grid gap-4 md:grid-cols-2">
          {faqs.map((faq, idx) => (
            <div 
              key={idx} 
              className="flex flex-col rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm transition hover:border-slate-300 hover:shadow-md"
            >
              <div className="flex items-center gap-2">
                <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-bold tracking-wide uppercase text-slate-600">
                  {faq.category}
                </span>
              </div>
              <h3 className="mt-3 text-base font-bold text-slate-900">{faq.question}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">{faq.answer}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Contact Support Banner */}
      <div className="mt-14 rounded-3xl border border-slate-200/80 bg-gradient-to-r from-emerald-50 via-teal-50 to-cyan-50 p-8 sm:p-10">
        <div className="flex flex-col items-center justify-between gap-6 sm:flex-row">
          <div className="text-center sm:text-left">
            <h3 className="text-xl font-bold text-slate-900">Chưa tìm thấy câu trả lời bạn cần?</h3>
            <p className="mt-1 text-sm text-slate-600">Đội ngũ tư vấn viên của chúng tôi luôn trực tuyến để hỗ trợ bạn kịp thời.</p>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Link 
              href="/contact" 
              className="inline-flex items-center gap-2 rounded-xl bg-slate-950 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-emerald-600"
            >
              <Mail className="size-4" />
              Liên hệ chúng tôi
            </Link>
            <a 
              href="tel:19008888" 
              className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-bold text-slate-800 shadow-sm transition hover:bg-slate-50"
            >
              <Phone className="size-4" />
              Hotline: 1900 8888
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
