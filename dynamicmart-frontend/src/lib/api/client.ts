import { apiRequest, type ApiRequestOptions } from "./request";
import { getAccessToken } from "@/lib/auth/access-token";
import { ApiError } from "./error";
import { isCatalogStandaloneDemo } from "./config";
import { setAnonymousSession, setAuthenticatedSession, type SessionUser } from "@/lib/auth/session";

export interface RefreshedSession {
  accessToken: string;
  accessTokenExpiresAt: string;
  user: SessionUser;
}

let refreshPromise: Promise<RefreshedSession> | null = null;

function withAccessToken(options?: ApiRequestOptions): ApiRequestOptions {
  const headers = new Headers(options?.headers);
  const accessToken = getAccessToken();

  if (accessToken && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return { ...options, headers };
}

export function refreshAuthenticatedSession(): Promise<RefreshedSession> {
  if (!refreshPromise) {
    const path = isCatalogStandaloneDemo()
      ? "/api/v1/catalog/demo/auth/refresh"
      : "/api/v1/auth/refresh";
    refreshPromise = apiRequest<RefreshedSession>(path, { method: "POST" })
      .then((response) => {
        setAuthenticatedSession(response.accessToken, response.user);
        return response;
      })
      .catch((error) => {
        setAnonymousSession();
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

async function authenticatedRequest<T>(path: string, options: ApiRequestOptions): Promise<T> {
  try {
    return await apiRequest<T>(path, withAccessToken(options));
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401 || path.includes("/auth/")) throw error;
    try {
      await refreshAuthenticatedSession();
    } catch {
      throw error;
    }
    return apiRequest<T>(path, withAccessToken(options));
  }
}

export const apiClient = {
  get: <T>(path: string, options?: ApiRequestOptions) => authenticatedRequest<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: ApiRequestOptions["body"], options?: ApiRequestOptions) => authenticatedRequest<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: ApiRequestOptions["body"], options?: ApiRequestOptions) => authenticatedRequest<T>(path, { ...options, method: "PUT", body }),
  patch: <T>(path: string, body?: ApiRequestOptions["body"], options?: ApiRequestOptions) => authenticatedRequest<T>(path, { ...options, method: "PATCH", body }),
  delete: <T>(path: string, options?: ApiRequestOptions) => authenticatedRequest<T>(path, { ...options, method: "DELETE" }),
};
