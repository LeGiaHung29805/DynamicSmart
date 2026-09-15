export function safeReturnTo(value: string | undefined): string {
  return value?.startsWith("/") && !value.startsWith("//") && !value.includes("\\") ? value : "/";
}
