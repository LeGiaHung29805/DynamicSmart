"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowLeft, ArrowRight, CheckCircle2, ChevronLeft, ChevronRight, Circle, Grid2X2, ImagePlus, Images,
  Layers3, PackagePlus, Pencil, RefreshCw, Search, Sparkles, Trash2,
} from "lucide-react";
import { formatVnd } from "@/components/common/Price";
import { LoadingState } from "@/components/common/PageState";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import {
  createProduct, createProductImage, createVariant, createVariantsBatch, deleteProductImage, getAdminProduct,
  getProductPublishReadiness, listAdminAttributes, listAdminCategories, listAdminProducts, listCategoryAttributes,
  setProductStatus, setVariantStatus, updateProduct, updateProductImage, updateVariant,
  type CreateVariantInput, type ProductImageInput, type ProductInput,
} from "../../api/admin-catalog.api";
import type {
  AdminProduct, AttributeAdmin, CategoryAdmin, CategoryAttributeAdmin, ProductImage,
  ProductPublishReadiness, ProductStatus, ProductVariant, VariantStatus,
} from "../../types";
import { catalogStatusLabel } from "../../utils/catalog-format";
import {
  AdminAttributeValueFields, attributeInputs, attributeMap, type AttributeValueMap,
} from "./AdminAttributeValueFields";
import { apiFieldErrors, errorMessage, fieldClass, Label, optionalNumber, textAreaClass, toDateTimeLocal, toSlug } from "./form-utils";

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
  variantId: "", imageUrl: "", altText: "", sortOrder: "0", primary: false,
};
const productStatuses: ProductStatus[] = ["DRAFT", "ACTIVE", "INACTIVE", "ARCHIVED"];
const variantStatuses: VariantStatus[] = ["ACTIVE", "INACTIVE", "ARCHIVED"];

type ProductWizardStep = "basic" | "attributes" | "variants" | "images" | "review";
type ProductSection = Exclude<ProductWizardStep, "basic">;

interface ProductWizardStepItem {
  id: ProductWizardStep;
  label: string;
  description: string;
  complete: boolean;
  current: boolean;
  disabled?: boolean;
  disabledHint?: string;
  href?: string;
  onSelect?: () => void;
}

function checkComplete(readiness: ProductPublishReadiness | undefined, code: string) {
  return readiness?.checks.find((check) => check.code === code)?.complete === true;
}

function productWizardState(product: AdminProduct | undefined, readiness: ProductPublishReadiness | undefined) {
  const basicReady = checkComplete(readiness, "PRODUCT_CATEGORY_INACTIVE")
    && checkComplete(readiness, "PRODUCT_CATEGORY_MUST_BE_LEAF");
  const attributesReady = checkComplete(readiness, "PRODUCT_ATTRIBUTES_INVALID");
  const variantsReady = [
    "PRODUCT_HAS_NO_ACTIVE_VARIANT",
    "VARIANT_CONFIGURATION_INVALID",
    "VARIANT_COMBINATION_DUPLICATED",
    "PRODUCT_HAS_NO_AVAILABLE_INVENTORY",
  ].every((code) => checkComplete(readiness, code));
  const imagesReady = checkComplete(readiness, "PRODUCT_PRIMARY_IMAGE_REQUIRED");
  return {
    basicReady,
    attributesReady,
    variantsReady,
    imagesReady,
    published: product?.status === "ACTIVE",
    hasVariant: Boolean(product?.variants.length),
  };
}

interface ProductCategoryOption {
  category: CategoryAdmin;
  parentGroupId: string;
  parentPath: string;
  hasChildren: boolean;
}

interface ProductCategoryGroup {
  id: string;
  label: string;
  options: ProductCategoryOption[];
}

function compareCategories(left: CategoryAdmin, right: CategoryAdmin) {
  return left.sortOrder - right.sortOrder || left.name.localeCompare(right.name, "vi");
}

function buildProductCategoryOptions(categories: CategoryAdmin[]): ProductCategoryOption[] {
  const categoryIds = new Set(categories.map((category) => category.id));
  const childrenByParent = new Map<string | null, CategoryAdmin[]>();

  for (const category of categories) {
    const parentId = category.parentId && categoryIds.has(category.parentId) ? category.parentId : null;
    const children = childrenByParent.get(parentId) ?? [];
    children.push(category);
    childrenByParent.set(parentId, children);
  }
  for (const children of childrenByParent.values()) children.sort(compareCategories);

  const options: ProductCategoryOption[] = [];
  const visited = new Set<string>();
  const visit = (category: CategoryAdmin, ancestors: CategoryAdmin[]) => {
    if (visited.has(category.id)) return;
    visited.add(category.id);
    const children = childrenByParent.get(category.id) ?? [];
    const parent = ancestors[ancestors.length - 1];
    options.push({
      category,
      parentGroupId: parent?.id ?? "root-categories",
      parentPath: ancestors.map((ancestor) => ancestor.name).join(" / "),
      hasChildren: children.length > 0,
    });
    for (const child of children) visit(child, [...ancestors, category]);
  };

  for (const root of childrenByParent.get(null) ?? []) visit(root, []);
  for (const category of [...categories].sort(compareCategories)) visit(category, []);
  return options;
}

function groupSelectableProductCategories(options: ProductCategoryOption[]): ProductCategoryGroup[] {
  const groups = new Map<string, ProductCategoryGroup>();

  for (const option of options) {
    if (option.hasChildren) continue;
    const existing = groups.get(option.parentGroupId);
    if (existing) {
      existing.options.push(option);
      continue;
    }
    groups.set(option.parentGroupId, {
      id: option.parentGroupId,
      label: option.parentPath || "Danh mục khác",
      options: [option],
    });
  }

  return [...groups.values()];
}

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

function allProductImages(product: AdminProduct): ProductImage[] {
  const images = [...product.images, ...product.variants.flatMap((variant) => variant.images)];
  return [...new Map(images.map((image) => [image.id, image])).values()]
    .sort((left, right) => left.sortOrder - right.sortOrder);
}

