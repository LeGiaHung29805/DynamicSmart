"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowLeft, ChevronLeft, ChevronRight, ImagePlus, Images, Layers3,
  PackagePlus, Pencil, RefreshCw, Search, Settings2, Trash2,
} from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { LoadingState } from "@/components/common/PageState";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import {
  createProduct, createProductImage, createVariant, deleteProductImage, getAdminProduct,
  listAdminAttributes, listAdminCategories, listAdminProducts, listCategoryAttributes,
  setProductStatus, setVariantStatus, updateProduct, updateProductImage, updateVariant,
  type ProductImageInput, type ProductInput,
} from "../../api/admin-catalog.api";
import type {
  AdminProduct, AttributeAdmin, CategoryAdmin, CategoryAttributeAdmin, ProductImage,
  ProductStatus, ProductVariant, VariantStatus,
} from "../../types";
import {
  AdminAttributeValueFields, attributeInputs, attributeMap, type AttributeValueMap,
} from "./AdminAttributeValueFields";
import { errorMessage, fieldClass, Label, optionalNumber, textAreaClass, toDateTimeLocal } from "./form-utils";

interface ProductDraft {
  categoryId: string;
  name: string;
  slug: string;
  shortDescription: string;
  description: string;
  defaultWeightGrams: string;
  defaultLengthCm: string;
  defaultWidthCm: string;
  defaultHeightCm: string;
  featured: boolean;
}

interface VariantDraft {
  id?: string;
  sku: string;
  name: string;
  priceVnd: string;
  weightGrams: string;
  lengthCm: string;
  widthCm: string;
  heightCm: string;
  sortOrder: string;
  initialOnHandQuantity: string;
}

interface ImageDraft {
  id?: string;
  variantId: string;
  imageUrl: string;
  contentType: string;
  sizeBytes: string;
  altText: string;
  sortOrder: string;
  primary: boolean;
}

const emptyProduct: ProductDraft = {
  categoryId: "", name: "", slug: "", shortDescription: "", description: "",
  defaultWeightGrams: "", defaultLengthCm: "", defaultWidthCm: "", defaultHeightCm: "", featured: false,
};
const emptyVariant: VariantDraft = {
  sku: "", name: "", priceVnd: "", weightGrams: "", lengthCm: "", widthCm: "", heightCm: "",
  sortOrder: "0", initialOnHandQuantity: "0",
};
const emptyImage: ImageDraft = {
  variantId: "", imageUrl: "", contentType: "image/jpeg", sizeBytes: "", altText: "", sortOrder: "0", primary: false,
};
const productStatuses: ProductStatus[] = ["DRAFT", "ACTIVE", "INACTIVE", "ARCHIVED"];
const variantStatuses: VariantStatus[] = ["ACTIVE", "INACTIVE", "ARCHIVED"];

function productDraft(product: AdminProduct): ProductDraft {
  return {
    categoryId: product.category.id,
    name: product.name,
    slug: product.slug,
    shortDescription: product.shortDescription ?? "",
    description: product.description ?? "",
    defaultWeightGrams: product.defaultWeightGrams?.toString() ?? "",
    defaultLengthCm: product.defaultLengthCm?.toString() ?? "",
    defaultWidthCm: product.defaultWidthCm?.toString() ?? "",
    defaultHeightCm: product.defaultHeightCm?.toString() ?? "",
    featured: product.featured,
  };
}

function variantDraft(variant: ProductVariant): VariantDraft {
  return {
    id: variant.id,
    sku: variant.sku,
    name: variant.name ?? "",
    priceVnd: String(variant.price.listPriceVnd),
    weightGrams: String(variant.weightGrams),
    lengthCm: variant.lengthCm?.toString() ?? "",
    widthCm: variant.widthCm?.toString() ?? "",
    heightCm: variant.heightCm?.toString() ?? "",
    sortOrder: String(variant.sortOrder),
    initialOnHandQuantity: "0",
  };
}

