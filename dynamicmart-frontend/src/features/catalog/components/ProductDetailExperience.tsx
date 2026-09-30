"use client";

import { useMemo, useState } from "react";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { BadgePercent, Check, Minus, PackageCheck, Plus, ShieldCheck, ShoppingBag, Zap } from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { useToast } from "@/components/ui/Toast";
import { isApiError } from "@/lib/api/error";
import { addVariantToCart, createBuyNowSession } from "../api/purchase.api";
import type { AttributeValue, ProductDetail, ProductImage, ProductVariant } from "../types";

function scalar(value: unknown): string {
  if (Array.isArray(value)) return value.map(String).join(", ");
  if (typeof value === "boolean") return value ? "Có" : "Không";
  return value === null || value === undefined ? "" : String(value);
}

function attributeValue(variant: ProductVariant, code: string): string | undefined {
  const value = variant.attributes.find((attribute) => attribute.attributeCode === code)?.value;
  return value === undefined ? undefined : scalar(value);
}

function primaryImage(images: ProductImage[]): ProductImage | undefined {
  return images.find((image) => image.primary) ?? images[0];
}

function loginReturnTo(pathname: string, variantId: string, quantity: number) {
  const resumeUntil = Date.now() + 15 * 60 * 1000;
  return `${pathname}?variant=${encodeURIComponent(variantId)}&quantity=${quantity}&resumeUntil=${resumeUntil}`;
}

