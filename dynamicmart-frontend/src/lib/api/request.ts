import type { ApiErrorPayload, ApiResponse } from "@/contracts/api/common";
import { isApiResponse } from "@/contracts/api/common";
import { getApiBaseUrl } from "./config";
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

  let response: Response;
  try {
    response = await fetch(`${baseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`, {
      ...init,
      body: requestBody,
      credentials: "include",
      headers: requestHeaders,
    });
  } catch {
    throw new ApiError(0, { code: "NETWORK_ERROR", message: "Không thể kết nối đến hệ thống. Vui lòng thử lại." });
  }

  if (response.status === 204) return undefined as T;

  const payload = (await response.json().catch(() => ({}))) as T | ApiResponse<T> | ApiErrorPayload;
  if (!response.ok) throw new ApiError(response.status, payload as ApiErrorPayload);

  return isApiResponse<T>(payload) ? payload.data : (payload as T);
}