function ProductWizardSidebar({ steps }: Readonly<{ steps: ProductWizardStepItem[] }>) {
  const completeCount = steps.filter((step) => step.complete).length;

  return (
    <aside className="order-first xl:order-last xl:sticky xl:top-24">
      <SurfacePanel>
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-black tracking-wider text-brand uppercase">Quy trình 5 bước</p>
            <h3 className="mt-1 text-xl font-black text-slate-950">Thiết lập sản phẩm</h3>
            <p className="mt-1 text-sm leading-6 text-slate-500">Dữ liệu được lưu theo từng bước để bạn có thể quay lại hoàn thiện sau.</p>
          </div>
          <span className="shrink-0 rounded-full bg-slate-100 px-3 py-1 text-xs font-black text-slate-700">{completeCount}/5</span>
        </div>

        <ol className="mt-5 space-y-2" aria-label="Các bước thiết lập sản phẩm">
          {steps.map((step, index) => {
            const content = (
              <>
                <span className={`grid size-9 shrink-0 place-items-center rounded-full text-sm font-black ${step.complete ? "bg-emerald-100 text-emerald-800" : step.current ? "bg-rose-100 text-rose-700" : "bg-slate-100 text-slate-500"}`}>
                  {step.complete ? <CheckCircle2 className="size-5" /> : index + 1}
                </span>
                <span className="min-w-0 text-left">
                  <span className="block text-sm font-black text-slate-950">{step.label}</span>
                  <span className="mt-0.5 block text-xs leading-5 text-slate-500">{step.description}</span>
                  <span className={`mt-1 block text-[11px] font-black uppercase tracking-wide ${step.current ? "text-rose-700" : step.complete ? "text-emerald-700" : step.disabled ? "text-slate-400" : "text-slate-600"}`}>
                    {step.current ? "Đang thực hiện" : step.complete ? "Đã hoàn thành" : step.disabled ? step.disabledHint ?? "Hoàn thành bước trước" : "Có thể thực hiện"}
                  </span>
                </span>
              </>
            );
            const className = `flex w-full items-start gap-3 rounded-2xl border px-3 py-3 transition ${step.current ? "border-rose-200 bg-rose-50" : step.disabled ? "cursor-not-allowed border-transparent bg-slate-50 opacity-65" : "border-transparent bg-white hover:border-emerald-200 hover:bg-emerald-50/50"}`;

            if (step.href && !step.disabled) {
              return <li key={step.id}><Link aria-current={step.current ? "step" : undefined} className={className} href={step.href}>{content}</Link></li>;
            }
            return <li key={step.id}><button aria-current={step.current ? "step" : undefined} className={className} disabled={step.disabled || !step.onSelect} onClick={step.onSelect} type="button">{content}</button></li>;
          })}
        </ol>

        <p className="mt-5 rounded-2xl bg-slate-50 px-4 py-3 text-xs leading-5 text-slate-600">
          Sản phẩm chưa công khai sẽ không xuất hiện với khách hàng. Bạn có thể lưu và tiếp tục vào lần sau.
        </p>
      </SurfacePanel>
    </aside>
  );
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
          <select aria-label="Lọc trạng thái" className={fieldClass} onChange={(event) => { setPage(0); setStatusFilter(event.target.value as ProductStatus | ""); }} value={statusFilter}><option value="">Mọi trạng thái</option>{productStatuses.map((status) => <option key={status} value={status}>{catalogStatusLabel(status)}</option>)}</select>
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
                <td className="px-4 py-3"><StatusBadge label={catalogStatusLabel(product.status)} tone={product.status === "ACTIVE" ? "success" : product.status === "DRAFT" ? "warning" : "neutral"} /></td>
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
  const [draft, setDraft] = useState<ProductDraft>(emptyProduct);
  const [slugCustomized, setSlugCustomized] = useState(Boolean(productId));
  const [preservedAttributeValues, setPreservedAttributeValues] = useState<AttributeValueMap>({});
  const [loadedProduct, setLoadedProduct] = useState<AdminProduct>();
  const [readiness, setReadiness] = useState<ProductPublishReadiness>();
  const [categoryLocked, setCategoryLocked] = useState(false);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  useEffect(() => {
    let active = true;
    Promise.all([
      listAdminCategories(),
      productId ? getAdminProduct(productId) : Promise.resolve(undefined),
      productId ? getProductPublishReadiness(productId) : Promise.resolve(undefined),
    ]).then(([categoryItems, product, productReadiness]) => {
      if (!active) return;
      setCategories(categoryItems);
      if (product) {
        setLoadedProduct(product);
        setReadiness(productReadiness);
        setDraft(productDraft(product));
        setSlugCustomized(true);
        setPreservedAttributeValues(attributeMap(product.attributes));
        setCategoryLocked(product.variants.length > 0);
      } else {
        const categoryOptions = buildProductCategoryOptions(categoryItems);
        setSlugCustomized(false);
        setCategoryLocked(false);
        setDraft({
          ...emptyProduct,
          categoryId: categoryOptions.find(({ category, hasChildren }) =>
            category.status === "ACTIVE" && !hasChildren)?.category.id ?? "",
        });
      }
    }).catch((caught) => {
      if (active) setError(errorMessage(caught, "Không thể mở biểu mẫu sản phẩm."));
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [productId]);

  function changeProductName(name: string) {
    setDraft((current) => ({
      ...current,
      name,
      slug: !productId && !slugCustomized ? toSlug(name) : current.slug,
    }));
  }

  function changeProductSlug(value: string) {
    const normalized = toSlug(value);
    if (!normalized && !productId) {
      setSlugCustomized(false);
      setDraft((current) => ({ ...current, slug: toSlug(current.name) }));
      return;
    }
    setSlugCustomized(true);
    setDraft((current) => ({ ...current, slug: normalized }));
  }

  function resetProductSlug() {
    setSlugCustomized(false);
    setDraft((current) => ({ ...current, slug: toSlug(current.name) }));
  }

  async function saveBasicInformation(event: FormEvent) {
    event.preventDefault();
    const submitter = (event.nativeEvent as SubmitEvent).submitter as HTMLButtonElement | null;
    const exitAfterSave = submitter?.value === "exit";
    try {
      setBusy(true);
      setError(undefined);
      setFormErrors({});
      const input: ProductInput = {
        categoryId: draft.categoryId,
        name: draft.name.trim(),
        slug: productId || slugCustomized ? draft.slug.trim() : "",
        shortDescription: draft.shortDescription.trim() || null,
        description: draft.description.trim() || null,
        defaultWeightGrams: optionalNumber(draft.defaultWeightGrams),
        defaultLengthCm: optionalNumber(draft.defaultLengthCm),
        defaultWidthCm: optionalNumber(draft.defaultWidthCm),
        defaultHeightCm: optionalNumber(draft.defaultHeightCm),
        featured: draft.featured,
        attributes: productId ? attributeInputs(preservedAttributeValues) : [],
      };
      const saved = productId ? await updateProduct(productId, input) : await createProduct(input);
      showToast(productId ? "Đã cập nhật thông tin cơ bản." : "Đã tạo sản phẩm nháp. Tiếp tục khai báo thuộc tính.", "success");
      router.push(exitAfterSave ? "/admin/catalog/products" : `/admin/catalog/products/${saved.id}?step=attributes`);
      router.refresh();
    } catch (caught) {
      setError(errorMessage(caught));
      setFormErrors(apiFieldErrors(caught));
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <LoadingState title={productId ? "Đang tải sản phẩm" : "Đang chuẩn bị biểu mẫu"} />;

  const cancelHref = productId ? `/admin/catalog/products/${productId}` : "/admin/catalog/products";
  const categoryGroups = groupSelectableProductCategories(
    buildProductCategoryOptions(categories)
      .filter(({ category }) => productId || category.status === "ACTIVE"),
  );
  const progress = productWizardState(loadedProduct, readiness);
  const detailHref = productId ? `/admin/catalog/products/${productId}` : undefined;
  const steps: ProductWizardStepItem[] = [
    { id: "basic", label: "Thông tin cơ bản", description: "Tên, danh mục, nội dung và vận chuyển", complete: progress.basicReady, current: true },
    { id: "attributes", label: "Thuộc tính sản phẩm", description: "Thông số theo danh mục đã chọn", complete: progress.attributesReady, current: false, disabled: !detailHref, disabledHint: "Lưu nháp trước", href: detailHref ? `${detailHref}?step=attributes` : undefined },
    { id: "variants", label: "Biến thể và tồn kho", description: "SKU, giá, thuộc tính và số lượng", complete: progress.variantsReady, current: false, disabled: !detailHref || !progress.attributesReady, disabledHint: "Hoàn thành thuộc tính", href: detailHref ? `${detailHref}?step=variants` : undefined },
    { id: "images", label: "Hình ảnh", description: "Ảnh chính và ảnh theo biến thể", complete: progress.imagesReady, current: false, disabled: !detailHref || !progress.hasVariant, disabledHint: "Tạo ít nhất một biến thể", href: detailHref ? `${detailHref}?step=images` : undefined },
    { id: "review", label: "Kiểm tra và xuất bản", description: "Rà soát điều kiện trước khi bán", complete: progress.published, current: false, disabled: !detailHref, disabledHint: "Lưu nháp trước", href: detailHref ? `${detailHref}?step=review` : undefined },
  ];

  return (
    <div className="space-y-6">
      {error ? <ErrorBanner message={error} /> : null}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-start gap-3">
          <Link aria-label="Quay lại" className="mt-1 grid size-10 place-items-center rounded-xl border border-slate-200 bg-white" href={cancelHref}><ArrowLeft className="size-4" /></Link>
          <div><p className="text-xs font-black tracking-wider text-brand uppercase">{productId ? "Chỉnh sửa sản phẩm" : "Tạo sản phẩm"} · Bước 1 / 5</p><h2 className="mt-1 text-2xl font-black text-slate-950">{productId ? draft.name || "Thông tin cơ bản" : "Thông tin cơ bản"}</h2><p className="mt-1 text-sm text-slate-500">Khai báo dữ liệu nền trước. Giá, SKU và số lượng sẽ được thiết lập tại bước biến thể.</p></div>
        </div>
      </div>

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <form className="min-w-0 space-y-6" onSubmit={saveBasicInformation}>
          <SurfacePanel>
            <div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 1 / 5</p><h3 className="mt-1 text-xl font-black">Nội dung cơ bản</h3><p className="mt-1 text-sm text-slate-500">Sản phẩm được tạo ở trạng thái nháp và chưa hiển thị với khách hàng.</p></div>
            <div className="mt-5 space-y-4">
              <label>
                <Label>Danh mục cuối cùng</Label>
                <select className={fieldClass} disabled={categoryLocked} onChange={(event) => { if (event.target.value !== draft.categoryId) setPreservedAttributeValues({}); setDraft({ ...draft, categoryId: event.target.value }); }} required value={draft.categoryId}>
                  <option value="">Chọn danh mục cuối cùng</option>
                  {categoryGroups.map((group) => (
                    <optgroup key={group.id} label={group.label}>
                      {group.options.map(({ category }) => (
                        <option key={category.id} value={category.id}>
                          {category.name}{productId && category.status !== "ACTIVE" ? ` (${catalogStatusLabel(category.status)})` : ""}
                        </option>
                      ))}
                    </optgroup>
                  ))}
                </select>
                {categoryLocked
                  ? <span className="mt-1.5 block text-xs text-amber-700">Danh mục được khóa vì sản phẩm đã có biến thể. Hãy tạo sản phẩm mới nếu cần chuyển sang bộ thuộc tính khác.</span>
                  : <span className="mt-1.5 block text-xs text-slate-500">Danh mục cha chỉ dùng làm tiêu đề nhóm; bạn chỉ chọn danh mục con. Khi đổi danh mục, thuộc tính cũ sẽ được đặt lại ở bước 2.</span>}
                <FieldError message={formErrors.categoryId} />
              </label>
              <div className="grid gap-4 md:grid-cols-2">
                <label><Label>Tên sản phẩm</Label><input className={fieldClass} maxLength={300} onChange={(event) => changeProductName(event.target.value)} required value={draft.name} /><FieldError message={formErrors.name} /></label>
                <label>
                  <div className="flex items-start justify-between gap-3"><Label>Đường dẫn (slug)</Label><button className="text-xs font-bold text-emerald-700 hover:text-emerald-900" onClick={resetProductSlug} type="button">Tạo lại từ tên</button></div>
                  <input className={fieldClass} maxLength={330} onChange={(event) => changeProductSlug(event.target.value)} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" placeholder="dynamic-phone-pro" required value={draft.slug} />
                  <FieldError message={formErrors.slug} />
                  <span className="mt-1.5 block text-xs text-slate-500">{productId ? "Slug hiện tại được giữ nguyên nếu bạn không sửa." : slugCustomized ? "Slug do bạn nhập sẽ không đổi khi sửa tên sản phẩm." : "Đang tự tạo theo tên; Catalog Service sẽ thêm -2, -3 nếu bị trùng."}</span>
                </label>
              </div>
              <label><Label>Mô tả ngắn</Label><textarea className={textAreaClass} maxLength={1000} onChange={(event) => setDraft({ ...draft, shortDescription: event.target.value })} value={draft.shortDescription} /><FieldError message={formErrors.shortDescription} /></label>
              <label><Label>Mô tả chi tiết</Label><textarea className={`${textAreaClass} min-h-40`} maxLength={20000} onChange={(event) => setDraft({ ...draft, description: event.target.value })} value={draft.description} /><FieldError message={formErrors.description} /></label>
            </div>
          </SurfacePanel>

          <SurfacePanel>
            <div><p className="text-xs font-black tracking-wider text-brand uppercase">Vận chuyển mặc định</p><h3 className="mt-1 text-xl font-black">Khối lượng và kích thước kiện hàng</h3><p className="mt-1 text-sm text-slate-500">Đây là số đo gói hàng dùng để giao vận, không phải Size S/M/L. Biến thể chỉ cần ghi đè khi kiện hàng thực tế khác nhau.</p></div>
            <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><NumberField error={formErrors.defaultWeightGrams} label="Khối lượng kiện (g)" value={draft.defaultWeightGrams} onChange={(value) => setDraft({ ...draft, defaultWeightGrams: value })} /><NumberField error={formErrors.defaultLengthCm} label="Dài kiện (cm)" value={draft.defaultLengthCm} onChange={(value) => setDraft({ ...draft, defaultLengthCm: value })} /><NumberField error={formErrors.defaultWidthCm} label="Rộng kiện (cm)" value={draft.defaultWidthCm} onChange={(value) => setDraft({ ...draft, defaultWidthCm: value })} /><NumberField error={formErrors.defaultHeightCm} label="Cao kiện (cm)" value={draft.defaultHeightCm} onChange={(value) => setDraft({ ...draft, defaultHeightCm: value })} /></div>
            <label className="mt-4 flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-3 text-sm font-semibold"><input checked={draft.featured} onChange={(event) => setDraft({ ...draft, featured: event.target.checked })} type="checkbox" /> Đề xuất sản phẩm ở khu vực nổi bật</label>
          </SurfacePanel>

          <div className="sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white/95 p-4 shadow-xl backdrop-blur"><p className="text-sm text-slate-500">Bước này chỉ lưu thông tin nền; sản phẩm vẫn ở trạng thái nháp.</p><div className="flex flex-wrap gap-2"><Link className="inline-flex min-h-11 items-center rounded-xl border border-slate-200 px-4 text-sm font-bold" href={cancelHref}>Hủy</Link><button className="inline-flex min-h-11 items-center rounded-xl border border-slate-300 bg-white px-4 text-sm font-bold text-slate-700 disabled:opacity-50" disabled={busy} name="saveAction" type="submit" value="exit">Lưu nháp và thoát</button><button className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-emerald-950 px-5 text-sm font-black text-white disabled:opacity-50" disabled={busy} name="saveAction" type="submit" value="continue">{busy ? "Đang lưu..." : <>Lưu và sang bước thuộc tính <ArrowRight className="size-4" /></>}</button></div></div>
        </form>
        <ProductWizardSidebar steps={steps} />
      </div>
    </div>
  );
}

export function AdminProductDetailPanel({ productId, initialStep }: Readonly<{ productId: string; initialStep?: string }>) {
  const [product, setProduct] = useState<AdminProduct>();
  const [readiness, setReadiness] = useState<ProductPublishReadiness>();
  const [attributes, setAttributes] = useState<AttributeAdmin[]>([]);
  const [mappings, setMappings] = useState<CategoryAttributeAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      const [loadedProduct, loadedAttributes, loadedReadiness] = await Promise.all([
        getAdminProduct(productId),
        listAdminAttributes(),
        getProductPublishReadiness(productId),
      ]);
      const loadedMappings = await listCategoryAttributes(loadedProduct.category.id);
      setProduct(loadedProduct);
      setReadiness(loadedReadiness);
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
  if (!product || !readiness) return <div className="space-y-4"><ErrorBanner message={error ?? "Không tìm thấy sản phẩm."} /><Link className="inline-flex items-center gap-2 text-sm font-bold text-brand" href="/admin/catalog/products"><ArrowLeft className="size-4" /> Quay lại danh sách</Link></div>;

  return <ProductOperations attributes={attributes} initialReadiness={readiness} initialStep={initialStep} key={product.id} mappings={mappings} onError={setError} onProductUpdated={setProduct} product={product} topError={error} />;
}

function NumberField({ label, value, onChange, error }: Readonly<{ label: string; value: string; onChange: (value: string) => void; error?: string }>) {
  return <label><Label>{label}</Label><input className={fieldClass} min="1" onChange={(event) => onChange(event.target.value)} type="number" value={value} /><FieldError message={error} /></label>;
}

function ProductOperations({ product, attributes, mappings: initialMappings, initialReadiness, initialStep, onProductUpdated, onError, topError }: Readonly<{
  product: AdminProduct;
  attributes: AttributeAdmin[];
  mappings: CategoryAttributeAdmin[];
  initialReadiness: ProductPublishReadiness;
  initialStep?: string;
  onProductUpdated: (product: AdminProduct) => void;
  onError: (message: string | undefined) => void;
  topError?: string;
}>) {
  const { showToast } = useToast();
  const initialImages = allProductImages(product);
  const [section, setSection] = useState<ProductSection>(() => {
    const progress = productWizardState(product, initialReadiness);
    const requested = ["attributes", "variants", "images", "review"].includes(initialStep ?? "")
      ? initialStep as ProductSection
      : undefined;
    if (requested === "review") return requested;
    if (requested === "images" && progress.hasVariant) return requested;
    if (requested === "variants" && progress.basicReady && progress.attributesReady) return requested;
    if (requested === "attributes") return requested;
    if (!progress.attributesReady) return "attributes";
    if (!progress.variantsReady) return "variants";
    if (!progress.imagesReady) return "images";
    return "review";
  });
  const [variants, setVariants] = useState(product.variants);
  const [images, setImages] = useState(initialImages);
  const [readiness, setReadiness] = useState(initialReadiness);
  const [mappings, setMappings] = useState(initialMappings);
  const [productAttributeValues, setProductAttributeValues] = useState<AttributeValueMap>(() => attributeMap(product.attributes));
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

  function selectSection(next: ProductSection) {
    setSection(next);
    const url = new URL(window.location.href);
    url.searchParams.set("step", next);
    window.history.replaceState(null, "", `${url.pathname}${url.search}${url.hash}`);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function refreshWorkspace() {
    const [freshProduct, freshReadiness] = await Promise.all([
      getAdminProduct(product.id),
      getProductPublishReadiness(product.id),
    ]);
    setVariants(freshProduct.variants);
    setImages(allProductImages(freshProduct));
    setReadiness(freshReadiness);
    setProductAttributeValues(attributeMap(freshProduct.attributes));
    setStatus(freshProduct.status);
    setPublishedAt(toDateTimeLocal(freshProduct.publishedAt));
    onProductUpdated(freshProduct);
    return { freshProduct, freshReadiness };
  }

  async function saveProductAttributes(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); onError(undefined);
      const input: ProductInput = {
        categoryId: product.category.id,
        name: product.name,
        slug: product.slug,
        shortDescription: product.shortDescription ?? null,
        description: product.description ?? null,
        defaultWeightGrams: product.defaultWeightGrams ?? null,
        defaultLengthCm: product.defaultLengthCm ?? null,
        defaultWidthCm: product.defaultWidthCm ?? null,
        defaultHeightCm: product.defaultHeightCm ?? null,
        featured: product.featured,
        attributes: attributeInputs(productAttributeValues),
      };
      await updateProduct(product.id, input);
      const refreshed = await refreshWorkspace();
      const attributeCheck = refreshed.freshReadiness.checks.find((check) => check.code === "PRODUCT_ATTRIBUTES_INVALID");
      if (attributeCheck?.complete) {
        showToast("Đã lưu thuộc tính sản phẩm.", "success");
        selectSection("variants");
      } else {
        onError(attributeCheck?.message ?? "Thuộc tính sản phẩm chưa đầy đủ.");
        showToast("Đã lưu bản nháp nhưng vẫn còn thuộc tính cần hoàn thiện.", "info");
      }
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  async function saveStatus(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); onError(undefined);
      await setProductStatus(product.id, status, publishedAt ? new Date(publishedAt).toISOString() : null);
      await refreshWorkspace();
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
      const editing = Boolean(variantForm.id);
      if (variantForm.id) {
        await updateVariant(product.id, variantForm.id, common);
      } else {
        await createVariant(product.id, { ...common, initialOnHandQuantity: Number(variantForm.initialOnHandQuantity) });
      }
      await refreshWorkspace();
      setVariantForm(emptyVariant); setVariantAttributes({});
      showToast(editing ? "Đã cập nhật biến thể." : "Đã tạo biến thể ở trạng thái chưa kích hoạt. Hãy kiểm tra rồi kích hoạt.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  async function createVariantMatrix(inputs: CreateVariantInput[]) {
    try {
      setBusy(true); onError(undefined);
      await createVariantsBatch(product.id, inputs);
      await refreshWorkspace();
      showToast(`Đã tạo ${inputs.length} biến thể từ ma trận thuộc tính.`, "success");
      return true;
    } catch (caught) {
      onError(errorMessage(caught));
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function changeVariantStatus(variant: ProductVariant, next: VariantStatus) {
    try {
      setBusy(true); onError(undefined);
      await setVariantStatus(product.id, variant.id, next);
      await refreshWorkspace();
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
        altText: imageForm.altText.trim() || null,
        sortOrder: Number(imageForm.sortOrder),
        primary: imageForm.primary,
      };
      const editing = Boolean(imageForm.id);
      if (imageForm.id) {
        await updateProductImage(product.id, imageForm.id, input);
      } else {
        await createProductImage(product.id, input);
      }
      await refreshWorkspace();
      setImageForm(emptyImage);
      showToast(editing ? "Đã cập nhật hình ảnh." : "Đã thêm hình ảnh.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  function editImage(image: ProductImage) {
    setImageForm({ id: image.id, variantId: image.variantId ?? "", imageUrl: image.imageUrl, altText: image.altText ?? "", sortOrder: String(image.sortOrder), primary: image.primary });
  }

  async function removeImage(image: ProductImage) {
    if (!window.confirm("Xóa hình ảnh này khỏi sản phẩm?")) return;
    try {
      setBusy(true); onError(undefined);
      await deleteProductImage(product.id, image.id);
      await refreshWorkspace();
      showToast("Đã xóa hình ảnh.", "success");
    } catch (caught) { onError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  const progress = productWizardState({ ...product, variants }, readiness);
  const steps: ProductWizardStepItem[] = [
    { id: "basic", label: "Thông tin cơ bản", description: "Tên, danh mục, nội dung và vận chuyển", complete: progress.basicReady, current: false, href: `/admin/catalog/products/${product.id}/edit` },
    { id: "attributes", label: "Thuộc tính sản phẩm", description: "Thông số theo danh mục đã chọn", complete: progress.attributesReady, current: section === "attributes", onSelect: () => selectSection("attributes") },
    { id: "variants", label: "Biến thể và tồn kho", description: "SKU, giá, thuộc tính và số lượng", complete: progress.variantsReady, current: section === "variants", disabled: !progress.basicReady || !progress.attributesReady, disabledHint: "Hoàn thành thuộc tính", onSelect: () => selectSection("variants") },
    { id: "images", label: "Hình ảnh", description: "Ảnh chính và ảnh theo biến thể", complete: progress.imagesReady, current: section === "images", disabled: !progress.hasVariant, disabledHint: "Tạo ít nhất một biến thể", onSelect: () => selectSection("images") },
    { id: "review", label: "Kiểm tra và xuất bản", description: "Rà soát điều kiện trước khi bán", complete: progress.published, current: section === "review", onSelect: () => selectSection("review") },
  ];

  return (
    <div className="space-y-6">
      {topError ? <ErrorBanner message={topError} /> : null}
      <SurfacePanel>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-start gap-3"><Link aria-label="Quay lại danh sách" className="mt-1 grid size-10 place-items-center rounded-xl border border-slate-200" href="/admin/catalog/products"><ArrowLeft className="size-4" /></Link><div><p className="text-xs font-black tracking-wider text-brand uppercase">Đang quản lý</p><h2 className="mt-1 text-2xl font-black text-slate-950">{product.name}</h2><p className="mt-1 text-sm text-slate-500">{product.category.name} · <span className="font-mono">{product.slug}</span></p></div></div>
          <div className="flex items-center gap-3"><StatusBadge label={catalogStatusLabel(product.status)} tone={product.status === "ACTIVE" ? "success" : product.status === "DRAFT" ? "warning" : "neutral"} /><Link className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-slate-200 px-4 text-sm font-bold" href={`/admin/catalog/products/${product.id}/edit`}><Pencil className="size-4" /> Sửa bước 1</Link></div>
        </div>
      </SurfacePanel>

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="min-w-0 space-y-6">
          {section === "attributes" ? <ProductAttributeWorkspace attributes={attributes} busy={busy} mappings={mappings} onSave={saveProductAttributes} product={product} setValues={setProductAttributeValues} values={productAttributeValues} /> : null}
          {section === "variants" ? <VariantWorkspace attributes={attributes} busy={busy} mappings={mappings} onChangeStatus={changeVariantStatus} onCreateBatch={createVariantMatrix} onEdit={editVariant} onReset={() => { setVariantForm(emptyVariant); setVariantAttributes({}); }} onSave={saveVariant} product={product} setVariantAttributes={setVariantAttributes} setVariantForm={setVariantForm} variantAttributes={variantAttributes} variantForm={variantForm} variants={variants} /> : null}
          {section === "images" ? <ImageWorkspace busy={busy} imageForm={imageForm} images={images} onEdit={editImage} onRemove={removeImage} onReset={() => setImageForm(emptyImage)} onSave={saveImage} product={product} setImageForm={setImageForm} variants={variants} /> : null}
          {section === "review" ? <ProductOverview busy={busy} images={images} onSaveStatus={saveStatus} product={product} publishedAt={publishedAt} readiness={readiness} setPublishedAt={setPublishedAt} setStatus={setStatus} status={status} variants={variants} /> : null}
          {section === "variants" && progress.variantsReady ? <NextStepBar label="Biến thể và tồn kho đã sẵn sàng" onNext={() => selectSection("images")} target="Tiếp tục thêm hình ảnh" /> : null}
          {section === "images" && progress.imagesReady ? <NextStepBar label="Ảnh chính đã sẵn sàng" onNext={() => selectSection("review")} target="Kiểm tra và xuất bản" /> : null}
        </div>
        <ProductWizardSidebar steps={steps} />
      </div>
    </div>
  );
}

function NextStepBar({ label, target, onNext }: Readonly<{ label: string; target: string; onNext: () => void }>) {
  return <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4"><p className="inline-flex items-center gap-2 text-sm font-bold text-emerald-900"><CheckCircle2 className="size-5" /> {label}</p><button className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" onClick={onNext} type="button">{target} <ArrowRight className="size-4" /></button></div>;
}

function ProductAttributeWorkspace({ product, attributes, mappings, values, setValues, busy, onSave }: Readonly<{
  product: AdminProduct;
  attributes: AttributeAdmin[];
  mappings: CategoryAttributeAdmin[];
  values: AttributeValueMap;
  setValues: (values: AttributeValueMap) => void;
  busy: boolean;
  onSave: (event: FormEvent) => void;
}>) {
  const productMappings = mappings.filter((mapping) => mapping.appliesTo === "PRODUCT");
  return (
    <SurfacePanel>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 2 / 5</p><h3 className="mt-1 text-xl font-black">Thuộc tính sản phẩm</h3><p className="mt-1 text-sm text-slate-500">Bộ thuộc tính được lấy theo danh mục <strong className="text-slate-800">{product.category.name}</strong>.</p></div>
        <Link className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-slate-200 px-3 text-xs font-bold text-slate-700" href={`/admin/catalog/products/${product.id}/edit`}><Pencil className="size-3.5" /> Đổi thông tin cơ bản</Link>
      </div>
      <form className="mt-6 space-y-6" onSubmit={onSave}>
        <div className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2"><p className="text-sm font-black text-slate-900">Thông số chung</p><span className="rounded-full bg-white px-2.5 py-1 text-xs font-bold text-slate-600">{productMappings.length} thuộc tính</span></div>
          <AdminAttributeValueFields appliesTo="PRODUCT" attributes={attributes} mappings={mappings} onChange={setValues} values={values} />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4"><p className="text-xs leading-5 text-slate-500">Các thuộc tính có dấu * phải được nhập trước khi sang bước biến thể.</p><button className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-emerald-950 px-5 text-sm font-black text-white disabled:opacity-50" disabled={busy} type="submit">{busy ? "Đang lưu..." : <>Lưu và sang bước biến thể <ArrowRight className="size-4" /></>}</button></div>
      </form>
    </SurfacePanel>
  );
}

function ProductOverview({ product, variants, images, readiness, status, setStatus, publishedAt, setPublishedAt, busy, onSaveStatus }: Readonly<{
  product: AdminProduct; variants: ProductVariant[]; images: ProductImage[]; status: ProductStatus;
  readiness: ProductPublishReadiness;
  setStatus: (value: ProductStatus) => void; publishedAt: string; setPublishedAt: (value: string) => void;
  busy: boolean; onSaveStatus: (event: FormEvent) => void;
}>) {
  return <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
    <div className="space-y-6"><SurfacePanel><h3 className="text-xl font-black">Tóm tắt sản phẩm</h3><dl className="mt-5 grid gap-3 sm:grid-cols-2"><Summary label="Danh mục" value={product.category.name} /><Summary label="Số biến thể" value={String(variants.length)} /><Summary label="Số hình ảnh" value={String(images.length)} /><Summary label="Nổi bật" value={product.featured ? "Có" : "Không"} /><Summary label="Khối lượng mặc định" value={product.defaultWeightGrams ? `${product.defaultWeightGrams.toLocaleString("vi-VN")} g` : "Chưa đặt"} /><Summary label="Ngày công khai" value={product.publishedAt ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(product.publishedAt)) : "Chưa công khai"} /></dl>{product.shortDescription ? <p className="mt-5 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">{product.shortDescription}</p> : null}</SurfacePanel><SurfacePanel><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-black tracking-wider text-brand uppercase">Checklist</p><h3 className="mt-1 text-xl font-black">Điều kiện xuất bản</h3></div><StatusBadge label={readiness.ready ? "Sẵn sàng" : "Chưa hoàn tất"} tone={readiness.ready ? "success" : "warning"} /></div><ul className="mt-5 space-y-3">{readiness.checks.map((check) => <li className={`flex gap-3 rounded-xl border p-3 ${check.complete ? "border-emerald-100 bg-emerald-50" : "border-amber-200 bg-amber-50"}`} key={check.code}>{check.complete ? <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-emerald-700" /> : <Circle className="mt-0.5 size-5 shrink-0 text-amber-700" />}<span><span className="block text-sm font-black text-slate-900">{check.label}</span>{!check.complete ? <span className="mt-0.5 block text-xs leading-5 text-amber-800">{check.message}</span> : null}</span></li>)}</ul></SurfacePanel></div>
    <SurfacePanel className="lg:sticky lg:top-24"><div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 5 / 5</p><h3 className="mt-1 text-xl font-black">Kiểm tra và xuất bản</h3><p className="mt-1 text-sm text-slate-500">Chỉ có thể công khai khi toàn bộ checklist đã hoàn tất.</p></div><form className="mt-5 space-y-4" onSubmit={onSaveStatus}><label><Label>Trạng thái</Label><select className={fieldClass} onChange={(event) => setStatus(event.target.value as ProductStatus)} value={status}>{productStatuses.map((item) => <option key={item} value={item}>{catalogStatusLabel(item)}</option>)}</select></label><label><Label>Ngày phát hành</Label><input className={fieldClass} onChange={(event) => setPublishedAt(event.target.value)} type="datetime-local" value={publishedAt} /></label>{status === "ACTIVE" && !readiness.ready ? <p className="rounded-xl bg-amber-50 px-3 py-2 text-xs font-semibold text-amber-800">Hoàn tất các mục còn thiếu trước khi công khai sản phẩm.</p> : null}<button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50" disabled={busy || (status === "ACTIVE" && !readiness.ready)} type="submit">{busy ? "Đang cập nhật..." : status === "ACTIVE" ? "Công khai sản phẩm" : "Cập nhật trạng thái"}</button></form></SurfacePanel>
  </div>;
}

interface VariantMatrixAxis {
  mapping: CategoryAttributeAdmin;
  attribute: AttributeAdmin;
}

interface VariantMatrixChoice {
  attributeId: string;
  attributeName: string;
  code: string;
  label: string;
  value: unknown;
}

function normalizedCombinationValue(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(normalizedCombinationValue).sort().join(",")}]`;
  return String(value ?? "").trim().toUpperCase();
}

function variantCombinationKey(values: Array<{ attributeId: string; value: unknown }>) {
  if (!values.length) return "__DEFAULT__";
  return values
    .map((value) => `${value.attributeId}=${normalizedCombinationValue(value.value)}`)
    .sort()
    .join("|");
}

function buildVariantCombinations(
  axes: VariantMatrixAxis[],
  selectedOptions: Record<string, string[]>,
  customValues: Record<string, string>,
): VariantMatrixChoice[][] {
  if (!axes.length) return [[]];
  let combinations: VariantMatrixChoice[][] = [[]];
  for (const axis of axes) {
    const selected = new Set(selectedOptions[axis.attribute.id] ?? []);
    const choices: VariantMatrixChoice[] = axis.attribute.dataType === "SELECT"
      ? axis.attribute.options
        .filter((option) => option.status === "ACTIVE" && selected.has(option.code))
        .map((option) => ({
          attributeId: axis.attribute.id,
          attributeName: axis.attribute.name,
          code: option.code,
          label: option.label,
          value: option.code,
        }))
      : [...new Set((customValues[axis.attribute.id] ?? "")
        .split(/[\n,]+/)
        .map((value) => value.trim())
        .filter(Boolean))]
        .filter((value) => axis.attribute.dataType === "TEXT" || Number.isFinite(Number(value)))
        .map((value) => ({
          attributeId: axis.attribute.id,
          attributeName: axis.attribute.name,
          code: value,
          label: value,
          value: axis.attribute.dataType === "NUMBER" || axis.attribute.dataType === "DECIMAL"
            ? Number(value)
            : value,
        }));
    if (!choices.length) {
      if (axis.mapping.required) return [];
      continue;
    }
    const next: VariantMatrixChoice[][] = [];
    for (const combination of combinations) {
      for (const choice of choices) {
        next.push([...combination, choice]);
        if (next.length > 100) return next;
      }
    }
    combinations = next;
  }
  return combinations;
}

function skuToken(value: string) {
  return toSlug(value).toUpperCase() || "VARIANT";
}

function matrixSku(prefix: string, combination: VariantMatrixChoice[]) {
  const suffixes = combination.length ? combination.map((choice) => skuToken(choice.code)) : ["DEFAULT"];
  return [skuToken(prefix), ...suffixes].join("-").slice(0, 100).replace(/-+$/, "");
}

function VariantMatrixCreator({ product, variants, attributes, mappings, busy, onCreate }: Readonly<{
  product: AdminProduct;
  variants: ProductVariant[];
  attributes: AttributeAdmin[];
  mappings: CategoryAttributeAdmin[];
  busy: boolean;
  onCreate: (inputs: CreateVariantInput[]) => Promise<boolean>;
}>) {
  const axes = useMemo<VariantMatrixAxis[]>(() => mappings
    .filter((mapping) => mapping.appliesTo === "VARIANT")
    .map((mapping) => ({
      mapping,
      attribute: attributes.find((attribute) => attribute.id === mapping.attributeId),
    }))
    .filter((axis): axis is VariantMatrixAxis => axis.attribute !== undefined
      && ["SELECT", "TEXT", "NUMBER", "DECIMAL"].includes(axis.attribute.dataType)), [attributes, mappings]);
  const unsupportedRequired = useMemo(() => mappings
    .filter((mapping) => mapping.appliesTo === "VARIANT" && mapping.required)
    .filter((mapping) => !["SELECT", "TEXT", "NUMBER", "DECIMAL"].includes(
      attributes.find((attribute) => attribute.id === mapping.attributeId)?.dataType ?? "",
    )), [attributes, mappings]);
  const [selectedOptions, setSelectedOptions] = useState<Record<string, string[]>>({});
  const [customValues, setCustomValues] = useState<Record<string, string>>({});
  const [skuPrefix, setSkuPrefix] = useState(skuToken(product.slug));
  const [priceVnd, setPriceVnd] = useState("");
  const [weightGrams, setWeightGrams] = useState(product.defaultWeightGrams?.toString() ?? "");
  const [initialQuantity, setInitialQuantity] = useState("0");

  const combinations = useMemo(
    () => buildVariantCombinations(axes, selectedOptions, customValues),
    [axes, customValues, selectedOptions],
  );
  const existingKeys = useMemo(() => new Set(variants.map((variant) => variantCombinationKey(
    variant.attributes.map((attribute) => ({ attributeId: attribute.attributeId, value: attribute.value })),
  ))), [variants]);
  const newCombinations = combinations.filter((combination) => !existingKeys.has(variantCombinationKey(
    combination.map((choice) => ({ attributeId: choice.attributeId, value: choice.value })),
  )));
  const tooMany = combinations.length > 100;
  const maximumSortOrder = variants.reduce((maximum, variant) => Math.max(maximum, variant.sortOrder), 0);

  function toggleOption(attributeId: string, code: string) {
    const current = selectedOptions[attributeId] ?? [];
    setSelectedOptions({
      ...selectedOptions,
      [attributeId]: current.includes(code)
        ? current.filter((item) => item !== code)
        : [...current, code],
    });
  }

  async function createMatrix(event: FormEvent) {
    event.preventDefault();
    if (!newCombinations.length || tooMany || unsupportedRequired.length) return;
    const inputs: CreateVariantInput[] = newCombinations.map((combination, index) => ({
      sku: matrixSku(skuPrefix, combination),
      name: combination.length ? combination.map((choice) => choice.label).join(" / ") : "Phiên bản mặc định",
      priceVnd: Number(priceVnd),
      weightGrams: Number(weightGrams),
      lengthCm: null,
      widthCm: null,
      heightCm: null,
      sortOrder: maximumSortOrder + ((index + 1) * 10),
      initialOnHandQuantity: Number(initialQuantity),
      attributes: combination.map((choice) => ({ attributeId: choice.attributeId, value: choice.value })),
    }));
    if (await onCreate(inputs)) {
      setSelectedOptions({});
      setCustomValues({});
    }
  }

  return (
    <SurfacePanel>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="text-xs font-black tracking-wider text-brand uppercase">Tạo nhanh</p><h3 className="mt-1 flex items-center gap-2 text-xl font-black"><Grid2X2 className="size-5 text-brand" /> Ma trận biến thể</h3><p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">Chọn các giá trị như Màu sắc và Kích cỡ; hệ thống sẽ tạo mọi tổ hợp trong một giao dịch. Những tổ hợp đã tồn tại được tự động bỏ qua.</p></div>
        <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-black text-emerald-800">{axes.length || 1} nhóm lựa chọn</span>
      </div>

      {unsupportedRequired.length ? <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-900">Danh mục có thuộc tính biến thể bắt buộc không phù hợp để tạo ma trận. Hãy dùng biểu mẫu thủ công bên dưới hoặc đổi cấu hình thuộc tính trước.</p> : null}
      {axes.length > 3 ? <p className="mt-4 rounded-xl bg-amber-50 px-4 py-3 text-xs leading-5 text-amber-800">Danh mục đang có hơn ba chiều biến thể. Nên giữ tối đa ba chiều để tránh sinh quá nhiều tổ hợp khó quản lý.</p> : null}

      {axes.length ? <div className="mt-5 grid gap-4 lg:grid-cols-2">{axes.map(({ attribute, mapping }) => (
        <fieldset className="rounded-2xl border border-slate-200 p-4" key={attribute.id}>
          <legend className="px-1 text-sm font-black text-slate-900">{attribute.name}{mapping.required ? <span className="text-red-600"> *</span> : null}</legend>
          {attribute.dataType === "SELECT" ? <div className="mt-2 flex flex-wrap gap-2">{attribute.options.filter((option) => option.status === "ACTIVE").map((option) => {
            const selected = (selectedOptions[attribute.id] ?? []).includes(option.code);
            return <button aria-pressed={selected} className={`min-h-9 rounded-xl border px-3 text-sm font-bold transition ${selected ? "border-emerald-700 bg-emerald-50 text-emerald-900" : "border-slate-200 bg-white text-slate-600 hover:border-emerald-300"}`} key={option.id} onClick={() => toggleOption(attribute.id, option.code)} type="button">{selected ? "✓ " : ""}{option.label}</button>;
          })}</div> : <div className="mt-2"><textarea aria-label={`Các giá trị ${attribute.name}`} className={`${textAreaClass} min-h-24`} onChange={(event) => setCustomValues({ ...customValues, [attribute.id]: event.target.value })} placeholder={attribute.dataType === "TEXT" ? "Ví dụ: Size S, Size M, Size L" : "Nhập mỗi giá trị trên một dòng"} value={customValues[attribute.id] ?? ""} /><p className="mt-1.5 text-xs text-slate-500">Nhập mỗi giá trị trên một dòng hoặc cách nhau bằng dấu phẩy.</p></div>}
        </fieldset>
      ))}</div> : <p className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm leading-6 text-slate-600">Danh mục không có thuộc tính tạo biến thể. Hệ thống sẽ tạo một <strong>phiên bản mặc định</strong> để giá, tồn kho, giỏ hàng và đơn hàng vẫn dùng chung một luồng Variant.</p>}

      <form className="mt-5 space-y-4 border-t border-slate-100 pt-5" onSubmit={createMatrix}>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <TextField label="Tiền tố SKU" maxLength={60} pattern="[A-Za-z0-9._-]+" required value={skuPrefix} onChange={setSkuPrefix} />
          <NumberTextField label="Giá chung (VND)" min="0" required value={priceVnd} onChange={setPriceVnd} />
          <NumberTextField label="Khối lượng kiện (g)" min="1" required value={weightGrams} onChange={setWeightGrams} />
          <NumberTextField label="Tồn đầu kỳ mỗi biến thể" min="0" required value={initialQuantity} onChange={setInitialQuantity} />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 px-4 py-3">
          <div><p className="text-sm font-black text-slate-900">{tooMany ? "Vượt giới hạn 100 tổ hợp" : `${newCombinations.length} biến thể mới sẽ được tạo`}</p><p className="mt-1 text-xs text-slate-500">{combinations.length - newCombinations.length} tổ hợp đã tồn tại sẽ được bỏ qua · kích thước kiện hàng kế thừa từ sản phẩm.</p></div>
          <button className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-emerald-950 px-5 text-sm font-black text-white disabled:cursor-not-allowed disabled:opacity-40" disabled={busy || tooMany || !newCombinations.length || Boolean(unsupportedRequired.length) || !priceVnd || !weightGrams} type="submit"><Sparkles className="size-4" />{busy ? "Đang tạo..." : `Tạo ${newCombinations.length} biến thể`}</button>
        </div>
        {newCombinations.length && !tooMany ? <div className="flex flex-wrap gap-2">{newCombinations.slice(0, 8).map((combination) => <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-slate-600" key={variantCombinationKey(combination.map((choice) => ({ attributeId: choice.attributeId, value: choice.value })))}>{combination.length ? combination.map((choice) => choice.label).join(" / ") : "Phiên bản mặc định"}</span>)}{newCombinations.length > 8 ? <span className="px-2 py-1 text-xs font-bold text-slate-500">+{newCombinations.length - 8} tổ hợp</span> : null}</div> : null}
      </form>
    </SurfacePanel>
  );
}

function VariantWorkspace({ product, variants, variantForm, setVariantForm, variantAttributes, setVariantAttributes, attributes, mappings, busy, onSave, onCreateBatch, onEdit, onReset, onChangeStatus }: Readonly<{
  product: AdminProduct;
  variants: ProductVariant[]; variantForm: VariantDraft; setVariantForm: (value: VariantDraft) => void;
  variantAttributes: AttributeValueMap; setVariantAttributes: (value: AttributeValueMap) => void;
  attributes: AttributeAdmin[]; mappings: CategoryAttributeAdmin[]; busy: boolean;
  onSave: (event: FormEvent) => void; onEdit: (variant: ProductVariant) => void; onReset: () => void;
  onCreateBatch: (inputs: CreateVariantInput[]) => Promise<boolean>;
  onChangeStatus: (variant: ProductVariant, status: VariantStatus) => Promise<void>;
}>) {
  return (
    <div className="space-y-6">
      <VariantMatrixCreator attributes={attributes} busy={busy} mappings={mappings} onCreate={onCreateBatch} product={product} variants={variants} />
      <div className="grid items-start gap-6 2xl:grid-cols-[minmax(0,1.1fr)_minmax(360px,.9fr)]">
      <SurfacePanel>
        <div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 3 / 5</p><h3 className="mt-1 text-xl font-black">Biến thể và tồn kho</h3><p className="mt-1 text-sm leading-6 text-slate-500">Bước này hoàn tất khi có ít nhất một biến thể đang hoạt động và còn tồn khả dụng. Có thể điều chỉnh thêm tại <Link className="font-bold text-emerald-700" href="/admin/catalog/inventory">Quản lý tồn kho</Link>.</p></div>
        <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Biến thể</th><th className="px-4 py-3">Giá</th><th className="px-4 py-3 text-center">Tồn khả dụng</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Sửa</th></tr></thead>
            <tbody className="divide-y divide-slate-100">{variants.map((variant) => <tr key={variant.id}><td className="px-4 py-3"><p className="font-bold text-slate-950">{variant.name || variant.sku}</p><p className="font-mono text-xs text-slate-500">{variant.sku}</p></td><td className="px-4 py-3 font-black text-emerald-800">{formatVnd(variant.price.listPriceVnd)}</td><td className="px-4 py-3 text-center font-black tabular-nums">{variant.inventory.availableQuantity}</td><td className="px-4 py-3"><select aria-label={`Trạng thái ${variant.sku}`} className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs font-bold" disabled={busy} onChange={(event) => void onChangeStatus(variant, event.target.value as VariantStatus)} value={variant.status}>{variantStatuses.map((item) => <option key={item} value={item}>{catalogStatusLabel(item)}</option>)}</select></td><td className="px-4 py-3 text-right"><button aria-label={`Sửa ${variant.sku}`} className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50" onClick={() => onEdit(variant)} type="button"><Pencil className="size-4" /></button></td></tr>)}</tbody>
          </table>
          {!variants.length ? <div className="p-8 text-center"><Layers3 className="mx-auto size-8 text-slate-300" /><p className="mt-3 font-bold text-slate-700">Sản phẩm chưa có biến thể</p><p className="mt-1 text-sm text-slate-500">Tạo biến thể đầu tiên ở biểu mẫu bên cạnh.</p></div> : null}
        </div>
      </SurfacePanel>

      <SurfacePanel className="2xl:sticky 2xl:top-24">
        <div className="flex items-center justify-between gap-3"><div><p className="text-xs font-black tracking-wider text-brand uppercase">{variantForm.id ? "Chỉnh sửa biến thể" : "Tạo thủ công"}</p><h3 className="mt-1 text-xl font-black">{variantForm.id ? variantForm.sku : "Một biến thể riêng"}</h3><p className="mt-1 text-sm text-slate-500">Dùng khi cần cấu hình đặc biệt hoặc sửa một biến thể. Size S/M/L được nhập ở phần thuộc tính; các số đo bên dưới là kiện hàng giao vận.</p></div>{variantForm.id ? <button className="text-sm font-bold text-slate-500" onClick={onReset} type="button">Hủy sửa</button> : <PackagePlus className="size-5 text-brand" />}</div>
        <form className="mt-5 space-y-4" onSubmit={onSave}>
          <div className="grid gap-3 sm:grid-cols-2"><TextField label="SKU" pattern="[A-Za-z0-9._-]+" required value={variantForm.sku} onChange={(value) => setVariantForm({ ...variantForm, sku: value })} /><TextField label="Tên biến thể" value={variantForm.name} onChange={(value) => setVariantForm({ ...variantForm, name: value })} /><NumberTextField label="Giá (VND)" min="0" required value={variantForm.priceVnd} onChange={(value) => setVariantForm({ ...variantForm, priceVnd: value })} /><NumberTextField label="Khối lượng kiện (g)" min="1" required value={variantForm.weightGrams} onChange={(value) => setVariantForm({ ...variantForm, weightGrams: value })} /><NumberTextField label="Dài kiện (cm)" min="1" value={variantForm.lengthCm} onChange={(value) => setVariantForm({ ...variantForm, lengthCm: value })} /><NumberTextField label="Rộng kiện (cm)" min="1" value={variantForm.widthCm} onChange={(value) => setVariantForm({ ...variantForm, widthCm: value })} /><NumberTextField label="Cao kiện (cm)" min="1" value={variantForm.heightCm} onChange={(value) => setVariantForm({ ...variantForm, heightCm: value })} /><NumberTextField label="Thứ tự" min="0" required value={variantForm.sortOrder} onChange={(value) => setVariantForm({ ...variantForm, sortOrder: value })} />{!variantForm.id ? <NumberTextField label="Tồn đầu kỳ" min="0" required value={variantForm.initialOnHandQuantity} onChange={(value) => setVariantForm({ ...variantForm, initialOnHandQuantity: value })} /> : null}</div>
          <div><p className="mb-3 text-sm font-black">Thuộc tính biến thể</p><AdminAttributeValueFields appliesTo="VARIANT" attributes={attributes} mappings={mappings} onChange={setVariantAttributes} values={variantAttributes} /></div>
          <button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50" disabled={busy} type="submit">{busy ? "Đang lưu..." : variantForm.id ? "Lưu biến thể" : "Thêm biến thể"}</button>
        </form>
      </SurfacePanel>
      </div>
    </div>
  );
}

function ImageWorkspace({ product, variants, images, imageForm, setImageForm, busy, onSave, onEdit, onRemove, onReset }: Readonly<{
  product: AdminProduct; variants: ProductVariant[]; images: ProductImage[]; imageForm: ImageDraft;
  setImageForm: (value: ImageDraft) => void; busy: boolean; onSave: (event: FormEvent) => void;
  onEdit: (image: ProductImage) => void; onRemove: (image: ProductImage) => Promise<void>; onReset: () => void;
}>) {
  return <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,.85fr)]">
    <SurfacePanel><div><p className="text-xs font-black tracking-wider text-brand uppercase">Bước 4 / 5</p><h3 className="mt-1 text-xl font-black">Thư viện hình ảnh</h3></div><div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">{images.map((image) => <div className="overflow-hidden rounded-2xl border border-slate-200" key={image.id}><div className="relative aspect-square bg-slate-100"><Image alt={image.altText ?? product.name} className="object-cover" fill sizes="(max-width: 640px) 50vw, 220px" src={image.imageUrl} unoptimized />{image.primary ? <span className="absolute top-2 left-2 rounded-full bg-emerald-950 px-2.5 py-1 text-[11px] font-black text-white">Ảnh chính</span> : null}</div><div className="flex items-center justify-between gap-2 p-2"><span className="truncate text-xs font-semibold text-slate-700">{image.variantId ? "Theo biến thể" : "Toàn sản phẩm"}</span><div className="flex"><button aria-label="Sửa ảnh" className="p-1.5" onClick={() => onEdit(image)} type="button"><Pencil className="size-3.5" /></button><button aria-label="Xóa ảnh" className="p-1.5 text-red-700" onClick={() => void onRemove(image)} type="button"><Trash2 className="size-3.5" /></button></div></div></div>)}</div>{!images.length ? <div className="mt-5 rounded-2xl border border-dashed border-slate-300 p-10 text-center"><Images className="mx-auto size-8 text-slate-300" /><p className="mt-3 font-bold text-slate-700">Chưa có hình ảnh</p><p className="mt-1 text-sm text-slate-500">Thêm ảnh sản phẩm hoặc ảnh riêng cho từng biến thể.</p></div> : null}</SurfacePanel>
    <SurfacePanel className="xl:sticky xl:top-24"><div className="flex items-center justify-between"><div><p className="text-xs font-black tracking-wider text-brand uppercase">{imageForm.id ? "Chỉnh sửa hình ảnh" : "Thêm hình ảnh"}</p><h3 className="mt-1 text-xl font-black">Ảnh sản phẩm</h3><p className="mt-1 text-sm text-slate-500">Để xuất bản, cần ít nhất một ảnh phạm vi Toàn sản phẩm được đặt làm ảnh chính.</p></div>{imageForm.id ? <button className="text-sm font-bold text-slate-500" onClick={onReset} type="button">Hủy sửa</button> : <ImagePlus className="size-5 text-brand" />}</div><form className="mt-5 space-y-4" onSubmit={onSave}><label><Label>URL ảnh</Label><input className={fieldClass} onChange={(event) => setImageForm({ ...imageForm, imageUrl: event.target.value })} placeholder="https://..." required type="url" value={imageForm.imageUrl} /></label><label><Label>Phạm vi hình ảnh</Label><select className={fieldClass} disabled={Boolean(imageForm.id)} onChange={(event) => setImageForm({ ...imageForm, variantId: event.target.value })} value={imageForm.variantId}><option value="">Toàn sản phẩm</option>{variants.map((variant) => <option key={variant.id} value={variant.id}>{variant.sku}</option>)}</select>{imageForm.id ? <span className="mt-1.5 block text-xs text-slate-500">Không thể đổi phạm vi của ảnh đã tạo. Hãy xóa và thêm ảnh mới nếu cần.</span> : <span className="mt-1.5 block text-xs text-slate-500">Ảnh đầu tiên của phạm vi được tự động chọn làm ảnh chính.</span>}</label><TextField label="Alt text" value={imageForm.altText} onChange={(value) => setImageForm({ ...imageForm, altText: value })} /><NumberTextField label="Thứ tự hiển thị" min="0" required value={imageForm.sortOrder} onChange={(value) => setImageForm({ ...imageForm, sortOrder: value })} /><label className="flex items-center gap-2 rounded-xl bg-slate-50 p-3 text-sm font-semibold"><input checked={imageForm.primary} onChange={(event) => setImageForm({ ...imageForm, primary: event.target.checked })} type="checkbox" /> Đặt làm ảnh chính</label><button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white" disabled={busy} type="submit">{busy ? "Đang lưu..." : imageForm.id ? "Lưu hình ảnh" : "Thêm hình ảnh"}</button></form></SurfacePanel>
  </div>;
}

function Summary({ label, value }: Readonly<{ label: string; value: string }>) {
  return <div className="rounded-xl bg-slate-50 p-4"><dt className="text-xs font-bold tracking-wide text-slate-500 uppercase">{label}</dt><dd className="mt-1 font-black text-slate-950">{value}</dd></div>;
}

function ErrorBanner({ message }: Readonly<{ message: string }>) {
  return <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-800" role="alert">{message}</p>;
}

function FieldError({ message }: Readonly<{ message?: string }>) {
  return message ? <span className="mt-1.5 block text-xs font-semibold text-red-700">{message}</span> : null;
}

function TextField({ label, value, onChange, ...props }: Readonly<{ label: string; value: string; onChange: (value: string) => void } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange">>) {
  return <label><Label>{label}</Label><input {...props} className={fieldClass} onChange={(event) => onChange(event.target.value)} value={value} /></label>;
}

function NumberTextField(props: Readonly<{ label: string; value: string; onChange: (value: string) => void } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type">>) {
  return <TextField {...props} type="number" />;
}
