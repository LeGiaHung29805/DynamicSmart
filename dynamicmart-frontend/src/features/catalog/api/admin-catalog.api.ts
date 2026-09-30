import { apiClient } from "@/lib/api/client";
import type {
  AdminInventoryItem,
  AdminProduct,
  AttributeAdmin,
  AttributeAppliesTo,
  AttributeDataType,
  AttributeOptionAdmin,
  CategoryAdmin,
  CategoryAttributeAdmin,
  InventoryAdjustment,
  PageResponse,
  ProductImage,
  ProductStatus,
  ProductVariant,
  VariantStatus,
} from "../types";

export interface AttributeValueInput {
  attributeId: string;
  value: unknown;
}

export interface CategoryInput {
  parentId?: string | null;
  code: string;
  name: string;
  slug: string;
  description?: string | null;
  sortOrder: number;
}

export interface AttributeInput {
  code: string;
  name: string;
  dataType: AttributeDataType;
  validationConfig?: unknown;
}

export interface AttributeOptionInput {
  code: string;
  label: string;
  sortOrder: number;
}

export interface CategoryAttributeInput {
  attributeId: string;
  appliesTo: AttributeAppliesTo;
  required: boolean;
  filterable: boolean;
  sortOrder: number;
}

export interface ProductInput {
  categoryId: string;
  name: string;
  slug: string;
  shortDescription?: string | null;
  description?: string | null;
  defaultWeightGrams?: number | null;
  defaultLengthCm?: number | null;
  defaultWidthCm?: number | null;
  defaultHeightCm?: number | null;
  featured: boolean;
  attributes: AttributeValueInput[];
}

export interface CreateVariantInput {
  sku: string;
  name?: string | null;
  priceVnd: number;
  weightGrams: number;
  lengthCm?: number | null;
  widthCm?: number | null;
  heightCm?: number | null;
  sortOrder: number;
  initialOnHandQuantity: number;
  attributes: AttributeValueInput[];
}

export type UpdateVariantInput = Omit<CreateVariantInput, "initialOnHandQuantity">;

export interface ProductImageInput {
  variantId?: string | null;
  imageUrl: string;
  contentType: string;
  sizeBytes: number;
  altText?: string | null;
  sortOrder: number;
  primary: boolean;
}

export function listAdminCategories() {
  return apiClient.get<CategoryAdmin[]>("/api/v1/catalog/admin/categories");
}

export function createCategory(input: CategoryInput) {
  return apiClient.post<CategoryAdmin>("/api/v1/catalog/admin/categories", input);
}

export function updateCategory(categoryId: string, input: CategoryInput) {
  return apiClient.put<CategoryAdmin>(`/api/v1/catalog/admin/categories/${categoryId}`, input);
}

export function setCategoryActive(categoryId: string, active: boolean) {
  return apiClient.patch<CategoryAdmin>(`/api/v1/catalog/admin/categories/${categoryId}/status`, { active });
}

export function listAdminAttributes() {
  return apiClient.get<AttributeAdmin[]>("/api/v1/catalog/admin/attributes");
}

export function createAttribute(input: AttributeInput) {
  return apiClient.post<AttributeAdmin>("/api/v1/catalog/admin/attributes", input);
}

export function updateAttribute(attributeId: string, input: AttributeInput) {
  return apiClient.put<AttributeAdmin>(`/api/v1/catalog/admin/attributes/${attributeId}`, input);
}

export function setAttributeActive(attributeId: string, active: boolean) {
  return apiClient.patch<AttributeAdmin>(`/api/v1/catalog/admin/attributes/${attributeId}/status`, { active });
}

export function createAttributeOption(attributeId: string, input: AttributeOptionInput) {
  return apiClient.post<AttributeOptionAdmin>(`/api/v1/catalog/admin/attributes/${attributeId}/options`, input);
}

export function updateAttributeOption(attributeId: string, optionId: string, input: AttributeOptionInput) {
  return apiClient.put<AttributeOptionAdmin>(`/api/v1/catalog/admin/attributes/${attributeId}/options/${optionId}`, input);
}

