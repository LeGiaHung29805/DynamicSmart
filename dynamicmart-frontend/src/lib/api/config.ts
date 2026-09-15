export function getApiBaseUrl(): string {
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!baseUrl) throw new Error("Thiếu NEXT_PUBLIC_API_BASE_URL.");
  return baseUrl.replace(/\/$/, "");
}

export function getInternalApiBaseUrl(): string {
  return (process.env.API_GATEWAY_INTERNAL_URL ?? getApiBaseUrl()).replace(/\/$/, "");
}
