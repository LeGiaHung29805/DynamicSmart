import Link from "next/link";
import { Heart, Menu, ShoppingBag, UserRound } from "lucide-react";

export function StoreLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="flex min-h-screen flex-col bg-[#f7f8f6]">
      <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/90 backdrop-blur-xl">
        <div className="mx-auto flex h-[4.5rem] w-full max-w-7xl items-center justify-between gap-5 px-4 sm:px-6 lg:px-8">
          <Link aria-label="DynamicMart - Trang chủ" className="group flex items-center gap-2.5" href="/">
            <span className="grid size-9 place-items-center rounded-xl bg-emerald-950 text-sm font-black text-white shadow-sm transition group-hover:-rotate-3 group-hover:bg-brand">D</span>
            <span className="text-xl font-black tracking-[-0.055em] text-slate-950">DYNAMIC<span className="text-brand">MART</span></span>
          </Link>

          <nav aria-label="Điều hướng chính" className="hidden h-full items-center gap-7 text-sm font-bold text-slate-600 md:flex">
            <Link className="flex h-full items-center border-b-2 border-transparent transition hover:border-brand hover:text-brand" href="/products">Sản phẩm</Link>
            <Link className="flex h-full items-center border-b-2 border-transparent transition hover:border-brand hover:text-brand" href="/products?sort=newest">Hàng mới</Link>
            <Link className="flex h-full items-center border-b-2 border-transparent transition hover:border-brand hover:text-brand" href="/products?sort=best-selling">Bán chạy</Link>
            <Link className="flex h-full items-center border-b-2 border-transparent transition hover:border-brand hover:text-brand" href="/products?promotion=true">Ưu đãi</Link>
          </nav>

          <div className="flex items-center gap-1">
            <Link aria-label="Sản phẩm yêu thích" className="hidden size-10 place-items-center rounded-xl text-slate-600 transition hover:bg-emerald-50 hover:text-brand sm:grid" href="/customer/account/wishlist"><Heart className="size-[1.15rem]" /></Link>
            <Link aria-label="Giỏ hàng" className="relative grid size-10 place-items-center rounded-xl text-slate-600 transition hover:bg-emerald-50 hover:text-brand" href="/cart"><ShoppingBag className="size-[1.15rem]" /><span className="absolute top-1.5 right-1.5 size-2 rounded-full bg-amber-400 ring-2 ring-white" /></Link>
            <Link aria-label="Đăng nhập" className="hidden size-10 place-items-center rounded-xl text-slate-600 transition hover:bg-emerald-50 hover:text-brand sm:grid" href="/login"><UserRound className="size-[1.15rem]" /></Link>
            <Link aria-label="Xem sản phẩm" className="ml-1 grid size-10 place-items-center rounded-xl bg-slate-950 text-white transition hover:bg-brand md:hidden" href="/products"><Menu className="size-5" /></Link>
          </div>
        </div>
      </header>

      <main className="flex-1">{children}</main>

      <footer className="border-t border-slate-800 bg-slate-950 text-slate-300">
        <div className="mx-auto grid w-full max-w-7xl gap-10 px-4 py-12 sm:grid-cols-2 sm:px-6 lg:grid-cols-[1.4fr_1fr_1fr_1fr] lg:px-8">
          <div>
            <Link className="text-xl font-black tracking-[-0.05em] text-white" href="/">DYNAMIC<span className="text-emerald-400">MART</span></Link>
            <p className="mt-4 max-w-xs text-sm leading-6 text-slate-400">Nền tảng mua sắm hiện đại, nơi mọi sản phẩm được tuyển chọn để nâng tầm trải nghiệm sống của bạn.</p>
          </div>
          <div><h3 className="text-sm font-bold text-white">Mua sắm</h3><div className="mt-4 flex flex-col gap-3 text-sm text-slate-400"><Link className="hover:text-emerald-300" href="/products">Tất cả sản phẩm</Link><Link className="hover:text-emerald-300" href="/products?sort=newest">Hàng mới</Link><Link className="hover:text-emerald-300" href="/products?promotion=true">Khuyến mãi</Link></div></div>
          <div><h3 className="text-sm font-bold text-white">Hỗ trợ</h3><div className="mt-4 flex flex-col gap-3 text-sm text-slate-400"><Link className="hover:text-emerald-300" href="/help">Trung tâm trợ giúp</Link><Link className="hover:text-emerald-300" href="/shipping">Vận chuyển</Link><Link className="hover:text-emerald-300" href="/returns">Đổi trả</Link></div></div>
          <div><h3 className="text-sm font-bold text-white">DynamicMart</h3><div className="mt-4 flex flex-col gap-3 text-sm text-slate-400"><Link className="hover:text-emerald-300" href="/about">Về chúng tôi</Link><Link className="hover:text-emerald-300" href="/contact">Liên hệ</Link><Link className="hover:text-emerald-300" href="/terms">Điều khoản</Link></div></div>
        </div>
        <div className="border-t border-slate-800"><div className="mx-auto flex max-w-7xl flex-col gap-2 px-4 py-5 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8"><p>© 2026 DynamicMart. All rights reserved.</p><p>Thiết kế cho trải nghiệm mua sắm tốt hơn.</p></div></div>
      </footer>
    </div>
  );
}
