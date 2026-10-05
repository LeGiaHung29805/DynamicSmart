import type { Metadata } from "next";
import { WishlistPage } from "@/features/wishlist";

export const metadata: Metadata = { title: "Yêu thích | DynamicMart" };

export default function Page() {
  return <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12"><WishlistPage /></div>;
}
