import type { Metadata } from "next";
import { ShieldCheck, FileText, Lock, CheckCircle2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Điều khoản dịch vụ | DynamicMart",
  description: "Điều khoản dịch vụ, quy định giao dịch và chính sách bảo mật tại DynamicMart.",
};

export default function TermsPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-amber-950 p-8 text-white shadow-xl sm:p-12">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full bg-amber-500/10 px-3.5 py-1 text-xs font-semibold text-amber-400 ring-1 ring-amber-500/20">
            <FileText className="size-3.5" />
            Pháp lý & Quy định
          </div>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-white sm:text-4xl">
            Điều khoản Dịch vụ & Chính sách Bảo mật
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-300 sm:text-base">
            Chào mừng bạn đến với DynamicMart. Khi truy cập và mua hàng trên hệ thống, bạn đồng ý tuân thủ các quy tắc và điều khoản dưới đây.
          </p>
        </div>
      </div>

      <div className="mt-12 space-y-8 rounded-3xl border border-slate-200/80 bg-white p-8 shadow-sm sm:p-10">
        <div>
          <h2 className="text-lg font-bold text-slate-900">1. Quy định về tài khoản và giao dịch</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Khách hàng có trách nhiệm bảo mật thông tin đăng nhập, mật khẩu cá nhân. Mọi hoạt động phát sinh từ tài khoản được xem là do chính chủ tài khoản thực hiện. DynamicMart có quyền tạm khóa các tài khoản có dấu hiệu gian lận khuyến mãi hoặc đặt đơn hàng ảo.
          </p>
        </div>

        <div className="border-t border-slate-100 pt-6">
          <h2 className="text-lg font-bold text-slate-900">2. Giá cả và thanh toán</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Tất cả giá bán niêm yết trên website đều là giá đã bao gồm thuế Giá trị gia tăng (VAT). Khách hàng có thể lựa chọn thanh toán khi nhận hàng (COD) hoặc thanh toán trực tuyến qua cổng VNPAY, ZaloPay, PayOS hoặc quét mã VietQR tự động.
          </p>
        </div>

        <div className="border-t border-slate-100 pt-6">
          <h2 className="text-lg font-bold text-slate-900">3. Chính sách bảo mật thông tin cá nhân</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Chúng tôi cam kết bảo mật tuyệt đối dữ liệu người dùng theo chuẩn mã hóa SSL/TLS. Thông tin chỉ được sử dụng cho mục đích xử lý đơn hàng, liên hệ giao nhận và gửi các thông báo ưu đãi liên quan đến tài khoản nếu bạn đồng ý.
          </p>
        </div>

        <div className="border-t border-slate-100 pt-6">
          <h2 className="text-lg font-bold text-slate-900">4. Giới hạn trách nhiệm</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            DynamicMart nỗ lực đảm bảo thông tin mô tả sản phẩm và hình ảnh chính xác nhất. Trong trường hợp có sự cố kỹ thuật hiển thị sai giá bất thường hoặc lỗi kho hàng ngoài ý muốn, chúng tôi sẽ liên hệ thông báo hủy đơn và hoàn tiền 100% cho quý khách.
          </p>
        </div>
      </div>
    </div>
  );
}
