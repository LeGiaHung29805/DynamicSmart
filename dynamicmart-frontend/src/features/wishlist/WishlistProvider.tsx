"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { customerApi } from "@/features/customer/api/customer.api";
import type { WishlistItem } from "@/features/customer/types/customer.types";
import { useAuthSession } from "@/lib/auth/session";

type WishlistContextValue = {
  items: WishlistItem[];
  loading: boolean;
  pending: ReadonlySet<string>;
  isFavorite: (productId: string) => boolean;
  toggle: (productId: string) => Promise<boolean>;
};

const WishlistContext = createContext<WishlistContextValue | null>(null);

export function WishlistProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const session = useAuthSession();
  const [items, setItems] = useState<WishlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (session.status === "loading") return;
    if (session.status === "anonymous") return;
    let cancelled = false;
    customerApi.wishlist().then((value) => { if (!cancelled) setItems(value.items); })
      .catch(() => { if (!cancelled) setItems([]); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [session.status]);

  const visibleItems = useMemo(() => session.status === "authenticated" ? items : [], [items, session.status]);
  const favoriteIds = useMemo(() => new Set(visibleItems.map((item) => item.productId)), [visibleItems]);
  const toggle = useCallback(async (productId: string) => {
    if (session.status !== "authenticated" || pending.has(productId)) return false;
    const removing = favoriteIds.has(productId);
    setPending((current) => new Set(current).add(productId));
    try {
      const value = removing ? await customerApi.removeFromWishlist(productId) : await customerApi.addToWishlist(productId);
      setItems(value.items); return !removing;
    } finally {
      setPending((current) => { const next = new Set(current); next.delete(productId); return next; });
    }
  }, [favoriteIds, pending, session.status]);

  const value = useMemo<WishlistContextValue>(() => ({ items: visibleItems, loading: session.status === "loading" || (session.status === "authenticated" && loading), pending,
    isFavorite: (productId) => favoriteIds.has(productId), toggle,
  }), [favoriteIds, loading, pending, session.status, toggle, visibleItems]);
  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

export function useWishlist() {
  const value = useContext(WishlistContext);
  if (!value) throw new Error("useWishlist must be used inside WishlistProvider");
  return value;
}
