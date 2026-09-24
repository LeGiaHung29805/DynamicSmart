import { apiClient } from "@/lib/api/client";
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

export function login(input: LoginInput) {
  return saveSession(apiClient.post<AuthResponse>("/api/v1/auth/login", input));
}

export function register(input: RegisterInput) {
  return saveSession(apiClient.post<AuthResponse>("/api/v1/auth/register", input));
}

export function refreshSession() {
  return saveSession(apiClient.post<AuthResponse>("/api/v1/auth/refresh"));
}

export async function logout() {
  try {
    await apiClient.post<void>("/api/v1/auth/logout");
  } finally {
    setAnonymousSession();
  }
}

export function requestPasswordReset(email: string) {
  return apiClient.post<void>("/api/v1/auth/password/forgot", { email });
}

export function resetPassword(token: string, password: string) {
  return apiClient.post<void>("/api/v1/auth/password/reset", { token, password });
}
