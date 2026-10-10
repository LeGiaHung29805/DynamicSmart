"use client";

import {
  AlertCircle,
  Heart,
  Inbox,
  RefreshCw,
  ShoppingBag,
  ShoppingCart,
  Trash2,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import type { ProductDetail } from "@/features/catalog/types";
import { cartApi } from "@/features/cart/api/cart.api";
import { apiClient } from "@/lib/api/client";
import { useAuthSession } from "@/lib/auth/session";
import { engagementApi } from "../api/engagement.api";
import { date, money } from "./EngagementShared";

interface DisplayWishlistItem {
  id: string;
  productId: string;
  name: string;
  slug?: string;
  categoryName?: string;
  imageUrl?: string;
  price?: number;
  variantId?: string;
  inStock?: boolean;
  savedAt: string;
}

export function CustomerWishlistPage() {
  const session = useAuthSession();
  const { showToast } = useToast();
  const [items, setItems] = useState<DisplayWishlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [isLive, setIsLive] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [addingToCartId, setAddingToCartId] = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        setErrorMsg("");
        const res = await engagementApi.wishlist.get();
        if (ignore) return;

        const rawItems = res.items ?? [];
        if (rawItems.length === 0) {
          setItems([]);
          setIsLive(true);
          setLoading(false);
          return;
        }

        // Tải chi tiết sản phẩm catalog cho từng item để hiển thị tên thật, ảnh thật, giá thật
        const enrichedItems: DisplayWishlistItem[] = await Promise.all(
          rawItems.map(async (raw) => {
            try {
              const product = await apiClient.get<ProductDetail>(
                `/api/v1/catalog/products/id/${raw.productId}`
              );

              const variant =
                product.variants?.find((v) => v.purchasable) ??
                product.variants?.[0];
              const primaryImg =
                product.images?.find((img) => img.primary)?.imageUrl ??
                product.images?.[0]?.imageUrl ??
                variant?.images?.[0]?.imageUrl;

              return {
                id: raw.id,
                productId: raw.productId,
                name: product.name || "Sản phẩm DynamicMart",
                slug: product.slug,
                categoryName: product.category?.name,
                imageUrl: primaryImg,
                price: variant?.price?.listPriceVnd,
                variantId: variant?.id,
                inStock: product.variants?.some((v) => v.purchasable) ?? true,
                savedAt: raw.createdAt,
              };
            } catch {
              // Trường hợp không tìm thấy trong catalog
              return {
                id: raw.id,
                productId: raw.productId,
                name: "Sản phẩm DynamicMart",
                savedAt: raw.createdAt,
              };
            }
          })
        );

        if (!ignore) {
          setItems(enrichedItems);
          setIsLive(true);
        }
      } catch {
        if (!ignore) {
          setItems([]);
          setIsLive(false);
          setErrorMsg("Không thể tải danh sách yêu thích. Vui lòng thử lại sau.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      ignore = true;
    };
  }, [refreshTrigger]);

  const handleRemove = async (productId: string, itemId: string) => {
    setDeletingId(itemId);
    try {
      await engagementApi.wishlist.remove(productId);
      setItems((current) => current.filter((value) => value.id !== itemId));
      showToast("Đã bỏ sản phẩm khỏi danh sách yêu thích", "info");
    } catch {
      setErrorMsg("Không thể bỏ sản phẩm khỏi danh sách yêu thích.");
    } finally {
      setDeletingId(null);
    }
  };

  const handleAddToCart = async (item: DisplayWishlistItem) => {
    if (!item.variantId) {
      showToast("Vui lòng vào trang sản phẩm để chọn phân loại cụ thể.", "info");
      return;
    }
    setAddingToCartId(item.id);
    try {
      await cartApi.add(item.productId, item.variantId, 1);
      showToast(`Đã thêm "${item.name}" vào giỏ hàng thành công!`, "success");
    } catch {
      showToast("Không thể thêm vào giỏ hàng. Vui lòng thử lại sau.", "error");
    } finally {
      setAddingToCartId(null);
    }
  };

  if (session.status === "anonymous") {
    return (
      <div className="space-y-6">
        <PageHeader
          description="Những sản phẩm bạn đã lưu để theo dõi và mua sắm sau."
          eyebrow="Tài khoản cá nhân"
          title="Sản phẩm yêu thích"
        />
        <SurfacePanel className="py-12 text-center">
          <Heart className="mx-auto size-12 text-rose-500 fill-rose-100" />
          <h2 className="mt-4 text-lg font-black text-slate-950">Đăng nhập để xem danh sách</h2>
          <p className="mt-1 text-sm text-slate-500">
            Hãy đăng nhập tài khoản để đồng bộ và lưu trữ các sản phẩm bạn quan tâm.
          </p>
          <Link
            className="mt-5 inline-flex rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-bold text-white hover:bg-slate-800"
            href="/login?returnTo=%2Fcustomer%2Faccount%2Fwishlist"
          >
            Đăng nhập ngay
          </Link>
        </SurfacePanel>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          description="Những sản phẩm bạn đã lưu để theo dõi giá cả và mua sắm khi có nhu cầu."
          eyebrow="Tài khoản cá nhân"
          title="Sản phẩm yêu thích"
        />

        <div className="flex items-center gap-2">
          {isLive ? (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-800 shadow-2xs">
              <span className="size-2 rounded-full bg-emerald-500" />
              Đồng bộ trực tiếp
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800 shadow-2xs">
              <AlertCircle className="size-3.5" />
              Mất kết nối API
            </span>
          )}

          <Button
            disabled={loading}
            size="sm"
            variant="outline"
            onClick={() => {
              setLoading(true);
              setRefreshTrigger((prev) => prev + 1);
            }}
          >
            <RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />
            Làm mới
          </Button>
        </div>
      </div>

      <SurfacePanel className="p-4 sm:p-6">
        {errorMsg ? (
          <p className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{errorMsg}</p>
        ) : null}

        {loading ? (
          <div className="py-16 text-center text-sm text-slate-500">
            <RefreshCw className="mx-auto size-7 animate-spin text-brand" />
            <p className="mt-3 font-semibold text-slate-700">Đang tải danh sách yêu thích...</p>
          </div>
        ) : items.length === 0 ? (
          <div className="py-14 text-center text-slate-400">
            <Inbox className="mx-auto size-12 stroke-1 text-slate-300" />
            <h3 className="mt-3 text-base font-bold text-slate-800">
              Danh sách yêu thích của bạn đang trống
            </h3>
            <p className="mt-1 text-xs text-slate-500 max-w-sm mx-auto">
              Hãy bấm vào biểu tượng trái tim ở các sản phẩm bạn quan tâm để lưu và theo dõi tại đây.
            </p>
            <Link
              className="mt-5 inline-flex items-center gap-2 rounded-xl bg-slate-950 px-4 py-2.5 text-xs font-bold text-white hover:bg-slate-800 transition"
              href="/products"
            >
              <ShoppingBag className="size-3.5" />
              Khám phá sản phẩm ngay
            </Link>
          </div>
        ) : (
          <div className="grid gap-3.5">
            {items.map((item) => {
              const productUrl = item.slug
                ? `/products/${item.slug}`
                : `/products/${item.productId}`;

              return (
                <div
                  className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300 hover:shadow-xs"
                  key={item.id}
                >
                  <div className="flex items-center gap-3.5 min-w-0">
                    <Link className="shrink-0" href={productUrl}>
                      {item.imageUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          alt={item.name}
                          className="size-16 rounded-xl border border-slate-200 object-cover"
                          src={item.imageUrl}
                        />
                      ) : (
                        <div className="grid size-16 place-items-center rounded-xl bg-rose-50 text-rose-500">
                          <Heart className="size-6 fill-current" />
                        </div>
                      )}
                    </Link>

                    <div className="min-w-0">
                      <Link
                        className="font-bold text-slate-950 text-sm hover:text-brand transition line-clamp-1"
                        href={productUrl}
                      >
                        {item.name}
                      </Link>

                      <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                        {item.categoryName ? (
                          <span className="rounded-md bg-slate-100 px-2 py-0.5 font-medium text-slate-600">
                            {item.categoryName}
                          </span>
                        ) : null}

                        {item.price ? (
                          <span className="font-bold text-emerald-800">
                            {money(item.price)}
                          </span>
                        ) : null}

                        <span>· Đã lưu {date(item.savedAt)}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                    {item.inStock && item.variantId ? (
                      <Button
                        className="gap-1.5 bg-emerald-950 hover:bg-emerald-900 text-white font-bold"
                        disabled={addingToCartId === item.id}
                        size="sm"
                        onClick={() => handleAddToCart(item)}
                      >
                        <ShoppingCart className="size-3.5" />
                        Thêm vào giỏ
                      </Button>
                    ) : null}

                    <Link
                      className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-700 transition hover:bg-slate-50"
                      href={productUrl}
                    >
                      <ShoppingBag className="size-3.5" />
                      Chi tiết
                    </Link>

                    <Button
                      aria-label="Bỏ yêu thích"
                      disabled={deletingId === item.id}
                      size="icon"
                      variant="ghost"
                      onClick={() => handleRemove(item.productId, item.id)}
                    >
                      <Trash2 className="size-4 text-slate-400 hover:text-rose-600 transition" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </SurfacePanel>
    </div>
  );
}
