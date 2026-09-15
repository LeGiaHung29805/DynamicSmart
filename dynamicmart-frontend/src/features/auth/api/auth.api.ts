import { apiClient } from "@/lib/api/client";
import { setAccessToken } from "@/lib/auth/access-token";

export interface AuthenticatedUser {
  id: string;
  email: string;
  fullName: string;
  role: "CUSTOMER" | "ADMIN";
}

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
  setAccessToken(response.accessToken);
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
  await apiClient.post<void>("/api/v1/auth/logout");
  setAccessToken(null);
}
