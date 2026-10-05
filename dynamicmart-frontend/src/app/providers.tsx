"use client";

import { ToastProvider } from "@/components/ui/Toast";
import { SessionBootstrap } from "@/features/auth";
import { WishlistProvider } from "@/features/wishlist";

export function Providers({ children }: Readonly<{ children: React.ReactNode }>) {
  return <ToastProvider><SessionBootstrap /><WishlistProvider>{children}</WishlistProvider></ToastProvider>;
}