export function ProductDetailExperience({ product, initialVariantId, initialQuantity = 1 }: Readonly<{
  product: ProductDetail;
  initialVariantId?: string;
  initialQuantity?: number;
}>) {
  const router = useRouter();
  const pathname = usePathname();
  const { showToast } = useToast();
  const initialVariant = product.variants.find((variant) => variant.id === initialVariantId)
    ?? (product.variants.length === 1 ? product.variants[0] : undefined);
  const [selections, setSelections] = useState<Record<string, string>>(() => Object.fromEntries(
    initialVariant?.attributes.map((attribute) => [attribute.attributeCode, scalar(attribute.value)]) ?? [],
  ));
  const [quantity, setQuantity] = useState(Math.max(1, initialQuantity));
  const [pendingAction, setPendingAction] = useState<"cart" | "buy" | null>(null);
  const [error, setError] = useState<string>();

  const attributeGroups = useMemo(() => {
    const groups = new Map<string, { name: string; values: string[] }>();
    product.variants.forEach((variant) => variant.attributes.forEach((attribute) => {
      const current = groups.get(attribute.attributeCode) ?? { name: attribute.attributeName, values: [] };
      const value = scalar(attribute.value);
      if (value && !current.values.includes(value)) current.values.push(value);
      groups.set(attribute.attributeCode, current);
    }));
    return [...groups.entries()].map(([code, group]) => ({ code, ...group }));
  }, [product.variants]);

  const selectedVariant = product.variants.find((variant) => attributeGroups.every(
    (group) => selections[group.code] && attributeValue(variant, group.code) === selections[group.code],
  ));
  const images = selectedVariant?.images.length ? selectedVariant.images : product.images;
  const [selectedImageId, setSelectedImageId] = useState<string | undefined>(() => primaryImage(
    initialVariant?.images.length ? initialVariant.images : product.images,
  )?.id);
  const selectedImage = images.find((image) => image.id === selectedImageId) ?? primaryImage(images);
  const maxQuantity = selectedVariant?.inventory.availableQuantity ?? 0;
  const canPurchase = Boolean(selectedVariant?.purchasable && quantity >= 1 && quantity <= maxQuantity);

  function selectAttribute(code: string, value: string) {
    const next = { ...selections, [code]: value };
    setSelections(next);
    const variant = product.variants.find((candidate) => attributeGroups.every(
      (group) => next[group.code] && attributeValue(candidate, group.code) === next[group.code],
    ));
    if (variant) {
      setQuantity(1);
      setSelectedImageId(primaryImage(variant.images)?.id ?? primaryImage(product.images)?.id);
      router.replace(`${pathname}?variant=${encodeURIComponent(variant.id)}&quantity=1`, { scroll: false });
    }
  }

  async function addToCart() {
    if (!selectedVariant || !canPurchase) return;
    try {
      setPendingAction("cart"); setError(undefined);
      await addVariantToCart(selectedVariant.id, quantity);
      showToast("Đã thêm sản phẩm vào giỏ hàng.", "success");
    } catch (caught) {
      if (isApiError(caught) && caught.status === 401) {
        router.push(`/login?returnTo=${encodeURIComponent(loginReturnTo(pathname, selectedVariant.id, quantity))}`);
      } else setError(isApiError(caught) ? caught.message : "Chưa thể thêm vào giỏ hàng.");
    } finally { setPendingAction(null); }
  }

  async function buyNow() {
    if (!selectedVariant || !canPurchase) return;
    try {
      setPendingAction("buy"); setError(undefined);
      const session = await createBuyNowSession(selectedVariant.id, quantity);
      router.push(`/checkout?sessionId=${encodeURIComponent(session.checkoutSessionId)}`);
    } catch (caught) {
      if (isApiError(caught) && caught.status === 401) {
        router.push(`/login?returnTo=${encodeURIComponent(loginReturnTo(pathname, selectedVariant.id, quantity))}`);
      } else setError(isApiError(caught) ? caught.message : "Chưa thể bắt đầu Mua ngay.");
    } finally { setPendingAction(null); }
  }

  const currentPrice = selectedVariant?.price;
  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,.95fr)] lg:gap-12">
      <div className="space-y-3">
        <div className="relative aspect-square overflow-hidden rounded-[2rem] bg-slate-100 shadow-xl shadow-slate-950/5">
          {selectedImage ? <Image alt={selectedImage.altText ?? product.name} className="object-cover" fill priority sizes="(max-width: 1024px) 100vw, 52vw" src={selectedImage.imageUrl} unoptimized /> : <div className="grid size-full place-items-center font-bold text-slate-400">Chưa có ảnh</div>}
          {currentPrice?.salePriceVnd !== null && currentPrice?.salePriceVnd !== undefined ? <span className="absolute top-5 left-5 inline-flex items-center gap-1 rounded-full bg-amber-300 px-4 py-2 text-xs font-black text-amber-950"><BadgePercent className="size-4" /> Giảm {currentPrice.directSalePercent ?? 0}%</span> : null}
        </div>
        {images.length > 1 ? <div className="flex gap-3 overflow-x-auto pb-1">{images.map((image) => <button aria-label={`Xem ảnh ${image.altText ?? product.name}`} className={`relative size-20 shrink-0 overflow-hidden rounded-xl border-2 bg-slate-100 ${selectedImage?.id === image.id ? "border-brand" : "border-transparent"}`} key={image.id} onClick={() => setSelectedImageId(image.id)} type="button"><Image alt="" className="object-cover" fill sizes="80px" src={image.imageUrl} unoptimized /></button>)}</div> : null}
      </div>

      <div className="space-y-6 lg:py-2">
        <div><p className="text-xs font-black tracking-[0.18em] text-brand uppercase">{product.category.name}</p><h1 className="mt-3 text-3xl font-black tracking-[-0.04em] text-slate-950 sm:text-4xl">{product.name}</h1><p className="mt-3 text-sm leading-6 text-slate-500">{product.shortDescription}</p></div>
        <div className="flex flex-wrap items-end gap-3 border-y border-slate-200 py-5">
          {currentPrice ? <><span className="text-3xl font-black text-emerald-800">{formatVnd(currentPrice.salePriceVnd ?? currentPrice.listPriceVnd)}</span>{currentPrice.salePriceVnd !== null && currentPrice.salePriceVnd !== undefined ? <del className="pb-1 text-sm text-slate-400">{formatVnd(currentPrice.listPriceVnd)}</del> : null}</> : <span className="text-sm font-semibold text-slate-500">Chọn đủ thuộc tính để xem giá</span>}
        </div>

        {attributeGroups.map((group) => <fieldset className="space-y-2" key={group.code}><legend className="text-sm font-bold text-slate-800">{group.name}{selections[group.code] ? <span className="ml-2 font-medium text-slate-500">· {selections[group.code]}</span> : null}</legend><div className="flex flex-wrap gap-2">{group.values.map((value) => <button aria-pressed={selections[group.code] === value} className={`inline-flex min-h-10 items-center gap-1.5 rounded-xl border px-4 text-sm font-bold transition ${selections[group.code] === value ? "border-emerald-700 bg-emerald-50 text-emerald-900" : "border-slate-200 bg-white text-slate-700 hover:border-emerald-300"}`} key={value} onClick={() => selectAttribute(group.code, value)} type="button">{selections[group.code] === value ? <Check className="size-3.5" /> : null}{value}</button>)}</div></fieldset>)}

        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>{selectedVariant ? <><p className="text-sm font-bold text-slate-900">{selectedVariant.name ?? selectedVariant.sku}</p><p className="mt-1 text-xs text-slate-500">SKU: {selectedVariant.sku} · {maxQuantity > 0 ? `Còn ${maxQuantity} sản phẩm` : "Hết hàng"}</p><p className="mt-1 text-xs text-slate-500">Khối lượng: {selectedVariant.weightGrams.toLocaleString("vi-VN")} g{selectedVariant.lengthCm && selectedVariant.widthCm && selectedVariant.heightCm ? ` · Gói ${selectedVariant.lengthCm} × ${selectedVariant.widthCm} × ${selectedVariant.heightCm} cm` : ""}</p></> : <p className="text-sm font-semibold text-amber-700">Vui lòng chọn đủ thuộc tính bắt buộc.</p>}</div>
            <div className="inline-flex items-center rounded-xl border border-slate-200"><button aria-label="Giảm số lượng" className="grid size-10 place-items-center disabled:opacity-40" disabled={quantity <= 1} onClick={() => setQuantity((current) => Math.max(1, current - 1))} type="button"><Minus className="size-4" /></button><input aria-label="Số lượng" className="h-10 w-12 border-x border-slate-200 text-center text-sm font-bold outline-none" max={Math.max(maxQuantity, 1)} min="1" onChange={(event) => setQuantity(Math.max(1, Number(event.target.value) || 1))} type="number" value={quantity} /><button aria-label="Tăng số lượng" className="grid size-10 place-items-center disabled:opacity-40" disabled={!selectedVariant || quantity >= maxQuantity} onClick={() => setQuantity((current) => Math.min(maxQuantity, current + 1))} type="button"><Plus className="size-4" /></button></div>
          </div>
          {error ? <p className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-danger" role="alert">{error}</p> : null}
          <div className="mt-5 grid gap-3 sm:grid-cols-2"><button className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-emerald-800 bg-white px-5 text-sm font-black text-emerald-900 transition hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-50" disabled={!canPurchase || pendingAction !== null} onClick={addToCart} type="button"><ShoppingBag className="size-4" />{pendingAction === "cart" ? "Đang thêm..." : "Thêm vào giỏ"}</button><button className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-emerald-950 px-5 text-sm font-black text-white transition hover:bg-brand disabled:cursor-not-allowed disabled:opacity-50" disabled={!canPurchase || pendingAction !== null} onClick={buyNow} type="button"><Zap className="size-4" />{pendingAction === "buy" ? "Đang xử lý..." : "Mua ngay"}</button></div>
        </div>
        <div className="grid gap-3 text-sm text-slate-600 sm:grid-cols-2"><p className="flex items-center gap-2"><PackageCheck className="size-5 text-brand" /> Tồn kho được kiểm tra tại máy chủ</p><p className="flex items-center gap-2"><ShieldCheck className="size-5 text-brand" /> Giá được xác thực lại khi đặt hàng</p></div>
      </div>
    </div>
  );
}

export function ProductSpecifications({ attributes }: Readonly<{ attributes: AttributeValue[] }>) {
  if (!attributes.length) return null;
  return <dl className="grid gap-px overflow-hidden rounded-2xl border border-slate-200 bg-slate-200 sm:grid-cols-2">{attributes.map((attribute) => <div className="flex items-center justify-between gap-4 bg-white px-5 py-4" key={attribute.id}><dt className="text-sm text-slate-500">{attribute.attributeName}</dt><dd className="text-sm font-bold text-slate-900">{scalar(attribute.value)}</dd></div>)}</dl>;
}
