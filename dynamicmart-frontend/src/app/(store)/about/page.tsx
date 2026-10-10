import type { Metadata } from "next";
import Link from "next/link";
import { Sparkles, ShoppingBag, ShieldCheck, HeartHandshake, Users, ArrowRight } from "lucide-react";

export const metadata: Metadata = {
  title: "Về chúng tôi | DynamicMart",
  description: "DynamicMart - Hệ sinh thái mua sắm thông minh, chất lượng cao và tin cậy.",
};

export default function AboutPage() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-xl sm:p-12">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full bg-indigo-500/10 px-3.5 py-1 text-xs font-semibold text-indigo-400 ring-1 ring-indigo-500/20">
            <Sparkles className="size-3.5" />
            Câu chuyện thương hiệu
          </div>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-white sm:text-4xl">
            Về DynamicMart
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-300 sm:text-base">
            Nền tảng mua sắm hiện đại kết hợp công nghệ microservices tiên tiến và trải nghiệm cá nhân hóa, giúp người tiêu dùng Việt Nam tiếp cận những sản phẩm chất lượng cao một cách thuận tiện nhất.
          </p>
        </div>
      </div>

      <div className="mt-12 grid gap-8 md:grid-cols-3">
        <div className="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
          <div className="grid size-12 place-items-center rounded-xl bg-indigo-50 text-indigo-600">
            <ShoppingBag className="size-6" />
          </div>
          <h3 className="mt-4 text-lg font-bold text-slate-900">Sản phẩm tuyển chọn</h3>
          <p className="mt-2 text-xs leading-5 text-slate-600">
            Mọi mặt hàng trên hệ thống đều được kiểm duyệt nguồn gốc xuất xứ nghiêm ngặt, đảm bảo 100% chính hãng và an toàn cho người tiêu dùng.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
          <div className="grid size-12 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
            <ShieldCheck className="size-6" />
          </div>
          <h3 className="mt-4 text-lg font-bold text-slate-900">Bảo đảm uy tín</h3>
          <p className="mt-2 text-xs leading-5 text-slate-600">
            Chính sách đổi trả minh bạch trong 7 ngày, thanh toán bảo mật với mã hóa nhiều lớp và chính sách đồng kiểm tận tay người nhận.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
          <div className="grid size-12 place-items-center rounded-xl bg-rose-50 text-rose-600">
            <HeartHandshake className="size-6" />
          </div>
          <h3 className="mt-4 text-lg font-bold text-slate-900">Trợ lý AI thông minh</h3>
          <p className="mt-2 text-xs leading-5 text-slate-600">
            Trang bị trợ lý thông minh hỗ trợ tìm kiếm sản phẩm, gợi ý voucher và tư vấn thắc mắc đơn hàng tức thì 24/7.
          </p>
        </div>
      </div>

      <div className="mt-12 rounded-3xl border border-slate-200/80 bg-white p-8 shadow-sm text-center">
        <h2 className="text-xl font-bold text-slate-900">Sẵn sàng trải nghiệm mua sắm cùng DynamicMart?</h2>
        <p className="mt-2 text-sm text-slate-500">Khám phá hàng ngàn sản phẩm công nghệ, gia dụng và thời trang với ưu đãi hấp dẫn.</p>
        <Link 
          href="/products" 
          className="mt-6 inline-flex items-center gap-2 rounded-xl bg-slate-950 px-6 py-3 text-sm font-bold text-white transition hover:bg-emerald-600"
        >
          Khám phá sản phẩm ngay <ArrowRight className="size-4" />
        </Link>
      </div>
    </div>
  );
}
