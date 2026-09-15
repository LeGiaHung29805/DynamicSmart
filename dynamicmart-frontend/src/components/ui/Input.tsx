import type { InputHTMLAttributes } from "react";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: string;
  label?: string;
}

export function Input({ className = "", error, id, label, ...props }: Readonly<InputProps>) {
  const inputId = id ?? props.name;
  return (
    <label className="block space-y-1.5" htmlFor={inputId}>
      {label ? <span className="text-sm font-medium text-foreground">{label}</span> : null}
      <input
        className={`min-h-10 w-full rounded-lg border bg-surface px-3 text-sm outline-none transition focus:border-brand focus:ring-2 focus:ring-brand/20 ${error ? "border-danger" : "border-border"} ${className}`}
        id={inputId}
        aria-invalid={Boolean(error)}
        {...props}
      />
      {error ? <span className="text-sm text-danger">{error}</span> : null}
    </label>
  );
}
