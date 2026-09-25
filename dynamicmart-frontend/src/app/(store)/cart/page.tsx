import type { Metadata } from "next";
import { CartPage } from "@/features/cart";
export const metadata: Metadata = { title: "Giỏ hàng" };
export default function Page() { return <CartPage />; }
