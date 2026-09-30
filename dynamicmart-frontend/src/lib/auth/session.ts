"use client";

import { useSyncExternalStore } from "react";
import { setAccessToken } from "./access-token";

export interface SessionUser {
  id: string;
  email: string;
  fullName: string;
  role: "CUSTOMER" | "ADMIN";
}

export type AuthSession =
  | { status: "loading"; user: null }
  | { status: "anonymous"; user: null }
  | { status: "authenticated"; user: SessionUser };

let session: AuthSession = { status: "loading", user: null };
const listeners = new Set<() => void>();

export function setAuthenticatedSession(accessToken: string, user: SessionUser) {
  setAccessToken(accessToken);
  session = { status: "authenticated", user };
  listeners.forEach((listener) => listener());
}

export function setAnonymousSession() {
  setAccessToken(null);
  session = { status: "anonymous", user: null };
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function snapshot() {
  return session;
}

const serverSnapshot: AuthSession = { status: "loading", user: null };

export function useAuthSession() {
  return useSyncExternalStore(subscribe, snapshot, () => serverSnapshot);
}
