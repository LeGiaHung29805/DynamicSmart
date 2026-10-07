import type { ApiErrorPayload, ApiResponse } from "@/contracts/api/common";
import { isApiResponse } from "@/contracts/api/common";
import { getApiBaseUrl, getApiFallbackUrl } from "./config";
import { ApiError } from "./error";

export type ApiRequestOptions = Omit<RequestInit, "body"> & {
  /** Đối tượng thường được JSON hóa; FormData/Blob/URLSearchParams giữ nguyên. */
  body?: BodyInit | object | null;
  baseUrl?: string;
};

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { baseUrl = getApiBaseUrl(), body, headers, ...init } = options;
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
  const shouldSerialize = body !== undefined && body !== null && typeof body === "object" && !isFormData && !(body instanceof URLSearchParams) && !(body instanceof Blob);
  const requestHeaders = new Headers(headers);

  if (shouldSerialize && !requestHeaders.has("Content-Type")) requestHeaders.set("Content-Type", "application/json");
  requestHeaders.set("Accept", "application/json");

  const requestBody: BodyInit | null | undefined = shouldSerialize
    ? JSON.stringify(body)
    : (body as BodyInit | null | undefined);

  const fetchFrom = (targetBaseUrl: string) => fetch(`${targetBaseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`, {
    ...init,
    body: requestBody,
    credentials: "include",
    headers: requestHeaders,
  });

  const fallbackUrl = getApiFallbackUrl(baseUrl);
  let response: Response;
  try {
    response = await fetchFrom(baseUrl);
  } catch {
    if (!fallbackUrl || fallbackUrl === baseUrl) {
      throw new ApiError(0, { code: "NETWORK_ERROR", message: "Không thể kết nối đến hệ thống. Vui lòng thử lại." });
    }
    try {
      response = await fetchFrom(fallbackUrl);
    } catch {
      throw new ApiError(0, { code: "NETWORK_ERROR", message: "Không thể kết nối API Gateway tại cổng 8080 hoặc 28080." });
    }
  }

  // Nếu 8080 thuộc một ứng dụng khác, trình duyệt thường nhận 404 HTML hoặc lỗi CORS.
  // Chỉ retry 404 không phải JSON để không che mất lỗi 404 hợp lệ từ DynamicMart.
  if (fallbackUrl && response.status === 404 && !response.headers.get("content-type")?.includes("application/json")) {
    try {
      response = await fetchFrom(fallbackUrl);
    } catch {
      // Giữ response ban đầu để phía dưới trả lỗi có ngữ cảnh thay vì mất hoàn toàn phản hồi.
    }
  }

  if (response.status === 204) return undefined as T;

  const payload = (await response.json().catch(() => ({}))) as T | ApiResponse<T> | ApiErrorPayload;
  if (!response.ok) throw new ApiError(response.status, payload as ApiErrorPayload);

  return isApiResponse<T>(payload) ? payload.data : (payload as T);
}
