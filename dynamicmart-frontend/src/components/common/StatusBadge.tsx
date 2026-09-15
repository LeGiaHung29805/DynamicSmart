type StatusTone = "neutral" | "success" | "warning" | "danger";

const toneClass: Record<StatusTone, string> = {
  neutral: "bg-slate-100 text-slate-700",
  success: "bg-emerald-100 text-emerald-800",
  warning: "bg-amber-100 text-amber-900",
  danger: "bg-red-100 text-red-800",
};

export function StatusBadge({ label, tone = "neutral" }: Readonly<{ label: string; tone?: StatusTone }>) {
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${toneClass[tone]}`}>{label}</span>;
}
