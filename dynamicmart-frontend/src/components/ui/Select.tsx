import type { SelectHTMLAttributes } from "react";

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  error?: string;
  label?: string;
}

export function Select({ children, className = "", error, id, label, ...props }: Readonly<SelectProps>) {
  const selectId = id ?? props.name;
  return (
    <label className="block space-y-1.5" htmlFor={selectId}>
      {label ? <span className="text-sm font-medium text-foreground">{label}</span> : null}
      <select
        className={`min-h-10 w-full rounded-lg border bg-surface px-3 text-sm outline-none transition focus:border-brand focus:ring-2 focus:ring-brand/20 ${error ? "border-danger" : "border-border"} ${className}`}
        id={selectId}
        aria-invalid={Boolean(error)}
        {...props}
      >
        {children}
      </select>
      {error ? <span className="text-sm text-danger">{error}</span> : null}
    </label>
  );
}
