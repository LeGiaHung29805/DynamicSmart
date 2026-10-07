import { Suspense } from "react";
import { StandaloneCheckoutPage } from "@/features/catalog/components/StandaloneCheckoutPage";

export default function CatalogDemoCheckoutRoute() {
  return <main className="mx-auto w-full max-w-3xl px-4 py-12 sm:px-6"><Suspense fallback={<p>Đang tải...</p>}><StandaloneCheckoutPage /></Suspense></main>;
}