export function AdminProductListPanel() {
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<ProductStatus | "">("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      const result = await listAdminProducts({
        keyword: query || undefined,
        status: statusFilter || undefined,
        page,
        size: 20,
      });
      setProducts(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (caught) {
      setError(errorMessage(caught, "Không thể tải danh sách sản phẩm."));
    } finally {
      setLoading(false);
    }
  }, [page, query, statusFilter]);

  useEffect(() => {
    // Initial API synchronization is intentionally started after the client mounts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  return (
    <div className="space-y-6">
      {error ? <ErrorBanner message={error} /> : null}
      <SurfacePanel>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-black tracking-wider text-brand uppercase">Sản phẩm</p>
            <h2 className="mt-1 text-2xl font-black text-slate-950">Danh sách sản phẩm</h2>
            <p className="mt-1 text-sm text-slate-500">{totalElements} sản phẩm · chọn một sản phẩm để quản lý Variant và hình ảnh.</p>
          </div>
          <div className="flex gap-2">
            <button aria-label="Tải lại" className="grid size-11 place-items-center rounded-xl border border-slate-200 bg-white" onClick={() => void load()} type="button"><RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} /></button>
            <Link className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" href="/admin/catalog/products/new"><PackagePlus className="size-4" /> Thêm sản phẩm</Link>
          </div>
        </div>

        <form className="mt-6 grid gap-3 lg:grid-cols-[minmax(0,1fr)_190px_auto]" onSubmit={(event) => { event.preventDefault(); setPage(0); setQuery(keyword.trim()); }}>
          <label className="relative"><Search className="absolute top-3.5 left-3 size-4 text-slate-400" /><input aria-label="Tìm sản phẩm" className={`${fieldClass} pl-10`} onChange={(event) => setKeyword(event.target.value)} placeholder="Tên, slug hoặc từ khóa..." value={keyword} /></label>
          <select aria-label="Lọc trạng thái" className={fieldClass} onChange={(event) => { setPage(0); setStatusFilter(event.target.value as ProductStatus | ""); }} value={statusFilter}><option value="">Mọi trạng thái</option>{productStatuses.map((status) => <option key={status}>{status}</option>)}</select>
          <button className="min-h-11 rounded-xl bg-emerald-950 px-5 text-sm font-black text-white" type="submit">Tìm kiếm</button>
        </form>

        <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Sản phẩm</th><th className="px-4 py-3">Danh mục</th><th className="px-4 py-3 text-center">Variant</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Thao tác</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {products.map((product) => <tr className="hover:bg-slate-50" key={product.id}>
                <td className="px-4 py-3"><Link className="font-bold text-slate-950 hover:text-brand" href={`/admin/catalog/products/${product.id}`}>{product.name}</Link><span className="mt-1 block text-xs text-slate-500">/{product.slug}</span></td>
                <td className="px-4 py-3 text-slate-600">{product.category.name}</td>
                <td className="px-4 py-3 text-center font-black tabular-nums">{product.variants.length}</td>
                <td className="px-4 py-3"><StatusBadge label={product.status} tone={product.status === "ACTIVE" ? "success" : product.status === "DRAFT" ? "warning" : "neutral"} /></td>
                <td className="px-4 py-3"><div className="flex justify-end gap-2"><Link className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50" href={`/admin/catalog/products/${product.id}`}>Quản lý</Link><Link aria-label={`Sửa ${product.name}`} className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50" href={`/admin/catalog/products/${product.id}/edit`}><Pencil className="size-4" /></Link></div></td>
              </tr>)}
            </tbody>
          </table>
          {!loading && !products.length ? <div className="p-10 text-center"><p className="font-bold text-slate-800">Không có sản phẩm phù hợp</p><p className="mt-1 text-sm text-slate-500">Thử đổi từ khóa, trạng thái hoặc tạo sản phẩm mới.</p></div> : null}
        </div>

        {totalPages > 1 ? <div className="mt-5 flex items-center justify-between gap-3"><p className="text-sm text-slate-500">Trang <strong className="text-slate-900">{page + 1}</strong> / {totalPages}</p><div className="flex gap-2"><button className="inline-flex min-h-10 items-center gap-1 rounded-xl border border-slate-200 px-3 text-sm font-bold disabled:opacity-40" disabled={page === 0 || loading} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button"><ChevronLeft className="size-4" /> Trước</button><button className="inline-flex min-h-10 items-center gap-1 rounded-xl border border-slate-200 px-3 text-sm font-bold disabled:opacity-40" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((current) => current + 1)} type="button">Sau <ChevronRight className="size-4" /></button></div></div> : null}
      </SurfacePanel>
    </div>
  );
}