export function setAttributeOptionActive(attributeId: string, optionId: string, active: boolean) {
  return apiClient.patch<AttributeOptionAdmin>(
    `/api/v1/catalog/admin/attributes/${attributeId}/options/${optionId}/status`,
    { active },
  );
}

export function listCategoryAttributes(categoryId: string) {
  return apiClient.get<CategoryAttributeAdmin[]>(`/api/v1/catalog/admin/categories/${categoryId}/attributes`);
}

export function mapCategoryAttribute(categoryId: string, input: CategoryAttributeInput) {
  return apiClient.put<CategoryAttributeAdmin>(`/api/v1/catalog/admin/categories/${categoryId}/attributes`, input);
}

export function unmapCategoryAttribute(categoryId: string, mappingId: string) {
  return apiClient.delete<void>(`/api/v1/catalog/admin/categories/${categoryId}/attributes/${mappingId}`);
}

export function listAdminProducts(query: {
  keyword?: string;
  status?: ProductStatus;
  page?: number;
  size?: number;
} = {}) {
  const params = new URLSearchParams();
  if (query.keyword) params.set("keyword", query.keyword);
  if (query.status) params.set("status", query.status);
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 50));
  return apiClient.get<PageResponse<AdminProduct>>(`/api/v1/catalog/admin/products?${params.toString()}`);
}

export function createProduct(input: ProductInput) {
  return apiClient.post<AdminProduct>("/api/v1/catalog/admin/products", input);
}

export function updateProduct(productId: string, input: ProductInput) {
  return apiClient.put<AdminProduct>(`/api/v1/catalog/admin/products/${productId}`, input);
}

export function setProductStatus(productId: string, status: ProductStatus, publishedAt?: string | null) {
  return apiClient.patch<AdminProduct>(`/api/v1/catalog/admin/products/${productId}/status`, {
    status,
    publishedAt: publishedAt || null,
  });
}

export function createVariant(productId: string, input: CreateVariantInput) {
  return apiClient.post<ProductVariant>(`/api/v1/catalog/admin/products/${productId}/variants`, input);
}

export function updateVariant(productId: string, variantId: string, input: UpdateVariantInput) {
  return apiClient.put<ProductVariant>(`/api/v1/catalog/admin/products/${productId}/variants/${variantId}`, input);
}

export function setVariantStatus(productId: string, variantId: string, status: VariantStatus) {
  return apiClient.patch<ProductVariant>(`/api/v1/catalog/admin/products/${productId}/variants/${variantId}/status`, { status });
}

export function createProductImage(productId: string, input: ProductImageInput) {
  return apiClient.post<ProductImage>(`/api/v1/catalog/admin/products/${productId}/images`, input);
}

export function updateProductImage(productId: string, imageId: string, input: ProductImageInput) {
  return apiClient.put<ProductImage>(`/api/v1/catalog/admin/products/${productId}/images/${imageId}`, input);
}

export function deleteProductImage(productId: string, imageId: string) {
  return apiClient.delete<void>(`/api/v1/catalog/admin/products/${productId}/images/${imageId}`);
}

export function listInventory(page = 0, size = 50) {
  return apiClient.get<PageResponse<AdminInventoryItem>>(`/api/v1/catalog/admin/inventory?page=${page}&size=${size}`);
}

export function adjustInventory(variantId: string, quantityDelta: number, reason: string) {
  return apiClient.post<InventoryAdjustment>(
    `/api/v1/catalog/admin/inventory/${variantId}/adjustments`,
    { quantityDelta, reason },
    { headers: { "Idempotency-Key": crypto.randomUUID() } },
  );
}

export function listInventoryAdjustments(variantId: string, page = 0, size = 20) {
  return apiClient.get<PageResponse<InventoryAdjustment>>(
    `/api/v1/catalog/admin/inventory/${variantId}/adjustments?page=${page}&size=${size}`,
  );
}
