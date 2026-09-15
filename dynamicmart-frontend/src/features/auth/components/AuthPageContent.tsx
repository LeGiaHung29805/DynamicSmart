interface AuthPageContentProps {
  eyebrow: string;
  title: string;
  description: string;
  children: React.ReactNode;
}

export function AuthPageContent({ eyebrow, title, description, children }: Readonly<AuthPageContentProps>) {
  return (
    <div>
      <p className="text-xs font-black tracking-[0.2em] text-brand uppercase">{eyebrow}</p>
      <h1 className="mt-3 text-3xl font-black tracking-[-0.045em] text-slate-950 sm:text-4xl">{title}</h1>
      <p className="mt-3 text-sm leading-6 text-slate-500">{description}</p>
      <div className="mt-8 rounded-3xl border border-slate-200 bg-white p-5 shadow-xl shadow-slate-950/[0.04] sm:p-7">{children}</div>
    </div>
  );
}
