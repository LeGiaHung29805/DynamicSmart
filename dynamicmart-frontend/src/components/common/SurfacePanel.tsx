import type { ReactNode } from "react";

interface SurfacePanelProps {
  children: ReactNode;
  className?: string;
}

export function SurfacePanel({ children, className = "" }: Readonly<SurfacePanelProps>) {
  return <section className={`rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm shadow-slate-950/[0.03] sm:p-7 ${className}`}>{children}</section>;
}
