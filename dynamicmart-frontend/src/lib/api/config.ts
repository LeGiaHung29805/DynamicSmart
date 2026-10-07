export function getApiBaseUrl(): string {
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return baseUrl.replace(/\/$/, "");
}

export function getApiFallbackUrl(baseUrl = getApiBaseUrl()): string | undefined {
  const configured = process.env.NEXT_PUBLIC_API_FALLBACK_URL;
  if (configured) return configured.replace(/\/$/, "");

  try {
    const url = new URL(baseUrl);
    if ((url.hostname === "localhost" || url.hostname === "127.0.0.1") && url.port === "8080") {
      url.port = "28080";
      return url.toString().replace(/\/$/, "");
    }
  } catch {
    // URL cấu hình không hợp lệ sẽ được xử lý như lỗi kết nối tại request layer.
  }
  return undefined;
}

export function getInternalApiBaseUrl(): string {
  return (process.env.API_GATEWAY_INTERNAL_URL ?? getApiBaseUrl()).replace(/\/$/, "");
}