export function AdminProductFormPanel({ productId }: Readonly<{ productId?: string }>) {
  const router = useRouter();
  const { showToast } = useToast();
  const [categories, setCategories] = useState<CategoryAdmin[]>([]);
  const [attributes, setAttributes] = useState<AttributeAdmin[]>([]);
  const [mappings, setMappings] = useState<CategoryAttributeAdmin[]>([]);
  const [draft, setDraft] = useState<ProductDraft>(emptyProduct);
  const [attributeValues, setAttributeValues] = useState<AttributeValueMap>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  useEffect(() => {
    let active = true;
    Promise.all([
      listAdminCategories(),
      listAdminAttributes(),
      productId ? getAdminProduct(productId) : Promise.resolve(undefined),
    ]).then(([categoryItems, attributeItems, product]) => {
      if (!active) return;
      setCategories(categoryItems);
      setAttributes(attributeItems);
      if (product) {
        setDraft(productDraft(product));
        setAttributeValues(attributeMap(product.attributes));
      } else {
        setDraft({ ...emptyProduct, categoryId: categoryItems.find((item) => item.status === "ACTIVE")?.id ?? categoryItems[0]?.id ?? "" });
      }
    }).catch((caught) => {
      if (active) setError(errorMessage(caught, "Không thể mở biểu mẫu sản phẩm."));
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [productId]);

  useEffect(() => {
    let active = true;
    if (!draft.categoryId) return;
    listCategoryAttributes(draft.categoryId)
      .then((items) => { if (active) setMappings(items); })
      .catch((caught) => { if (active) setError(errorMessage(caught, "Không thể tải thuộc tính của danh mục.")); });
    return () => { active = false; };
  }, [draft.categoryId]);

  async function saveProduct(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true);
      setError(undefined);
      const input: ProductInput = {
        categoryId: draft.categoryId,
        name: draft.name.trim(),
        slug: draft.slug.trim(),
        shortDescription: draft.shortDescription.trim() || null,
        description: draft.description.trim() || null,
        defaultWeightGrams: optionalNumber(draft.defaultWeightGrams),
        defaultLengthCm: optionalNumber(draft.defaultLengthCm),
        defaultWidthCm: optionalNumber(draft.defaultWidthCm),
        defaultHeightCm: optionalNumber(draft.defaultHeightCm),
        featured: draft.featured,
        attributes: attributeInputs(attributeValues),
      };
      const saved = productId ? await updateProduct(productId, input) : await createProduct(input);
      showToast(productId ? "Đã cập nhật sản phẩm." : "Đã tạo sản phẩm nháp.", "success");
      router.push(`/admin/catalog/products/${saved.id}`);
      router.refresh();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <LoadingState title={productId ? "Đang tải sản phẩm" : "Đang chuẩn bị biểu mẫu"} />;

  const cancelHref = productId ? `/admin/catalog/products/${productId}` : "/admin/catalog/products";
  return (
    <form className="space-y-6" onSubmit={saveProduct}>
      {error ? <ErrorBanner message={error} /> : null}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-start gap-3"><Link aria-label="Quay lại" className="mt-1 grid size-10 place-items-center rounded-xl border border-slate-200 bg-white" href={cancelHref}><ArrowLeft className="size-4" /></Link><div><p className="text-xs font-black tracking-wider text-brand uppercase">{productId ? "Chỉnh sửa" : "Tạo mới"}</p><h2 className="mt-1 text-2xl font-black text-slate-950">{productId ? draft.name || "Sản phẩm" : "Sản phẩm mới"}</h2><p className="mt-1 text-sm text-slate-500">Hoàn tất thông tin chung trước; Variant và ảnh được quản lý sau khi sản phẩm được tạo.</p></div></div>
      </div>

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(340px,.85fr)]">
        <SurfacePanel>
          <div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 1</p><h3 className="mt-1 text-xl font-black">Thông tin chung</h3></div>
          <div className="mt-5 space-y-4">
            <label><Label>Danh mục</Label><select className={fieldClass} onChange={(event) => { setDraft({ ...draft, categoryId: event.target.value }); setAttributeValues({}); }} required value={draft.categoryId}><option value="">Chọn danh mục</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name} ({category.status})</option>)}</select></label>
            <div className="grid gap-4 md:grid-cols-2"><label><Label>Tên sản phẩm</Label><input className={fieldClass} maxLength={300} onChange={(event) => setDraft({ ...draft, name: event.target.value })} required value={draft.name} /></label><label><Label>Đường dẫn (slug)</Label><input className={fieldClass} onChange={(event) => setDraft({ ...draft, slug: event.target.value })} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" placeholder="dynamic-phone-pro" required value={draft.slug} /></label></div>
            <label><Label>Mô tả ngắn</Label><textarea className={textAreaClass} maxLength={1000} onChange={(event) => setDraft({ ...draft, shortDescription: event.target.value })} value={draft.shortDescription} /></label>
            <label><Label>Mô tả chi tiết</Label><textarea className={`${textAreaClass} min-h-40`} maxLength={20000} onChange={(event) => setDraft({ ...draft, description: event.target.value })} value={draft.description} /></label>
          </div>
        </SurfacePanel>

        <div className="space-y-6 xl:sticky xl:top-24">
          <SurfacePanel>
            <div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 2</p><h3 className="mt-1 text-xl font-black">Vận chuyển mặc định</h3><p className="mt-1 text-sm text-slate-500">Variant có thể ghi đè các thông số này.</p></div>
            <div className="mt-5 grid grid-cols-2 gap-3"><NumberField label="Khối lượng (g)" value={draft.defaultWeightGrams} onChange={(value) => setDraft({ ...draft, defaultWeightGrams: value })} /><NumberField label="Dài (cm)" value={draft.defaultLengthCm} onChange={(value) => setDraft({ ...draft, defaultLengthCm: value })} /><NumberField label="Rộng (cm)" value={draft.defaultWidthCm} onChange={(value) => setDraft({ ...draft, defaultWidthCm: value })} /><NumberField label="Cao (cm)" value={draft.defaultHeightCm} onChange={(value) => setDraft({ ...draft, defaultHeightCm: value })} /></div>
            <label className="mt-4 flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-3 text-sm font-semibold"><input checked={draft.featured} onChange={(event) => setDraft({ ...draft, featured: event.target.checked })} type="checkbox" /> Hiển thị ở khu vực nổi bật</label>
          </SurfacePanel>
          <SurfacePanel>
            <div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 3</p><h3 className="mt-1 text-xl font-black">Thuộc tính sản phẩm</h3></div>
            <div className="mt-5"><AdminAttributeValueFields appliesTo="PRODUCT" attributes={attributes} mappings={mappings} onChange={setAttributeValues} values={attributeValues} /></div>
          </SurfacePanel>
        </div>
      </div>

      <div className="sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white/95 p-4 shadow-xl backdrop-blur"><p className="text-sm text-slate-500">Kiểm tra các trường bắt buộc trước khi lưu.</p><div className="flex gap-2"><Link className="inline-flex min-h-11 items-center rounded-xl border border-slate-200 px-4 text-sm font-bold" href={cancelHref}>Hủy</Link><button className="min-h-11 rounded-xl bg-emerald-950 px-5 text-sm font-black text-white disabled:opacity-50" disabled={busy} type="submit">{busy ? "Đang lưu..." : productId ? "Lưu thay đổi" : "Tạo sản phẩm nháp"}</button></div></div>
    </form>
  );
}

