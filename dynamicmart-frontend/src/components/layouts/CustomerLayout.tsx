import Link from "next/link";

const accountLinks = [
  ["Hồ sơ", "/customer/account/profile"],
  ["Địa chỉ", "/customer/account/addresses"],
  ["Đơn hàng", "/customer/account/orders"],
  ["Mã giảm giá", "/customer/account/vouchers"],
] as const;

export function CustomerLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="mx-auto grid w-full max-w-6xl gap-6 px-4 py-8 md:grid-cols-[220px_1fr] sm:px-6">
      <aside className="rounded-xl border border-border bg-surface p-4">
        <h1 className="mb-3 font-semibold">Tài khoản</h1>
        <nav aria-label="Điều hướng tài khoản" className="flex gap-1 overflow-x-auto md:flex-col">
          {accountLinks.map(([label, href]) => (
            <Link className="rounded-md px-3 py-2 text-sm hover:bg-slate-100 hover:text-brand" href={href} key={href}>
              {label}
            </Link>
          ))}
        </nav>
      </aside>
      <main>{children}</main>
    </div>
  );
}
