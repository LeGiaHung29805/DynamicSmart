export function safeReturnTo(value: string | undefined): string {
  if (!value?.startsWith("/") || value.startsWith("//") || value.includes("\\")) return "/";
  const target = new URL(value, "https://dynamicmart.local");
  const rawDeadline = target.searchParams.get("resumeUntil");
  if (rawDeadline) {
    const deadline = Number(rawDeadline);
    if (!Number.isFinite(deadline) || deadline < Date.now()) {
      target.searchParams.delete("variant");
      target.searchParams.delete("quantity");
      target.searchParams.delete("resumeUntil");
    }
  }
  return `${target.pathname}${target.search}${target.hash}`;
}