export function AdminProductDetailPanel({ productId }: Readonly<{ productId: string }>) {
  const [product, setProduct] = useState<AdminProduct>();
  const [attributes, setAttributes] = useState<AttributeAdmin[]>([]);
  const [mappings, setMappings] = useState<CategoryAttributeAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      const [loadedProduct, loadedAttributes] = await Promise.all([getAdminProduct(productId), listAdminAttributes()]);
      const loadedMappings = await listCategoryAttributes(loadedProduct.category.id);
      setProduct(loadedProduct);
      setAttributes(loadedAttributes);
      setMappings(loadedMappings);
    } catch (caught) {
      setError(errorMessage(caught, "Không thể tải chi tiết sản phẩm."));
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => {
    // Initial API synchronization is intentionally started after the client mounts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  if (loading) return <LoadingState title="Đang tải chi tiết sản phẩm" />;
  if (!product) return <div className="space-y-4"><ErrorBanner message={error ?? "Không tìm thấy sản phẩm."} /><Link className="inline-flex items-center gap-2 text-sm font-bold text-brand" href="/admin/catalog/products"><ArrowLeft className="size-4" /> Quay lại danh sách</Link></div>;

  return <ProductOperations attributes={attributes} key={product.id} mappings={mappings} onError={setError} onProductUpdated={setProduct} product={product} topError={error} />;
}

function NumberField({ label, value, onChange }: Readonly<{ label: string; value: string; onChange: (value: string) => void }>) {
  return <label><Label>{label}</Label><input className={fieldClass} min="1" onChange={(event) => onChange(event.target.value)} type="number" value={value} /></label>;
}

type ProductSection = "overview" | "variants" | "images";

function ProductOperations({ product, attributes, mappings: initialMappings, onProductUpdated, onError, topError }: Readonly<{
  product: AdminProduct;
  attributes: AttributeAdmin[];
  mappings: CategoryAttributeAdmin[];
  onProductUpdated: (product: AdminProduct) => void;
  onError: (message: string | undefined) => void;
  topError?: string;
}>) {
  const { showToast } = useToast();
  const [section, setSection] = useState<ProductSection>("overview");
  const [variants, setVariants] = useState(product.variants);
  const [images, setImages] = useState(product.images);
  const [mappings, setMappings] = useState(initialMappings);
  const [variantForm, setVariantForm] = useState<VariantDraft>(emptyVariant);
  const [variantAttributes, setVariantAttributes] = useState<AttributeValueMap>({});
  const [imageForm, setImageForm] = useState<ImageDraft>(emptyImage);
  const [status, setStatus] = useState<ProductStatus>(product.status);
  const [publishedAt, setPublishedAt] = useState(toDateTimeLocal(product.publishedAt));
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (mappings.length) return;
    let active = true;
    listCategoryAttributes(product.category.id).then((items) => { if (active) setMappings(items); }).catch((caught) => onError(errorMessage(caught)));
    return () => { active = false; };
  }, [mappings.length, onError, product.category.id]);

  async function saveStatus(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); onError(undefined);
      const saved = await setProductStatus(product.id, status, publishedAt ? new Date(publishedAt).toISOString() : null);
      onProductUpdated(saved);
      showToast("Đã cập nhật trạng thái sản phẩm.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  async function saveVariant(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); onError(undefined);
      const common = {
        sku: variantForm.sku.trim(),
        name: variantForm.name.trim() || null,
        priceVnd: Number(variantForm.priceVnd),
        weightGrams: Number(variantForm.weightGrams),
        lengthCm: optionalNumber(variantForm.lengthCm),
        widthCm: optionalNumber(variantForm.widthCm),
        heightCm: optionalNumber(variantForm.heightCm),
        sortOrder: Number(variantForm.sortOrder),
        attributes: attributeInputs(variantAttributes),
      };
      const saved = variantForm.id
        ? await updateVariant(product.id, variantForm.id, common)
        : await createVariant(product.id, { ...common, initialOnHandQuantity: Number(variantForm.initialOnHandQuantity) });
      setVariants((current) => variantForm.id ? current.map((item) => item.id === saved.id ? saved : item) : [...current, saved]);
      setVariantForm(emptyVariant); setVariantAttributes({});
      showToast(variantForm.id ? "Đã cập nhật biến thể." : "Đã tạo biến thể.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  async function changeVariantStatus(variant: ProductVariant, next: VariantStatus) {
    try {
      setBusy(true); onError(undefined);
      const saved = await setVariantStatus(product.id, variant.id, next);
      setVariants((current) => current.map((item) => item.id === saved.id ? saved : item));
      showToast("Đã cập nhật trạng thái biến thể.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  function editVariant(variant: ProductVariant) {
    setVariantForm(variantDraft(variant));
    setVariantAttributes(attributeMap(variant.attributes));
  }

  async function saveImage(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); onError(undefined);
      const input: ProductImageInput = {
        variantId: imageForm.variantId || null,
        imageUrl: imageForm.imageUrl.trim(),
        contentType: imageForm.contentType.trim(),
        sizeBytes: Number(imageForm.sizeBytes),
        altText: imageForm.altText.trim() || null,
        sortOrder: Number(imageForm.sortOrder),
        primary: imageForm.primary,
      };
      const saved = imageForm.id
        ? await updateProductImage(product.id, imageForm.id, input)
        : await createProductImage(product.id, input);
      setImages((current) => imageForm.id ? current.map((item) => item.id === saved.id ? saved : item) : [...current, saved]);
      setImageForm(emptyImage);
      showToast(imageForm.id ? "Đã cập nhật hình ảnh." : "Đã thêm hình ảnh.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  function editImage(image: ProductImage) {
    setImageForm({ id: image.id, variantId: image.variantId ?? "", imageUrl: image.imageUrl, contentType: "image/jpeg", sizeBytes: "", altText: image.altText ?? "", sortOrder: String(image.sortOrder), primary: image.primary });
  }

  async function removeImage(image: ProductImage) {
    if (!window.confirm("Xóa hình ảnh này khỏi sản phẩm?")) return;
    try {
      setBusy(true); onError(undefined);
      await deleteProductImage(product.id, image.id);
      setImages((current) => current.filter((item) => item.id !== image.id));
      showToast("Đã xóa hình ảnh.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  const sections = [
    { id: "overview", label: "Tổng quan", icon: Settings2 },
    { id: "variants", label: `Biến thể (${variants.length})`, icon: Layers3 },
    { id: "images", label: `Hình ảnh (${images.length})`, icon: Images },
  ] as const;

  return (
    <div className="space-y-6">
      {topError ? <ErrorBanner message={topError} /> : null}
      <SurfacePanel>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-start gap-3"><Link aria-label="Quay lại danh sách" className="mt-1 grid size-10 place-items-center rounded-xl border border-slate-200" href="/admin/catalog/products"><ArrowLeft className="size-4" /></Link><div><p className="text-xs font-black tracking-wider text-brand uppercase">Đang quản lý</p><h2 className="mt-1 text-2xl font-black text-slate-950">{product.name}</h2><p className="mt-1 text-sm text-slate-500">{product.category.name} · <span className="font-mono">{product.slug}</span></p></div></div>
          <div className="flex items-center gap-3"><StatusBadge label={product.status} tone={product.status === "ACTIVE" ? "success" : product.status === "DRAFT" ? "warning" : "neutral"} /><Link className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-bold" href={`/admin/catalog/products/${product.id}/edit`}><Pencil className="size-4" /> Sửa thông tin</Link></div>
        </div>
        <nav aria-label="Nội dung sản phẩm" className="mt-6 flex gap-2 overflow-x-auto border-t border-slate-100 pt-4">
          {sections.map(({ id, label, icon: Icon }) => <button aria-current={section === id ? "page" : undefined} className={`inline-flex min-h-10 shrink-0 items-center gap-2 rounded-xl px-4 text-sm font-bold ${section === id ? "bg-emerald-950 text-white" : "bg-slate-50 text-slate-600 hover:bg-slate-100"}`} key={id} onClick={() => setSection(id)} type="button"><Icon className="size-4" /> {label}</button>)}
        </nav>
      </SurfacePanel>

      {section === "overview" ? <ProductOverview busy={busy} images={images} onSaveStatus={saveStatus} product={product} publishedAt={publishedAt} setPublishedAt={setPublishedAt} setStatus={setStatus} status={status} variants={variants} /> : null}
      {section === "variants" ? <VariantWorkspace attributes={attributes} busy={busy} mappings={mappings} onChangeStatus={changeVariantStatus} onEdit={editVariant} onReset={() => { setVariantForm(emptyVariant); setVariantAttributes({}); }} onSave={saveVariant} setVariantAttributes={setVariantAttributes} setVariantForm={setVariantForm} variantAttributes={variantAttributes} variantForm={variantForm} variants={variants} /> : null}
      {section === "images" ? <ImageWorkspace busy={busy} imageForm={imageForm} images={images} onEdit={editImage} onRemove={removeImage} onReset={() => setImageForm(emptyImage)} onSave={saveImage} product={product} setImageForm={setImageForm} variants={variants} /> : null}
    </div>
  );
}

function ProductOverview({ product, variants, images, status, setStatus, publishedAt, setPublishedAt, busy, onSaveStatus }: Readonly<{
  product: AdminProduct; variants: ProductVariant[]; images: ProductImage[]; status: ProductStatus;
  setStatus: (value: ProductStatus) => void; publishedAt: string; setPublishedAt: (value: string) => void;
  busy: boolean; onSaveStatus: (event: FormEvent) => void;
}>) {
  return <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
    <SurfacePanel><h3 className="text-xl font-black">Tóm tắt sản phẩm</h3><dl className="mt-5 grid gap-3 sm:grid-cols-2"><Summary label="Danh mục" value={product.category.name} /><Summary label="Số biến thể" value={String(variants.length)} /><Summary label="Số hình ảnh" value={String(images.length)} /><Summary label="Nổi bật" value={product.featured ? "Có" : "Không"} /><Summary label="Khối lượng mặc định" value={product.defaultWeightGrams ? `${product.defaultWeightGrams.toLocaleString("vi-VN")} g` : "Chưa đặt"} /><Summary label="Ngày công khai" value={product.publishedAt ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(product.publishedAt)) : "Chưa công khai"} /></dl>{product.shortDescription ? <p className="mt-5 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">{product.shortDescription}</p> : null}</SurfacePanel>
    <SurfacePanel className="lg:sticky lg:top-24"><div><p className="text-xs font-black tracking-wider text-brand uppercase">Xuất bản</p><h3 className="mt-1 text-xl font-black">Trạng thái bán</h3></div><form className="mt-5 space-y-4" onSubmit={onSaveStatus}><label><Label>Trạng thái</Label><select className={fieldClass} onChange={(event) => setStatus(event.target.value as ProductStatus)} value={status}>{productStatuses.map((item) => <option key={item}>{item}</option>)}</select></label><label><Label>Ngày phát hành</Label><input className={fieldClass} onChange={(event) => setPublishedAt(event.target.value)} type="datetime-local" value={publishedAt} /></label><button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50" disabled={busy} type="submit">{busy ? "Đang cập nhật..." : "Cập nhật trạng thái"}</button></form></SurfacePanel>
  </div>;
}

function VariantWorkspace({ variants, variantForm, setVariantForm, variantAttributes, setVariantAttributes, attributes, mappings, busy, onSave, onEdit, onReset, onChangeStatus }: Readonly<{
  variants: ProductVariant[]; variantForm: VariantDraft; setVariantForm: (value: VariantDraft) => void;
  variantAttributes: AttributeValueMap; setVariantAttributes: (value: AttributeValueMap) => void;
  attributes: AttributeAdmin[]; mappings: CategoryAttributeAdmin[]; busy: boolean;
  onSave: (event: FormEvent) => void; onEdit: (variant: ProductVariant) => void; onReset: () => void;
  onChangeStatus: (variant: ProductVariant, status: VariantStatus) => Promise<void>;
}>) {
  return <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,.85fr)]">
    <SurfacePanel><div><p className="text-xs font-black tracking-wider text-brand uppercase">SKU & giá</p><h3 className="mt-1 text-xl font-black">Danh sách biến thể</h3></div><div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Biến thể</th><th className="px-4 py-3">Giá</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Sửa</th></tr></thead><tbody className="divide-y divide-slate-100">{variants.map((variant) => <tr key={variant.id}><td className="px-4 py-3"><p className="font-bold text-slate-950">{variant.name || variant.sku}</p><p className="font-mono text-xs text-slate-500">{variant.sku}</p></td><td className="px-4 py-3 font-black text-emerald-800">{formatVnd(variant.price.listPriceVnd)}</td><td className="px-4 py-3"><select aria-label={`Trạng thái ${variant.sku}`} className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs font-bold" disabled={busy} onChange={(event) => void onChangeStatus(variant, event.target.value as VariantStatus)} value={variant.status}>{variantStatuses.map((item) => <option key={item}>{item}</option>)}</select></td><td className="px-4 py-3 text-right"><button aria-label={`Sửa ${variant.sku}`} className="rounded-lg border border-slate-200 p-2" onClick={() => onEdit(variant)} type="button"><Pencil className="size-4" /></button></td></tr>)}</tbody></table>{!variants.length ? <p className="p-8 text-center text-sm text-slate-500">Sản phẩm chưa có biến thể.</p> : null}</div></SurfacePanel>
    <SurfacePanel className="xl:sticky xl:top-24"><div className="flex items-center justify-between"><div><p className="text-xs font-black tracking-wider text-brand uppercase">{variantForm.id ? "Chỉnh sửa" : "Tạo mới"}</p><h3 className="mt-1 text-xl font-black">{variantForm.id ? variantForm.sku : "Biến thể mới"}</h3></div>{variantForm.id ? <button className="text-sm font-bold text-slate-500" onClick={onReset} type="button">Hủy sửa</button> : <PackagePlus className="size-5 text-brand" />}</div><form className="mt-5 space-y-4" onSubmit={onSave}><div className="grid gap-3 sm:grid-cols-2"><TextField label="SKU" pattern="[A-Za-z0-9._-]+" required value={variantForm.sku} onChange={(value) => setVariantForm({ ...variantForm, sku: value })} /><TextField label="Tên biến thể" value={variantForm.name} onChange={(value) => setVariantForm({ ...variantForm, name: value })} /><NumberTextField label="Giá (VND)" min="0" required value={variantForm.priceVnd} onChange={(value) => setVariantForm({ ...variantForm, priceVnd: value })} /><NumberTextField label="Khối lượng (g)" min="1" required value={variantForm.weightGrams} onChange={(value) => setVariantForm({ ...variantForm, weightGrams: value })} /><NumberTextField label="Dài (cm)" min="1" value={variantForm.lengthCm} onChange={(value) => setVariantForm({ ...variantForm, lengthCm: value })} /><NumberTextField label="Rộng (cm)" min="1" value={variantForm.widthCm} onChange={(value) => setVariantForm({ ...variantForm, widthCm: value })} /><NumberTextField label="Cao (cm)" min="1" value={variantForm.heightCm} onChange={(value) => setVariantForm({ ...variantForm, heightCm: value })} /><NumberTextField label="Thứ tự" min="0" required value={variantForm.sortOrder} onChange={(value) => setVariantForm({ ...variantForm, sortOrder: value })} />{!variantForm.id ? <NumberTextField label="Tồn đầu kỳ" min="0" required value={variantForm.initialOnHandQuantity} onChange={(value) => setVariantForm({ ...variantForm, initialOnHandQuantity: value })} /> : null}</div><div><p className="mb-3 text-sm font-black">Thuộc tính biến thể</p><AdminAttributeValueFields appliesTo="VARIANT" attributes={attributes} mappings={mappings} onChange={setVariantAttributes} values={variantAttributes} /></div><button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" disabled={busy} type="submit">{busy ? "Đang lưu..." : variantForm.id ? "Lưu biến thể" : "Thêm biến thể"}</button></form></SurfacePanel>
  </div>;
}

function ImageWorkspace({ product, variants, images, imageForm, setImageForm, busy, onSave, onEdit, onRemove, onReset }: Readonly<{
  product: AdminProduct; variants: ProductVariant[]; images: ProductImage[]; imageForm: ImageDraft;
  setImageForm: (value: ImageDraft) => void; busy: boolean; onSave: (event: FormEvent) => void;
  onEdit: (image: ProductImage) => void; onRemove: (image: ProductImage) => Promise<void>; onReset: () => void;
}>) {
  return <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,.85fr)]">
    <SurfacePanel><div><p className="text-xs font-black tracking-wider text-brand uppercase">Media</p><h3 className="mt-1 text-xl font-black">Thư viện hình ảnh</h3></div><div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">{images.map((image) => <div className="overflow-hidden rounded-2xl border border-slate-200" key={image.id}><div className="relative aspect-square bg-slate-100"><Image alt={image.altText ?? product.name} className="object-cover" fill sizes="(max-width: 640px) 50vw, 220px" src={image.imageUrl} unoptimized />{image.primary ? <span className="absolute top-2 left-2 rounded-full bg-emerald-950 px-2.5 py-1 text-[11px] font-black text-white">Ảnh chính</span> : null}</div><div className="flex items-center justify-between gap-2 p-2"><span className="truncate text-xs font-semibold text-slate-700">{image.variantId ? "Theo biến thể" : "Toàn sản phẩm"}</span><div className="flex"><button aria-label="Sửa ảnh" className="p-1.5" onClick={() => onEdit(image)} type="button"><Pencil className="size-3.5" /></button><button aria-label="Xóa ảnh" className="p-1.5 text-red-700" onClick={() => void onRemove(image)} type="button"><Trash2 className="size-3.5" /></button></div></div></div>)}</div>{!images.length ? <div className="mt-5 rounded-2xl border border-dashed border-slate-300 p-10 text-center"><Images className="mx-auto size-8 text-slate-300" /><p className="mt-3 font-bold text-slate-700">Chưa có hình ảnh</p><p className="mt-1 text-sm text-slate-500">Thêm ảnh sản phẩm hoặc ảnh riêng cho từng Variant.</p></div> : null}</SurfacePanel>
    <SurfacePanel className="xl:sticky xl:top-24"><div className="flex items-center justify-between"><div><p className="text-xs font-black tracking-wider text-brand uppercase">{imageForm.id ? "Chỉnh sửa" : "Thêm mới"}</p><h3 className="mt-1 text-xl font-black">Hình ảnh</h3></div>{imageForm.id ? <button className="text-sm font-bold text-slate-500" onClick={onReset} type="button">Hủy sửa</button> : <ImagePlus className="size-5 text-brand" />}</div><form className="mt-5 space-y-4" onSubmit={onSave}><label><Label>URL ảnh</Label><input className={fieldClass} onChange={(event) => setImageForm({ ...imageForm, imageUrl: event.target.value })} required type="url" value={imageForm.imageUrl} /></label><label><Label>Gắn với biến thể</Label><select className={fieldClass} onChange={(event) => setImageForm({ ...imageForm, variantId: event.target.value })} value={imageForm.variantId}><option value="">Toàn sản phẩm</option>{variants.map((variant) => <option key={variant.id} value={variant.id}>{variant.sku}</option>)}</select></label><TextField label="Alt text" value={imageForm.altText} onChange={(value) => setImageForm({ ...imageForm, altText: value })} /><div className="grid gap-3 sm:grid-cols-2"><TextField label="Content-Type" required value={imageForm.contentType} onChange={(value) => setImageForm({ ...imageForm, contentType: value })} /><NumberTextField label="Kích thước (byte)" min="1" required value={imageForm.sizeBytes} onChange={(value) => setImageForm({ ...imageForm, sizeBytes: value })} /><NumberTextField label="Thứ tự" min="0" required value={imageForm.sortOrder} onChange={(value) => setImageForm({ ...imageForm, sortOrder: value })} /></div><label className="flex items-center gap-2 rounded-xl bg-slate-50 p-3 text-sm font-semibold"><input checked={imageForm.primary} onChange={(event) => setImageForm({ ...imageForm, primary: event.target.checked })} type="checkbox" /> Đặt làm ảnh chính</label><button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" disabled={busy} type="submit">{busy ? "Đang lưu..." : imageForm.id ? "Lưu hình ảnh" : "Thêm hình ảnh"}</button></form></SurfacePanel>
  </div>;
}

function Summary({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-xl bg-slate-50 p-4"><dt className="text-xs font-bold tracking-wide text-slate-500 uppercase">{label}</dt><dd className="mt-1 font-black text-slate-950">{value}</dd></div>;
}

function ErrorBanner({ message }: Readonly<{ message: string }>) {
  return <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-800" role="alert">{message}</p>;
}

function TextField({ label, value, onChange, ...props }: Readonly<{ label: string; value: string; onChange: (value: string) => void } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange">>) {
  return <label><Label>{label}</Label><input {...props} className={fieldClass} onChange={(event) => onChange(event.target.value)} value={value} /></label>;
}

function NumberTextField(props: Readonly<{ label: string; value: string; onChange: (value: string) => void } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type">>) {
  return <TextField {...props} type="number" />;
}
