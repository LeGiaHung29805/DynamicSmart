import { apiClient } from "@/lib/api/client";
import { isCatalogStandaloneDemo } from "@/lib/api/config";
import { setAnonymousSession, setAuthenticatedSession, type SessionUser } from "@/lib/auth/session";

export type AuthenticatedUser = SessionUser;

export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  user: AuthenticatedUser;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput extends LoginInput {
  fullName: string;
}

async function saveSession(request: Promise<AuthResponse>) {
  const response = await request;
  setAuthenticatedSession(response.accessToken, response.user);
  return response;
}

function authPath(path: string) {
  const base = isCatalogStandaloneDemo() ? "/api/v1/catalog/demo/auth" : "/api/v1/auth";
  return `${base}/${path}`;
}

export function login(input: LoginInput) {
  return saveSession(apiClient.post<AuthResponse>(authPath("login"), input));
}

export function register(input: RegisterInput) {
  return saveSession(apiClient.post<AuthResponse>(authPath("register"), input));
}

export function refreshSession() {
  return saveSession(apiClient.post<AuthResponse>(authPath("refresh")));
}

export async function logout() {
  try {
    await apiClient.post<void>(authPath("logout"));
  } catch {
    // Thu hồi phiên phía máy chủ có thể thất bại do mất mạng; vẫn phải xóa token khỏi trình duyệt.
  } finally {
    setAnonymousSession();
  }
}

export function requestPasswordReset(email: string) {
  return apiClient.post<void>(authPath("password/forgot"), { email });
}

export function resetPassword(token: string, password: string) {
  return apiClient.post<void>(authPath("password/reset"), { token, password });
}
