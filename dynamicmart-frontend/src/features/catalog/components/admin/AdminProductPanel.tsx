"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { ImagePlus, PackagePlus, Pencil, RefreshCw, Search, Trash2 } from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import {
  createProduct,
  createProductImage,
  createVariant,
  deleteProductImage,
  listAdminAttributes,
  listAdminCategories,
  listAdminProducts,
  listCategoryAttributes,
  setProductStatus,
  setVariantStatus,
  updateProduct,
  updateProductImage,
  updateVariant,
  type ProductImageInput,
  type ProductInput,
} from "../../api/admin-catalog.api";
import type {
  AdminProduct,
  AttributeAdmin,
  CategoryAdmin,
  CategoryAttributeAdmin,
  ProductImage,
  ProductStatus,
  ProductVariant,
  VariantStatus,
} from "../../types";
import {
  AdminAttributeValueFields,
  attributeInputs,
  attributeMap,
  type AttributeValueMap,
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

const emptyProduct: ProductDraft = { categoryId: "", name: "", slug: "", shortDescription: "", description: "", defaultWeightGrams: "", defaultLengthCm: "", defaultWidthCm: "", defaultHeightCm: "", featured: false };
const emptyVariant: VariantDraft = { sku: "", name: "", priceVnd: "", weightGrams: "", lengthCm: "", widthCm: "", heightCm: "", sortOrder: "0", initialOnHandQuantity: "0" };
const emptyImage: ImageDraft = { variantId: "", imageUrl: "", contentType: "image/jpeg", sizeBytes: "", altText: "", sortOrder: "0", primary: false };
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

export function AdminProductPanel() {
  const { showToast } = useToast();
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [categories, setCategories] = useState<CategoryAdmin[]>([]);
  const [attributes, setAttributes] = useState<AttributeAdmin[]>([]);
  const [mappings, setMappings] = useState<CategoryAttributeAdmin[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<string>();
  const [editingProductId, setEditingProductId] = useState<string>();
  const [draft, setDraft] = useState<ProductDraft>(emptyProduct);
  const [attributeValues, setAttributeValues] = useState<AttributeValueMap>({});
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<ProductStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const selectedProduct = useMemo(() => products.find((product) => product.id === selectedProductId), [products, selectedProductId]);

  const load = useCallback(async () => {
    try {
      setLoading(true); setError(undefined);
      const [productPage, categoryItems, attributeItems] = await Promise.all([
        listAdminProducts({ keyword: query || undefined, status: statusFilter || undefined, size: 100 }),
        listAdminCategories(),
        listAdminAttributes(),
      ]);
      setProducts(productPage.content);
      setCategories(categoryItems);
      setAttributes(attributeItems);
      setDraft((current) => ({ ...current, categoryId: current.categoryId || categoryItems.find((item) => item.status === "ACTIVE")?.id || categoryItems[0]?.id || "" }));
      setSelectedProductId((current) => current && productPage.content.some((item) => item.id === current) ? current : productPage.content[0]?.id);
    } catch (caught) { setError(errorMessage(caught, "Không thể tải sản phẩm quản trị.")); }
    finally { setLoading(false); }
  }, [query, statusFilter]);

  useEffect(() => {
    // Initial API synchronization is intentionally started after the client mounts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  useEffect(() => {
    let active = true;
    if (!draft.categoryId) return;
    listCategoryAttributes(draft.categoryId)
      .then((items) => { if (active) setMappings(items); })
      .catch((caught) => { if (active) setError(errorMessage(caught, "Không thể tải schema danh mục.")); });
    return () => { active = false; };
  }, [draft.categoryId]);

  function resetProductForm() {
    setEditingProductId(undefined);
    setDraft({ ...emptyProduct, categoryId: categories.find((item) => item.status === "ACTIVE")?.id || categories[0]?.id || "" });
    setAttributeValues({});
  }

  function beginEdit(product: AdminProduct) {
    setSelectedProductId(product.id);
    setEditingProductId(product.id);
    setDraft(productDraft(product));
    setAttributeValues(attributeMap(product.attributes));
  }

  function replaceProduct(product: AdminProduct) {
    setProducts((current) => current.map((item) => item.id === product.id ? product : item));
  }

  async function saveProduct(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); setError(undefined);
      const input: ProductInput = {
        categoryId: draft.categoryId,
        name: draft.name,
        slug: draft.slug,
        shortDescription: draft.shortDescription || null,
        description: draft.description || null,
        defaultWeightGrams: optionalNumber(draft.defaultWeightGrams),
        defaultLengthCm: optionalNumber(draft.defaultLengthCm),
        defaultWidthCm: optionalNumber(draft.defaultWidthCm),
        defaultHeightCm: optionalNumber(draft.defaultHeightCm),
        featured: draft.featured,
        attributes: attributeInputs(attributeValues),
      };
      const saved = editingProductId ? await updateProduct(editingProductId, input) : await createProduct(input);
      setProducts((current) => editingProductId ? current.map((item) => item.id === saved.id ? saved : item) : [saved, ...current]);
      setSelectedProductId(saved.id);
      showToast(editingProductId ? "Đã cập nhật sản phẩm." : "Đã tạo sản phẩm nháp.", "success");
      resetProductForm();
    } catch (caught) { setError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  return (
    <div className="space-y-6">
      {error ? <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p> : null}
      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(380px,.8fr)]">
        <SurfacePanel>
          <div className="flex items-center justify-between gap-3"><div><h2 className="text-xl font-black">Danh sách sản phẩm</h2><p className="mt-1 text-sm text-slate-500">Tìm kiếm và chọn sản phẩm để quản lý biến thể.</p></div><button aria-label="Tải lại" className="grid size-10 place-items-center rounded-xl border border-slate-200" onClick={() => void load()} type="button"><RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} /></button></div>
          <form className="mt-5 grid gap-3 sm:grid-cols-[minmax(0,1fr)_170px_auto]" onSubmit={(event) => { event.preventDefault(); setQuery(keyword.trim()); }}><label className="relative"><Search className="absolute top-3 left-3 size-4 text-slate-400" /><input aria-label="Tìm sản phẩm" className={`${fieldClass} pl-10`} onChange={(event) => setKeyword(event.target.value)} placeholder="Tên hoặc slug..." value={keyword} /></label><select aria-label="Lọc trạng thái" className={fieldClass} onChange={(event) => setStatusFilter(event.target.value as ProductStatus | "")} value={statusFilter}><option value="">Mọi trạng thái</option>{productStatuses.map((status) => <option key={status}>{status}</option>)}</select><button className="rounded-xl bg-emerald-950 px-5 text-sm font-black text-white" type="submit">Tìm</button></form>
          <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Sản phẩm</th><th className="px-4 py-3">Danh mục</th><th className="px-4 py-3">Biến thể</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Sửa</th></tr></thead><tbody className="divide-y divide-slate-100">{products.map((product) => <tr className={selectedProductId === product.id ? "bg-emerald-50/50" : ""} key={product.id}><td className="px-4 py-3"><button className="text-left" onClick={() => setSelectedProductId(product.id)} type="button"><span className="block font-bold text-slate-900">{product.name}</span><span className="text-xs text-slate-500">/{product.slug}</span></button></td><td className="px-4 py-3 text-xs">{product.category.name}</td><td className="px-4 py-3 text-xs font-bold">{product.variants.length}</td><td className="px-4 py-3"><StatusBadge label={product.status} tone={product.status === "ACTIVE" ? "success" : product.status === "DRAFT" ? "warning" : "neutral"} /></td><td className="px-4 py-3 text-right"><button className="rounded-lg border p-2" onClick={() => beginEdit(product)} type="button"><Pencil className="size-4" /></button></td></tr>)}</tbody></table>{!loading && !products.length ? <p className="p-8 text-center text-sm text-slate-500">Chưa có sản phẩm phù hợp.</p> : null}</div>
        </SurfacePanel>

        <SurfacePanel className="xl:sticky xl:top-6">
          <div className="flex items-center justify-between"><div><p className="text-xs font-black tracking-wider text-brand uppercase">{editingProductId ? "Chỉnh sửa" : "Tạo mới"}</p><h2 className="mt-1 text-xl font-black">Thông tin sản phẩm</h2></div>{editingProductId ? <button className="text-sm font-bold text-slate-500" onClick={resetProductForm} type="button">Hủy sửa</button> : <PackagePlus className="size-5 text-brand" />}</div>
          <form className="mt-5 max-h-[72vh] space-y-4 overflow-y-auto pr-1" onSubmit={saveProduct}>
            <label><Label>Danh mục</Label><select className={fieldClass} onChange={(event) => { setDraft({ ...draft, categoryId: event.target.value }); setAttributeValues({}); }} required value={draft.categoryId}><option value="">Chọn danh mục</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name} ({category.status})</option>)}</select></label>
            <label><Label>Tên sản phẩm</Label><input className={fieldClass} maxLength={300} onChange={(event) => setDraft({ ...draft, name: event.target.value })} required value={draft.name} /></label>
            <label><Label>Slug</Label><input className={fieldClass} onChange={(event) => setDraft({ ...draft, slug: event.target.value })} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" required value={draft.slug} /></label>
            <label><Label>Mô tả ngắn</Label><textarea className={textAreaClass} maxLength={1000} onChange={(event) => setDraft({ ...draft, shortDescription: event.target.value })} value={draft.shortDescription} /></label>
            <label><Label>Mô tả chi tiết</Label><textarea className={`${textAreaClass} min-h-32`} maxLength={20000} onChange={(event) => setDraft({ ...draft, description: event.target.value })} value={draft.description} /></label>
            <div className="grid grid-cols-2 gap-3"><NumberField label="Khối lượng (g)" value={draft.defaultWeightGrams} onChange={(value) => setDraft({ ...draft, defaultWeightGrams: value })} /><NumberField label="Dài (cm)" value={draft.defaultLengthCm} onChange={(value) => setDraft({ ...draft, defaultLengthCm: value })} /><NumberField label="Rộng (cm)" value={draft.defaultWidthCm} onChange={(value) => setDraft({ ...draft, defaultWidthCm: value })} /><NumberField label="Cao (cm)" value={draft.defaultHeightCm} onChange={(value) => setDraft({ ...draft, defaultHeightCm: value })} /></div>
            <label className="flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-3 text-sm font-semibold"><input checked={draft.featured} onChange={(event) => setDraft({ ...draft, featured: event.target.checked })} type="checkbox" /> Sản phẩm nổi bật</label>
            <div><p className="mb-3 text-sm font-black text-slate-900">Thuộc tính sản phẩm</p><AdminAttributeValueFields appliesTo="PRODUCT" attributes={attributes} mappings={mappings} onChange={setAttributeValues} values={attributeValues} /></div>
            <button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50" disabled={busy} type="submit">{busy ? "Đang lưu..." : editingProductId ? "Lưu sản phẩm" : "Tạo sản phẩm nháp"}</button>
          </form>
        </SurfacePanel>
      </div>
      {selectedProduct ? <ProductOperations attributes={attributes} key={selectedProduct.id} mappings={selectedProduct.category.id === draft.categoryId ? mappings : []} onError={setError} onProductUpdated={replaceProduct} product={selectedProduct} /> : null}
    </div>
  );
}

