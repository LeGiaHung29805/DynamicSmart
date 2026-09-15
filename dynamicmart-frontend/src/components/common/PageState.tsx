"use client";

import { Button } from "@/components/ui/Button";

type PageStateProps = {
  actionLabel?: string;
  description: string;
  onAction?: () => void;
  title: string;
};

export function EmptyState({ actionLabel, description, onAction, title }: Readonly<PageStateProps>) {
  return <StateCard actionLabel={actionLabel} description={description} onAction={onAction} title={title} />;
}

export function ErrorState({ actionLabel = "Thử lại", description, onAction, title = "Có lỗi xảy ra" }: Readonly<Partial<PageStateProps>>) {
  return <StateCard actionLabel={actionLabel} description={description ?? "Không thể tải dữ liệu. Vui lòng thử lại."} onAction={onAction} title={title} />;
}

export function LoadingState({ title = "Đang tải dữ liệu" }: Readonly<{ title?: string }>) {
  return (
    <div className="rounded-xl border border-border bg-surface p-6" role="status">
      <p className="sr-only">{title}</p>
      <div className="h-5 w-48 animate-pulse rounded bg-slate-200" />
      <div className="mt-3 h-4 w-full max-w-md animate-pulse rounded bg-slate-100" />
    </div>
  );
}

function StateCard({ actionLabel, description, onAction, title }: Readonly<PageStateProps>) {
  return (
    <section className="rounded-xl border border-border bg-surface p-6 text-center">
      <h2 className="text-lg font-semibold">{title}</h2>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-muted">{description}</p>
      {actionLabel && onAction ? <Button className="mt-5" onClick={onAction}>{actionLabel}</Button> : null}
    </section>
  );
}
