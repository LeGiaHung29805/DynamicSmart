"use client";

import { useMemo, useState } from "react";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { BadgePercent, Check, Minus, PackageCheck, PackageX, Plus, ShieldCheck, ShoppingBag, Star, Zap } from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { useToast } from "@/components/ui/Toast";
import { isApiError } from "@/lib/api/error";
import { isCatalogStandaloneDemo } from "@/lib/api/config";
import { WishlistButton } from "@/features/wishlist";
import { addVariantToCart, createBuyNowSession } from "../api/purchase.api";
import type { AttributeValue, ProductDetail, ProductImage, ProductVariant } from "../types";
import {
  buildVariantSelectionGroups,
  exactVariantForSelections,
  optionAvailability,
  selectionAfterChoosingOption,
  selectionsForVariant,
  singleValueSelections,
  variantValue,
} from "../utils/variant-selection";

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
  const attributeGroups = useMemo(
    () => buildVariantSelectionGroups(product.variants),
    [product.variants],
  );
  const requestedVariant = product.variants.find((variant) => variant.id === initialVariantId);
  const automaticVariant = product.variants.length === 1
    ? product.variants[0]
    : attributeGroups.length === 0
      ? product.variants.find((variant) => variant.purchasable) ?? product.variants[0]
      : undefined;
  const initialSelections = requestedVariant || automaticVariant
    ? selectionsForVariant(requestedVariant ?? automaticVariant)
    : singleValueSelections(attributeGroups);
  const initialVariant = requestedVariant
    ?? automaticVariant
    ?? exactVariantForSelections(product.variants, attributeGroups, initialSelections);
  const initialAvailableQuantity = initialVariant?.inventory.availableQuantity ?? 0;
  const [selectedVariantId, setSelectedVariantId] = useState<string | undefined>(initialVariant?.id);
  const [selections, setSelections] = useState<Record<string, string>>(initialSelections);
  const [quantity, setQuantity] = useState(Math.min(
    Math.max(1, initialQuantity),
    Math.max(initialAvailableQuantity, 1),
  ));
  const [pendingAction, setPendingAction] = useState<"cart" | "buy" | null>(null);
  const [error, setError] = useState<string>();
  const [selectionNotice, setSelectionNotice] = useState<string>();

  const selectedVariant = product.variants.find((variant) => variant.id === selectedVariantId);
  const allOutOfStock = product.variants.every((variant) => !variant.purchasable);
  const images = selectedVariant?.images.length ? selectedVariant.images : product.images;
  const [selectedImageId, setSelectedImageId] = useState<string | undefined>(() => primaryImage(
    initialVariant?.images.length ? initialVariant.images : product.images,
  )?.id);
  const selectedImage = images.find((image) => image.id === selectedImageId) ?? primaryImage(images);
  const maxQuantity = selectedVariant?.inventory.availableQuantity ?? 0;
  const canPurchase = Boolean(selectedVariant?.purchasable && quantity >= 1 && quantity <= maxQuantity);
  const reviewCount = product.reviewCount ?? 0;
  const averageRating = product.averageRating ?? 0;

  function chooseVariant(variant: ProductVariant) {
    setSelectedVariantId(variant.id);
    setSelections(selectionsForVariant(variant));
    setQuantity(1);
    setSelectedImageId(primaryImage(variant.images)?.id ?? primaryImage(product.images)?.id);
    setSelectionNotice(undefined);
    router.replace(`${pathname}?variant=${encodeURIComponent(variant.id)}&quantity=1`, { scroll: false });
  }

  function selectAttribute(code: string, value: string) {
    const nextSelection = selectionAfterChoosingOption(product.variants, selections, code, value);
    const next = nextSelection.selections;
    const variant = exactVariantForSelections(product.variants, attributeGroups, next);
    setSelections(next);
    setSelectionNotice(nextSelection.clearedIncompatible
      ? "Một số lựa chọn trước đó không có cùng tổ hợp nên đã được bỏ chọn. Vui lòng chọn lại các mục còn thiếu."
      : undefined);
    setSelectedVariantId(variant?.id);
    setQuantity(1);
    setSelectedImageId(primaryImage(variant?.images.length ? variant.images : product.images)?.id);
    if (variant) {
      router.replace(`${pathname}?variant=${encodeURIComponent(variant.id)}&quantity=1`, { scroll: false });
    } else {
      router.replace(pathname, { scroll: false });
    }
  }

  async function addToCart() {
    if (!selectedVariant || !canPurchase) return;
    try {
      setPendingAction("cart"); setError(undefined);
      await addVariantToCart(product.id, selectedVariant.id, quantity);
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
      const checkoutPath = isCatalogStandaloneDemo() ? "/catalog-demo/checkout" : "/checkout";
      router.push(`${checkoutPath}?sessionId=${encodeURIComponent(session.id)}`);
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
          <WishlistButton className="absolute top-5 right-5" productId={product.id} />
        </div>
        {images.length > 1 ? <div className="flex gap-3 overflow-x-auto pb-1">{images.map((image) => <button aria-label={`Xem ảnh ${image.altText ?? product.name}`} className={`relative size-20 shrink-0 overflow-hidden rounded-xl border-2 bg-slate-100 ${selectedImage?.id === image.id ? "border-brand" : "border-transparent"}`} key={image.id} onClick={() => setSelectedImageId(image.id)} type="button"><Image alt="" className="object-cover" fill sizes="80px" src={image.imageUrl} unoptimized /></button>)}</div> : null}
      </div>

      <div className="space-y-6 lg:py-2">
        <div><p className="text-xs font-black tracking-[0.18em] text-brand uppercase">{product.category.name}</p><h1 className="mt-3 text-3xl font-black tracking-[-0.04em] text-slate-950 sm:text-4xl">{product.name}</h1>{reviewCount > 0 ? <p className="mt-3 inline-flex items-center gap-1.5 text-sm font-bold text-amber-700"><Star className="size-4 fill-current" /> {averageRating.toFixed(1)} <span className="font-medium text-slate-500">({reviewCount} đánh giá)</span></p> : null}<p className="mt-3 text-sm leading-6 text-slate-500">{product.shortDescription}</p></div>
        <div className="flex flex-wrap items-end gap-3 border-y border-slate-200 py-5">
          {currentPrice ? <><span className="text-3xl font-black text-emerald-800">{formatVnd(currentPrice.salePriceVnd ?? currentPrice.listPriceVnd)}</span>{currentPrice.salePriceVnd !== null && currentPrice.salePriceVnd !== undefined ? <del className="pb-1 text-sm text-slate-400">{formatVnd(currentPrice.listPriceVnd)}</del> : null}</> : <span className="text-sm font-semibold text-slate-500">Chọn đủ thuộc tính để xem giá</span>}
        </div>

        {allOutOfStock ? (
          <div className="flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-rose-900" role="status">
            <PackageX className="mt-0.5 size-5 shrink-0" />
            <div><p className="font-black">Sản phẩm tạm hết hàng</p><p className="mt-1 text-sm leading-6 text-rose-800">Bạn vẫn có thể xem đầy đủ các phiên bản, giá và thông tin sản phẩm. Chức năng mua sẽ mở lại khi có tồn kho.</p></div>
          </div>
        ) : null}

        {attributeGroups.map((group) => (
          <fieldset className="space-y-2" key={group.code}>
            <legend className="text-sm font-bold text-slate-800">{group.name}{selections[group.code] ? <span className="ml-2 font-medium text-slate-500">· {group.labels[selections[group.code]] ?? selections[group.code]}</span> : null}</legend>
            <div className="flex flex-wrap gap-2">
              {group.values.map((value) => {
                const availability = optionAvailability(product.variants, selections, group.code, value);
                const globalAvailability = optionAvailability(product.variants, {}, group.code, value);
                const selected = selections[group.code] === value;
                return (
                  <button
                    aria-pressed={selected}
                    className={`inline-flex min-h-10 items-center gap-1.5 rounded-xl border px-4 text-sm font-bold transition ${selected && availability.purchasable ? "border-emerald-700 bg-emerald-50 text-emerald-900" : selected ? "border-rose-300 bg-rose-50 text-rose-800" : !availability.exists && globalAvailability.purchasable ? "border-amber-200 bg-amber-50 text-amber-800 hover:border-amber-400" : availability.purchasable ? "border-slate-200 bg-white text-slate-700 hover:border-emerald-300" : "border-rose-200 bg-rose-50/60 text-rose-700 line-through hover:border-rose-300"}`}
                    key={value}
                    onClick={() => selectAttribute(group.code, value)}
                    title={!availability.exists && globalAvailability.purchasable ? "Chọn mục này sẽ bỏ những lựa chọn không tương thích" : !globalAvailability.purchasable ? "Các biến thể có lựa chọn này đang hết hàng" : undefined}
                    type="button"
                  >
                    {selected ? <Check className="size-3.5" /> : null}
                    {group.labels[value] ?? value}
                    {!globalAvailability.purchasable ? <span className="ml-1 text-[10px] font-black tracking-wide uppercase">Hết hàng</span> : !availability.exists ? <span className="ml-1 text-[10px] font-black tracking-wide uppercase">Đổi tổ hợp</span> : null}
                  </button>
                );
              })}
            </div>
          </fieldset>
        ))}
        {selectionNotice ? <p className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs font-semibold leading-5 text-amber-900" role="status">{selectionNotice}</p> : null}

        {attributeGroups.length === 0 && product.variants.length > 1 ? <section aria-labelledby="product-variants-title" className="space-y-3">
          <div className="flex items-end justify-between gap-3">
            <div><p className="text-xs font-black tracking-[0.16em] text-brand uppercase">Phiên bản sản phẩm</p><h2 className="mt-1 text-lg font-black text-slate-950" id="product-variants-title">Tất cả {product.variants.length} biến thể</h2></div>
            <p className="text-xs text-slate-500">Chọn một phiên bản còn hàng</p>
          </div>
          <div className="grid max-h-72 gap-2 overflow-y-auto pr-1 sm:grid-cols-2">
            {product.variants.map((variant) => {
              const selected = selectedVariant?.id === variant.id;
              const variantPrice = variant.price.salePriceVnd ?? variant.price.listPriceVnd;
              const details = variant.attributes.map((attribute) => `${attribute.attributeName}: ${attribute.displayValue || variantValue(attribute.value)}`).join(" · ");
              return (
                <button
                  aria-pressed={selected}
                  className={`rounded-2xl border p-3 text-left transition ${selected && variant.purchasable ? "border-emerald-700 bg-emerald-50 shadow-sm" : selected ? "border-rose-300 bg-rose-50 shadow-sm" : variant.purchasable ? "border-slate-200 bg-white hover:border-emerald-300" : "border-slate-200 bg-slate-50 opacity-80 hover:border-rose-300"}`}
                  key={variant.id}
                  onClick={() => chooseVariant(variant)}
                  type="button"
                >
                  <span className="flex items-start justify-between gap-3"><span className="min-w-0"><span className="block truncate text-sm font-black text-slate-950">{variant.name || variant.sku}</span><span className="mt-0.5 block font-mono text-[11px] text-slate-500">{variant.sku}</span></span><span className={`shrink-0 rounded-full px-2 py-1 text-[10px] font-black uppercase ${variant.purchasable ? "bg-emerald-100 text-emerald-800" : "bg-rose-100 text-rose-800"}`}>{variant.purchasable ? `Còn ${variant.inventory.availableQuantity}` : "Hết hàng"}</span></span>
                  {details ? <span className="mt-2 block line-clamp-2 text-xs leading-5 text-slate-600">{details}</span> : <span className="mt-2 block text-xs text-slate-400">Phiên bản theo SKU</span>}
                  <span className="mt-2 block text-sm font-black text-emerald-800">{formatVnd(variantPrice)}</span>
                </button>
              );
            })}
          </div>
        </section> : attributeGroups.length > 0 ? <p className="rounded-xl bg-slate-50 px-4 py-3 text-xs leading-5 text-slate-600">Có {product.variants.length} cấu hình sản phẩm. Hãy chọn lần lượt các thuộc tính phía trên; hệ thống chỉ giữ nguyên đúng tổ hợp bạn đã chọn.</p> : null}

        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>{selectedVariant ? <><p className="text-sm font-bold text-slate-900">{selectedVariant.name ?? selectedVariant.sku}</p><p className="mt-1 text-xs text-slate-500">SKU: {selectedVariant.sku} · {maxQuantity > 0 ? `Còn ${maxQuantity} sản phẩm` : "Hết hàng"}</p><p className="mt-1 text-xs text-slate-500">Khối lượng: {selectedVariant.weightGrams.toLocaleString("vi-VN")} g{selectedVariant.lengthCm && selectedVariant.widthCm && selectedVariant.heightCm ? ` · Kiện hàng ${selectedVariant.lengthCm} × ${selectedVariant.widthCm} × ${selectedVariant.heightCm} cm` : ""}</p></> : <p className="text-sm font-semibold text-amber-700">Vui lòng chọn đủ màu sắc, kích cỡ hoặc cấu hình để xác định chính xác sản phẩm.</p>}</div>
            <div className="inline-flex items-center rounded-xl border border-slate-200"><button aria-label="Giảm số lượng" className="grid size-10 place-items-center disabled:opacity-40" disabled={!selectedVariant?.purchasable || quantity <= 1} onClick={() => setQuantity((current) => Math.max(1, current - 1))} type="button"><Minus className="size-4" /></button><input aria-label="Số lượng" className="h-10 w-12 border-x border-slate-200 text-center text-sm font-bold outline-none disabled:bg-slate-100 disabled:text-slate-400" disabled={!selectedVariant?.purchasable} max={Math.max(maxQuantity, 1)} min="1" onChange={(event) => setQuantity(Math.min(Math.max(1, Number(event.target.value) || 1), Math.max(maxQuantity, 1)))} type="number" value={quantity} /><button aria-label="Tăng số lượng" className="grid size-10 place-items-center disabled:opacity-40" disabled={!selectedVariant?.purchasable || quantity >= maxQuantity} onClick={() => setQuantity((current) => Math.min(maxQuantity, current + 1))} type="button"><Plus className="size-4" /></button></div>
          </div>
          {error ? <p className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-danger" role="alert">{error}</p> : null}
          <div className="mt-5 grid gap-3 sm:grid-cols-2"><button className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-emerald-800 bg-white px-5 text-sm font-black text-emerald-900 transition hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-50" disabled={!canPurchase || pendingAction !== null} onClick={addToCart} type="button"><ShoppingBag className="size-4" />{pendingAction === "cart" ? "Đang thêm..." : !selectedVariant ? "Chọn đủ thuộc tính" : !selectedVariant.purchasable ? "Biến thể hết hàng" : "Thêm vào giỏ"}</button><button className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-emerald-950 px-5 text-sm font-black text-white transition hover:bg-brand disabled:cursor-not-allowed disabled:opacity-50" disabled={!canPurchase || pendingAction !== null} onClick={buyNow} type="button"><Zap className="size-4" />{pendingAction === "buy" ? "Đang xử lý..." : !selectedVariant ? "Chọn đủ thuộc tính" : !selectedVariant.purchasable ? "Biến thể hết hàng" : "Mua ngay"}</button></div>
        </div>
        <div className="grid gap-3 text-sm text-slate-600 sm:grid-cols-2"><p className="flex items-center gap-2"><PackageCheck className="size-5 text-brand" /> Tồn kho được kiểm tra tại máy chủ</p><p className="flex items-center gap-2"><ShieldCheck className="size-5 text-brand" /> Giá được xác thực lại khi đặt hàng</p></div>
      </div>
    </div>
  );
}

export function ProductSpecifications({ attributes }: Readonly<{ attributes: AttributeValue[] }>) {
  if (!attributes.length) return null;
  return <dl className="grid gap-px overflow-hidden rounded-2xl border border-slate-200 bg-slate-200 sm:grid-cols-2">{attributes.map((attribute) => <div className="flex items-center justify-between gap-4 bg-white px-5 py-4" key={attribute.id}><dt className="text-sm text-slate-500">{attribute.attributeName}</dt><dd className="text-sm font-bold text-slate-900">{attribute.displayValue || variantValue(attribute.value)}</dd></div>)}</dl>;
}
