export interface PurchaseResumeState {
  variantId?: string;
  quantity: number;
}

export function safeReturnTo(value: string | undefined, now = Date.now()): string {
  if (!value?.startsWith("/") || value.startsWith("//") || value.includes("\\")) return "/";
  const target = new URL(value, "https://dynamicmart.local");
  const rawDeadline = target.searchParams.get("resumeUntil");
  if (rawDeadline) {
    const deadline = Number(rawDeadline);
    if (!Number.isFinite(deadline) || deadline < now) {
      target.searchParams.delete("variant");
      target.searchParams.delete("quantity");
      target.searchParams.delete("resumeUntil");
    }
  }
  return `${target.pathname}${target.search}${target.hash}`;
}

export function purchaseResumeState(
  query: { variant?: string; quantity?: string; resumeUntil?: string },
  now = Date.now(),
): PurchaseResumeState {
  if (query.resumeUntil) {
    const deadline = Number(query.resumeUntil);
    if (!Number.isFinite(deadline) || deadline < now) return { quantity: 1 };
  }
  const parsedQuantity = Number.parseInt(query.quantity ?? "1", 10);
  return {
    variantId: query.variant,
    quantity: Number.isFinite(parsedQuantity) ? Math.max(parsedQuantity, 1) : 1,
  };
}
