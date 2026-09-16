import Link from "next/link";
import { AdminNavigation } from "./AdminNavigation";

export function AdminLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="min-h-screen bg-[#f7f8f6] md:grid md:grid-cols-[260px_minmax(0,1fr)]">
      <aside className="bg-emerald-950 px-4 py-5 text-white sm:px-6 md:min-h-screen md:px-5 md:py-7">
        <Link aria-label="DynamicMart Admin - Tổng quan" className="inline-flex items-center gap-3" href="/admin">
          <span className="grid size-10 place-items-center rounded-xl bg-white text-sm font-black text-emerald-950">D</span>
          <span><span className="block text-lg font-black tracking-[-0.05em]">DYNAMIC<span className="text-emerald-300">MART</span></span><span className="block text-[10px] font-bold tracking-[0.2em] text-emerald-200 uppercase">Admin workspace</span></span>
        </Link>
        <AdminNavigation />
      </aside>
      <main className="min-w-0 px-4 py-7 sm:px-6 lg:px-10 lg:py-10">
        <div className="mx-auto w-full max-w-7xl">{children}</div>
      </main>
    </div>
  );
}
