import { apiRequest, type ApiRequestOptions } from "./request";
import { getAccessToken } from "@/lib/auth/access-token";

function withAccessToken(options?: ApiRequestOptions): ApiRequestOptions {
  const headers = new Headers(options?.headers);
  const accessToken = getAccessToken();

  if (accessToken && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return { ...options, headers };
}

export const apiClient = {
  get: <T>(path: string, options?: ApiRequestOptions) => apiRequest<T>(path, { ...withAccessToken(options), method: "GET" }),
  post: <T>(path: string, body?: ApiRequestOptions["body"], options?: ApiRequestOptions) => apiRequest<T>(path, { ...withAccessToken(options), method: "POST", body }),
  put: <T>(path: string, body?: ApiRequestOptions["body"], options?: ApiRequestOptions) => apiRequest<T>(path, { ...withAccessToken(options), method: "PUT", body }),
  patch: <T>(path: string, body?: ApiRequestOptions["body"], options?: ApiRequestOptions) => apiRequest<T>(path, { ...withAccessToken(options), method: "PATCH", body }),
  delete: <T>(path: string, options?: ApiRequestOptions) => apiRequest<T>(path, { ...withAccessToken(options), method: "DELETE" }),
};
