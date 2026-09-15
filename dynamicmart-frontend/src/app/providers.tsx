"use client";

import { ToastProvider } from "@/components/ui/Toast";
import { SessionBootstrap } from "@/features/auth";

export function Providers({ children }: Readonly<{ children: React.ReactNode }>) {
  return <ToastProvider><SessionBootstrap />{children}</ToastProvider>;
}
