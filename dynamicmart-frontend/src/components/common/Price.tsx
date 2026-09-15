export function formatVnd(value: number): string {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
}

export function Price({ value }: Readonly<{ value: number }>) {
  return <span className="font-semibold tabular-nums">{formatVnd(value)}</span>;
}
