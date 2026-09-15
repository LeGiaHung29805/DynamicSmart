import Link from "next/link";
import { BadgeCheck, LockKeyhole, Sparkles } from "lucide-react";

export function AuthPageLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <main className="grid min-h-screen bg-[#f7f8f6] lg:grid-cols-2">
      <section className="relative hidden overflow-hidden bg-emerald-950 p-10 text-white lg:flex lg:flex-col lg:justify-between xl:p-16">
        <div className="absolute inset-0 opacity-40 [background-image:radial-gradient(circle_at_14%_15%,#6ee7b7_0,transparent_22rem),radial-gradient(circle_at_90%_85%,#fbbf24_0,transparent_21rem)]" />
        <Link className="relative z-10 inline-flex items-center gap-2 text-xl font-black tracking-[-0.055em]" href="/">
          <span className="grid size-10 place-items-center rounded-xl bg-white text-sm text-emerald-950 shadow-lg">D</span>
          DYNAMIC<span className="text-emerald-300">MART</span>
        </Link>

        <div className="relative z-10 max-w-xl">
          <span className="mb-6 grid size-14 place-items-center rounded-2xl bg-white/10 ring-1 ring-white/20"><Sparkles className="size-6 text-amber-300" /></span>
          <p className="text-xs font-black tracking-[0.22em] text-emerald-300 uppercase">Mua sắm thông minh hơn</p>
          <h1 className="mt-4 text-5xl font-black leading-[1.06] tracking-[-0.055em]">Mọi lựa chọn phù hợp, ngay trong tầm tay.</h1>
          <p className="mt-6 max-w-lg text-base leading-7 text-emerald-50/80">Đăng nhập để lưu sản phẩm yêu thích, nhận ưu đãi cá nhân và theo dõi đơn hàng thật dễ dàng.</p>
        </div>

        <div className="relative z-10 grid gap-3 text-sm text-emerald-50/90">
          <p className="flex items-center gap-3"><BadgeCheck className="size-5 text-emerald-300" /> Voucher hiển thị theo đúng tài khoản của bạn</p>
          <p className="flex items-center gap-3"><LockKeyhole className="size-5 text-emerald-300" /> Phiên đăng nhập được bảo vệ bằng cookie an toàn</p>
        </div>
      </section>

      <section className="flex items-center justify-center px-4 py-10 sm:px-6 lg:px-12">
        <div className="w-full max-w-md">
          <Link className="mb-12 inline-flex items-center gap-2 text-lg font-black tracking-[-0.055em] text-slate-950 lg:hidden" href="/">
            <span className="grid size-9 place-items-center rounded-xl bg-emerald-950 text-xs text-white">D</span>
            DYNAMIC<span className="text-brand">MART</span>
          </Link>
          {children}
        </div>
      </section>
    </main>
  );
}