function NumberField({ label, value, onChange }: Readonly<{ label: string; value: string; onChange: (value: string) => void }>) {
  return <label><Label>{label}</Label><input className={fieldClass} min="1" onChange={(event) => onChange(event.target.value)} type="number" value={value} /></label>;
}

function ProductOperations({ product, attributes, mappings: initialMappings, onProductUpdated, onError }: Readonly<{
  product: AdminProduct;
  attributes: AttributeAdmin[];
  mappings: CategoryAttributeAdmin[];
  onProductUpdated: (product: AdminProduct) => void;
  onError: (message: string | undefined) => void;
}>) {
  const { showToast } = useToast();
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
      onProductUpdated(saved); showToast("Đã cập nhật trạng thái sản phẩm.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  async function saveVariant(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); onError(undefined);
      const common = {
        sku: variantForm.sku,
        name: variantForm.name || null,
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
        imageUrl: imageForm.imageUrl,
        contentType: imageForm.contentType,
        sizeBytes: Number(imageForm.sizeBytes),
        altText: imageForm.altText || null,
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

  return (
    <div className="space-y-6">
      <SurfacePanel>
        <div className="flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-black tracking-wider text-brand uppercase">Đang quản lý</p><h2 className="mt-1 text-2xl font-black">{product.name}</h2><p className="mt-1 text-sm text-slate-500">{product.category.name} · {variants.length} biến thể · {images.length} ảnh</p></div><form className="flex flex-wrap items-end gap-3" onSubmit={saveStatus}><label><Label>Trạng thái</Label><select className={fieldClass} onChange={(event) => setStatus(event.target.value as ProductStatus)} value={status}>{productStatuses.map((item) => <option key={item}>{item}</option>)}</select></label><label><Label>Ngày phát hành</Label><input className={fieldClass} onChange={(event) => setPublishedAt(event.target.value)} type="datetime-local" value={publishedAt} /></label><button className="min-h-10 rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" disabled={busy} type="submit">Cập nhật</button></form></div>
      </SurfacePanel>

      <div className="grid items-start gap-6 xl:grid-cols-2">
        <SurfacePanel>
          <div><p className="text-xs font-black tracking-wider text-brand uppercase">SKU & giá</p><h3 className="mt-1 text-xl font-black">Biến thể sản phẩm</h3></div>
          <div className="mt-5 space-y-3">{variants.map((variant) => <div className="rounded-2xl border border-slate-200 p-4" key={variant.id}><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="font-black">{variant.name || variant.sku}</p><p className="mt-1 font-mono text-xs text-slate-500">{variant.sku}</p><p className="mt-2 text-sm font-black text-emerald-800">{formatVnd(variant.price.listPriceVnd)}</p></div><div className="flex items-center gap-2"><StatusBadge label={variant.status} tone={variant.status === "ACTIVE" ? "success" : "neutral"} /><button className="rounded-lg border p-2" onClick={() => editVariant(variant)} type="button"><Pencil className="size-4" /></button></div></div><div className="mt-3 flex flex-wrap gap-2">{variantStatuses.map((item) => <button className={`rounded-lg px-2.5 py-1.5 text-xs font-bold ${variant.status === item ? "bg-emerald-100 text-emerald-900" : "border border-slate-200"}`} disabled={busy || variant.status === item} key={item} onClick={() => void changeVariantStatus(variant, item)} type="button">{item}</button>)}</div></div>)}</div>
          <form className="mt-5 space-y-4 rounded-2xl bg-slate-50 p-4" onSubmit={saveVariant}><div className="flex items-center justify-between"><h4 className="font-black">{variantForm.id ? "Sửa biến thể" : "Thêm biến thể"}</h4>{variantForm.id ? <button className="text-xs font-bold text-slate-500" onClick={() => { setVariantForm(emptyVariant); setVariantAttributes({}); }} type="button">Hủy sửa</button> : <PackagePlus className="size-4" />}</div><div className="grid gap-3 sm:grid-cols-2"><TextField label="SKU" pattern="[A-Za-z0-9._-]+" required value={variantForm.sku} onChange={(value) => setVariantForm({ ...variantForm, sku: value })} /><TextField label="Tên biến thể" value={variantForm.name} onChange={(value) => setVariantForm({ ...variantForm, name: value })} /><NumberTextField label="Giá (VND)" min="0" required value={variantForm.priceVnd} onChange={(value) => setVariantForm({ ...variantForm, priceVnd: value })} /><NumberTextField label="Khối lượng (g)" min="1" required value={variantForm.weightGrams} onChange={(value) => setVariantForm({ ...variantForm, weightGrams: value })} /><NumberTextField label="Dài (cm)" min="1" value={variantForm.lengthCm} onChange={(value) => setVariantForm({ ...variantForm, lengthCm: value })} /><NumberTextField label="Rộng (cm)" min="1" value={variantForm.widthCm} onChange={(value) => setVariantForm({ ...variantForm, widthCm: value })} /><NumberTextField label="Cao (cm)" min="1" value={variantForm.heightCm} onChange={(value) => setVariantForm({ ...variantForm, heightCm: value })} /><NumberTextField label="Thứ tự" min="0" required value={variantForm.sortOrder} onChange={(value) => setVariantForm({ ...variantForm, sortOrder: value })} />{!variantForm.id ? <NumberTextField label="Tồn đầu kỳ" min="0" required value={variantForm.initialOnHandQuantity} onChange={(value) => setVariantForm({ ...variantForm, initialOnHandQuantity: value })} /> : null}</div><div><p className="mb-3 text-sm font-black">Thuộc tính biến thể</p><AdminAttributeValueFields appliesTo="VARIANT" attributes={attributes} mappings={mappings} onChange={setVariantAttributes} values={variantAttributes} /></div><button className="min-h-10 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" disabled={busy} type="submit">{variantForm.id ? "Lưu biến thể" : "Thêm biến thể"}</button></form>
        </SurfacePanel>

        <SurfacePanel>
          <div><p className="text-xs font-black tracking-wider text-brand uppercase">Media</p><h3 className="mt-1 text-xl font-black">Hình ảnh sản phẩm</h3></div>
          <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">{images.map((image) => <div className="overflow-hidden rounded-2xl border border-slate-200" key={image.id}><div className="relative aspect-square bg-slate-100"><Image alt={image.altText ?? product.name} className="object-cover" fill sizes="(max-width: 640px) 50vw, 220px" src={image.imageUrl} unoptimized /></div><div className="flex items-center justify-between gap-2 p-2"><span className="truncate text-[10px] font-bold">{image.primary ? "Ảnh chính" : image.variantId ? "Theo biến thể" : "Sản phẩm"}</span><div className="flex"><button className="p-1.5" onClick={() => editImage(image)} type="button"><Pencil className="size-3.5" /></button><button className="p-1.5 text-red-700" onClick={() => void removeImage(image)} type="button"><Trash2 className="size-3.5" /></button></div></div></div>)}</div>
          <form className="mt-5 space-y-4 rounded-2xl bg-slate-50 p-4" onSubmit={saveImage}><div className="flex items-center justify-between"><h4 className="font-black">{imageForm.id ? "Sửa ảnh" : "Thêm ảnh"}</h4>{imageForm.id ? <button className="text-xs font-bold text-slate-500" onClick={() => setImageForm(emptyImage)} type="button">Hủy sửa</button> : <ImagePlus className="size-4" />}</div><label><Label>URL ảnh</Label><input className={fieldClass} onChange={(event) => setImageForm({ ...imageForm, imageUrl: event.target.value })} required type="url" value={imageForm.imageUrl} /></label><div className="grid gap-3 sm:grid-cols-2"><label><Label>Gắn với biến thể</Label><select className={fieldClass} onChange={(event) => setImageForm({ ...imageForm, variantId: event.target.value })} value={imageForm.variantId}><option value="">Toàn sản phẩm</option>{variants.map((variant) => <option key={variant.id} value={variant.id}>{variant.sku}</option>)}</select></label><TextField label="Alt text" value={imageForm.altText} onChange={(value) => setImageForm({ ...imageForm, altText: value })} /><TextField label="Content-Type" required value={imageForm.contentType} onChange={(value) => setImageForm({ ...imageForm, contentType: value })} /><NumberTextField label="Kích thước (byte)" min="1" required value={imageForm.sizeBytes} onChange={(value) => setImageForm({ ...imageForm, sizeBytes: value })} /><NumberTextField label="Thứ tự" min="0" required value={imageForm.sortOrder} onChange={(value) => setImageForm({ ...imageForm, sortOrder: value })} /></div><label className="flex items-center gap-2 text-sm font-semibold"><input checked={imageForm.primary} onChange={(event) => setImageForm({ ...imageForm, primary: event.target.checked })} type="checkbox" /> Đặt làm ảnh chính</label><button className="min-h-10 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" disabled={busy} type="submit">{imageForm.id ? "Lưu hình ảnh" : "Thêm hình ảnh"}</button></form>
        </SurfacePanel>
      </div>
    </div>
  );
}

function TextField({ label, value, onChange, ...props }: Readonly<{ label: string; value: string; onChange: (value: string) => void } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange">>) {
  return <label><Label>{label}</Label><input {...props} className={fieldClass} onChange={(event) => onChange(event.target.value)} value={value} /></label>;
}

function NumberTextField(props: Readonly<{ label: string; value: string; onChange: (value: string) => void } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type">>) {
  return <TextField {...props} type="number" />;
}
